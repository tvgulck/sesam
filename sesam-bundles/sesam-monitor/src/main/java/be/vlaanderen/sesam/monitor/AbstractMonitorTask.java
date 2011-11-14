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

package be.vlaanderen.sesam.monitor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import be.vlaanderen.sesam.monitor.internal.util.BodyUtils;

/**
 * A task to be run at a periodic interval.
 * <p>
 * Caveat, this is just http, so might be better named HttpMonitorTask.
 * 
 * @author Kristof Heirwegh
 */
public abstract class AbstractMonitorTask implements BeanNameAware, Runnable { // InitializingBean, FIXME

	private static final Logger log = LoggerFactory.getLogger(AbstractMonitorTask.class);

	private static final Logger monitorLog = LoggerFactory.getLogger("monitor.log");

	private static final String RESPONSEBODYPOSTFIX = "-responseBody.bin";

	private static final String REQUESTBODYPOSTFIX = "-requestBody.bin";

	private static final String NOTIFICATIONLEVELS = " IGNORE INFO WARN ERROR ";

	protected static final String NOTIFICATIONTYPE_SUCCESS = "success";

	protected static final String NOTIFICATIONTYPE_TIMEOUT = "timeout";

	protected static final String NOTIFICATIONTYPE_CONNECTIONREFUSED = "Connection refused";

	protected static final String NOTIFICATIONTYPE_RESPONSEHEADER = "responseHeader:";

	protected static final String NOTIFICATIONTYPE_RESPONSEBODY = "responseBody";

	protected static final String NOTIFICATIONTYPE_EXCEPTION = "exception";

	private String name;

	private boolean active = true;

	private Boolean valid;

	private String requestHostname;

	private int requestPort;

	private String requestUri;

	private String requestMethod = "GET";

	private String requestHttpVersion = "HTTP/1.1";

	private Map<String, String> requestHeaders = new LinkedHashMap<String, String>();

	// TODO create objects so we can check not exists or optional
	private Map<String, String> responseHeaders = new LinkedHashMap<String, String>();

	private long responseTimeoutMillis = 60000;

	private int responseStatusCode = 200;

	private boolean checkResponseBody;

	private String responseBodyMd5Sum;

	private LinkedHashMap<String, String> notificationsLevels = getDefaultNotificationLevels();

	private String schedule = "0 0 * * * *"; // hourly

	private File requestBodyFile;

	@Autowired
	@Qualifier("configurationTaskFileRootFolder")
	private String taskRootFolder;

	// ---------------------------------------------------------

	public void setBeanName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public long getResponseTimeoutMillis() {
		return responseTimeoutMillis;
	}

	public void setResponseTimeoutMillis(long responseTimeoutMillis) {
		this.responseTimeoutMillis = responseTimeoutMillis;
	}

	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, String> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public LinkedHashMap<String, String> getNotificationsLevels() {
		return notificationsLevels;
	}

	public void setNotificationsLevels(LinkedHashMap<String, String> notificationsLevels) {
		this.notificationsLevels = notificationsLevels;
	}

	public boolean isCheckResponseBody() {
		return checkResponseBody;
	}

	public void setCheckResponseBody(boolean checkResponseBody) {
		this.checkResponseBody = checkResponseBody;
	}

	public File getRequestBodyFile() {
		return requestBodyFile;
	}

	public String getRequestHostname() {
		return requestHostname;
	}

	public void setRequestHostname(String requestHostname) {
		this.requestHostname = requestHostname;
	}

	public int getRequestPort() {
		return requestPort;
	}

	public void setRequestPort(int requestPort) {
		this.requestPort = requestPort;
	}

	public String getRequestUri() {
		return requestUri;
	}

	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}

	public String getRequestHttpVersion() {
		return requestHttpVersion;
	}

	public void setRequestHttpVersion(String requestHttpVersion) {
		this.requestHttpVersion = requestHttpVersion;
	}

	public String getTaskRootFolder() {
		return taskRootFolder;
	}

	public void setTaskRootFolder(String taskRootFolder) {
		this.taskRootFolder = taskRootFolder;
	}

	public int getResponseStatusCode() {
		return responseStatusCode;
	}

	public void setResponseStatusCode(int responseStatusCode) {
		this.responseStatusCode = responseStatusCode;
	}

	public String getResponseBodyMd5Sum() {
		return responseBodyMd5Sum;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	// ---------------------------------------------------------

	public void afterPropertiesSet() throws Exception {
		if (isValid() && isActive()) {
			if ("POST".equals(requestMethod)) {
				requestBodyFile = new File(taskRootFolder, getName() + REQUESTBODYPOSTFIX);
			}

			if (checkResponseBody) {
				File f = new File(taskRootFolder, getName() + RESPONSEBODYPOSTFIX);
				responseBodyMd5Sum = BodyUtils.getMd5Sum(f);
				if (responseBodyMd5Sum == null) {
					valid = false;
				}
			}
		}
	}

	public boolean isValid() {
		if (valid == null) {
			valid = true;
			if (schedule == null || "".equals(schedule)) {
				log.warn("Invalid MonitorTask: Need a schedule time (cron format).");
				valid = false;
			}
			if (valid && checkResponseBody) {
				File f = new File(taskRootFolder, getName() + RESPONSEBODYPOSTFIX);
				if (!f.isFile()) {
					log.warn("Invalid MonitorTask: File with response body not found (" + f.getAbsolutePath() + ")");
					valid = false;
				}
			}
			if (valid && "POST".equals(requestMethod)) {
				File f = new File(taskRootFolder, getName() + REQUESTBODYPOSTFIX);
				if (!f.isFile()) {
					log.warn("Invalid MonitorTask: File with request body not found (" + f.getAbsolutePath() + ")");
					valid = false;
				}
			}
			if (valid) {
				for (String s : getNotificationsLevels().values()) {
					if (!NOTIFICATIONLEVELS.contains(" " + s + " ")) {
						log.warn("Invalid MonitorTask: Notificationlevels are not valid (Choose one of: IGNORE, INFO, WARN or ERROR)");
						valid = false;
					}
				}
			}
		}
		return valid;
	}

	// ---------------------------------------------------------

	private static LinkedHashMap<String, String> DEFAULTNOTIFICATIONLEVELS;

	public static LinkedHashMap<String, String> getDefaultNotificationLevels() {
		if (DEFAULTNOTIFICATIONLEVELS == null) {
			DEFAULTNOTIFICATIONLEVELS = new LinkedHashMap<String, String>();
			DEFAULTNOTIFICATIONLEVELS.put(NOTIFICATIONTYPE_SUCCESS, "INFO");
			DEFAULTNOTIFICATIONLEVELS.put("2..", "INFO");
			DEFAULTNOTIFICATIONLEVELS.put("3..", "INFO");
			DEFAULTNOTIFICATIONLEVELS.put("4..", "WARN");
			DEFAULTNOTIFICATIONLEVELS.put("5..", "ERROR");
			DEFAULTNOTIFICATIONLEVELS.put(NOTIFICATIONTYPE_EXCEPTION, "WARN");
			DEFAULTNOTIFICATIONLEVELS.put(NOTIFICATIONTYPE_TIMEOUT, "ERROR");
			DEFAULTNOTIFICATIONLEVELS.put(NOTIFICATIONTYPE_CONNECTIONREFUSED, "ERROR");
			DEFAULTNOTIFICATIONLEVELS.put(NOTIFICATIONTYPE_RESPONSEBODY, "ERROR");
			DEFAULTNOTIFICATIONLEVELS.put(NOTIFICATIONTYPE_RESPONSEHEADER + ".*", "WARN");
			DEFAULTNOTIFICATIONLEVELS.put(".*", "WARN");
		}
		return new LinkedHashMap<String, String>(DEFAULTNOTIFICATIONLEVELS);
	}

	protected void logResult(String type, long duration, String message) {
		if (type == null || "".equals(type)) {
			log.warn("logResult: Empty type changed to Exception");
			type = NOTIFICATIONTYPE_EXCEPTION;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		sb.append(" ");
		sb.append(duration);
		sb.append(" ");
		sb.append(getRequestHostname());
		sb.append(" ");
		sb.append(type);
		sb.append(" ");
		sb.append(message);

		for (Entry<String, String> entry : getNotificationsLevels().entrySet()) {
			if (type.matches(entry.getKey())) {
				if ("INFO".equals(entry.getValue())) {
					monitorLog.info(sb.toString());

				} else if ("WARN".equals(entry.getValue())) {
					monitorLog.warn(sb.toString());

				} else if ("ERROR".equals(entry.getValue())) {
					monitorLog.error(sb.toString());

				} else if ("IGNORE".equals(entry.getValue())) {
					monitorLog.debug(sb.toString());

				} else {
					log.warn("Unknown loglevel ?? (" + entry.getValue() + ")");
				}
				return;
			}
		}
		log.warn("No NotificationLevel found for given type: " + type);
	}
}
