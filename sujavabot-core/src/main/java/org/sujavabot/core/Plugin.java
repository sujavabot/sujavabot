package org.sujavabot.core;

public interface Plugin extends Configurable {
	public default void initializePlugin() {
	}
	
	public default void initializeBot(Bot bot) {
	}
}
