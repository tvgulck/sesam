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

package be.vlaanderen.sesam.proxy;

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpChunk;

import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpChunkResult;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpResult;

/**
 * Service for working with Actors (retrieving, executing).
 * 
 * @author Kristof Heirwegh
 */
public interface ActorService {

	void notifyInboundRequestActors(final Conversation conversation,
			final MessageCallback<List<HttpResult>> resultCallback);

	void notifyInboundChunkActors(final Conversation conversation, final HttpChunk chunk,
			final MessageCallback<List<HttpChunkResult>> resultCallback);

	void notifyOutboundResponseActors(final Conversation conversation,
			final MessageCallback<List<HttpResult>> resultCallback);

	void notifyOutboundChunkActors(final Conversation conversation, final HttpChunk chunk,
			final MessageCallback<List<HttpChunkResult>> resultCallback);

	void notifyStatusExceptionActors(final Conversation conversation, final MessageCallback<List<Void>> callback);

	void notifyStatusFinishedActors(final Conversation conversation, final MessageCallback<List<Void>> callback);
}