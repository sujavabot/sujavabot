package org.sujavabot.core.commands;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.util.Events;

public class AliasEvaluator {
	
	public static class AliasContext {
		private String nick;
		private String user;
		private String channel;
		private List<String> args;
		
		public AliasContext(String nick, String user, String channel, List<String> args) {
			this.nick = nick;
			this.user = user;
			this.channel = channel;
			this.args = args;
		}
		
		public AliasContext(Event<?> e, List<String> args) {
			User user = Events.getUser(e);
			Channel channel = Events.getChannel(e);
			AuthorizedUser authUser = Authorization.getCurrentUser();
			if(user != null)
				this.nick = user.getNick();
			if(channel != null)
				this.channel = channel.getName();
			if(authUser != null)
				this.user = authUser.getName();
			this.args = args;
		}
	}
	
	private static final Pattern ESCAPE = Pattern.compile("\\\\.");
	
	public static Object evaluateAliases(Object raw, AliasContext context) {
		if(raw instanceof Object[]) {
			Object[] orig = (Object[]) raw;
			Object[] ret = new Object[orig.length];
			for(int i = 0; i < orig.length; i++)
				ret[i] = evaluateAliases(orig[i], context);
			return ret;
		}
		if(raw instanceof String)
			return evaluateString((String) raw, context);
		return raw;
	}
	
	private static String evaluateString(String raw, AliasContext context) {
		StringBuilder sb = new StringBuilder();
		Matcher m = ESCAPE.matcher(raw);
		int start = 0;
		while(m.find()) {
			sb.append(evaluateSubstring(raw.substring(start, m.start()), context));
			String es;
			switch(m.group().charAt(1)) {
			case 'n':
				es = "\n";
				break;
			case 't':
				es = "\t";
				break;
			default:
				es = m.group().substring(1, 2);
				break;
			}
			sb.append(es);
			start = m.end();
		}
		sb.append(evaluateSubstring(raw.substring(start, raw.length()), context));
		return sb.toString();
	}
	
	private static final Pattern EVALUABLE = Pattern.compile("(\\$|%)(nick|user|channel|\\d+)");
	
	private static String evaluateSubstring(String raw, AliasContext context) {
		StringBuilder sb = new StringBuilder();
		Matcher m = EVALUABLE.matcher(raw);
		int start = 0;
		while(m.find()) {
			sb.append(raw.substring(start, m.start()));
			String rep = m.group();
			if("nick".equals(m.group(2)) && context.nick != null)
				rep = context.nick;
			else if("user".equals(m.group(2)) && context.user != null)
				rep = context.user;
			else if("channel".equals(m.group(2)) && context.channel != null)
				rep = context.channel;
			else if(m.group(2).matches("\\d+")) {
				int idx = Integer.parseInt(m.group(2));
				if(idx < context.args.size())
					rep = context.args.get(idx);
			}
			sb.append(rep);
		}
		sb.append(raw.substring(start, raw.length()));
		return sb.toString();
	}
}
