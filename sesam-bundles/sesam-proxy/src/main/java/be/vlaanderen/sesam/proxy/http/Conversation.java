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

package be.vlaanderen.sesam.proxy.http;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.sesam.config.Rule;
import be.vlaanderen.sesam.proxy.actors.Actor;

public class Conversation {

	private static final Logger log = LoggerFactory.getLogger(Conversation.class);

	private static final AtomicLong CONVERSATIONCOUNT = new AtomicLong(0);

	private final Long conversationId = CONVERSATIONCOUNT.incrementAndGet();

	private final long startNanoTime = System.nanoTime();

	private final Map<Actor, Object> actorData = new HashMap<Actor, Object>();

	private HttpIOHandler handler;

	private boolean finished;

	private HttpRequest request;

	private HttpResponse response;

	private long responseSize;

	private long duration; // ms

	private Rule rule;

	public Conversation(HttpIOHandler handler, HttpRequest request) {
		this.handler = handler;
		this.request = request;
		log.debug("New conversation started: " + conversationId);
	}

	public long getStartNanoTime() {
		return startNanoTime;
	}

	public HttpRequest getRequest() {
		return request;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
	}

	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public Long getConversationId() {
		return conversationId;
	}

	public HttpIOHandler getHandler() {
		return handler;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * This is the size of the content (eg. excluding headers).
	 * 
	 * @return
	 */
	public long getResponseSize() {
		return responseSize;
	}

	public void setResponseSize(long responseSize) {
		this.responseSize = responseSize;
	}

	public void responseSizeAdd(long size) {
		this.responseSize += size;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * Keep some custom data to be used in a further stage of the conversation
	 * 
	 * @param actor
	 * @param data
	 */
	public void setActorData(Actor actor, Object data) {
		actorData.put(actor, data);
	}

	public Object getActorData(Actor actor) {
		return actorData.get(actor);
	}
}
