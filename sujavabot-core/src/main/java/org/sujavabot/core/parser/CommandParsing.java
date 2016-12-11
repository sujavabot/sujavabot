package org.sujavabot.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class CommandParsing {
	private CommandParsing() {}
	
	private static class ChItr {
		public final String raw;
		public int pos;
		
		public ChItr(String raw) {
			this.raw = raw;
			pos = 0;
		}
		
		public boolean hasNext() {
			return pos < raw.length();
		}
		
		public char peek() {
			if(!hasNext())
				throw new NoSuchElementException();
			return raw.charAt(pos);
		}
		
		public ChItr advance() {
			pos++;
			return this;
		}
		
		public char next() {
			if(!hasNext())
				throw new NoSuchElementException();
			return raw.charAt(pos++);
		}
	}
	
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
	
	private static char unescape(ChItr ci) {
		char c = ci.next();
		if(ci.hasNext())
			c = ci.next();
		if('n' == c)
			c = '\n';
		else if('t' == c)
			c = '\t';
		return c;
	}
	
	private static ParsedSubstring quoting(ChItr ci) {
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
	
	private static ParsedSubstring exp(ChItr ci) {
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
