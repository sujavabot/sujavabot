package org.sujavabot.core.parser;

import java.util.Objects;

public class ParsedSubstring implements Cloneable {
	private final boolean literal;
	private final String original;
	private String replacement;
	
	public ParsedSubstring(boolean literal, String original) {
		this(literal, original, literal ? original : '[' + original + ']');
	}
	
	public ParsedSubstring(boolean literal, String original, String replacement) {
		this.literal = literal;
		this.original = original;
		this.replacement = replacement;
	}
	
	@Override
	public ParsedSubstring clone() {
		try {
			return (ParsedSubstring) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}
	
	@Override
	public int hashCode() {
		return original.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ParsedSubstring) {
			ParsedSubstring o = (ParsedSubstring) obj;
			return literal == o.literal && Objects.equals(original, o.original) && Objects.equals(replacement, o.replacement);
		}
		return false;
	}

	public String getReplacement() {
		return replacement;
	}

	public ParsedSubstring setReplacement(String replacement) {
		this.replacement = replacement;
		return this;
	}

	public boolean isLiteral() {
		return literal;
	}

	public String getOriginal() {
		return original;
	}
	
	@Override
	public String toString() {
		return replacement;
	}
}