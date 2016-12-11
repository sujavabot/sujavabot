package org.sujavabot.core.parser;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandParsing {
	private CommandParsing() {}
	
	public static List<ParsedSubstring> parse(String raw) {
		List<ParsedSubstring> substrings = new ArrayList<>();
		int start = 0;
		int depth = 0;
		for(int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if('[' == c) {
				if(depth == 0) {
					substrings.add(new ParsedSubstring(true, raw.substring(start, i)));
					start = i+1;
				}
				depth++;
			} else if(']' == c) {
				depth--;
				if(depth == 0) {
					substrings.add(new ParsedSubstring(false, raw.substring(start, i)));
					start = i+1;
				} else if(depth < 0)
					break;
			}
		}
		substrings.add(new ParsedSubstring(depth <= 0, raw.substring(start, raw.length())));
			
		return substrings;
	}
}
