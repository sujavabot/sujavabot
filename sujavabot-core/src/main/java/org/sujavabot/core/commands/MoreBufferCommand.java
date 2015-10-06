package org.sujavabot.core.commands;

import java.util.List;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.SujavaBot;

public class MoreBufferCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		String more = null;
		if(cause instanceof MessageEvent<?>) {
			MessageEvent<?> m = (MessageEvent<?>) cause;
			more = bot.continueBuffer(m.getChannel(), m.getUser());
		}
		return more;
	}

}
