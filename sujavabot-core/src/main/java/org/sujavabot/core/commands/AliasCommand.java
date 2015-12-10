package org.sujavabot.core.commands;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public class AliasCommand extends AbstractReportingCommand {
	protected static final Pattern SUB = Pattern.compile("(\\$|%)(@|nick|\\{([0-9]*):([0-9]*)\\}|([0-9]+))");

	protected static final Function<String, String> DIRECT = Functions.identity();
	protected static final Function<String, String> QUOTED = (s) -> s.replace("\\", "\\\\").replace("\"", "\\\"");
	
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
			Function<String, String> escape = ("$".equals(m.group(1)) ? DIRECT : QUOTED);
			if("@".equals(m.group(2))) {
				sb.append(escape.apply(joined.toString()));
			} else if("nick".equals(m.group(1))) {
				sb.append(escape.apply(getUser(cause).getNick()));
			} else if(m.group(2).startsWith("{")) {
				int from = (m.group(3).isEmpty() ? 0 : Integer.parseInt(m.group(3)));
				int to = (m.group(4).isEmpty() ? args.size() : Integer.parseInt(m.group(4)));
				if(from <= args.size() && to <= args.size() && from <= to) {
					List<String> sub = args.subList(from, to);
					sb.append(escape.apply(StringUtils.join(sub, " ")));
				}
			} else {
				int i = Integer.parseInt(m.group(2));
				if(i < args.size())
					sb.append(escape.apply(args.get(i)));
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
