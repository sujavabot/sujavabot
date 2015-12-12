package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class ActionCommand extends AbstractReportingCommand {

	@Override
	protected void reportMessage(SujavaBot bot, Event<?> cause, String result, boolean isChannelMessage) {
		reportAction(bot, cause, result, isChannelMessage);
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("perform an action");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for(String arg : args.subList(1, args.size())) {
			sb.append(sep);
			sb.append(arg);
			sep = " ";
		}
		return sb.toString();
	}
	
}
