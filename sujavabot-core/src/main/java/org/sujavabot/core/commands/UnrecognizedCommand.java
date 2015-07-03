package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class UnrecognizedCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, String[] args) {
		return "Unrecognized command: " + args[0];
	}

}
