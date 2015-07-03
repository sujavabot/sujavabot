package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class ParseErrorCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, String[] args) {
		return "Parse error for input: " + args[args.length-1];
	}

}
