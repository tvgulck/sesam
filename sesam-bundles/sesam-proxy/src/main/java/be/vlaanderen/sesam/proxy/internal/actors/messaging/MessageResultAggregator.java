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

package be.vlaanderen.sesam.proxy.internal.actors.messaging;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.sesam.proxy.actors.MessageCallback;

/**
 * Call the given callback when all responses have been collected.
 * 
 * @author Kristof Heirwegh
 */
public class MessageResultAggregator<R> implements MessageCallback<R> {

	private static final Logger log = LoggerFactory.getLogger(MessageResultAggregator.class);

	private final MessageCallback<List<R>> callback;

	private final List<R> results;

	private int messageCount;

	/**
	 * 
	 * @param actors
	 * @param conversation
	 *            the task all actors should handle (1 instance for all, so must be immutable or 'synchronized')
	 * @param result
	 */
	public MessageResultAggregator(int messageCount, MessageCallback<List<R>> callback) {
		this.callback = callback;
		this.messageCount = messageCount;
		this.results = new ArrayList<R>(messageCount);
		if (messageCount == 0)
			callback.execute(results);
	}

	public void execute(R result) {
		synchronized (callback) {
			log.debug("Actor called back");
			results.add(result);
			messageCount--;
			if (messageCount == 0) {
				log.debug("All actors called back");
				callback.execute(results);
			}
		}
	}

}
