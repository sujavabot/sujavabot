package org.sujavabot.core.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Events;
import org.sujavabot.core.util.Messages;

public abstract class AbstractReportingCommand implements Command {
	
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
		return Messages.sanitize(result);
	}
	
	protected String prefix(SujavaBot bot, Event<?> cause, String result) {
		if(Events.getChannel(cause) == null)
			return "";
		return Events.getUser(cause).getNick() + ": ";
	}

	protected void reportMessage(SujavaBot bot, Event<?> cause, String result, boolean isChannelMessage) {
		User user = Events.getUser(cause);
		Channel channel = Events.getChannel(cause);
		if(isChannelMessage) {
			result = Messages.sanitize(result);
			String msg = bot.buffer(channel, user, prefix(bot, cause, result), result);
			channel.send().message(msg);
		} else {
			String p = prefix(bot, cause, result);
			for(String msg : result.split("[\r\n]+")) {
				msg = Messages.sanitize(msg);
				String[] sb = Messages.splitPM(bot, user.getNick(), p + msg);
				while(true) {
					user.send().message(sb[0]);
					if(sb[1] == null)
						break;
					sb = Messages.splitPM(bot, user.getNick(), p + sb[1]);
				}
			}
		}
	}
	
	protected void reportAction(SujavaBot bot, Event<?> cause, String result, boolean isChannelAction) {
		User user = Events.getUser(cause);
		Channel channel = Events.getChannel(cause);
		int maxlen = Messages.maxlenAction(bot, isChannelAction ? channel.getName() : user.getNick());
		if(result.length() > maxlen)
			result = result.substring(0, maxlen);
		if(isChannelAction)
			channel.send().action(result);
		else
			user.send().action(result);
	}
	
	@Override
	public void init(SujavaBot bot) {
	}
	
	protected abstract Map<String, String> helpTopics();
	
	protected String invokeHelp(SujavaBot bot, Event<?> cause, List<String> args) {
		return invokeHelp(bot, cause, args, null);
	}
	
	protected String invokeHelp(SujavaBot bot, Event<?> cause, List<String> args, String topic) {
		return new HelpCommand().invoke(bot, cause, Arrays.asList("help", args.get(0), topic));
	}
	
	@Override
	public String help(String helpwith) {
		Map<String, String> helpTopics = helpTopics();
		String help = helpTopics.get(helpwith);
		return (help != null ? help : helpTopics.get(null));
	}
	
	@Override
	public void report(SujavaBot bot, Event<?> cause, String result) {
		if(result == null)
			return;
		if(cause instanceof MessageEvent<?>) {
			reportMessage(bot, cause, result, true);
		} else if(cause instanceof ActionEvent<?>) {
			reportAction(bot, cause, result, Events.getChannel(cause) != null);
		} else if(cause instanceof PrivateMessageEvent<?>) {
			reportMessage(bot, cause, result, false);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
