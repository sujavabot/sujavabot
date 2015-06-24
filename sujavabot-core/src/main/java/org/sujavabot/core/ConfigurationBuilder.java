package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationBuilder extends org.pircbotx.Configuration.Builder<Bot> {

	protected List<Plugin> plugins = new ArrayList<>();

	@Override
	public Configuration buildConfiguration() {
		return new Configuration(this);
	}

	@Override
	public Configuration buildForServer(String hostname) {
		return (Configuration) super.buildForServer(hostname);
	}

	@Override
	public Configuration buildForServer(String hostname, int port) {
		return (Configuration) super.buildForServer(hostname, port);
	}

	@Override
	public Configuration buildForServer(String hostname, int port, String password) {
		return (Configuration) super.buildForServer(hostname, port, password);
	}
	
	public List<Plugin> getPlugins() {
		return plugins;
	}
	
	public void setPlugins(List<Plugin> pluginClasses) {
		this.plugins = pluginClasses;
	}
	
	public void addPlugin(Plugin pluginClass) {
		plugins.add(pluginClass);
	}
}