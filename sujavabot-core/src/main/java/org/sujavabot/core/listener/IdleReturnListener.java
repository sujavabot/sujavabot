package org.sujavabot.core.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Events;
import org.sujavabot.core.xml.HelperConvertable;

public class IdleReturnListener extends ListenerAdapter<PircBotX> implements HelperConvertable<IdleReturnListener> {

	protected long timeout = 0;
	protected String command = null;
	
	protected transient Map<AuthorizedUser, Long> lastActive = new ConcurrentHashMap<>();
	
	protected void fire(Event<PircBotX> event, boolean force) {
		if (lastActive == null) lastActive = new HashMap<>();
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(Events.getUser(event), false);
		long now = System.currentTimeMillis();
		long lastActive = this.lastActive.getOrDefault(user, Long.MIN_VALUE);
		if (force || lastActive + timeout < now) {
			this.lastActive.put(user, now);
			Authorization.run(bot, user, user.getAllGroups(), user.getOwnedGroups(), () -> {
				CommandReceiverListener.run(bot, () -> {
					bot.getCommands().perform(event, command);
				});
			});
		}
	}
	
	@Override
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		fire(event, true);
	}
	
	@Override
	public void onAction(ActionEvent<PircBotX> event) throws Exception {
		fire(event, false);
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		fire(event, false);
	}
	
	@Override
	public void configure(MarshalHelper helper, IdleReturnListener defaults) {
		helper.field("timeout", Long.class, () -> timeout);
		helper.field("command", String.class, () -> command);
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("timeout", Long.class, (l) -> timeout = l);
		helper.field("command", String.class, (s) -> command = s);
	}
}
