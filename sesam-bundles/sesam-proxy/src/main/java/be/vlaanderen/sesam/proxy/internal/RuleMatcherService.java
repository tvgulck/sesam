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

import be.vlaanderen.sesam.config.Rule;

/**
 * Using Visitor pattern to match rules.
 * 
 * @author Kristof Heirwegh
 * 
 */
public abstract class RuleMatcherService<M> {

	public static String ANY = "*";

	/**
	 * Override to add your own checks, don't forget to call super.
	 * 
	 * @param localaddress
	 *            The local server-address this request was received on, this is only relevant in multi-homed setups.
	 *            When available it can be used to split communication between network interfaces.
	 * @param port
	 *            the local port this request was received on.
	 * @return if this rule matches the given parameters
	 */
	public boolean isMatch(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Rule rule, M message) {
		if (rule.getLocalRequestAddress() == null || ANY.equals(rule.getLocalRequestAddress())
				|| rule.getLocalRequestAddress().equals(localAddress.getAddress().toString())) {
			if (rule.getLocalRequestPort() == localAddress.getPort()) {
				if (rule.getRemoteRequestAddressRange() == null
						|| rule.getRemoteRequestAddressRange().withinRange(remoteAddress))
					return true;
			}
		}
		return false;
	}
}
