package org.sujavabot.core;

public interface Plugin extends Configurable {
	public String getName();
	
	public void initializePlugin() throws Exception;
	public void initializeBot(Bot bot) throws Exception;
}
