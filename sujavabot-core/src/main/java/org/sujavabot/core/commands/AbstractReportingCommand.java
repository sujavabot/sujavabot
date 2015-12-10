package org.sujavabot.core.commands;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public abstract class AbstractReportingCommand implements Command {
	protected static User getUser(Event<?> event) {
		try {
			return (User) event.getClass().getMethod("getUser").invoke(event);
		} catch(Exception e) {
			return null;
		}
	}
	
	protected static Channel getChannel(Event<?> event) {
		try {
			return (Channel) event.getClass().getMethod("getChannel").invoke(event);
		} catch(Exception e) {
			return null;
		}
	}
	
	protected static Map<String, String> buildHelp(String defaultHelp, String... specificHelp) {
		Map<String, String> helpTopics = new HashMap<>();
		for(int i = 0; i < specificHelp.length - 1; i += 2) {
			helpTopics.put(specificHelp[i], specificHelp[i+1]);
			defaultHelp += (i == 0 ? " (topics: " : ", ") + specificHelp[i];
		}
		if(specificHelp.length > 0)
			defaultHelp += ")";
		helpTopics.put(null, defaultHelp);
		return helpTopics;
	}
	
	protected String sanitize(String result) {
		return (result == null ? null : result.replaceAll("[\r\n]", " ").replaceAll("\\s+", " ").trim());
	}

	protected void reportMessage(SujavaBot bot, MessageEvent<?> cause, String result) {
		cause.getChannel().send().message(bot.buffer(cause.getChannel(), cause.getUser(), result));
	}
	
	protected void reportAction(SujavaBot bot, ActionEvent<?> cause, String result) {
		cause.getChannel().send().message(result);
	}
	
	protected void reportPrivateMessage(SujavaBot bot, PrivateMessageEvent<?> cause, String result) {
		cause.getUser().send().message(result);
	}
	
	@Override
	public void init(SujavaBot bot) {
	}
	
	protected abstract Map<String, String> helpTopics();
	
	@Override
	public String help(String helpwith) {
		Map<String, String> helpTopics = helpTopics();
		String help = helpTopics.get(helpwith);
		return (help != null ? help : helpTopics.get(null));
	}
	
	@Override
	public void report(SujavaBot bot, Event<?> cause, String result) {
		result = sanitize(result);
		if(result == null)
			return;
		if(cause instanceof MessageEvent<?>) {
			MessageEvent<?> m = (MessageEvent<?>) cause;
			reportMessage(bot, m, result);
		} else if(cause instanceof ActionEvent<?>) {
			ActionEvent<?> a = (ActionEvent<?>) cause;
			reportAction(bot, a, result);
		} else if(cause instanceof PrivateMessageEvent<?>) {
			PrivateMessageEvent<?> p = (PrivateMessageEvent<?>) cause;
			reportPrivateMessage(bot, p, result);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
