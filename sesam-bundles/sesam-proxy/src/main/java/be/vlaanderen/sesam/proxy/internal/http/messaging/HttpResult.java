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

import org.jboss.netty.handler.codec.http.HttpResponse;

import be.vlaanderen.sesam.proxy.actors.Actor;

/**
 * Request or response will only be set if actor "canChangeData" is set.
 * <p>
 * You must use "synchronised" on request/response if you want to change them!!
 * 
 * @author Kristof Heirwegh
 * 
 */
public class HttpResult {

	private final long conversationId;

	private boolean changedData;

	private boolean success = true;

	private String reason;

	private HttpResponse errorResponse;

	private Actor messageHandler;

	public HttpResult(long conversationId, Actor messageHandler) {
		this.conversationId = conversationId;
		this.messageHandler = messageHandler;
	}

	public boolean isChangedData() {
		return changedData;
	}

	public void setChangedData(boolean changedData) {
		this.changedData = changedData;
	}

	public boolean isSuccess() {
		return success;
	}

	/**
	 * If you set success to false you should also set the reason. (and optionally response default == 500)
	 * <p>
	 * You can use <code>HttpResponses</code> to get a readymade response.
	 * 
	 * @param success
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Actor getMessageHandler() {
		return messageHandler;
	}

	public long getConversationId() {
		return conversationId;
	}

	/**
	 * Only required if success == false;
	 * 
	 * @return Reason of failure
	 */
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public HttpResponse getErrorResponse() {
		return errorResponse;
	}

	public void setErrorResponse(HttpResponse errorResponse) {
		this.errorResponse = errorResponse;
	}
}
