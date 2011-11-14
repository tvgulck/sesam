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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.sesam.proxy.LoggingService;
import be.vlaanderen.sesam.proxy.http.Conversation;

/**
 * A logging actor that uses a LoggingService to do the actual persisting of the logstatements.
 * <p>
 * The format complies with AWStats for easy parsing.
 * <p>
 * If you need another loggingImplementation, create another LoggingService, and pass it along to a ServiceLoggingActor.
 * 
 * @author Kristof Heirwegh
 */
public class LogbackLoggingService implements LoggingService {

	// If your log records are EXACTLY like this (NCSA combined/XLF/ELF log format):
	// 62.161.78.73 - - [dd/mmm/yyyy:hh:mm:ss +0x00] "GET /page.html HTTP/1.1" 200 1234 "http://www.from.com/from.htm"
	// "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)"
	// You must use : LogFormat=1
	// This is same than: LogFormat="%host %other %logname %time1 %methodurl %code %bytesd %refererquot %uaquot"

	private static final Logger commlog = LoggerFactory.getLogger("traffic.log");

	private static final Logger log = LoggerFactory.getLogger(LogbackLoggingService.class);

	private static final String LOGFORMATKEY = "traffic.log.format";

	private static final SimpleDateFormat TIME1FORMATTER = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]");

	private String propertiesLocation;

	private String logFormat = "%host %other %logname %time1 %methodurl %code %bytesd %refererquot %uaquot";

	private List<Field> fields = new ArrayList<Field>();

	/**
	 * Load properties for format & parse to fields.
	 */
	public void init() {
		log.debug("Initializing Communication Logging");

		if (propertiesLocation != null && !"".equals(propertiesLocation)) {
			File f = new File(propertiesLocation);
			if (f.isFile()) {
				Reader r = null;
				try {
					r = new FileReader(f);
					Properties props = new Properties();
					props.load(r);
					if (props.containsKey(LOGFORMATKEY)) {
						logFormat = props.getProperty(LOGFORMATKEY);
					} else {
						log.warn("Could not find \"" + LOGFORMATKEY + "\"-key, using defaults.");
					}
				} catch (IOException e) {
					log.warn("Failed reading properties, using defaults (" + e.getMessage() + ").");
				} finally {
					if (r != null)
						try {
							r.close();
						} catch (IOException e) {
						}
				}
			} else {
				log.warn("Properties file not found, using defaults (" + f.getAbsolutePath() + ").");
			}
		} else {
			log.info("Properties location not set, using defaults.");
		}

		parseFormatString();
	}

	private void parseFormatString() {
		fields.clear();
		if (logFormat == null || "".equals(logFormat.trim())) {
			return;
		} else {
			String[] parts = logFormat.trim().split(" ");
			for (String p : parts) {
				Field f = Field.getFieldByName(p);
				if (f != null) {
					fields.add(f);
				} else {
					log.warn("Invalid fieldname: " + p);
				}
			}
		}
	}

	public String getPropertiesLocation() {
		return propertiesLocation;
	}

	public void setPropertiesLocation(String propertiesLocation) {
		this.propertiesLocation = propertiesLocation;
	}

	public String getLogFormat() {
		return logFormat;
	}

	public void setLogFormat(String logFormat) {
		this.logFormat = logFormat;
	}

	// ---------------------------------------------------------
	// -- LoggingActor --
	// ---------------------------------------------------------

	/**
	 * This method does not block.
	 */
	public void log(String message, Conversation c) {
		StringBuilder sb = getBase(c);
		sb.append(message);

		log.info(sb.toString());
	}

	// ---------------------------------------------------------

	public void logConversationFinished(Conversation c) {
		StringBuilder sb = new StringBuilder();
		for (Field f : fields) {
			f.appendValue(c, sb);
			sb.append(" ");
		}
		sb.setLength(sb.length() - 1);
		commlog.info(sb.toString());
	}

	public void logException(Conversation c, String message) {
		StringBuilder sb = getBase(c);
		sb.append(message);
		log.info("Exception: " + sb.toString());
	}

	// ---------------------------------------------------------

	private static enum Field {
		HOST("%host") {

			public void appendValue(Conversation c, StringBuilder sb) {
				InetSocketAddress addr = (InetSocketAddress) c.getHandler().getInboundChannel().getRemoteAddress();
				if (addr.getAddress() != null)
					sb.append(addr.getAddress().getHostAddress());
				else
					sb.append("?.?.?.?");
			}
		},
		OTHER("%other") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append("-");
			}
		},
		LOGNAME("%logname") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append("-");
			}
		},
		TIME1("%time1") {

			public void appendValue(Conversation c, StringBuilder sb) {
				synchronized (TIME1FORMATTER) {
					sb.append(TIME1FORMATTER.format(new Date()));
				}
			}
		},
		METHODURL("%methodurl") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append("\"");
				sb.append(c.getRequest().getMethod().getName());
				sb.append(" ");
				sb.append(c.getRequest().getUri());
				sb.append(" ");
				sb.append(c.getRequest().getProtocolVersion().getText());
				sb.append("\"");
			}
		},
		CODE("%code") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append(c.getResponse().getStatus().getCode());
			}
		},
		BYTESD("%bytesd") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append(c.getResponseSize());
			}
		},
		REFERERQUOT("%refererquot") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append("\"");
				sb.append(HttpHeaders.getHeader(c.getRequest(), HttpHeaders.Names.REFERER, ""));
				sb.append("\"");
			}
		},
		UAQUOTE("%uaquot") {

			public void appendValue(Conversation c, StringBuilder sb) {
				sb.append("\"");
				sb.append(HttpHeaders.getHeader(c.getRequest(), HttpHeaders.Names.USER_AGENT, ""));
				sb.append("\"");
			}
		};

		// ---------------------------------------------------------

		private final String fieldname;

		private Field(String fieldname) {
			this.fieldname = fieldname;
		}

		abstract public void appendValue(Conversation c, StringBuilder sb);

		public static Field getFieldByName(String fieldName) {
			if (fieldName == null || "".equals(fieldName)) {
				return null;
			} else {
				for (Field f : values()) {
					if (f.fieldname.equals(fieldName)) {
						return f;
					}
				}
				return null;
			}
		}
	}

	// ---------------------------------------------------------

	private StringBuilder getBase(Conversation c) {
		Date now = new Date();
		StringBuilder sb = new StringBuilder();
		sb.append(now);
		sb.append(" ");
		sb.append(c.getConversationId());
		sb.append(" ");

		if (c.getHandler().getInboundChannel() != null) {
			InetSocketAddress localAddr = (InetSocketAddress) c.getHandler().getInboundChannel().getLocalAddress();
			InetSocketAddress remoteAddr = (InetSocketAddress) c.getHandler().getInboundChannel().getRemoteAddress();
			sb.append(remoteAddr.getAddress());
			sb.append(":");
			sb.append(remoteAddr.getPort());
			sb.append(" --> ");
			sb.append(localAddr.getAddress());
			sb.append(":");
			sb.append(localAddr.getPort());
		} else {
			sb.append("??");
		}

		if (c.getHandler().getOutboundChannel() != null) {
			InetSocketAddress remoteAddr = (InetSocketAddress) c.getHandler().getOutboundChannel().getRemoteAddress();
			sb.append(" --> ");
			sb.append(remoteAddr.getAddress());
			sb.append(":");
			sb.append(remoteAddr.getPort());
		}
		sb.append(" - ");

		return sb;
	}
}
