package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;

public class ConfigurationBuilder extends org.pircbotx.Configuration.Builder<PircBotX> {

	protected List<Plugin> plugins = new ArrayList<>();

	public Configuration buildConfiguration(File configFile) {
		return new Configuration(configFile, this);
	}
	
	@Override
	public Configuration buildConfiguration() {
		throw new UnsupportedOperationException("use buildConfiguration(File)");
	}

	public List<Plugin> getPlugins() {
		return plugins;
	}
	
	public void setPlugins(List<Plugin> plugins) {
		this.plugins = plugins;
	}
	
	public void addPlugin(Plugin plugin) {
		plugins.add(plugin);
	}
}