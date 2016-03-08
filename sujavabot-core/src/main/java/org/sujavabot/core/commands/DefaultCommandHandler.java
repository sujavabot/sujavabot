package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Events;

public class DefaultCommandHandler extends AbstractCommandHandler {

	public DefaultCommandHandler(SujavaBot bot) {
		super(bot);
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		AuthorizedUser user = Authorization.getCurrentUser();
		Command c = null;
		if(user != null)
			c = user.getAllCommands().get(name);
		if(c == null)
			c = bot.getRootCommands().get(cause, name);
		if(c == null && !"_unrecognized".equals(name))
			c = get(cause, "_unrecognized");
		return c;
	}

}
