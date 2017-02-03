package org.sujavabot.plugin.urlhandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;
import org.sujavabot.plugin.urlhandler.AddressRanges.AddressRange;

public class URLTitleListener extends ListenerAdapter<PircBotX> implements HelperConvertable<URLTitleListener> {

	private List<AddressRange> whitelist;
	
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
						title = URLs.title(url, whitelist);
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

	@Override
	public void configure(MarshalHelper helper, URLTitleListener defaults) {
		for(AddressRange r : whitelist)
			helper.field("allow", String.class, () -> r.toString());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		whitelist = new ArrayList<>();
		helper.field("allow", String.class, (s) -> whitelist.add(AddressRanges.parseAddressRange(s)));
	}
	
}
