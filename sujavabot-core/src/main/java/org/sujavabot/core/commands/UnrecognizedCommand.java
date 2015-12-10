package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class UnrecognizedCommand extends AbstractReportingCommand implements HiddenCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("unrecognized command");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		return "Unrecognized command: " + args.get(0);
	}

}
