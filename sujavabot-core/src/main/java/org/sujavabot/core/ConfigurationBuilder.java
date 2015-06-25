package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;

public class ConfigurationBuilder extends org.pircbotx.Configuration.Builder<PircBotX> {

	protected List<File> pluginConfigs = new ArrayList<>();

	public Configuration buildConfiguration(File configFile) {
		return new Configuration(configFile, this);
	}
	
	@Override
	public Configuration buildConfiguration() {
		throw new UnsupportedOperationException("use buildConfiguration(File)");
	}

	public List<File> getPluginConfigs() {
		return pluginConfigs;
	}
	
	public void setPluginConfigs(List<File> pluginConfigs) {
		this.pluginConfigs = pluginConfigs;
	}
	
	public void addPluginConfig(File pluginConfig) {
		pluginConfigs.add(pluginConfig);
	}
}