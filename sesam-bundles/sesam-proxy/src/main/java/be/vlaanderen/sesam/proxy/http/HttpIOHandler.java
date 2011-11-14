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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;

import be.vlaanderen.sesam.proxy.IOHandler;

public interface HttpIOHandler extends IOHandler {

	/**
	 * To EXT client
	 * 
	 * @param context
	 * @param response
	 */
	void sendResponse(Conversation conversation);

	/**
	 * To EXT client
	 * 
	 * @param context
	 * @param response
	 */
	void sendResponseChunk(HttpChunk chunk);

	/**
	 * To INT server
	 * 
	 * @param context
	 * @param request
	 */
	void sendRequest(Conversation conversation);

	/**
	 * To INT server
	 * 
	 * @param context
	 * @param request
	 */
	void sendRequestChunk(HttpChunk chunk);

	// ---------------------------------------------------------

	/**
	 * Needed to set up the connection with the INT server, do not use directly.
	 * <p>
	 * Use sendRequest(...);
	 * 
	 * @return
	 */
	ChannelUpstreamHandler getOutboundHandler();

	/**
	 * The channel connected to INT server
	 * 
	 * @param outboundchannel
	 */
	void setOutboundChannel(Conversation conversation, Channel outboundchannel);

	/**
	 * Unblock EXT client so it can send again if keepAlive. Close connection otherwise.
	 */
	void finishedConversation(Conversation conversation, boolean forceClose);

	/**
	 * A part of a conversation finished
	 * 
	 * @param conversation
	 */
	void finishedMessageEvent();

	boolean isKeepAlive();

	/**
	 * Is the outboundhandler connected? (eg. is the channel set)
	 * 
	 * @return
	 */
	boolean isConnected();

	/**
	 * Get the chunks belonging to current Request, blocks if not (yet) available
	 * 
	 * @return
	 */
	HttpChunk getInboundChunk();

	Channel getInboundChannel();

	Channel getOutboundChannel();
}
