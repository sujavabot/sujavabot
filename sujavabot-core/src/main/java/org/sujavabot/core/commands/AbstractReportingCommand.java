package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public abstract class AbstractReportingCommand implements Command {

	@Override
	public void report(SujavaBot bot, Event<?> cause, String result) {
		if(cause instanceof MessageEvent<?>) {
			MessageEvent<?> m = (MessageEvent<?>) cause;
			m.getChannel().send().message(m.getUser(), result);
		} else if(cause instanceof ActionEvent<?>) {
			ActionEvent<?> a = (ActionEvent<?>) cause;
			a.getChannel().send().message(a.getUser(), result);
		} else if(cause instanceof PrivateMessageEvent<?>) {
			PrivateMessageEvent<?> p = (PrivateMessageEvent<?>) cause;
			p.getUser().send().message(result);
		}
	}

}
