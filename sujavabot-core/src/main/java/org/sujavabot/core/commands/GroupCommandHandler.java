package org.sujavabot.core.commands;

import java.util.List;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class GroupCommandHandler extends AbstractCommandHandler {
	protected AuthorizedGroup group;
	
	public GroupCommandHandler(AuthorizedGroup group) {
		super(null);
		this.group = group;
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		for(AuthorizedGroup subgroup : group.getSubgroups()) {
			Command c = subgroup.getCommands().get(cause, name);
			if(c != null)
				return c;
		}
		return null;
	}

}
