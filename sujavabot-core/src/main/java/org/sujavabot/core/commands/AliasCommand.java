package org.sujavabot.core.commands;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class AliasCommand extends AbstractReportingCommand {
	protected static final Pattern SUB = Pattern.compile("\\$([0-9]+)");

	protected String alias;
	
	public AliasCommand(String alias) {
		this.alias = alias;
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		StringBuilder sb = new StringBuilder();
		int end = 0;
		Matcher m = SUB.matcher(alias);
		while(m.find()) {
			sb.append(alias.substring(end, m.start()));
			int i = Integer.parseInt(m.group(1));
			if(i < args.size())
				sb.append(args.get(i));
			end = m.end();
		}
		sb.append(alias.substring(end, alias.length()));
		return bot.getCommands().invoke(cause, sb.toString());
	}

}
