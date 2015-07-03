package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.sujavabot.core.commands.RootCommandHandler;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;

public class ConfigurationBuilder extends org.pircbotx.Configuration.Builder<PircBotX> {

	protected List<File> pluginConfigs = new ArrayList<>();
	
	protected List<AuthorizedGroup> groups = new ArrayList<>();
	
	protected List<AuthorizedUser> users = new ArrayList<>();

	public ConfigurationBuilder() {
		XStream x = XStreams.configure(new XStream());
		x.alias("group", AuthorizedGroup.class);
		AuthorizedGroup root = (AuthorizedGroup) x.fromXML(ConfigurationBuilder.class.getResource("root-group.xml"));
		groups.add(root);
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
	
	public List<AuthorizedGroup> getGroups() {
		return groups;
	}
	
	public List<AuthorizedUser> getUsers() {
		return users;
	}
}