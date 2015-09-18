package org.sujavabot.core.commands;

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
	
	@Override
	public void report(SujavaBot bot, Event<?> cause, String result) {
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

}
