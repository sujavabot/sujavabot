package org.sujavabot.core.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;

public abstract class Events {
	private Events() {}
	
	private static final Object NONE = new Object();
	
	private static final Map<Class<?>, Object> getUserMethods = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Object> getChannelMethods = new ConcurrentHashMap<>();
	
	private static ThreadLocal<User> overrides = new ThreadLocal<>();
	
	public static void overrideUser(User u, Runnable task) {
		User prev = overrides.get();
		try {
			overrides.set(u);
			task.run();
		} finally {
			overrides.set(prev);
		}
	}
	
	public static User getUser(Event<?> e) {
		if(!getUserMethods.containsKey(e.getClass())) {
			try {
				getUserMethods.put(e.getClass(), e.getClass().getMethod("getUser"));
			} catch(NoSuchMethodException ex) {
				getUserMethods.put(e.getClass(), NONE);
			}
		}
		Object m = getUserMethods.get(e.getClass());
		if(m == NONE)
			return null;
		try {
			User u = overrides.get();
			if(u == null)
				u = (User) ((Method) m).invoke(e);
			return u;
		} catch(Exception ex) {
			throw Throwables.as(RuntimeException.class, ex);
		}
	}
	
	public static Channel getChannel(Event<?> e) {
		if(!getChannelMethods.containsKey(e.getClass())) {
			try {
				getChannelMethods.put(e.getClass(), e.getClass().getMethod("getChannel"));
			} catch(NoSuchMethodException ex) {
				getChannelMethods.put(e.getClass(), NONE);
			}
		}
		Object m = getChannelMethods.get(e.getClass());
		if(m == NONE)
			return null;
		try {
			return (Channel) ((Method) m).invoke(e);
		} catch(Exception ex) {
			throw Throwables.as(RuntimeException.class, ex);
		}
	}
}
