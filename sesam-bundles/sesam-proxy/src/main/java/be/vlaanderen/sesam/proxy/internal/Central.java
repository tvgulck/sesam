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

/**
 * Central point for decision making.
 * 
 * @author Kristof Heirwegh
 */
public interface Central {

	// TODO -- deze central is bedoeld voor niet-http requests (raw)
	// Idem als IOHandler -- niet veel common ground met http

	// void handleIncoming(ChannelHandlerContext context, MessageEvent inbound);
	//
	// void handleOutgoing(ChannelHandlerContext context, MessageEvent outbound);
}