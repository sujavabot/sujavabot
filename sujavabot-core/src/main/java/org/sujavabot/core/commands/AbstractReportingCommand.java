package org.sujavabot.core.commands;

import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public abstract class AbstractReportingCommand implements Command {

	protected String prefix(User user, String result) {
		return user.getNick() + ": " + result;
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
	public void report(SujavaBot bot, Event<?> cause, String result) {
		if(result.isEmpty())
			return;
		if(cause instanceof MessageEvent<?>) {
			MessageEvent<?> m = (MessageEvent<?>) cause;
			reportMessage(bot, m, prefix(m.getUser(), result));
		} else if(cause instanceof ActionEvent<?>) {
			ActionEvent<?> a = (ActionEvent<?>) cause;
			reportAction(bot, a, prefix(a.getUser(), result));
		} else if(cause instanceof PrivateMessageEvent<?>) {
			PrivateMessageEvent<?> p = (PrivateMessageEvent<?>) cause;
			reportPrivateMessage(bot, p, prefix(p.getUser(), result));
		}
	}

}
