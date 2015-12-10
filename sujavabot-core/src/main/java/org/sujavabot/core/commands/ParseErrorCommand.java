package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class ParseErrorCommand extends AbstractReportingCommand implements HiddenCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("parse error");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		return "Parse error for input: " + args.get(1);
	}

}
