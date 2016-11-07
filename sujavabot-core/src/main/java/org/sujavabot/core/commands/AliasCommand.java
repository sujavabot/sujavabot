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
	protected static final Pattern ESCAPE = Pattern.compile("\\\\.");
	protected static final Pattern VAR = Pattern.compile(
			"(\\$|%)("
			+ "@|"
			+ "nick|"
			+ "user|"
			+ "channel|"
			+ "count|"
			+ "\\{([0-9]*):([0-9]*)\\}|"
			+ "<([0-9]*):([0-9]*)>|"
			+ "([0-9]+)|"
			+ "\\(([^\\\\\"]+|\"([^\\\\\"]*\\\\[\\\\\"])*[^\\\\\"]*\")\\)"
			+ ")(=(\\S+|\"([^\\\\\"]*\\\\[\\\\\"])*[^\\\\\"]*\"))?");
	protected static final Pattern SPLIT = Pattern.compile("(" + ESCAPE.pattern() + ")|(" + VAR.pattern() + ")");
	
	protected static final Function<String, String> DIRECT = Functions.identity();
	protected static final Function<String, String> QUOTED = (s) -> ("\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
	
	public static class ApplyCommand extends AbstractReportingCommand {

		@Override
		public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
			if(args.size() < 2)
				return invokeHelp(bot, cause, args);
			String alias = args.get(1);
			List<String> aliasArgs = new ArrayList<>();
			aliasArgs.add(args.get(0));
			aliasArgs.addAll(args.subList(2, args.size()));
			return applyAlias(bot, cause, aliasArgs, alias);
		}

		@Override
		protected Map<String, String> helpTopics() {
			return buildHelp(
					"<alias> [<args>...]: apply arguments for an alias and return the result"
					);
					
		}
		
	}
	
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
	protected String invokeHelp(SujavaBot bot, Event<?> cause, List<String> args, String topic) {
		return "is an alias for: " + alias;
	}
	
	protected static String unescape(String s) {
		Matcher m = ESCAPE.matcher(s);
		int end = 0;
		StringBuilder sb = new StringBuilder();
		while(m.find()) {
			sb.append(s.substring(end, m.start()));
			if("\\t".equals(m.group()))
				sb.append("\t");
			else if("\\n".equals(m.group()))
				sb.append("\\n");
			else
				sb.append(m.group().substring(1));
			end = m.end();
		}
		sb.append(s.substring(end));
		return sb.toString();
	}
	
	public static String applyAlias(SujavaBot bot, Event<?> cause, List<String> args, String alias) {
		Matcher m = SPLIT.matcher(alias);
		int end = 0;
		StringBuilder sb = new StringBuilder();
		while(m.find()) {
			sb.append(alias.substring(end, m.start()));
			if(ESCAPE.matcher(m.group()).matches()) {
				sb.append(m.group());
			} else {
				sb.append(applyVars(bot, cause, args, m.group()));
			}
			end = m.end();
		}
		sb.append(alias.substring(end));
		return sb.toString();
	}
	
	protected static String applyVars(SujavaBot bot, Event<?> cause, List<String> args, String alias) {
		StringBuilder joined = new StringBuilder();
		for(String arg : args.subList(1, args.size())) {
			if(joined.length() > 0)
				joined.append(" ");
			joined.append(arg);
		}
		StringBuilder sb = new StringBuilder();
		int end = 0;
		Matcher m = VAR.matcher(alias);
		while(m.find()) {
			String defaultValue = (m.group(10) == null ? "" : m.group(11));
			if(defaultValue.startsWith("\"")) {
				defaultValue = defaultValue.substring(1, defaultValue.length()-1);
				defaultValue = unescape(defaultValue);
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
			} else if("count".equals(m.group(2))) {
				sb.append(escape.apply(String.valueOf(args.size()-1)));
			} else if(m.group(2).startsWith("(")) {
				String expr = m.group(2);
				expr = expr.substring(1, expr.length() - 1);
				if(expr.startsWith("\""))
					expr = expr.substring(1, expr.length()-1);
				String val = bot.getCommands().invoke(cause, expr);
				sb.append(escape.apply(val));
			} else if(m.group(2).startsWith("{")) {
				int from = (m.group(3).isEmpty() ? 1 : Integer.parseInt(m.group(3)));
				int to = (m.group(4).isEmpty() ? args.size() : Integer.parseInt(m.group(4)));
				if(from <= args.size() && to <= args.size() && from <= to) {
					List<String> sub = new ArrayList<>(args.subList(from, to));
					for(int i = 0; i < sub.size(); i++)
						sub.set(i, sub.get(i));
					sb.append(escape.apply(StringUtils.join(sub, " ")));
				}
			} else if(m.group(2).startsWith("<")) {
				int from = (m.group(5).isEmpty() ? 1 : Integer.parseInt(m.group(5)));
				int to = (m.group(6).isEmpty() ? args.size() : Integer.parseInt(m.group(6)));
				if(from <= args.size() && to <= args.size() && from <= to) {
					List<String> sub = new ArrayList<>(args.subList(from, to));
					for(int i = 0; i < sub.size(); i++)
						sub.set(i, escape.apply(sub.get(i)));
					sb.append(StringUtils.join(sub, " "));
				}
			} else {
				int i = Integer.parseInt(m.group(2));
				if(i < args.size())
					sb.append(escape.apply(args.get(i)));
				else
					sb.append(escape.apply(defaultValue));
			}
			end = m.end();
		}
		sb.append(alias.substring(end, alias.length()));
		return sb.toString();
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		String applied = applyAlias(bot, cause, args, alias);
		System.err.println(">>" + applied);
		Object[] parsed = bot.getCommands().parse(applied);
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
