package org.sujavabot.core.commands;

import java.lang.reflect.Method;

import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class DefaultCommandHandler extends AbstractCommandHandler {

	public DefaultCommandHandler(SujavaBot bot) {
		super(bot);
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		AuthorizedUser user;
		try {
			Method getUser = cause.getClass().getMethod("getUser");
			user = bot.getAuthorizedUser((User) getUser.invoke(cause));
		} catch(Exception e) {
			user = null;
		}
		Command c = null;
		if(user != null)
			c = user.getCommands().get(cause, name);
		if(c == null)
			c = bot.getRootCommands().get(cause, name);
		return c;
	}

}
