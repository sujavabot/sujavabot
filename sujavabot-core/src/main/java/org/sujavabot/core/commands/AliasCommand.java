package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Events;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public class AliasCommand extends AbstractReportingCommand {
	protected static final Pattern SUB = Pattern.compile("(\\$|%)(@|nick|user|channel|\\{([0-9]*):([0-9]*)\\}|([0-9]+))(=(\\S+|\"([^\\\\]*\\\\[\\\\\"])*[^\\\\]*\"))?");

	protected static final Function<String, String> DIRECT = Functions.identity();
	protected static final Function<String, String> QUOTED = (s) -> ("\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
	
	protected String alias;

	protected transient Command reporter;
	
	public AliasCommand(String alias) {
		this.alias = alias;
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("is an alias for: " + alias);
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
			String defaultValue = (m.group(6) == null ? "" : m.group(7));
			if(defaultValue.startsWith("\"")) {
				defaultValue = defaultValue.substring(1, defaultValue.length()-1);
				defaultValue = defaultValue.replaceAll("\\\\([\\\\\"])", "$1");
			}
			sb.append(alias.substring(end, m.start()));
			Function<String, String> escape = ("$".equals(m.group(1)) ? DIRECT : QUOTED);
			if("@".equals(m.group(2))) {
				sb.append(escape.apply(joined.toString()));
			} else if("nick".equals(m.group(2))) {
				sb.append(escape.apply(Events.getUser(cause).getNick()));
			} else if("user".equals(m.group(2))) {
				sb.append(escape.apply(Authorization.getCurrentUser().getName()));
			} else if(m.group(2).startsWith("channel")) {
				Channel channel = Events.getChannel(cause);
				String c = (channel == null ? defaultValue : channel.getName());
				sb.append(escape.apply(c));
			} else if(m.group(2).startsWith("{")) {
				int from = (m.group(3).isEmpty() ? 0 : Integer.parseInt(m.group(3)));
				int to = (m.group(4).isEmpty() ? args.size() : Integer.parseInt(m.group(4)));
				if(from <= args.size() && to <= args.size() && from <= to) {
					List<String> sub = args.subList(from, to);
					sb.append(escape.apply(StringUtils.join(sub, " ")));
				}
			} else {
				int i = Integer.parseInt(m.group(5));
				if(i < args.size())
					sb.append(escape.apply(args.get(i)));
				else
					sb.append(escape.apply(defaultValue));
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
		List<String> args = new ArrayList<>();
		for(int i = 0; i < cmd.length; i++) {
			String arg;
			if(cmd[i] instanceof Object[]) {
				String[] subcmd = flatten(bot, cause, (Object[]) cmd[i]);
				arg = bot.getCommands().invoke(cause, subcmd);
			}
			else
				arg = (String) cmd[i];
			if(arg != null)
				args.add(arg);
		}
		return args.toArray(new String[args.size()]);
	}
	
	@Override
	public String toString() {
		return alias;
	}
}
