package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;

public class ConfigurationBuilder extends org.pircbotx.Configuration.Builder<PircBotX> {
	
	public static ConfigurationBuilder createDefault() {
		XStream x = XStreams.configure(new XStream());
		return (ConfigurationBuilder) x.fromXML(ConfigurationBuilder.class.getResource("default-configuration.xml"));
	}

	protected List<File> pluginConfigs = new ArrayList<>();
	
	protected Map<String, AuthorizedGroup> groups = new LinkedHashMap<>();
	
	protected Map<String, AuthorizedUser> users = new LinkedHashMap<>();
	
	protected List<Listener<?>> botListeners = new ArrayList<>();

	public ConfigurationBuilder() {
	}
	
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
	
	public Map<String, AuthorizedGroup> getGroups() {
		return groups;
	}
	
	public Map<String, AuthorizedUser> getUsers() {
		return users;
	}

	public void setGroups(Map<String, AuthorizedGroup> groups) {
		this.groups = groups;
	}

	public void setUsers(Map<String, AuthorizedUser> users) {
		this.users = users;
	}

	public List<Listener<?>> getBotListeners() {
		return botListeners;
	}
}