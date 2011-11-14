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

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import be.vlaanderen.sesam.config.HttpRule;
import be.vlaanderen.sesam.config.Rule;
import be.vlaanderen.sesam.config.service.ConfigurationService;
import be.vlaanderen.sesam.proxy.internal.RuleMatcherService;

/**
 * Using Visitor pattern to match rules.
 * 
 * @author Kristof Heirwegh
 * 
 */
@Component
public class HttpRuleMatcherService extends RuleMatcherService<HttpRequest> {

	@Autowired
	private ConfigurationService configService;

	@Override
	public boolean isMatch(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Rule rule,
			HttpRequest message) {
		if (rule instanceof HttpRule) {
			if (super.isMatch(localAddress, remoteAddress, rule, message)) {
				// TODO custom checks op queryparameters / uri / headers
				return true;
			}
		}
		return false;
	}

	public Rule findRule(InetSocketAddress localAddr, InetSocketAddress remoteAddr, HttpRequest message) {
		List<Rule> rules = configService.getRules();
		if (rules != null && rules.size() > 0) {
			for (Rule rule : rules) {
				if (isMatch(localAddr, remoteAddr, rule, message))
					return rule;
			}
		}
		return null;
	}

}
