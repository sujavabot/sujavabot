package org.sujavabot.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;

public class Configuration extends org.pircbotx.Configuration<PircBotX> {
	
	protected File configFile;
	protected List<Plugin> plugins = new ArrayList<>();

	public Configuration(File configFile, ConfigurationBuilder builder) {
		super(builder);
		this.configFile = configFile;
		plugins.addAll(builder.getPlugins());
	}
	
	public List<Plugin> getPlugins() {
		return plugins;
	}
	
	public File getConfigFile() {
		return configFile;
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
		builder.setPlugins(getPlugins());
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
		
		return builder;
	}
}
