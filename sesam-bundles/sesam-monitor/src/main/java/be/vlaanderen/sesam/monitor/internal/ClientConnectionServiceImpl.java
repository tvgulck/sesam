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

package be.vlaanderen.sesam.monitor.internal;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import be.vlaanderen.sesam.monitor.ClientConnectionService;

/**
 * Creates clientconnections.
 * 
 * @author Kristof Heirwegh
 * 
 */
@Component
public class ClientConnectionServiceImpl implements ClientConnectionService, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(ClientConnectionServiceImpl.class);

	private static final int MAX_RESPONSE_SIZE = 10 * 1024 * 1024;

	private ClientSocketChannelFactory FACTORY;

	private Timer timer;

	public void afterPropertiesSet() throws Exception {
		ExecutorService locutus = Executors.newCachedThreadPool();
		timer = new HashedWheelTimer();
		FACTORY = new NioClientSocketChannelFactory(locutus, locutus);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				FACTORY.releaseExternalResources();
				timer.stop();
			}
		}));
	}

	public ChannelFuture createClientChannel(SimpleChannelUpstreamHandler handler, InetSocketAddress addr,
			long readTimeoutMillis) {
		log.debug("Setting up client connection to: {} timeout: {} millis.", addr.getAddress().toString(),
				readTimeoutMillis);

		log.debug("building bootstrap");
		ClientBootstrap cb = new ClientBootstrap(FACTORY);
		// cb.setOption("tcpNoDelay", true);

		cb.getPipeline().addLast("codec", new HttpClientCodec());
		cb.getPipeline().addLast("aggregator", new HttpChunkAggregator(MAX_RESPONSE_SIZE));

		cb.getPipeline().addLast("inflater", new HttpContentDecompressor());
		// not used atm.
		// cb.getPipeline().addLast("chunkedWriter", new ChunkedWriteHandler());
		cb.getPipeline().addLast("timeout", new ReadTimeoutHandler(timer, readTimeoutMillis, TimeUnit.MILLISECONDS));
		cb.getPipeline().addLast("handler", handler);

		log.debug("connecting");
		return cb.connect(addr);
	}
}