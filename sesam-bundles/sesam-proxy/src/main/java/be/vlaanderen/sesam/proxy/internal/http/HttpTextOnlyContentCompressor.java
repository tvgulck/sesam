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

import java.util.Queue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.embedder.EncoderEmbedder;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.sesam.proxy.internal.util.jsr166.LinkedTransferQueue;

/**
 * Compress response, but only if its contentype is text.
 * 
 * <p>
 * Caveat: Methods were copied from supertype as we cannot get at the queue containing the accept_encodings, and the
 * method to decide encoding does not have enough information (response).
 * 
 * @author Kristof Heirwegh
 * 
 */
public class HttpTextOnlyContentCompressor extends HttpContentCompressor {

	private static final Logger log = LoggerFactory.getLogger(HttpTextOnlyContentCompressor.class);

	private final Queue<String> acceptEncodingQueue = new LinkedTransferQueue<String>();

	private volatile EncoderEmbedder<ChannelBuffer> encoder;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (!(msg instanceof HttpMessage)) {
			ctx.sendUpstream(e);
			return;
		}

		HttpMessage m = (HttpMessage) msg;
		String acceptedEncoding = m.getHeader(HttpHeaders.Names.ACCEPT_ENCODING);
		if (acceptedEncoding == null) {
			acceptedEncoding = HttpHeaders.Values.IDENTITY;
		}
		boolean offered = acceptEncodingQueue.offer(acceptedEncoding);
		assert offered;

		ctx.sendUpstream(e);
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Object msg = e.getMessage();
		if (msg instanceof HttpResponse && ((HttpResponse) msg).getStatus().getCode() == 100) {
			// 100-continue response must be passed through.
			ctx.sendDownstream(e);
		} else if (msg instanceof HttpMessage) {
			HttpMessage m = (HttpMessage) msg;

			encoder = null;

			// Determine the content encoding.
			String acceptEncoding = acceptEncodingQueue.poll();
			if (acceptEncoding == null) {
				throw new IllegalStateException("cannot send more responses than requests");
			}

			HttpResponse hr = (HttpResponse) e.getMessage();
			String type = hr.getHeader(HttpHeaders.Names.CONTENT_TYPE);
			if (type != null && type.startsWith("text")) {

				boolean hasContent = m.isChunked() || m.getContent().readable();
				if (hasContent && (encoder = newContentEncoder(acceptEncoding)) != null) {
					// Encode the content and remove or replace the existing headers
					// so that the message looks like a decoded message.
					m.setHeader(HttpHeaders.Names.CONTENT_ENCODING, getTargetContentEncoding(acceptEncoding));

					if (!m.isChunked()) {
						ChannelBuffer content = m.getContent();
						// Encode the content.
						content = ChannelBuffers.wrappedBuffer(encode(content), finishEncode());

						// Replace the content.
						m.setContent(content);
						if (m.containsHeader(HttpHeaders.Names.CONTENT_LENGTH)) {
							m.setHeader(HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(content.readableBytes()));
						}
					}
				}
			}

			// Because HttpMessage is a mutable object, we can simply forward the write request.
			ctx.sendDownstream(e);
		} else if (msg instanceof HttpChunk) {
			HttpChunk c = (HttpChunk) msg;
			ChannelBuffer content = c.getContent();

			// Encode the chunk if necessary.
			if (encoder != null) {
				if (!c.isLast()) {
					content = encode(content);
					if (content.readable()) {
						c.setContent(content);
						ctx.sendDownstream(e);
					}
				} else {
					ChannelBuffer lastProduct = finishEncode();

					// Generate an additional chunk if the decoder produced
					// the last product on closure,
					if (lastProduct.readable()) {
						Channels.write(ctx, Channels.succeededFuture(e.getChannel()),
								new DefaultHttpChunk(lastProduct), e.getRemoteAddress());
					}

					// Emit the last chunk.
					ctx.sendDownstream(e);
				}
			} else {
				ctx.sendDownstream(e);
			}
		} else {
			ctx.sendDownstream(e);
		}
	}

	// ---------------------------------------------------------

	private ChannelBuffer encode(ChannelBuffer buf) {
		encoder.offer(buf);
		return ChannelBuffers.wrappedBuffer(encoder.pollAll(new ChannelBuffer[encoder.size()]));
	}

	private ChannelBuffer finishEncode() {
		ChannelBuffer result;
		if (encoder.finish()) {
			result = ChannelBuffers.wrappedBuffer(encoder.pollAll(new ChannelBuffer[encoder.size()]));
		} else {
			result = ChannelBuffers.EMPTY_BUFFER;
		}
		encoder = null;
		return result;
	}
}
