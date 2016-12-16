package org.sujavabot.core.parser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Templates {
	private Templates() {}
	
	private static final Function<String, String> BARE = (s) -> s;
	private static final Function<String, String> QUOTED = (s) -> quote(s);
	
	private static final Function<ChItr, Boolean> BARE_KEY = (ci) -> {
		char c = ci.peek();
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	};
	
	private static final Function<ChItr, Boolean> BRACED_KEY = (ci) -> (ci.peek() != '}');
	private static final Function<ChItr, Boolean> ANGLED_KEY = (ci) -> (ci.peek() != '>');
	
	private static final Pattern ARG_RANGE_KEY = Pattern.compile("(\\d*):(\\d*)");
	private static final Pattern ARG_KEY = Pattern.compile("\\d+");
	
	public static String apply(String template, Map<String, String> aliases, List<String> args) {
		return apply(template, (s) -> aliases.get(s), args);
	}
	
	public static String apply(String template, Function<String, String> aliases, List<String> args) {
		ChItr ci = new ChItr(template);
		StringBuilder sb = new StringBuilder();
		while(ci.hasNext()) {
			char c = ci.peek();
			if('\\' == c)
				sb.append(CommandParsing.unescape(ci));
			else if('$' == c || '%' == c) {
				Function<String, String> wrap = ('$' == c ? BARE : QUOTED);
				Function<ChItr, Boolean> keychar;
				char kt = ci.advance().peek();
				if('{' == kt) {
					keychar = BRACED_KEY;
					ci.advance();
				} else if('<' == kt) {
					keychar = ANGLED_KEY;
					ci.advance();
				} else
					keychar = BARE_KEY;
				String key = "";
				while(ci.hasNext() && keychar.apply(ci)) {
					key += ci.next();
				}
				if('{' == kt || '<' == kt)
					ci.advance();
				Matcher m;
				if((m = ARG_RANGE_KEY.matcher(key)).matches()) {
					int from = m.group(1).isEmpty() ? 0 : Integer.parseInt(m.group(1));
					int to = m.group(2).isEmpty() ? args.size() : Integer.parseInt(m.group(2));
					StringBuilder vb = new StringBuilder();
					String sep = "";
					for(String arg : args.subList(from, to)) {
						vb.append(sep);
						if('<' == kt)
							arg = wrap.apply(arg);
						vb.append(arg);
					}
					String val = vb.toString();
					if('{' == kt)
						val = wrap.apply(val);
					sb.append(val);
				} else if((m = ARG_KEY.matcher(key)).matches()) {
					
				}
			}
		}
	}
	
	static String quote(String s) {
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		ChItr ci = new ChItr(s);
		while(ci.hasNext()) {
			char c = ci.next();
			if('\\' == c)
				sb.append("\\\\");
			else if('\n' == c)
				sb.append("\\n");
			else if('\t' == c)
				sb.append("\\t");
			else if('"' == c)
				sb.append("\\\"");
			else
				sb.append(c);
		}
		sb.append('"');
		return sb.toString();
	}
}
