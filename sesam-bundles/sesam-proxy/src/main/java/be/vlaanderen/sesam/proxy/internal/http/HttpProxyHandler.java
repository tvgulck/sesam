/*
 * Copyright 2011 Vlaams Gewest
 *
 * This file is part of SESAM, the Service Endpoint Security And Monitoring framework.
 *
 * SESAM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SESAM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SESAM.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.vlaanderen.sesam.proxy.internal.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.sesam.proxy.http.Conversation;
import be.vlaanderen.sesam.proxy.http.HttpIOHandler;

/**
 * Responsible for mapping inbound external request with outbound internal request, delegated to logic pipeline from
 * rule.
 * <p>
 * All IO should pass through here.
 * 
 * <pre>
 *     _       _
 * ext  \     /  int
 *     \ \   / /
 *      \ \ / /
 *         O --> Central (decisionpoint for logic)
 *        | |
 *        | |
 *       logic (actors)
 * </pre>
 * 
 * @author Kristof Heirwegh
 * 
 */
public class HttpProxyHandler extends SimpleChannelUpstreamHandler implements HttpIOHandler {

	private static final Logger log = LoggerFactory.getLogger(HttpProxyHandler.class);

	// This lock guards against the race condition that overrides the
	// OP_READ flag incorrectly.
	// See the related discussion: http://markmail.org/message/x7jc6mqx6ripynqf
	private final Object trafficLock = new Object();

	private final ReentrantLock busyLock = new ReentrantLock();

	private final Condition busyDone = busyLock.newCondition();

	private volatile boolean busy;

	private HttpCentral central;

	private OutboundHandler outboundHandler;

	private Channel inboundChannel; // EXTERNAL

	private volatile Channel outboundChannel; // INTERNAL

	private volatile Conversation conversation;

	private boolean keepAlive;

	private boolean mustSend100Continue;

	private static final long chunkRetrievalTimeout = 60000; // ms

	private LinkedBlockingQueue<HttpChunk> inboundQueue = new LinkedBlockingQueue<HttpChunk>(4);

	public HttpProxyHandler() {
	}

	public HttpProxyHandler(HttpCentral central) {
		this.central = central;
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.debug("Channel Open: " + e.getChannel().getId());
		inboundChannel = e.getChannel();
		outboundHandler = new OutboundHandler(e.getChannel());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		Channel channel = e.getChannel();
		log.debug("received from EXT " + channel.getId());

		if (e.getMessage() instanceof HttpChunk) {
			inboundQueue.put((HttpChunk) e.getMessage());
		} else {
			busyLock.lock();
			try {
				if (busy)
					busyDone.await();
				busy = true;
				if (inboundQueue.size() != 0) {
					inboundQueue.clear();
					log.warn("Not all chunks were read (removing old ones).");
				}
			} finally {
				busyLock.unlock();
			}

			if (inboundChannel.isConnected()) {
				handleMessageEvent((HttpRequest) e.getMessage());
			} else {
				log.info("Connection failed while waiting, request will be thrown away silently.");
			}
		}
	}

	public HttpChunk getInboundChunk() {
		log.debug("inbound chunk requested");

		try {
			HttpChunk e = inboundQueue.poll(chunkRetrievalTimeout, TimeUnit.MILLISECONDS);
			if (e != null) {
				return (e);
			} else {
				if (conversation != null) {
					central.handleException("Timeout retrieving chunk", conversation);
				} else {
					log.info("Data receive timeout, closing channel");
					closeOnFlush(inboundChannel);
				}
			}
		} catch (InterruptedException ex) {
			if (conversation != null) {
				central.handleException("Timeout retrieving chunk", conversation);
			} else {
				log.info("Interrupted while waiting for Chunk");
				closeOnFlush(inboundChannel);
			}
		}
		return null;
	}

	// - -TODO not used
	// public HttpChunk getOutboundChunk() {
	// if (outboundHandler != null) {
	// log.debug("outbound chunk requested: " + outboundChannel.isReadable());
	// return outboundHandler.getChunk();
	// }
	// return null;
	// }

	// ---------------------------------------------------------

	private void handleMessageEvent(HttpRequest r) {
		log.debug("Handling a new Request");
		if (conversation == null) {
			HttpRequest req = (HttpRequest) r;
			conversation = new Conversation(this, req);
			setKeepAlive(HttpHeaders.isKeepAlive(req));
			setMustSend100Continue(is100ContinueExpected(req));

			central.handleIncomingRequest(conversation);
		} else {
			log.warn("Illegal state, conversation is not set!");
		}
	}

	public void finishedMessageEvent() {
		log.debug("Finished a MessageEvent");

		busyLock.lock();
		try {
			busy = false;
			busyDone.signal();
		} finally {
			busyLock.unlock();
		}
	}

	public void sendResponse(Conversation conversation) {
		synchronized (trafficLock) {
			log.debug("SendResponse");
			HttpResponse hr = conversation.getResponse();
			if (isKeepAlive() && !hr.isChunked() && HttpHeaders.getContentLength(hr) == 0)
				hr.setHeader(HttpHeaders.Names.CONTENT_LENGTH, hr.getContent().readableBytes());

			// -- netty chunks when receiving, irrespective of chunked state of response sent by server.
			// -- so we need to set the header if response does not yet contain it.
			if (hr.isChunked()) {
				hr.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
				hr.removeHeader(HttpHeaders.Names.CONTENT_LENGTH);
			}

			conversation.responseSizeAdd(hr.getContent().readableBytes()); // uncompressed
			inboundChannel.write(hr);
		}
	}

	public void sendResponseChunk(HttpChunk chunk) {
		synchronized (trafficLock) {
			log.debug("SendResponseChunk - size: " + chunk.getContent().readableBytes());
			conversation.responseSizeAdd(chunk.getContent().readableBytes());
			inboundChannel.write(chunk);
		}
	}

	public void sendRequest(Conversation conversation) {
		synchronized (trafficLock) {
			log.debug("SendRequest");
			if (outboundChannel != null) {
				outboundChannel.write(conversation.getRequest());
				if (isMustSend100Continue()) {
					send100Continue(inboundChannel);
					setMustSend100Continue(false);
				}
			} else {
				central.handleException("Connection with server lost", conversation, HttpResponses.httpResponse502());
			}
		}
	}

	public void sendRequestChunk(HttpChunk chunk) {
		synchronized (trafficLock) {
			log.debug("SendRequestChunk");
			if (outboundChannel != null) {
				outboundChannel.write(chunk);
			} else {
				central.handleException("No connection with server!", conversation, HttpResponses.httpResponse502());
			}
		}
	}

	// ---------------------------------------------------------

	/**
	 * Received when the connection with the internal Server is made
	 */
	public void setOutboundChannel(Conversation conversation, Channel outboundChannel) {
		this.outboundChannel = outboundChannel;
	}

	public void finishedConversation(Conversation conversation, boolean forceClose) {
		log.debug("Finished conversation on channel: " + inboundChannel.getId());
		if (!isKeepAlive() || forceClose) {
			closeOnFlush(inboundChannel);
			if (outboundChannel != null) {
				closeOnFlush(outboundChannel);
			}
		}
		this.conversation = null;
	}

	// ---------------------------------------------------------

	// ---------------------------------------------------------

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.debug("EXT channel closed. " + e.getChannel().getId());

		busyLock.lock();
		try {
			if (busy) {
				busyDone.signal();
			}
		} finally {
			busyLock.unlock();
		}

		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		if (conversation != null) {
			central.handleException(e.getCause().getMessage(), conversation, HttpResponses.httpResponse500());
		} else {
			log.info("Communication exception outside of conversation: " + e.getCause().getMessage());
			if (e.getChannel().isConnected()) {
				e.getChannel().write(HttpResponses.httpResponse500());
			}
		}
		closeOnFlush(e.getChannel());
	}

	// ---------------------------------------------------------

	// TODO actors cannot be async yet.
	public class OutboundHandler extends SimpleChannelUpstreamHandler {

		private final Channel inboundChannel;

		// - -TODO not used
		private LinkedBlockingQueue<HttpChunk> chunks = new java.util.concurrent.LinkedBlockingQueue<HttpChunk>(2);

		public OutboundHandler(Channel inboundChannel) {
			this.inboundChannel = inboundChannel;
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			log.debug("received a response from INT for: " + inboundChannel.getId());

			if (conversation != null) {
				if (e.getChannel().getId() == outboundChannel.getId()) {
					if (e.getMessage() instanceof HttpResponse) {
						HttpResponse response = (HttpResponse) e.getMessage();
						conversation.setResponse(response);
						central.handleOutgoingResponse(conversation);

					} else {
						HttpChunk chunk = (HttpChunk) e.getMessage();
						central.handleOutgoingChunk(conversation, chunk);
					}
				} else {
					log.warn("OutboundHandler: Channels don't match " + e.getChannel().getId() + " - "
							+ outboundChannel.getId());
				}
			} else {
				log.warn("Received INT message for nonexistent conversation: " + inboundChannel.getId());
			}
		}

		// - -TODO not used
		// public HttpChunk getChunk() {
		// try {
		// HttpChunk c = chunks.take();
		// if (c != null) {
		// return c;
		// } else {
		// central.handleException("Was expecting a Chunk (outbound)", conversation);
		// }
		// } catch (InterruptedException e1) {
		// central.handleException("Interrupted while waiting for Chunk", conversation);
		// }
		// return null;
		// }

		// @Override
		// public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// log.debug("ChannelInterest changed INT");
		// synchronized (trafficLock) {
		// if (e.getChannel().isWritable()) {
		// inboundChannel.setReadable(true);
		// }
		// }
		// }

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			log.debug("INT channel closed. " + e.getChannel().getId());
			outboundChannel = null;
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			if (conversation != null) {
				central.handleException(e.getCause().getMessage(), conversation, HttpResponses.httpResponse500());
			}
			closeOnFlush(e.getChannel());
		}
	}

	// ---------------------------------------------------------

	private void send100Continue(Channel c) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
		c.write(response);
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		log.debug("Closing channel " + ch.getId());
		if (ch.isConnected()) {
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}

	// ---------------------------------------------------------

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public boolean isMustSend100Continue() {
		return mustSend100Continue;
	}

	public void setMustSend100Continue(boolean mustSend100Continue) {
		this.mustSend100Continue = mustSend100Continue;
	}

	/**
	 * Only used for wiring outbound pipeline together. You should not need this.
	 * 
	 * @return
	 */
	public OutboundHandler getOutboundHandler() {
		return outboundHandler;
	}

	public boolean isConnected() {
		return (outboundChannel != null && outboundChannel.isConnected());
	}

	public Channel getInboundChannel() {
		return inboundChannel;
	}

	public Channel getOutboundChannel() {
		return outboundChannel;
	}
}
