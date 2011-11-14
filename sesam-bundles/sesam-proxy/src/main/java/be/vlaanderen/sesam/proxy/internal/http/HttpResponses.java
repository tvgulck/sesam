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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

/**
 * Readymade responses to return to EXT.
 * 
 * @author Kristof Heirwegh
 */
public class HttpResponses {

	/**
	 * Singleton
	 */
	private HttpResponses() {
	}

	/**
	 * 404 - De gevraagde pagina werd niet gevonden.
	 * 
	 * @return
	 */
	public static HttpResponse httpResponse404() {
		String message = "404 - De gevraagde pagina werd niet gevonden.";
		ChannelBuffer cb = ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8);
		HttpResponse r404 = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
		r404.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		r404.setContent(cb);
		return r404;
	}

	/**
	 * 500 - Interne serverfout.
	 * 
	 * @return
	 */
	public static HttpResponse httpResponse500() {
		String message = "500 - Interne serverfout.";
		ChannelBuffer cb = ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8);
		HttpResponse r500 = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		r500.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		r500.setContent(cb);
		return r500;
	}

	/**
	 * 502 - Kon geen verbinding maken met interne server
	 * 
	 * @return
	 */
	public static HttpResponse httpResponse502() {
		String message = "502 - Kon geen verbinding maken met interne server.";
		ChannelBuffer cb = ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8);
		HttpResponse r502 = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
		r502.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		r502.setContent(cb);
		return r502;
	}

	/**
	 * 504 - Aanvraag werd niet tijdig uitgevoerd.
	 * 
	 * @return
	 */
	public static HttpResponse httpResponse504() {
		String message = "504 - Aanvraag werd niet tijdig uitgevoerd.";
		ChannelBuffer cb = ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8);
		HttpResponse r504 = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.GATEWAY_TIMEOUT);
		r504.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		r504.setContent(cb);
		return r504;
	}
}
