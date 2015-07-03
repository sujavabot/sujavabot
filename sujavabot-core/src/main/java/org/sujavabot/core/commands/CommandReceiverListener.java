package org.sujavabot.core.commands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.SujavaBot;

public class CommandReceiverListener extends ListenerAdapter<PircBotX> {
	protected String prefix;
	
	public CommandReceiverListener(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		String m = event.getMessage();
		if(!m.startsWith(prefix))
			return;
		m = m.substring(prefix.length());
		((SujavaBot) event.getBot()).getCommands().perform(event, m);
	}
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
		String m = event.getMessage();
		if(!m.startsWith(prefix))
			return;
		m = m.substring(prefix.length());
		((SujavaBot) event.getBot()).getCommands().perform(event, m);
	}
}
