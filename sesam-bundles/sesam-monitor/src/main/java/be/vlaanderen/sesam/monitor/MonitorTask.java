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

package be.vlaanderen.sesam.monitor;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import be.vlaanderen.sesam.monitor.internal.util.BodyUtils;

/**
 * ...
 * 
 * @author Kristof Heirwegh
 */
public class MonitorTask extends AbstractMonitorTask {

	private static final Logger log = LoggerFactory.getLogger(MonitorTask.class);

	private final AtomicBoolean running = new AtomicBoolean();

	private long start;

	private long timewaited;

	@Autowired
	private ClientConnectionService clientService;

	// ---------------------------------------------------------

	public ClientConnectionService getClientService() {
		return clientService;
	}

	public void setClientService(ClientConnectionService clientService) {
		this.clientService = clientService;
	}

	// ---------------------------------------------------------

	public void run() {
		if (running.get()) {
			// Do not run (the same instance) concurrent (has state).
			log.warn("Task: " + getName() + " is still running! (skipping)");
			return;

		} else {
			running.set(true);
			start = System.currentTimeMillis();
			timewaited = 0;

			try {
				log.debug("Running task: " + getName());

				InetSocketAddress addr = new InetSocketAddress(getRequestHostname(), getRequestPort());
				if (addr.getAddress() == null) {
					logResult(NOTIFICATIONTYPE_EXCEPTION,
							"Kon ip-adres van host niet vinden, foute hostnaam of niet in DNS? (Host: "
									+ getRequestHostname() + ")");
					running.set(false);

				} else {
					OutboundHandler out = new OutboundHandler();
					ChannelFuture cf = clientService.createClientChannel(out, addr, getResponseTimeoutMillis());
					boolean res = cf.awaitUninterruptibly(getResponseTimeoutMillis());
					if (res) {
						timewaited = System.currentTimeMillis() - start;
						if (cf.isSuccess()) {
							out.sendRequest(cf.getChannel());
						} // else is handled by exceptioncaught
					} else {
						logResult(NOTIFICATIONTYPE_TIMEOUT);
						cf.cancel();
						running.set(false);
					}
				}
			} catch (Exception e) {
				logResult(NOTIFICATIONTYPE_EXCEPTION, e.getMessage());
				running.set(false);
			}
		}
	}

	// ---------------------------------------------------------

	private HttpRequest buildRequest() throws Exception {
		HttpMethod method = HttpMethod.valueOf(getRequestMethod());
		HttpRequest request = new DefaultHttpRequest(HttpVersion.valueOf(getRequestHttpVersion()), method,
				getRequestUri());
		for (Entry<String, String> entry : getRequestHeaders().entrySet()) {
			request.addHeader(entry.getKey(), entry.getValue());
		}
		if (request.getHeader(HttpHeaders.Names.HOST) == null) {
			request.addHeader(HttpHeaders.Names.HOST, getRequestHostname() + ":" + getRequestPort());
		}

		return request;
	}

	private class OutboundHandler extends SimpleChannelUpstreamHandler {

		public void sendRequest(final Channel channel) throws Exception {
			HttpRequest request = buildRequest();

			// -- requests with body
			if (HttpMethod.POST.equals(request.getMethod())) {
				ChannelBuffer cb = BodyUtils.fileToBuffer(getRequestBodyFile());
				final long fileLength = cb.readableBytes();
				request.addHeader(HttpHeaders.Names.CONTENT_LENGTH, fileLength);
				request.setContent(cb);
			}

			ChannelFuture writeFuture = channel.write(request);
			boolean res = writeFuture.awaitUninterruptibly(getResponseTimeoutMillis() - timewaited);
			if (!res) {
				running.set(false);
				writeFuture.cancel();
				logResult(NOTIFICATIONTYPE_TIMEOUT);
			}

			// if you want to do it chunked
			// request.addHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
			// if (request.getHeader(HttpHeaders.Names.CONTENT_LENGTH) != null)
			// request.removeHeader(HttpHeaders.Names.CONTENT_LENGTH);

			// ChannelFuture writeFuture;

			// if you want to do it streaming
			// FIXME This doesn't work -- only for serverside ???

			// final RandomAccessFile raf = new RandomAccessFile(getRequestBodyFile(), "r");

			// Write the header.
			// writeFuture = channel.write(request);
			// writeFuture.addListener(new ChannelFutureListener() {
			// public void operationComplete(ChannelFuture future) throws Exception {
			// // write the body
			// log.debug("Writing POST body ({} bytes)", fileLength);
			// channel.write(new ChunkedFile(raf, 0, fileLength, 8192)).addListener(new ChannelFutureListener() {
			// public void operationComplete(ChannelFuture future) throws Exception {
			// log.debug("Post body written");
			// }
			// });
			// }
			// });
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			log.debug("Received response");
			if (running.get()) {
				try {
					timewaited = System.currentTimeMillis() - start;
					if (timewaited > getResponseTimeoutMillis()) {
						logResult(NOTIFICATIONTYPE_TIMEOUT);

					} else {
						if (e.getMessage() instanceof HttpResponse) {
							HttpResponse response = (HttpResponse) e.getMessage();

							if (checkStatusCode(response) && checkHeaders(response) && checkBody(response)) {

								logResult(NOTIFICATIONTYPE_SUCCESS);
							}
						} else {
							log.warn("Was expecting a HttpMessage, not chunk");
						}
					}
					e.getChannel().close();

				} catch (Exception ex) {
					log.warn("Exception while testing response: " + ex.getMessage());
				} finally {
					running.set(false);
				}
			} else {
				log.debug("received a response outside of test");
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			log.debug("Received exception");
			if (running.get()) {
				try {

					if (e.getCause() instanceof ReadTimeoutException) {
						logResult(NOTIFICATIONTYPE_TIMEOUT);
					} else {
						if (e.getCause().getMessage() == null || "".equals(e.getCause().getMessage().trim()))
							logResult(e.getCause().getClass().getSimpleName());
						else {
							logResult(e.getCause().getMessage());
						}
					}
				} finally {
					running.set(false);
				}
			} else {
				// can happen when we cancel connection
				log.debug("received a response outside of test");
			}
		}
	}

	protected void logResult(String type) {
		logResult(type, "");
	}

	protected void logResult(String type, String message) {
		long time = (System.currentTimeMillis() - start);
		logResult(type, time, message);
		log.debug("Finished task in: {} ms. (result: {})", time, type + " " + message);
	}

	// ---------------------------------------------------------

	boolean checkStatusCode(HttpResponse response) {
		HttpResponseStatus status = response.getStatus();
		if (status.getCode() == getResponseStatusCode()) {
			return true;
		} else {
			logResult("" + status.getCode(), status.getReasonPhrase());
			return false;
		}
	}

	boolean checkHeaders(HttpResponse response) {
		for (Entry<String, String> entry : getResponseHeaders().entrySet()) {
			List<String> hdrs = response.getHeaders(entry.getKey());
			if (hdrs.size() == 0) {
				logResult(NOTIFICATIONTYPE_RESPONSEHEADER + entry.getKey(), "Header not found.");
				return false;
			} else {
				boolean found = false;
				for (String value : hdrs) {
					if (value.matches(entry.getValue())) {
						found = true;
						break;
					}
				}
				if (!found) {
					logResult(NOTIFICATIONTYPE_RESPONSEHEADER + entry.getKey(), "Header value doesn't match.");
					return false;
				}
			}
		}
		return true;
	}

	boolean checkBody(HttpResponse response) {
		if (isCheckResponseBody()) {
			ChannelBuffer cb = response.getContent();
			if (cb.equals(ChannelBuffers.EMPTY_BUFFER)) {
				logResult(NOTIFICATIONTYPE_RESPONSEBODY, "Response does not contain a body.");
				return false;
			}

			String res = BodyUtils.getMd5Sum(new ChannelBufferInputStream(cb));
			if (res == null) {
				logResult(NOTIFICATIONTYPE_RESPONSEBODY, "Failed creating MD5 checksum of response.");
				return false;
			} else if (!res.equals(getResponseBodyMd5Sum())) {
				logResult(NOTIFICATIONTYPE_RESPONSEBODY, "MD5 checksum failed (expected: " + getResponseBodyMd5Sum()
						+ " received: " + res + ")");
				return false;
			}
		}
		return true;
	}
}
