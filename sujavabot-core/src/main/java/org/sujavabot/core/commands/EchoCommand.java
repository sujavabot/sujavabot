package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.SujavaBot;

public class EchoCommand extends AbstractReportingCommand {
	
	@Override
	protected String prefix(SujavaBot bot, Event<?> cause, String result) {
		return "";
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("respond with text");
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
