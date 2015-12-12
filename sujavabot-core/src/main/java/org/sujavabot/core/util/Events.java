package org.sujavabot.core.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;

public abstract class Events {
	private Events() {}
	
	private static Map<Class<?>, Method> getUserMethods = new ConcurrentHashMap<>();
	private static Map<Class<?>, Method> getChannelMethods = new ConcurrentHashMap<>();
	
	public static User getUser(Event<?> e) {
		if(!getUserMethods.containsKey(e.getClass())) {
			try {
				getUserMethods.put(e.getClass(), e.getClass().getMethod("getUser"));
			} catch(NoSuchMethodException ex) {
				getUserMethods.put(e.getClass(), null);
			}
		}
		Method m = getUserMethods.get(e.getClass());
		if(m == null)
			return null;
		try {
			return (User) m.invoke(e);
		} catch(Exception ex) {
			throw Throwables.as(RuntimeException.class, ex);
		}
	}
	
	public static Channel getChannel(Event<?> e) {
		if(!getChannelMethods.containsKey(e.getClass())) {
			try {
				getChannelMethods.put(e.getClass(), e.getClass().getMethod("getChannel"));
			} catch(NoSuchMethodException ex) {
				getChannelMethods.put(e.getClass(), null);
			}
		}
		Method m = getChannelMethods.get(e.getClass());
		if(m == null)
			return null;
		try {
			return (Channel) m.invoke(e);
		} catch(Exception ex) {
			throw Throwables.as(RuntimeException.class, ex);
		}
	}
}
