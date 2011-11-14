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

import be.vlaanderen.sesam.proxy.actors.Actor;
import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;

/**
 * Called before and after a request has been handled. These are meant for passive actions (for instance logging) active
 * actions can be done in the in/outbound handlers.
 * 
 * @author Kristof Heirwegh
 * 
 */
public interface HttpStatusActor extends Actor {

	/**
	 * Called when an exception has occured. You cannon influence the result anymore.
	 * <p>
	 * Call onFinish once you have handled the request (but not before! onFinish must be the last action)
	 */
	void handleException(final Conversation c, MessageCallback<Void> onFinish);

	/**
	 * Called after conversation is finished. You cannot influence the result anymore (everything has been sent already)
	 * <p>
	 * Call onFinish once you have handled the request (but not before! onFinish must be the last action)
	 */
	void handleConversationFinished(final Conversation c, MessageCallback<Void> onFinish);
}