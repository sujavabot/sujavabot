package org.sujavabot.core.listener;

import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class GreetingListener extends ListenerAdapter<PircBotX> {

	@Override
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser());
		List<AuthorizedGroup> groups = user.getAllGroups();
		List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();
		Authorization.run(bot, user, groups, ownedGroups, () -> {
			Authorization auth = Authorization.getAuthorization();
			CommandReceiverListener.exec.execute(() -> {
				auth.run(() -> {
					Command cmd = bot.getCommands().get(event, "greeting");
					if(cmd != null && cmd != bot.getCommands().get(event, "_unrecognized")) {
						bot.getCommands().perform(event, "greeting");
					}
				});
			});
		});
	}
	
}
