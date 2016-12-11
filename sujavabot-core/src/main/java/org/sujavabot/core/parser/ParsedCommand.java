package org.sujavabot.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ParsedCommand {
	protected String raw;
	protected List<ParsedSubstring> substrings;
	
	public ParsedCommand(String raw) {
		this(raw, CommandParsing.parse(raw));
	}
	
	public ParsedCommand(String raw, List<ParsedSubstring> substrings) {
		this.raw = raw;
		this.substrings = substrings;
	}
	
	public String getRaw() {
		return raw;
	}
	
	public List<ParsedSubstring> getSubstrings() {
		return substrings;
	}

	public ParsedCommand apply(Function<String, String> fn) {
		List<ParsedSubstring> nss = new ArrayList<>();
		for(ParsedSubstring ps : substrings) {
			ps = ps.clone();
			if(!ps.isLiteral())
				ps.setReplacement(fn.apply(ps.getOriginal()));
			nss.add(ps);
		}
		return new ParsedCommand(raw, nss);
	}
	
	public String render() {
		StringBuilder sb = new StringBuilder();
		for(ParsedSubstring ps : substrings)
			sb.append(ps.getReplacement());
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return substrings.toString();
	}
	
}
