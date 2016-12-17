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
	
	public static String message(Throwable t) {
		String m = t.getMessage();
		while(t.getCause() != null) {
			t = t.getCause();
			if(t.getMessage() != null)
				m = t.getMessage();
		}
		if(m == null)
			m = t.getClass().getSimpleName();
		return m;
	}
	
	private Throwables() {}
}
