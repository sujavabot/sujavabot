package org.sujavabot.core.parser;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandParsing {
	private CommandParsing() {}
	
	public static List<ParsedSubstring> parse(String raw) {
		List<ParsedSubstring> ss = new ArrayList<>();
		ChItr ci = new ChItr(raw);
		
		StringBuilder sb = new StringBuilder();
		while(ci.hasNext()) {
			char c = ci.peek();
			if('"' == c) {
				ss.add(new ParsedSubstring(true, sb.toString()));
				ss.add(quoting(ci));
				sb = new StringBuilder();
			} else if('[' == c) {
				ss.add(new ParsedSubstring(true, sb.toString()));
				ss.add(exp(ci));
				sb = new StringBuilder();
			} else if('\\' == c) {
				sb.append(unescape(ci));
			} else
				sb.append(ci.next());
		}
		ss.add(new ParsedSubstring(true, sb.toString()));
		
		return ss;
	}
	
	static char unescape(ChItr ci) {
		char c = ci.next();
		if(ci.hasNext())
			c = ci.next();
		if('n' == c)
			c = '\n';
		else if('t' == c)
			c = '\t';
		return c;
	}
	
	static ParsedSubstring quoting(ChItr ci) {
		StringBuilder sb = new StringBuilder();
		char qc = ci.next();
		while(ci.hasNext()) {
			char c = ci.peek();
			if('\\' == c)
				sb.append(unescape(ci));
			else if(qc == c) {
				ci.advance();
				String s = sb.toString();
				return new ParsedSubstring(true, qc + s + qc, s);
			}
			else
				sb.append(ci.next());
		}
		String s = sb.toString();
		return new ParsedSubstring(true, qc + s, s);
	}
	
	static ParsedSubstring exp(ChItr ci) {
		ci.advance();
		StringBuilder sb = new StringBuilder();
		int depth = 1;
		while(ci.hasNext()) {
			char c = ci.peek();
			if('\\' == c)
				sb.append(unescape(ci));
			else if('"' == c)
				sb.append(quoting(ci).getOriginal());
			else if('[' == c) {
				sb.append(ci.next());
				depth++;
			} else if(']' == c) {
				ci.advance();
				if(--depth == 0)
					break;
				sb.append(']');
			} else
				sb.append(ci.next());
		}
		return new ParsedSubstring(false, sb.toString());
	}
}
