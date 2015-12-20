package org.sujavabot.core.util;

import java.lang.reflect.Constructor;

public abstract class Throwables {
	
	public static <T extends Throwable> T as(Class<T> type, Throwable t) {
		if(type.isInstance(t))
			return type.cast(t);
		try {
			Constructor<T> ctor = type.getConstructor(Throwable.class);
			return ctor.newInstance(t);
		} catch(Exception e) {
			throw new Error(e);
		}
	}
	
	private Throwables() {}
}
