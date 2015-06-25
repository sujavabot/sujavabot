package org.sujavabot.core;

public interface Plugin {
	public String getName();
	
	public void initializePlugin() throws Exception;
	public void initializeBot(Bot bot) throws Exception;
}
