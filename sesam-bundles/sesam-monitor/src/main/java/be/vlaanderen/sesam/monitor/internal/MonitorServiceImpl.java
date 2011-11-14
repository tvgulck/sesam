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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import be.vlaanderen.sesam.monitor.MonitorService;
import be.vlaanderen.sesam.monitor.MonitorTask;

@Component
@SuppressWarnings("rawtypes")
public class MonitorServiceImpl implements MonitorService {

	private static final Logger log = LoggerFactory.getLogger(MonitorServiceImpl.class);

	@Autowired
	private TaskScheduler scheduler;

	private List<ScheduledFuture> futures = new ArrayList<ScheduledFuture>();

	public void monitor(Collection<MonitorTask> tasks) {
		// -- cancel old
		for (ScheduledFuture future : futures) {
			future.cancel(false);
		}
		futures.clear();

		// -- schedule new --
		for (MonitorTask task : tasks) {
			log.info("Scheduling a new task: " + task.getName() + "  [" + task.getSchedule() + "]");
			if (task.isValid()) {
				futures.add(scheduler.schedule(task, new CronTrigger(task.getSchedule())));
			}
		}
	}
}