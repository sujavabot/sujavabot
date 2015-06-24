package org.sujavabot.core;

import com.thoughtworks.xstream.converters.Converter;

public interface Configurable {
	public Converter getConfigurableConverter();
}
