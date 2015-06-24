package org.sujavabot.core.util;

import java.lang.reflect.Constructor;

public abstract class Throwables {
	
	public static <T extends Throwable> T as(Class<T> type, String message, Throwable t) {
		if(type.isInstance(t))
			return type.cast(t);
		try {
			Constructor<T> ctor = type.getConstructor(String.class, Throwable.class);
			return ctor.newInstance(message, t);
		} catch(Exception e) {
			throw Throwables.as(RuntimeException.class, "Unable to create " + type, e);
		}
	}
	
	public static <T extends Throwable> T as(Class<T> type, Throwable t) {
		if(type.isInstance(t))
			return type.cast(t);
		try {
			Constructor<T> ctor = type.getConstructor(Throwable.class);
			return ctor.newInstance(t);
		} catch(NoSuchMethodException e) {
			return Throwables.as(type, null, t);
		} catch(Exception e) {
			throw Throwables.as(RuntimeException.class, "Unable to create " + type, e);
		}
	}
	
	private Throwables() {}
}
