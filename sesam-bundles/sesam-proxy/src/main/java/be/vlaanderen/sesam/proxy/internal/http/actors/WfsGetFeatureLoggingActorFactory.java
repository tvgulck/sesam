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

package be.vlaanderen.sesam.proxy.internal.http.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

import akka.actor.TypedActor;
import be.vlaanderen.sesam.proxy.http.actors.HttpInboundActor;

/**
 * Instantiates an Akka actor. Doing this with "<akka:typed-actor ..." isn't working. (Classloader probs)
 * 
 * <p>
 * The advantage of using a BeanFactory is that the Actors can be used as regular beans (eg. you can @Autowire them).
 * 
 * @author Kristof Heirwegh
 * 
 */
public class WfsGetFeatureLoggingActorFactory implements FactoryBean<HttpInboundActor>, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(WfsGetFeatureLoggingActorFactory.class);

	private String implementation;

	private int timeout = 5000;

	private HttpInboundActor instance;

	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {
		if (implementation == null || "".equals(implementation))
			implementation = "be.vlaanderen.sesam.proxy.internal.http.actors.WfsGetFeatureLoggingActor";

		Class<? extends HttpInboundActor> impl = (Class<? extends HttpInboundActor>) ClassUtils.forName(implementation,
				null);

		instance = (HttpInboundActor) TypedActor.newInstance(HttpInboundActor.class, impl, timeout);

		log.debug(" - Created WfsGetFeatureLoggingActor Actor instance.");
	}

	// ---------------------------------------------------------

	public HttpInboundActor getObject() throws Exception {
		return instance;
	}

	public Class<?> getObjectType() {
		return (instance == null ? null : instance.getClass());
	}

	public boolean isSingleton() {
		return true;
	}

	public String getImplementation() {
		return implementation;
	}

	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
