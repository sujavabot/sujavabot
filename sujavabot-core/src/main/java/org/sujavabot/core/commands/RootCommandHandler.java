package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class RootCommandHandler extends AbstractCommandHandler {

	protected Command unrecognized = new UnrecognizedCommand();
	
	public RootCommandHandler(SujavaBot bot) {
		super(bot);
		commands.put("__parse_error", new ParseErrorCommand());
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		return unrecognized;
	}

}
