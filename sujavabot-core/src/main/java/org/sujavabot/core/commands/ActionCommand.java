package org.sujavabot.core.commands;

import java.util.List;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.SujavaBot;

public class ActionCommand extends AbstractReportingCommand {

	@Override
	protected void reportMessage(SujavaBot bot, MessageEvent<?> cause, String result) {
		cause.getChannel().send().action(result);
	}
	
	@Override
	protected void reportAction(SujavaBot bot, ActionEvent<?> cause, String result) {
		cause.getChannel().send().action(result);
	}
	
	@Override
	protected void reportPrivateMessage(SujavaBot bot, PrivateMessageEvent<?> cause, String result) {
		cause.getUser().send().action(result);
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
