package org.sujavabot.core.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sujavabot.core.Authorization;

public class SchedulerPool {
	private static ScheduledExecutorService instance = Executors.newSingleThreadScheduledExecutor();
	
	private static Map<Object, ScheduledExecutorService> instances = new HashMap<>();
	
	public static ScheduledExecutorService get() {
		return get(Authorization.getCurrentUser());
	}
	
	public static synchronized ScheduledExecutorService get(Object sync) {
		ScheduledExecutorService ret = instances.get(sync);
		if(ret == null)
			instances.put(sync, ret = Executors.newSingleThreadScheduledExecutor());
		return ret;
	}
	
}
