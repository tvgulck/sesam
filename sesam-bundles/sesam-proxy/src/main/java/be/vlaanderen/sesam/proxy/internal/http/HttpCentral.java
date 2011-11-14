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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import be.vlaanderen.sesam.config.Rule;
import be.vlaanderen.sesam.proxy.ActorService;
import be.vlaanderen.sesam.proxy.ClientSocketChannelService;
import be.vlaanderen.sesam.proxy.LoggingService;
import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;
import be.vlaanderen.sesam.proxy.http.HttpIOHandler;
import be.vlaanderen.sesam.proxy.internal.Central;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpChunkResult;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpResult;

/**
 * Rulebased decision maker (HTTP).
 * 
 * @author Kristof Heirwegh
 */
@Component
public class HttpCentral implements Central {

	// TODO need to build something to check for timeout, without using a thread per message (so something to loop over
	// tasks) -- supervisor -- spring threaded?

	private static final Logger log = LoggerFactory.getLogger(HttpCentral.class);

	@Autowired
	private ClientSocketChannelService<Conversation> socketService;

	@Autowired
	private LoggingService connLog;

	@Autowired
	private HttpRuleMatcherService ruleMatcher;

	@Autowired
	private ActorService actorService;

	private Executor executorService;

	public HttpCentral() {
		// TODO move to BundleActivator; (might do this as spring bean as well)
		executorService = Executors.newCachedThreadPool();
	}

	public void handleIncomingRequest(final Conversation conversation) {
		log.debug("handleIncoming request: " + conversation.getConversationId());

		try {
			InetSocketAddress localAddr = (InetSocketAddress) conversation.getHandler().getInboundChannel()
					.getLocalAddress();
			InetSocketAddress remoteAddr = (InetSocketAddress) conversation.getHandler().getInboundChannel()
					.getRemoteAddress();

			log.debug("finding rule");
			Rule rule = ruleMatcher.findRule(localAddr, remoteAddr, conversation.getRequest());
			if (rule != null) {
				conversation.setRule(rule);
				actorService.notifyInboundRequestActors(conversation, new MessageCallback<List<HttpResult>>() {

					public void execute(List<HttpResult> results) {
						handleInboundRequestActorResults(conversation, results);
					}
				});
			} else {
				handleException("No rule found for request: " + conversation.getRequest().getMethod().getName() + " - "
						+ conversation.getRequest().getUri(), conversation, HttpResponses.httpResponse404());
			}
		} catch (Exception e) {
			handleException("failed handling incoming request: " + e.getMessage() + " (" + e.getClass().getSimpleName()
					+ ")", conversation, HttpResponses.httpResponse500());
		}
	}

	public void handleOutgoingResponse(final Conversation conversation) {
		log.debug("handleOutgoing response - " + conversation.getConversationId());

		actorService.notifyOutboundResponseActors(conversation, new MessageCallback<List<HttpResult>>() {

			public void execute(List<HttpResult> results) {
				handleOutboundResponseActorResults(conversation, results);
			}
		});
	}

	public void handleOutgoingChunk(final Conversation conversation, final HttpChunk chunk) {
		log.debug("handleOutgoing chunk - " + conversation.getConversationId());

		actorService.notifyOutboundChunkActors(conversation, chunk, new MessageCallback<List<HttpChunkResult>>() {

			public void execute(List<HttpChunkResult> results) {
				handleOutboundChunkActorResults(conversation, chunk, results);
			}
		});
	}

	/**
	 * Just logs the exception (no response is sent).
	 * 
	 * @param message
	 * @param conversation
	 */
	public void handleException(String message, final Conversation conversation) {
		connLog.logException(conversation, message);

		actorService.notifyStatusExceptionActors(conversation, new MessageCallback<List<Void>>() {

			public void execute(List<Void> results) {
				conversation.getHandler().finishedConversation(conversation, true);
				conversation.getHandler().finishedMessageEvent();
			}
		});
	}

	/**
	 * Log an exception and send back an error response to the client.
	 * 
	 * @param message
	 * @param conversation
	 * @param response
	 */
	public void handleException(String message, Conversation conversation, HttpResponse response) {
		if (conversation.getHandler().getInboundChannel().isConnected()) {
			conversation.setResponse(response);
			conversation.getHandler().sendResponse(conversation);
			finishedConversation(conversation, true);

		} else {
			handleException(message, conversation);
		}
	}

	// ----------------------------------------------------
	// -- the different actions for the different states --
	// ----------------------------------------------------

	// ---------------
	// --- INBOUND ---
	// ---------------

	/**
	 * Note that the first fail decides the response;
	 * 
	 * @param task
	 */
	private void handleInboundRequestActorResults(Conversation conversation, List<HttpResult> results) {
		log.debug("handleInboundActorResults");
		for (HttpResult r : results) {
			if (!r.isSuccess()) {
				handleException(r.getReason(), conversation,
						r.getErrorResponse() == null ? HttpResponses.httpResponse500() : r.getErrorResponse());
				return;
			}
		}

		if (conversation.getHandler().isKeepAlive() && conversation.getHandler().isConnected())
			forwardInboundRequest(conversation);
		else
			createInternalClientChannel(conversation);
	}

	private void createInternalClientChannel(final Conversation conversation) {
		log.debug("createInternalClientChannel - " + Thread.currentThread().getId());
		socketService.createClientChannel(conversation, new MessageCallback<ChannelFuture>() {

			public void execute(ChannelFuture cf) {
				if (cf.isSuccess()) {
					conversation.getHandler().setOutboundChannel(conversation, cf.getChannel());
					forwardInboundRequest(conversation);
				} else {
					handleException("Failed to connect to internal server: " + cf.getCause().getMessage(),
							conversation, HttpResponses.httpResponse502());
				}
			}
		});
	}

	// ---------------------------------------------------------

	private void forwardInboundRequest(final Conversation conversation) {
		log.debug("forwardInboundRequest");
		final HttpIOHandler handler = conversation.getHandler();
		handler.sendRequest(conversation);
		if (conversation.getRequest().isChunked()) {
			handleIncomingChunk(conversation);
		}
	}

	private void handleIncomingChunk(final Conversation conversation) {
		final HttpChunk chunk = conversation.getHandler().getInboundChunk();
		if (chunk != null) {
			actorService.notifyInboundChunkActors(conversation, chunk, new MessageCallback<List<HttpChunkResult>>() {

				public void execute(List<HttpChunkResult> results) {
					handleInboundChunkActorResults(conversation, chunk, results);
				}
			});
		} else {
			handleException("Missing a chunk.", conversation);
		}
	}

	private void handleInboundChunkActorResults(Conversation conversation, HttpChunk chunk,
			List<HttpChunkResult> results) {
		log.debug("handleInboundChunkActorResults");
		boolean dataChanged = false;
		HttpChunk finalChunk = chunk;
		for (HttpChunkResult r : results) {
			if (!r.isSuccess()) {
				handleException(r.getReason(), conversation,
						r.getErrorResponse() == null ? HttpResponses.httpResponse500() : r.getErrorResponse());
				return;
			} else {
				if (r.isChangedData() && !dataChanged) {
					finalChunk = r.getChunk();
					dataChanged = true;
				}
			}
		}

		conversation.getHandler().sendRequestChunk(finalChunk);

		if (!finalChunk.isLast()) {
			handleIncomingChunk(conversation);
		}
	}

	// ----------------
	// --- OUTBOUND ---
	// ----------------

	private void forwardOutboundResponse(Conversation conversation) {
		log.debug("forwardOutboundResponse");
		HttpIOHandler handler = conversation.getHandler();
		handler.sendResponse(conversation);
		if (!conversation.getResponse().isChunked()) {
			finishedConversation(conversation, false);
		}

		// if (conversation.getResponse().isChunked()) {
		// log.debug("Reading Chunks");
		// HttpChunk chunk;
		// do {
		// chunk = handler.getOutboundChunk();
		// if (chunk != null) {
		// // TODO iterate actors
		// handler.sendResponseChunk(chunk);
		// } else {
		// handleException("Missing a chunk.", conversation);
		// }
		// } while (!chunk.isLast());
		// }
		// finishedConversation(conversation, false);
	}

	private void forwardOutboundChunk(Conversation conversation, HttpChunk chunk) {
		log.debug("forwardOutboundChunk");
		HttpIOHandler handler = conversation.getHandler();
		handler.sendResponseChunk(chunk);
		if (chunk.isLast()) {
			finishedConversation(conversation, false);
		}
	}

	/**
	 * Note that the first fail decides the response;
	 * 
	 * @param task
	 */
	private void handleOutboundResponseActorResults(Conversation conversation, List<HttpResult> results) {
		log.debug("handleOutboundResponseActorResults");
		for (HttpResult r : results) {
			if (!r.isSuccess()) {
				handleException(r.getReason(), conversation,
						r.getErrorResponse() == null ? HttpResponses.httpResponse500() : r.getErrorResponse());
				return;
			}
		}
		forwardOutboundResponse(conversation);
	}

	private void handleOutboundChunkActorResults(Conversation conversation, HttpChunk chunk,
			List<HttpChunkResult> results) {
		log.debug("handleOutboundChunkActorResults");
		boolean dataChanged = false;
		HttpChunk finalChunk = chunk;
		for (HttpChunkResult r : results) {
			if (!r.isSuccess()) {
				handleException(r.getReason(), conversation,
						r.getErrorResponse() == null ? HttpResponses.httpResponse500() : r.getErrorResponse());
				return;
			} else {
				if (r.isChangedData() && !dataChanged) {
					finalChunk = r.getChunk();
					dataChanged = true;
				}
			}
		}
		forwardOutboundChunk(conversation, finalChunk);
	}

	// -----------
	// --- END ---
	// -----------

	private void finishedConversation(final Conversation conversation, final boolean forceClose) {
		conversation.setFinished(true);
		actorService.notifyStatusFinishedActors(conversation, new MessageCallback<List<Void>>() {

			public void execute(List<Void> results) {
				log.debug("finishedConversation " + conversation.getConversationId());
				conversation.setDuration((System.nanoTime() - conversation.getStartNanoTime()) / 1000000);
				connLog.logConversationFinished(conversation);

				conversation.getHandler().finishedConversation(conversation, forceClose);
				conversation.getHandler().finishedMessageEvent();
			}
		});
	}

	public Executor getExecutorService() {
		return executorService;
	}
}
