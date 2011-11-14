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

package be.vlaanderen.sesam.config;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic Rule, checking the TCP part of the request, more specific checking is done by the specific implementations (eg. HttpRule).
 * 
 * TODO should be in sesam-proxy.
 * 
 * @author Kristof Heirwegh
 */
public abstract class Rule {

	private String name;

	private Integer localRequestPort;

	private String localRequestAddress;

	/**
	 * TODO move to an (access)actor this is a filter
	 * Use this field to limit the range of ip-addresses that will be passed through 
	 * Leave null for any
	 */
	private IPRange remoteRequestAddressRange;

	private String forwardAddress;

	private Integer forwardPort;

	/**
	 * These actors will be notified when data is received / sent.
	 */
	private List<String> actors = new ArrayList<String>();

	public Rule() {}

	// ---------------------------------------------------------

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getForwardAddress() {
		return forwardAddress;
	}

	public void setForwardAddress(String forwardAddress) {
		this.forwardAddress = forwardAddress;
	}

	public Integer getForwardPort() {
		return forwardPort;
	}

	public void setForwardPort(Integer forwardPort) {
		this.forwardPort = forwardPort;
	}

	public Integer getLocalRequestPort() {
		return localRequestPort;
	}

	public void setLocalRequestPort(Integer localRequestPort) {
		this.localRequestPort = localRequestPort;
	}

	public String getLocalRequestAddress() {
		return localRequestAddress;
	}

	public void setLocalRequestAddress(String localRequestAddress) {
		this.localRequestAddress = localRequestAddress;
	}

	public IPRange getRemoteRequestAddressRange() {
		return remoteRequestAddressRange;
	}

	public void setRemoteRequestAddressRange(IPRange remoteRequestAddressRange) {
		this.remoteRequestAddressRange = remoteRequestAddressRange;
	}

	public List<String> getActors() {
		return actors;
	}

	public void setActors(List<String> actors) {
		this.actors = actors;
	}

	public InetSocketAddress getForwardInetAddress() {
		if (forwardAddress == null || forwardPort == null) {
			return null;
		} else {
			return new InetSocketAddress(forwardAddress, forwardPort);
		}
	}
}