package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;

public class Configuration extends org.pircbotx.Configuration<Bot> {
	
	protected List<Plugin> plugins = new ArrayList<>();

	protected Configuration(ConfigurationBuilder builder) {
		super(builder);
		plugins.addAll(builder.getPlugins());
	}
	
	public List<Plugin> getPlugins() {
		return plugins;
	}
}
