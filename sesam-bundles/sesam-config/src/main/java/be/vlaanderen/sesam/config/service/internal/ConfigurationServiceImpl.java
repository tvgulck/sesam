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

package be.vlaanderen.sesam.config.service.internal;

import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import be.vlaanderen.sesam.config.service.ConfigurationChangedConsumer;
import be.vlaanderen.sesam.config.service.ConfigurationService;
import be.vlaanderen.sesam.config.Rule;

/**
 * Simple static ConfigurationService using a spring bean as data-source.
 * 
 * @author Kristof Heirwegh
 */
public class ConfigurationServiceImpl implements ConfigurationService {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	private List<Rule> rules;

	@Autowired(required = false)
	Set<ConfigurationChangedConsumer> changeConsumers;

	public void init() {
		if (rules == null) {
			throw new MissingResourceException("Need a set of rules", "List<Rule>", "(n/a)");
		}
		updateChangeConsumers();
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	// ---------------------------------------------------------

	private void updateChangeConsumers() {
		if (changeConsumers != null) {
			for (ConfigurationChangedConsumer ccc : changeConsumers) {
				log.debug("Updated a changeConsumer");
				ccc.onConfigurationChanged(rules);
			}
		}
	}
}