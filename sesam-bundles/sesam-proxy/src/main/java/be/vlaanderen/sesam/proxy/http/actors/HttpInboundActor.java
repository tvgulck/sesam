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

package be.vlaanderen.sesam.proxy.http.actors;

import org.jboss.netty.handler.codec.http.HttpChunk;

import be.vlaanderen.sesam.proxy.actors.Actor;
import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpChunkResult;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpResult;

/**
 * Called when request is received from External
 * 
 * @author Kristof Heirwegh
 */
public interface HttpInboundActor extends Actor {

	/**
	 * If the actor can change the data, it must be the single actor that changes data or be wrapped in a SequenceActor
	 * to prevent concurrent editing.
	 * 
	 * @return
	 */
	Boolean canChangeData();

	/**
	 * Chunks will not be passed to this actor if headersonly is true;
	 * 
	 * @return
	 */
	Boolean isHeadersOnly();

	/**
	 * Call onFinish once you have handled the request (but not before! onFinish must be the last action)
	 */
	void handleInboundRequest(final Conversation c, HttpResult result, MessageCallback<HttpResult> onFinish);

	/**
	 * Call onFinish once you have handled the request (but not before! onFinish must be the last action)
	 */
	void handleInboundChunk(HttpChunk chunk, final Conversation c, HttpResult result,
			MessageCallback<HttpChunkResult> onFinish);
}