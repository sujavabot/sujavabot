package org.sujavabot.core.listener;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class GreetingListener extends ListenerAdapter<PircBotX> {

	@Override
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		SujavaBot bot = (SujavaBot) event.getBot();
		Command cmd = bot.getCommands().get(event, "@greeting");
		if(cmd != null) {
			bot.getCommands().perform(event, "@greeting");
		}
	}
	
}
