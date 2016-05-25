package org.sujavabot.core.listener;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class CommandReceiverListener extends ListenerAdapter<PircBotX>
implements HelperConvertable<CommandReceiverListener> {
	public static final Executor exec = Executors.newCachedThreadPool();
	
	protected String prefix;
	
	public CommandReceiverListener(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(!event.getMessage().startsWith(prefix))
			return;
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser(), true);
		List<AuthorizedGroup> groups = user.getAllGroups();
		List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();
		Authorization.run(bot, user, groups, ownedGroups, () -> {
			Authorization auth = Authorization.getAuthorization();
			exec.execute(() -> {
				auth.run(() -> {
					String m = event.getMessage();
					m = m.substring(prefix.length());
					try {
						bot.getCommands().perform(event, m);
					} catch(Exception e) {
						bot.getCommands().perform(event, "_exception " + e);
					}
				});
			});
		});
	}
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
		if(!event.getMessage().startsWith(prefix))
			return;
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser(), true);
		List<AuthorizedGroup> groups = user.getAllGroups();
		List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();
		Authorization.run(bot, user, groups, ownedGroups, () -> {
			Authorization auth = Authorization.getAuthorization();
			exec.execute(() -> {
				auth.run(() -> {
					String m = event.getMessage();
					m = m.substring(prefix.length());
					bot.getCommands().perform(event, m);
				});
			});
		});
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void configure(MarshalHelper helper, CommandReceiverListener defaults) {
		helper.field("prefix", String.class, () -> getPrefix());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("prefix", String.class, s -> setPrefix(s));
	}
}
