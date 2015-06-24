package org.sujavabot.core.util;

import java.lang.reflect.Constructor;

public abstract class Throwables {
	
	public static <T extends Throwable> T as(Class<T> type, String message, Throwable orig) {
		if(type.isInstance(orig))
			return type.cast(orig);
		try {
			Constructor<T> ctor = type.getConstructor(String.class, Throwable.class);
			return ctor.newInstance(message, orig);
		} catch(Exception e) {
			throw Throwables.as(RuntimeException.class, "Unable to create " + type, e);
		}
	}
	
	public static <T extends Throwable> T as(Class<T> type, Throwable orig) {
		if(type.isInstance(orig))
			return type.cast(orig);
		try {
			Constructor<T> ctor = type.getConstructor(Throwable.class);
			return ctor.newInstance(orig);
		} catch(NoSuchMethodException e) {
			return Throwables.as(type, null, orig);
		} catch(Exception e) {
			throw Throwables.as(RuntimeException.class, "Unable to create " + type, e);
		}
	}
	
	private Throwables() {}
}
