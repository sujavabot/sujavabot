package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.Command;

public class RootCommandHandler extends GroupCommandHandler {

	protected Command unrecognized = new UnrecognizedCommand();
	
	public RootCommandHandler(AuthorizedGroup root) {
		super(root);
		commands.put(".unrecognized", new UnrecognizedCommand());
		commands.put(".parse.error", new ParseErrorCommand());
		
		commands.put("action", new ActionCommand());
		commands.put("echo", new EchoCommand());
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		Command c = super.getDefaultCommand(cause, name);
		if(c == null && !".unrecognized".equals(name))
			c = get(cause, ".unrecognized");
		return c;
	}

}
