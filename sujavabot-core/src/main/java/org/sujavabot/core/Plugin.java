package org.sujavabot.core;

public interface Plugin {
	public String getName();
	
	public void load(SujavaBot bot) throws Exception;
	public void unload(SujavaBot bot) throws Exception;
}
