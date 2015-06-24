package org.sujavabot.plugin.nop;

import org.sujavabot.core.Bot;
import org.sujavabot.core.Plugin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class NopPlugin implements Plugin {

	@Override
	public String getName() {
		return "nop";
	}
	
	@Override
	public Converter getConfigurableConverter(XStream x) {
		return new NopPluginConverter(x);
	}

	@Override
	public void initializePlugin() {
	}

	@Override
	public void initializeBot(Bot bot) {
	}

}
