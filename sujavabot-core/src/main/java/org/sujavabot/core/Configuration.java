package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;

public class Configuration extends org.pircbotx.Configuration<PircBotX> {
	
	protected File configFile;
	protected List<File> pluginConfigs = new ArrayList<>();
	protected Map<String, AuthorizedGroup> groups = new LinkedHashMap<>();
	protected Map<String, AuthorizedUser> users = new LinkedHashMap<>();
	protected List<Listener<?>> botListeners = new ArrayList<>();

	public Configuration(File configFile, ConfigurationBuilder builder) {
		super(builder);
		this.configFile = configFile;
		pluginConfigs.addAll(builder.getPluginConfigs());
		groups.putAll(builder.getGroups());
		users.putAll(builder.getUsers());
		botListeners.addAll(builder.getBotListeners());
	}

	public List<File> getPluginConfigs() {
		return pluginConfigs;
	}
	
	public File getConfigFile() {
		return configFile;
	}
	
	public Map<String, AuthorizedGroup> getGroups() {
		return groups;
	}
	
	public Map<String, AuthorizedUser> getUsers() {
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
		
		builder.getBotListeners().addAll(botListeners);
		
		return builder;
	}
}
