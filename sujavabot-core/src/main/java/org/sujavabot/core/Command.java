package org.sujavabot.core;

import org.pircbotx.hooks.Event;

public interface Command {
	public String invoke(SujavaBot bot, Event<?> cause, String[] args);
	public void report(SujavaBot bot, Event<?> cause, String result);
}
