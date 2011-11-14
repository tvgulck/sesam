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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.FailedChannelFuture;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.sesam.proxy.ClientSocketChannelService;
import be.vlaanderen.sesam.proxy.actors.MessageCallback;
import be.vlaanderen.sesam.proxy.http.Conversation;

/**
 * Creates clientconnections.
 * 
 * @author Kristof Heirwegh
 * 
 */
public class HttpClientSocketChannelServiceImpl implements ClientSocketChannelService<Conversation> {

	private static final Logger log = LoggerFactory.getLogger(HttpClientSocketChannelServiceImpl.class);

	private final ClientSocketChannelFactory FACTORY;

	private ExecutionHandler executionHandler;

	public HttpClientSocketChannelServiceImpl() {
		// thread isn't broken -- chunks are kept in a queue, so no need for this
		executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 32000000));
		// TODO configurable (although you shouldn't make this any larger)
		
		Executor executor = Executors.newCachedThreadPool();
		FACTORY = new NioClientSocketChannelFactory(executor, executor);

		// TODO move to BundleActivator or group otherwise
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				FACTORY.releaseExternalResources();
			}
		}));
	}

	public void createClientChannel(final Conversation conversation, final MessageCallback<ChannelFuture> onFinish) {
		log.debug("Setting up client connection - " + Thread.currentThread().getId());

		InetSocketAddress addr = conversation.getRule().getForwardInetAddress();
		if (addr == null || addr.getAddress() == null) {
			onFinish.execute(new FailedChannelFuture(conversation.getHandler().getInboundChannel(), new Throwable(
					"No forwarding address set in rule (or host not found in DNS).")));

		} else {
			log.debug("building bootstrap");
			// we shouldn't do this, but pipeline needs the handler
			ClientBootstrap cb = new ClientBootstrap(FACTORY);
			cb.setOption("tcpNoDelay", true);
			if (HttpHeaders.isKeepAlive(conversation.getRequest()))
				cb.setOption("child.keepAlive", true);

			cb.getPipeline().addLast("codec", new HttpClientCodec());
			cb.getPipeline().addLast("inflater", new HttpContentDecompressor());
			cb.getPipeline().addLast("synco", executionHandler);
			cb.getPipeline().addLast("handler", conversation.getHandler().getOutboundHandler());

			log.debug("connecting");
			ChannelFuture cf = cb.connect(addr);

			cf.addListener(new ChannelFutureListener() {

				public void operationComplete(ChannelFuture future) throws Exception {
					log.debug("finished");
					onFinish.execute(future);
				}
			});
		}
	}
}
