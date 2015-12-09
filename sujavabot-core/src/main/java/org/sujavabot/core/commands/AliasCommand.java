package org.sujavabot.core.commands;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class AliasCommand extends AbstractReportingCommand {
	protected static final Pattern SUB = Pattern.compile("\\$(@|([0-9]+))");

	protected String alias;

	protected transient Command reporter;
	
	public AliasCommand(String alias) {
		this.alias = alias;
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		StringBuilder joined = new StringBuilder();
		for(String arg : args.subList(1, args.size())) {
			if(joined.length() > 0)
				joined.append(" ");
			joined.append(arg);
		}
		StringBuilder sb = new StringBuilder();
		int end = 0;
		Matcher m = SUB.matcher(alias);
		while(m.find()) {
			sb.append(alias.substring(end, m.start()));
			if("@".equals(m.group(1))) {
				sb.append(joined.toString());
			} else {
				int i = Integer.parseInt(m.group(1));
				if(i < args.size())
					sb.append(args.get(i));
			}
			end = m.end();
		}
		sb.append(alias.substring(end, alias.length()));
		Object[] parsed = bot.getCommands().parse(sb.toString());
		String[] flat = flatten(bot, cause, parsed);
		if(flat.length > 0) {
			Command reporter = bot.getCommands().get(cause, flat[0]);
			String result = bot.getCommands().invoke(cause, flat);
			this.reporter = reporter;
			return result;
		} else {
			reporter = null;
			return null;
		}
	}

	@Override
	public void report(SujavaBot bot, Event<?> cause, String result) {
		if(reporter != null)
			reporter.report(bot, cause, result);
	}
	
	protected String[] flatten(SujavaBot bot, Event<?> cause, Object[] cmd) {
		String[] flat = new String[cmd.length];
		for(int i = 0; i < cmd.length; i++) {
			if(cmd[i] instanceof Object[]) {
				String[] subcmd = flatten(bot, cause, (Object[]) cmd[i]);
				flat[i] = bot.getCommands().invoke(cause, subcmd);
			}
			else
				flat[i] = (String) cmd[i];
		}
		return flat;
	}
	
}
