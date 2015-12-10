package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class HelpCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<command> [<topic>]: show help");
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() == 1)
			return help(null);
		String command = args.get(1);
		String topic = StringUtils.join(args.subList(2, args.size()), " ");
		return command + (topic.isEmpty() ? "" : " " + topic) + ": " + bot.getCommands().get(cause, command).help(topic);
	}

}
