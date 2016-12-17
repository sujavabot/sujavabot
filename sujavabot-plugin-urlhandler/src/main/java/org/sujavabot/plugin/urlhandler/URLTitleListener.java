package org.sujavabot.plugin.urlhandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.listener.CommandReceiverListener;
import org.sujavabot.core.util.Throwables;

public class URLTitleListener extends ListenerAdapter<PircBotX> {

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser(), true);
		List<AuthorizedGroup> groups = user.getAllGroups();
		List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();

		for(String word : event.getMessage().split("\\s+")) {
			URL url;
			try {
				url = new URL(word);
			} catch(MalformedURLException e) {
				continue;
			}
			Authorization.run(bot, user, groups, ownedGroups, () -> {
				CommandReceiverListener.run(bot, () -> {
					String title;
					try {
						title = URLs.title(url);
						if(title == null)
							return;
						event.getChannel().send().message(title + " - " + url);
					} catch(Exception e) {
						bot.getCommands().perform(event, "_exception " + Throwables.message(e));
					}
				});
			});
		}
	}
	
}
