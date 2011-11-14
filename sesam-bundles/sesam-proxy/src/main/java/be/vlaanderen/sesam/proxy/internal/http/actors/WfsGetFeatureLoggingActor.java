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

package be.vlaanderen.sesam.proxy.internal.http.actors;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;

import akka.actor.TypedActor;
import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;
import be.vlaanderen.sesam.proxy.http.actors.HttpInboundActor;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpChunkResult;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpResult;

/**
 * Logs WfsRequests using Logback
 * 
 * @author Kristof Heirwegh
 * 
 */
public class WfsGetFeatureLoggingActor extends TypedActor implements HttpInboundActor, BeanNameAware {

	private static final Logger log = LoggerFactory.getLogger(WfsGetFeatureLoggingActor.class);

	private static final Logger commlog = LoggerFactory.getLogger("wfsRequest.log");

	private static final SimpleDateFormat DATEFORMATTER = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]");

	private String name;

	public void setBeanName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Boolean canChangeData() {
		return false;
	}

	public Boolean isHeadersOnly() {
		return false;
	}

	// ---------------------------------------------------------

	public void handleInboundRequest(Conversation c, HttpResult result, MessageCallback<HttpResult> onFinish) {
		log.debug("Checking Request");
		try {
			if (HttpMethod.GET.equals(c.getRequest().getMethod())) {
				log.debug("GET getFeatureRequest");
				QueryStringDecoder qsd = new QueryStringDecoder(c.getRequest().getUri());
				if (containsKeyVal(qsd, "service", "wfs") && containsKeyVal(qsd, "request", "getfeature")) {
					// TODO parse parameters
					logRequest(c, c.getRequest().getUri());
				} else {
					log.debug("Not a GetFeature request.");
				}
			} else if (HttpMethod.POST.equals(c.getRequest().getMethod())) {
				if (!c.getRequest().isChunked()) {
					String data = bufferToString(c, c.getRequest().getContent());
					handlePostData(c, data);
				}

			} else {
				// not supported
				log.debug("Unsupported request type.");
			}
		} catch (Exception e) {
			log.warn("Failed handling request: " + e.getMessage());
		} finally {
			onFinish.execute(result);
		}
	}

	public void handleInboundChunk(HttpChunk chunk, Conversation c, HttpResult result,
			MessageCallback<HttpChunkResult> onFinish) {
		log.debug("Checking Chunk");
		try {
			StringBuilder sb;
			if (c.getActorData(this) != null)
				sb = (StringBuilder) c.getActorData(this);
			else
				sb = new StringBuilder();

			sb.append(bufferToString(c, chunk.getContent()));

			if (chunk.isLast()) {
				handlePostData(c, sb.toString());
			} else {
				c.setActorData(this, sb);
			}
		} catch (Exception e) {
			log.warn("Failed handling chunk: " + e.getMessage());
		} finally {
			onFinish.execute(null);
		}
	}

	// ---------------------------------------------------------

	private String bufferToString(Conversation c, ChannelBuffer cb) {
		if (cb.readable()) {
			Charset cs;
			String contentType = c.getRequest().getHeader(HttpHeaders.Names.CONTENT_TYPE);
			if (contentType != null && contentType.contains("UTF-8"))
				cs = CharsetUtil.UTF_8;
			else
				cs = CharsetUtil.ISO_8859_1;
			return c.getRequest().getContent().toString(cs);
		} else {
			return "";
		}
	}

	// TODO might want to parse this in a decent way, but that will be multiple times slower than this.
	private void handlePostData(Conversation c, String data) {
		String res = null;
		int pos = data.indexOf("<wfs:GetFeature");
		if (pos > -1) {
			data = data.replaceAll("\\n", "");
			int start = data.indexOf("<wfs:Query", pos);
			int end = data.indexOf("</wfs:Query>", start);
			if (end > start) {
				logRequest(c, data.substring(start, end + 11));
			} else {
				logRequest(c, "Invalid wfs:query node in request. (" + res + ")");
			}

		} else {
			log.debug("Not a GetFeature request.");
		}
	}

	private boolean containsKeyVal(QueryStringDecoder qsd, String key, String value) {
		if (value == null || key == null)
			return false;

		if (qsd.getParameters().containsKey(key)) {
			for (String s : qsd.getParameters().get(key)) {
				if (value.equalsIgnoreCase(s))
					return true;
			}
		}
		return false;
	}

	private void logRequest(Conversation c, String request) {
		InetSocketAddress addr = (InetSocketAddress) c.getHandler().getInboundChannel().getRemoteAddress();

		StringBuilder sb = new StringBuilder();
		sb.append(DATEFORMATTER.format(new Date()));
		sb.append(" ");
		sb.append(addr.getAddress().getHostAddress());
		sb.append(" ");
		sb.append(c.getRequest().getMethod().getName());
		sb.append(" ");
		sb.append(request);

		commlog.info(sb.toString());
	}
}
