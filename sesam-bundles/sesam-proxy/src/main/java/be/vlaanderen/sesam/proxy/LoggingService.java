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

import be.vlaanderen.sesam.proxy.http.Conversation;

/**
 * Service to log communication events.
 * <p>
 * not intended to be used for other application events. (use generic logging)
 * 
 * @author Kristof Heirwegh
 */
public interface LoggingService {

	void logException(Conversation c, String message);

	/**
	 * Call onFinish once you have handled the request (but not before! onFinish must be the last action)
	 * <p>
	 * This is more of a notify than a handle, you can't interact or influence the exception handling
	 */
	void logConversationFinished(final Conversation c);
}