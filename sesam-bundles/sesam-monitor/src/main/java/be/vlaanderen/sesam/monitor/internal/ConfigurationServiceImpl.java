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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import be.vlaanderen.sesam.monitor.ClientConnectionService;
import be.vlaanderen.sesam.monitor.ConfigurationService;
import be.vlaanderen.sesam.monitor.MonitorService;
import be.vlaanderen.sesam.monitor.MonitorTask;

@Component
public class ConfigurationServiceImpl implements ConfigurationService, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	private static final long SCANTIMEMILLIS = 60000;

	@Autowired
	@Qualifier("configurationFile")
	private String configurationFile;

	@Autowired
	private BundleContext bundleContext;

	@Autowired
	private ClientConnectionService clientService;

	@Autowired
	@Qualifier("configurationTaskFileRootFolder")
	private String taskRootFolder;

	@Autowired
	private MonitorService monitorService;

	private long lastModified;

	public void afterPropertiesSet() throws Exception {
		checkConfigurationChange();
	}

	@SuppressWarnings("unchecked")
	@Scheduled(fixedDelay = SCANTIMEMILLIS)
	public void checkConfigurationChange() {
		log.debug("Checking for configuration changes.");
		try {
			File f = new File(configurationFile);
			if (f.isFile()) {
				long l = f.lastModified();
				if (l != lastModified) {
					loadConfiguration();
				}
			} else {
				log.warn("Configuration file not found!");
				monitorService.monitor(Collections.EMPTY_LIST);
			}
		} catch (Exception e) {
			log.warn("Failed checking for configuration changes: " + e.getMessage());
		}
	}

	// ---------------------------------------------------------

	private void loadConfiguration() {
		lastModified = (new File(configurationFile)).lastModified();
		OsgiBundleXmlApplicationContext ctx = new OsgiBundleXmlApplicationContext(new String[] { "file:"
				+ configurationFile });
		ctx.setBundleContext(bundleContext);
		ctx.refresh();
		Map<String, MonitorTask> tasks = ctx.getBeansOfType(MonitorTask.class);
		List<MonitorTask> badUns = new ArrayList<MonitorTask>();

		// FIXME
		// need to manually wire beans, OSGi issue, manually parsing isn't giving the same result as automatically at
		// startup...
		for (MonitorTask mt : tasks.values()) {
			mt.setClientService(clientService);
			mt.setTaskRootFolder(taskRootFolder);
			try {
				mt.afterPropertiesSet();
				if (!mt.isActive() || !mt.isValid())
					badUns.add(mt);
			} catch (Exception e) {
				log.warn("Failed initializing task: " + mt.getName() + " -- " + e.getMessage());
				badUns.add(mt);
			}
		}

		tasks.values().removeAll(badUns);

		monitorService.monitor(tasks.values());
	}
}