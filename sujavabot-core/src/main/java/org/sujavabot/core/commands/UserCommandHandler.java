package org.sujavabot.core.commands;

import org.pircbotx.Channel;
import org.pircbotx.User;
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
		for(AuthorizedGroup group : user.getGroups()) {
			Command c = group.getCommands().get(cause, name);
			if(c != null)
				return c;
		}
		Channel channel = getChannel(cause);
		SujavaBot bot = (SujavaBot) cause.getBot();
		if(channel != null) {
			AuthorizedGroup cgroup = bot.getAuthorizedGroups().get(channel.getName());
			if(cgroup != null) {
				Command c = cgroup.getCommands().get(cause, name);
				if(c != null)
					return c;
			}
			User user = getUser(cause);
			if(user != null) {
				AuthorizedGroup cugroup = bot.getAuthorizedGroups().get(channel.getName() + ":" + user.getNick());
				if(cugroup != null) {
					Command c = cugroup.getCommands().get(cause, name);
					if(c != null)
						return c;
				}
			}
		}
		if(!"@nobody".equals(user.getName())) {
			AuthorizedUser nobody = bot.getAuthorizedUsers().get("@nobody");
			if(nobody != null) {
				Command c = nobody.getCommands().get(cause, name);
				if(c != null)
					return c;
			}
		}
		return null;
	}


}
