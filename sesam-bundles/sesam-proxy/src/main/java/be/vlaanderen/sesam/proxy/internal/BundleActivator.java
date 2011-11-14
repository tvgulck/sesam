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

package be.vlaanderen.sesam.proxy.internal;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import akka.actor.Actors;
import be.vlaanderen.sesam.config.Rule;
import be.vlaanderen.sesam.config.service.ConfigurationChangedConsumer;
import be.vlaanderen.sesam.config.service.ConfigurationService;
import be.vlaanderen.sesam.proxy.internal.http.HttpCentral;
import be.vlaanderen.sesam.proxy.internal.http.HttpServerPipelineFactory;

/**
 * Main startup entrypoint of the application.
 * 
 * <p>
 * Starts up server, keeps track of and starts/stops listening on required ports.
 * 
 * @author Kristof Heirwegh
 * 
 */
public class BundleActivator implements ConfigurationChangedConsumer {

	private static final Logger log = LoggerFactory.getLogger(BundleActivator.class);

	@Autowired
	private BundleContext context;

	@Autowired
	private HttpCentral central;

	@Autowired
	private ConfigurationService configService;

	private Map<Integer, Channel> channels = new HashMap<Integer, Channel>();

	private ChannelFactory serverChannelFactory;

	private ServerBootstrap serverBootstrap;

	private ExecutionHandler executionHandler;

	private HttpServerPipelineFactory pipelineFactory;

	public void start() {
		log.info(context.getBundle().getSymbolicName() + " - " + "Opgestart.");
		startServer();
	}

	public void stop() {
		stopServer();
		stopActors();
		log.info(context.getBundle().getSymbolicName() + " - " + "Gestopt.");
	}

	// ---------------------------------------------------------

	private void startServer() {
		Executor executor = Executors.newCachedThreadPool();
		serverChannelFactory = new NioServerSocketChannelFactory(executor, executor);
		executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(32, 1048576, 33554432)); // TODO
																													// configurable
		serverBootstrap = new ServerBootstrap(serverChannelFactory);
		pipelineFactory = new HttpServerPipelineFactory(executionHandler, central);

		serverBootstrap.setPipelineFactory(pipelineFactory);

		onConfigurationChanged(configService.getRules());
	}

	private void stopServer() {
		try {
			serverChannelFactory.releaseExternalResources();
			serverBootstrap.releaseExternalResources();
			executionHandler.releaseExternalResources();
		} catch (Exception e) {
			log.warn("Failed to cleanly stop server: " + e.getMessage());
		}
	}

	private void stopActors() {
		Actors.registry().shutdownAll();
	}

	// ---------------------------------------------------------

	@Override
	public synchronized void onConfigurationChanged(List<Rule> rules) {
		// must update channels we are listening to
		Map<Integer, Channel> oldChannels = new HashMap<Integer, Channel>();
		oldChannels.putAll(channels);

		if (rules != null && rules.size() > 0) {
			for (Rule rule : rules) {
				Integer port = rule.getLocalRequestPort();
				if (port != null && port > 0) {
					if (channels.containsKey(port)) { // port can be used multiple times so test on channels.
						oldChannels.remove(port);
					} else {
						Channel c = serverBootstrap.bind(new InetSocketAddress(port));
						channels.put(port, c);
						log.info(" - Started listening on port: " + port);
					}
				}
			}
		}

		for (Entry<Integer, Channel> entry : oldChannels.entrySet()) {
			channels.remove(entry.getKey());
			entry.getValue().unbind().addListener(new ChannelFutureListener() {

				public void operationComplete(ChannelFuture future) throws Exception {
					InetSocketAddress addr = (InetSocketAddress) future.getChannel().getLocalAddress();
					log.info("Stopped listening on port: " + addr.getPort() + " -- "
							+ (future.isSuccess() ? "Success" : "Fail"));
				}
			});
		}
	}
}
