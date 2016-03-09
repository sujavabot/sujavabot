package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class NopCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("does nothing");
	}
	
	@Override
	public void report(SujavaBot bot, Event<?> cause, String result) {
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		return null;
	}

}
