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

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a new pipeline when a channel (connection) is opened. Delegation (based on rules) is not yet done here
 * because rules can depend on sent content (http headers).
 * 
 * @author Kristof Heirwegh
 */
public class HttpServerPipelineFactory implements ChannelPipelineFactory {

	private static final Logger log = LoggerFactory.getLogger(HttpServerPipelineFactory.class);

	private final ExecutionHandler executionHandler;

	private final HttpCentral central;

	public HttpServerPipelineFactory(ExecutionHandler executionHandler, HttpCentral central) {
		this.executionHandler = executionHandler;
		this.central = central;
	}

	// ---------------------------------------------------------

	public ChannelPipeline getPipeline() throws Exception {
		log.debug("Received a pipelineRequest.");
		return Channels.pipeline(new HttpServerCodec(), new HttpTextOnlyContentCompressor(), executionHandler, // Must
																												// be
																												// shared
				new HttpProxyHandler(central));
	}
}
