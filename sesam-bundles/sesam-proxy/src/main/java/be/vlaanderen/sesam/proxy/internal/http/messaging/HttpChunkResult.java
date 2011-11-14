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

package be.vlaanderen.sesam.proxy.internal.http.messaging;

import org.jboss.netty.handler.codec.http.HttpChunk;

import be.vlaanderen.sesam.proxy.actors.Actor;

/**
 * @author Kristof Heirwegh
 * 
 */
public class HttpChunkResult extends HttpResult {

	public HttpChunkResult(long conversationId, Actor messageHandler) {
		super(conversationId, messageHandler);
	}

	private HttpChunk chunk;

	/**
	 * You must use "synchronised" if you want to change this shared clone
	 * 
	 * @return
	 */
	public HttpChunk getChunk() {
		return chunk;
	}

	public void setChunk(HttpChunk chunk) {
		this.chunk = chunk;
	}
}
