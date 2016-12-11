package org.sujavabot.core.parser;

import java.util.NoSuchElementException;

public class ChItr {
	public final String s;
	public int pos;
	
	public ChItr(String s) {
		this.s = s;
		pos = 0;
	}
	
	public boolean hasNext() {
		return pos < s.length();
	}
	
	public char peek() {
		if(!hasNext())
			throw new NoSuchElementException();
		return s.charAt(pos);
	}
	
	public ChItr advance() {
		if(!hasNext())
			throw new NoSuchElementException();
		pos++;
		return this;
	}
	
	public char next() {
		if(!hasNext())
			throw new NoSuchElementException();
		return s.charAt(pos++);
	}
}