package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;

public class Configuration extends org.pircbotx.Configuration<PircBotX> {
	
	protected File configFile;
	protected List<File> pluginConfigs = new ArrayList<>();
	protected List<AuthorizedGroup> groups = new ArrayList<>();
	protected List<AuthorizedUser> users = new ArrayList<>();

	public Configuration(File configFile, ConfigurationBuilder builder) {
		super(builder);
		this.configFile = configFile;
		pluginConfigs.addAll(builder.getPluginConfigs());
		groups.addAll(builder.getGroups());
		users.addAll(builder.getUsers());
	}

	public List<File> getPluginConfigs() {
		return pluginConfigs;
	}
	
	public File getConfigFile() {
		return configFile;
	}
	
	public List<AuthorizedGroup> getGroups() {
		return groups;
	}
	
	public List<AuthorizedUser> getUsers() {
		return users;
	}
	
	public ConfigurationBuilder createBuilder() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		
		builder.setAutoNickChange(isAutoNickChange());
		builder.setAutoReconnect(isAutoReconnect());
		builder.setAutoSplitMessage(isAutoSplitMessage());
		builder.setBotFactory(getBotFactory());
		builder.setCapEnabled(isCapEnabled());
		builder.setChannelPrefixes(getChannelPrefixes());
		builder.setDccAcceptTimeout(getDccAcceptTimeout());
		builder.setDccFilenameQuotes(isDccFilenameQuotes());
		builder.setDccLocalAddress(getDccLocalAddress());
		builder.setDccPassiveRequest(isDccPassiveRequest());
		builder.setDccPorts(getDccPorts());
		builder.setDccResumeAcceptTimeout(getDccResumeAcceptTimeout());
		builder.setDccTransferBufferSize(getDccTransferBufferSize());
		builder.setEncoding(getEncoding());
		builder.setFinger(getFinger());
		builder.setIdentServerEnabled(isIdentServerEnabled());
		builder.setLocalAddress(getLocalAddress());
		builder.setLocale(getLocale());
		builder.setLogin(getLogin());
		builder.setMaxLineLength(getMaxLineLength());
		builder.setMessageDelay(getMessageDelay());
		builder.setName(getName());
		builder.setNickservPassword(getNickservPassword());
		builder.setPluginConfigs(getPluginConfigs());
		builder.setRealName(getRealName());
		builder.setServerHostname(getServerHostname());
		builder.setServerPassword(getServerPassword());
		builder.setServerPort(getServerPort());
		builder.setShutdownHookEnabled(isShutdownHookEnabled());
		builder.setSocketFactory(getSocketFactory());
		builder.setSocketTimeout(getSocketTimeout());
		builder.setVersion(getVersion());
		builder.setWebIrcAddress(getWebIrcAddress());
		builder.setWebIrcEnabled(isWebIrcEnabled());
		builder.setWebIrcHostname(getWebIrcHostname());
		builder.setWebIrcPassword(getWebIrcPassword());
		builder.setWebIrcUsername(getWebIrcUsername());
		
		builder.setGroups(getGroups());
		builder.setUsers(getUsers());
		
		return builder;
	}
}
