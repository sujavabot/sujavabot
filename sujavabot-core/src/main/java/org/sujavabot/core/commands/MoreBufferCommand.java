package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.SujavaBot;

public class MoreBufferCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("show the next buffered message");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		String more = null;
		if(cause instanceof MessageEvent<?>) {
			MessageEvent<?> m = (MessageEvent<?>) cause;
			more = bot.getOutputBuffers().get(m.getChannel()).get(m.getUser());
		}
		return more;
	}

}
