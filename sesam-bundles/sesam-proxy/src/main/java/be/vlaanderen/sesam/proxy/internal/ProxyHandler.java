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

package be.vlaanderen.sesam.proxy.internal;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for mapping inbound external request with outbound internal request, delegated to logic pipeline from
 * rule.
 * 
 * <pre>
 *     _       _
 * ext  \     /  int
 *     \ \   / /
 *      \ \ / /
 *         O --> logic dependent on rules
 *        | |
 *        | |
 *       logic
 * </pre>
 * 
 * @author Kristof Heirwegh
 * 
 */
public class ProxyHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);

	// This lock guards against the race condition that overrides the
	// OP_READ flag incorrectly.
	// See the related discussion: http://markmail.org/message/x7jc6mqx6ripynqf
	final Object trafficLock = new Object();

	private ClientSocketChannelFactory clientSocketChannelFactory;

	private volatile Channel outboundChannel;

	public ProxyHandler() {
	}

	public ProxyHandler(ClientSocketChannelFactory clientSocketChannelFactory) {
		this.clientSocketChannelFactory = clientSocketChannelFactory;
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		log.debug("Channel Open");

		// Move to later

		// Suspend incoming traffic until connected to the remote host.
		final Channel inboundChannel = e.getChannel();
		inboundChannel.setReadable(false);

		// Start the connection attempt.
		ClientBootstrap cb = new ClientBootstrap(clientSocketChannelFactory);
		cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel()));
		ChannelFuture f = cb.connect(new InetSocketAddress("localhost", 5432));

		outboundChannel = f.getChannel();
		f.addListener(new ChannelFutureListener() {

			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					// Connection attempt succeeded:
					// Begin to accept incoming traffic.
					inboundChannel.setReadable(true);
				} else {
					// Close the connection if the connection attempt has failed.
					inboundChannel.close();
				}
			}
		});
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		log.debug("moving data");

		ChannelBuffer msg = (ChannelBuffer) e.getMessage();
		// System.out.println(">>> " + ChannelBuffers.hexDump(msg));
		synchronized (trafficLock) {
			outboundChannel.write(msg);
			// If outboundChannel is saturated, do not read until notified in
			// OutboundHandler.channelInterestChanged().
			if (!outboundChannel.isWritable()) {
				e.getChannel().setReadable(false);
			}
		}
	}

	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// If inboundChannel is not saturated anymore, continue accepting
		// the incoming traffic from the outboundChannel.
		synchronized (trafficLock) {
			if (e.getChannel().isWritable()) {
				outboundChannel.setReadable(true);
			}
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		closeOnFlush(e.getChannel());
	}

	private class OutboundHandler extends SimpleChannelUpstreamHandler {

		private final Channel inboundChannel;

		OutboundHandler(Channel inboundChannel) {
			this.inboundChannel = inboundChannel;
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
			ChannelBuffer msg = (ChannelBuffer) e.getMessage();
			// System.out.println("<<< " + ChannelBuffers.hexDump(msg));
			synchronized (trafficLock) {
				inboundChannel.write(msg);
				// If inboundChannel is saturated, do not read until notified in
				// HexDumpProxyInboundHandler.channelInterestChanged().
				if (!inboundChannel.isWritable()) {
					e.getChannel().setReadable(false);
				}
			}
		}

		@Override
		public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			// If outboundChannel is not saturated anymore, continue accepting
			// the incoming traffic from the inboundChannel.
			synchronized (trafficLock) {
				if (e.getChannel().isWritable()) {
					inboundChannel.setReadable(true);
				}
			}
		}

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			closeOnFlush(inboundChannel);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			e.getCause().printStackTrace();
			closeOnFlush(e.getChannel());
		}
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if (ch.isConnected()) {
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
