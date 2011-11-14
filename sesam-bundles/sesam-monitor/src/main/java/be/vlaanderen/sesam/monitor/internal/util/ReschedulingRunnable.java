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

package be.vlaanderen.sesam.monitor.internal.util;

/**
 * FIXME
 * needed for threadpooltasscheduler.
 * <p>This is a copy from org.springframework package (service not provided for OSGi)
 * <p>Check if still needed on upgrade versions.
 */

/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.ErrorHandler;

/**
 * Internal adapter that reschedules an underlying {@link Runnable} according
 * to the next execution time suggested by a given {@link Trigger}.
 *
 * <p>Necessary because a native {@link ScheduledExecutorService} supports
 * delay-driven execution only. The flexibility of the {@link Trigger} interface
 * will be translated onto a delay for the next execution time (repeatedly).
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 */
class ReschedulingRunnable extends DelegatingErrorHandlingRunnable implements ScheduledFuture<Object> {

	private final Trigger trigger;

	private final SimpleTriggerContext triggerContext = new SimpleTriggerContext();

	private final ScheduledExecutorService executor;

	private volatile ScheduledFuture currentFuture;

	private volatile Date scheduledExecutionTime;

	private final Object triggerContextMonitor = new Object();


	public ReschedulingRunnable(Runnable delegate, Trigger trigger, ScheduledExecutorService executor, ErrorHandler errorHandler) {
		super(delegate, errorHandler);
		this.trigger = trigger;
		this.executor = executor;
	}


	public ScheduledFuture schedule() {
		synchronized (this.triggerContextMonitor) {
			this.scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext);
			if (this.scheduledExecutionTime == null) {
				return null;
			}
			long initialDelay = this.scheduledExecutionTime.getTime() - System.currentTimeMillis();
			this.currentFuture = this.executor.schedule(this, initialDelay, TimeUnit.MILLISECONDS);
			return this;
		}
	}

	@Override
	public void run() {
		Date actualExecutionTime = new Date();
		super.run();
		Date completionTime = new Date();
		synchronized (this.triggerContextMonitor) {
			this.triggerContext.update(this.scheduledExecutionTime, actualExecutionTime, completionTime);
		}
		if (!this.currentFuture.isCancelled()) {
			schedule();
		}
	}


	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.currentFuture.cancel(mayInterruptIfRunning);
	}

	public boolean isCancelled() {
		return this.currentFuture.isCancelled();
	}

	public boolean isDone() {
		return this.currentFuture.isDone();
	}

	public Object get() throws InterruptedException, ExecutionException {
		return this.currentFuture.get();
	}

	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.currentFuture.get(timeout, unit);
	}

	public long getDelay(TimeUnit unit) {
		return this.currentFuture.getDelay(unit);
	}

	public int compareTo(Delayed other) {
		if (this == other) {
			return 0;
		}
		long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
		return (diff == 0 ? 0 : ((diff < 0)? -1 : 1));
	}

}
