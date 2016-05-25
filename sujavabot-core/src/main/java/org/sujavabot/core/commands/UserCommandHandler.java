package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class UserCommandHandler extends AbstractCommandHandler {
	protected AuthorizedUser user;
	
	public UserCommandHandler(AuthorizedUser user) {
		super(null);
		this.user = user;
	}

	@Override
	public Command getDefaultCommand(Event<?> cause, String name) {
		SujavaBot bot = (SujavaBot) cause.getBot();
		Command c = user.getAllCommands().get(name);
		if(c != null)
			return c;
		List<AuthorizedGroup> groups = new ArrayList<>();
		groups.addAll(user.getGroups());
		Channel channel = getChannel(cause);
		if(channel != null) {
			AuthorizedGroup cgroup = bot.getAuthorizedGroupByName(channel.getName());
			if(cgroup != null)
				groups.add(0, cgroup);
			AuthorizedGroup cugroup = bot.getAuthorizedGroupByName(channel.getName() + ":" + user.getName());
			if(cugroup != null)
				groups.add(0, cugroup);
		}
		for(AuthorizedGroup group : groups) {
			c = group.getAllCommands().get(name);
			if(c != null)
				return c;
		}
		return null;
	}


}
