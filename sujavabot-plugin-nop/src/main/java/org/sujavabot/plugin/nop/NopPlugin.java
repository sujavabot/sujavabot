package org.sujavabot.plugin.nop;

import org.sujavabot.core.Plugin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class NopPlugin implements Plugin {

	@Override
	public Converter getConfigurableConverter(XStream x) {
		return new NopPluginConverter(x);
	}

}
