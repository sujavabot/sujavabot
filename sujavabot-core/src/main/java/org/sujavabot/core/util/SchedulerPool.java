package org.sujavabot.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.sujavabot.core.Authorization;

public class SchedulerPool {
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
