package org.sujavabot.core;

import java.util.List;

import org.pircbotx.hooks.Event;

public interface Command {
	public void init(SujavaBot bot);
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args);
	public void report(SujavaBot bot, Event<?> cause, String result);
}
