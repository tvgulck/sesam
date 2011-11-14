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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class IPRange {

	private BigInteger from;
	private BigInteger to;

	public BigInteger getFrom() {
		return from;
	}

	public void setFrom(String from) {
		try {
			this.from = ipToBigInteger(InetAddress.getByName(from));
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Not a valid ip address");
		}
	}

	public BigInteger getTo() {
		return to;
	}

	public void setTo(String to) {
		try {
			this.to = ipToBigInteger(InetAddress.getByName(to));
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Not a valid ip address");
		}
	}

	// ---------------------------------------------------------

	// TODO

	public boolean withinRange(InetSocketAddress address) {
		BigInteger test = ipToBigInteger(address.getAddress());
		// FIXME
		return true;
	}

	public static BigInteger ipToBigInteger(InetAddress address) {
		byte[] octets = address.getAddress();
		return new BigInteger(1, octets);
	}
	
	// TODO make unittest
//	public static void main(String[] params) {
//		try {
//			BigInteger a = ipToBigInteger(InetAddress.getByName("255.10.255.1"));
//
//			if (!a.equals(b))
//				System.out.println("Hoezo niet gelijk? a:" + a + " - b:" + b);
//			else
//				System.out.println("equal");
//			
//		} catch (UnknownHostException e) {}
//
//	}
}