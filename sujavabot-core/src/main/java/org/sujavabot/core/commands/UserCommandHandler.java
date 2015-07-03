package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;

public class UserCommandHandler extends AbstractCommandHandler {
	protected AuthorizedUser user;
	
	public UserCommandHandler(AuthorizedUser user) {
		super(null);
		this.user = user;
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		for(AuthorizedGroup group : user.getGroups()) {
			Command c = group.getCommands().get(cause, name);
			if(c != null)
				return c;
		}
		return null;
	}


}
