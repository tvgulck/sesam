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

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Main startup entrypoint of the application.
 * 
 * <p>Starts up the service.
 * 
 * @author Kristof Heirwegh
 */
public class BundleActivator {

	private static final Logger log = LoggerFactory.getLogger(BundleActivator.class);
	
	@Autowired
	private BundleContext context;

	public void start() {
		log.info(context.getBundle().getSymbolicName() + " - " + "Opgestart.");
	}

	public void stop() {
		log.info(context.getBundle().getSymbolicName() + " - " + "Gestopt.");
	}
}