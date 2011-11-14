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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.netty.handler.codec.http.HttpChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import be.vlaanderen.sesam.config.Rule;
import be.vlaanderen.sesam.proxy.ActorService;
import be.vlaanderen.sesam.proxy.actors.Actor;
import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;
import be.vlaanderen.sesam.proxy.http.actors.HttpInboundActor;
import be.vlaanderen.sesam.proxy.http.actors.HttpOutboundActor;
import be.vlaanderen.sesam.proxy.http.actors.HttpStatusActor;
import be.vlaanderen.sesam.proxy.internal.actors.messaging.MessageResultAggregator;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpChunkResult;
import be.vlaanderen.sesam.proxy.internal.http.messaging.HttpResult;

/**
 * Finds actors by their name, caches results
 * 
 * @author Kristof Heirwegh
 * 
 */
@Component
public class HttpActorServiceImpl implements ActorService {

	private static final Logger log = LoggerFactory.getLogger(HttpActorServiceImpl.class);

	@Autowired
	private ApplicationContext appContext;

	// pipeline actors?

	private final Map<Rule, List<HttpInboundActor>> inboundActors = new WeakHashMap<Rule, List<HttpInboundActor>>();

	private final Map<Rule, List<HttpInboundActor>> inboundChunkActors = new WeakHashMap<Rule, List<HttpInboundActor>>();

	private final Map<Rule, List<HttpOutboundActor>> outboundActors = new WeakHashMap<Rule, List<HttpOutboundActor>>();

	private final Map<Rule, List<HttpOutboundActor>> outboundChunkActors = new WeakHashMap<Rule, List<HttpOutboundActor>>();

	private final Map<Rule, List<HttpStatusActor>> statusActors = new WeakHashMap<Rule, List<HttpStatusActor>>();

	// ---------------------------------------------------------

	/**
	 * TODO caveat -- actors that change request should be called sequentially. (will also relieve the need for
	 * synchronised) FIXME concurrent editing possible! (check canchangedata flag (and put in pipeline))
	 * 
	 * @param conversation
	 */
	public void notifyInboundRequestActors(final Conversation conversation,
			final MessageCallback<List<HttpResult>> resultCallback) {
		log.debug("notifyInboundRequestActors");
		List<HttpInboundActor> actors = getInboundActors(conversation.getRule());
		if (actors.size() > 0) {
			MessageResultAggregator<HttpResult> aggr = new MessageResultAggregator<HttpResult>(actors.size(),
					new MessageCallback<List<HttpResult>>() {

						public void execute(List<HttpResult> results) {
							resultCallback.execute(results);
						}
					});
			for (HttpInboundActor actor : actors) {
				HttpResult result = new HttpResult(conversation.getConversationId(), actor);
				actor.handleInboundRequest(conversation, result, aggr);
			}
		} else {
			resultCallback.execute(new ArrayList<HttpResult>(0));
		}
	}

	// FIXME concurrent editing possible! (check canchangedata flag)
	public void notifyInboundChunkActors(final Conversation conversation, final HttpChunk chunk,
			final MessageCallback<List<HttpChunkResult>> resultCallback) {
		log.debug("notifyInboundChunkActors");
		List<HttpInboundActor> actors = getInboundChunkActors(conversation.getRule());
		if (actors.size() > 0) {
			MessageResultAggregator<HttpChunkResult> aggr = new MessageResultAggregator<HttpChunkResult>(actors.size(),
					new MessageCallback<List<HttpChunkResult>>() {

						public void execute(List<HttpChunkResult> results) {
							resultCallback.execute(new ArrayList<HttpChunkResult>());
						}
					});
			for (HttpInboundActor actor : actors) {
				HttpChunkResult result = new HttpChunkResult(conversation.getConversationId(), actor);
				actor.handleInboundChunk(chunk, conversation, result, aggr);
			}
		} else {
			resultCallback.execute(new ArrayList<HttpChunkResult>(0));
		}
	}

	// FIXME concurrent editing possible! (check canchangedata flag)
	public void notifyOutboundResponseActors(final Conversation conversation,
			final MessageCallback<List<HttpResult>> resultCallback) {
		log.debug("notifyOutboundResponseActors");
		List<HttpOutboundActor> actors = getOutboundActors(conversation.getRule());
		if (actors.size() > 0) {
			MessageResultAggregator<HttpResult> aggr = new MessageResultAggregator<HttpResult>(actors.size(),
					new MessageCallback<List<HttpResult>>() {

						public void execute(List<HttpResult> results) {
							resultCallback.execute(new ArrayList<HttpResult>());
						}
					});
			for (HttpOutboundActor actor : actors) {
				HttpResult result = new HttpResult(conversation.getConversationId(), actor);
				actor.handleOutboundRequest(conversation, result, aggr);
			}
		} else {
			resultCallback.execute(new ArrayList<HttpResult>(0));
		}
	}

	// FIXME concurrent editing possible! (check canchangedata flag)
	public void notifyOutboundChunkActors(final Conversation conversation, final HttpChunk chunk,
			final MessageCallback<List<HttpChunkResult>> resultCallback) {
		log.debug("notifyOutboundChunkActors");
		List<HttpOutboundActor> actors = getOutboundChunkActors(conversation.getRule());
		if (actors.size() > 0) {
			MessageResultAggregator<HttpChunkResult> aggr = new MessageResultAggregator<HttpChunkResult>(actors.size(),
					new MessageCallback<List<HttpChunkResult>>() {

						public void execute(List<HttpChunkResult> result) {
							resultCallback.execute(result);
						}
					});
			for (HttpOutboundActor actor : actors) {
				HttpChunkResult result = new HttpChunkResult(conversation.getConversationId(), actor);
				actor.handleOutboundChunk(chunk, conversation, result, aggr);
			}
		} else {
			resultCallback.execute(new ArrayList<HttpChunkResult>(0));
		}
	}

	public void notifyStatusExceptionActors(final Conversation conversation,
			final MessageCallback<List<Void>> resultCallback) {
		log.debug("notifyStatusExceptionActors");
		List<HttpStatusActor> actors = getStatusActors(conversation.getRule());
		if (actors.size() > 0) {
			MessageResultAggregator<Void> aggr = new MessageResultAggregator<Void>(actors.size(),
					new MessageCallback<List<Void>>() {

						public void execute(List<Void> results) {
							resultCallback.execute(results);
						}
					});
			for (HttpStatusActor actor : actors) {
				actor.handleException(conversation, aggr);
			}
		} else {
			resultCallback.execute(new ArrayList<Void>(0));
		}
	}

	public void notifyStatusFinishedActors(final Conversation conversation,
			final MessageCallback<List<Void>> resultCallback) {
		log.debug("notifyStatusFinishedActors");
		List<HttpStatusActor> actors = getStatusActors(conversation.getRule());
		if (actors.size() > 0) {
			MessageResultAggregator<Void> aggr = new MessageResultAggregator<Void>(actors.size(),
					new MessageCallback<List<Void>>() {

						public void execute(List<Void> results) {
							resultCallback.execute(results);
						}
					});
			for (HttpStatusActor actor : actors) {
				actor.handleConversationFinished(conversation, aggr);
			}
		} else {
			resultCallback.execute(new ArrayList<Void>(0));
		}
	}

	// ---------------------------------------------------------

	public List<HttpInboundActor> getInboundActors(Rule r) {
		synchronized (inboundActors) {
			if (!inboundActors.containsKey(r)) {
				retrieveActors(r);
			}
			return inboundActors.get(r);
		}
	}

	public List<HttpOutboundActor> getOutboundActors(Rule r) {
		synchronized (outboundActors) {
			if (!outboundActors.containsKey(r)) {
				retrieveActors(r);
			}
			return outboundActors.get(r);
		}
	}

	public List<HttpInboundActor> getInboundChunkActors(Rule r) {
		synchronized (inboundChunkActors) {
			if (!inboundChunkActors.containsKey(r)) {
				retrieveActors(r);
			}
			return inboundChunkActors.get(r);
		}
	}

	public List<HttpOutboundActor> getOutboundChunkActors(Rule r) {
		synchronized (outboundChunkActors) {
			if (!outboundChunkActors.containsKey(r)) {
				retrieveActors(r);
			}
			return outboundChunkActors.get(r);
		}
	}

	public List<HttpStatusActor> getStatusActors(Rule r) {
		synchronized (statusActors) {
			if (!statusActors.containsKey(r)) {
				retrieveActors(r);
			}
			return statusActors.get(r);
		}
	}

	// ---------------------------------------------------------

	private void retrieveActors(Rule r) {
		synchronized (inboundActors) {
			synchronized (outboundActors) {
				synchronized (statusActors) {
					synchronized (inboundChunkActors) {
						synchronized (outboundChunkActors) {
							List<HttpInboundActor> inb = new ArrayList<HttpInboundActor>();
							inboundActors.put(r, inb);
							List<HttpOutboundActor> outb = new ArrayList<HttpOutboundActor>();
							outboundActors.put(r, outb);
							List<HttpInboundActor> inbChunk = new ArrayList<HttpInboundActor>();
							inboundChunkActors.put(r, inbChunk);
							List<HttpOutboundActor> outbChunk = new ArrayList<HttpOutboundActor>();
							outboundChunkActors.put(r, outbChunk);
							List<HttpStatusActor> stat = new ArrayList<HttpStatusActor>();
							statusActors.put(r, stat);

							log.trace("finding actors");
							for (String actorId : r.getActors()) {
								if (appContext.containsBean(actorId) && appContext.getBean(actorId) instanceof Actor) {
									Actor a = (Actor) appContext.getBean(actorId);
									if (a instanceof HttpInboundActor) {
										HttpInboundActor hia = (HttpInboundActor) a;
										inb.add(hia);
										if (!hia.isHeadersOnly()) {
											inbChunk.add(hia);
										}
									} else if (a instanceof HttpOutboundActor) {
										HttpOutboundActor hoa = (HttpOutboundActor) a;
										outb.add(hoa);
										if (!hoa.isHeadersOnly()) {
											outbChunk.add(hoa);
										}
									} else if (a instanceof HttpStatusActor) {
										stat.add((HttpStatusActor) a);
									} else {
										log.warn("Unknown HttpActor Type: " + a.getClass().getSimpleName()
												+ " (will be ignored)");
									}
								} else {
									log.warn("Actor " + actorId + " not found! (will be ignored)");
								}
							}
						}
					}
				}
			}
		}
	}
}
