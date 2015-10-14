package org.sujavabot.plugin.markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringContent {

	private static class CharSubSequence implements CharSequence {
		public int start;
		public int len;
		public char[] c;

		public CharSubSequence(char[] c, int start, int len) {
			this.c = c;
			this.start = start;
			this.len = len;
		}

		@Override
		public int length() {
			return len;
		}

		@Override
		public char charAt(int index) {
			if(index < 0 || index >= len)
				throw new IndexOutOfBoundsException();
			return c[start + index];
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new CharSubSequence(c, this.start + start, end - start);
		}

		@Override
		public String toString() {
			return new String(c, start, len);
		}
	}

	public static final Pattern TRAILERS = Pattern.compile(",|\\?|!");
	public static final Pattern WORD = Pattern.compile("(?i)(\\s+|^)[a-z\\-_0-9]+(['’][a-z\\-_0-9]+)*['’]?");
	public static final Pattern LINK = Pattern.compile("(?i)(\\s+|^)(https?:|ftp:|www\\.)\\S*");
	public static final Pattern NUMBER = Pattern.compile("(\\s+|^)\\$?[0-9]+(\\.[0-9]*)?|\\.[0-9]+");
	public static final Pattern ELLIPSES = Pattern.compile("\\.(\\s*\\.)*");

	public static final Pattern TOKEN = any(
			LINK,
			NUMBER,
			WORD,
			ELLIPSES,
			TRAILERS
			);

	private static Pattern any(Pattern... patterns) {
		String sep = "";
		StringBuilder sb = new StringBuilder();
		for(Pattern p : patterns) {
			sb.append(sep);
			sb.append('(');
			sb.append(p.pattern());
			sb.append(')');
			sep = "|";
		}
		return Pattern.compile(sb.toString());
	}

	public static List<String> parse(String s) {
		List<String> tokens = new ArrayList<>();
		int i = 0;
		Matcher m = TOKEN.matcher(s);
		while(m.find()) {
			String p = s.substring(i, m.start()).trim();
			if(!p.isEmpty()) {
				for(String t : p.split("\\s+"))
					tokens.add(t);
			}
			tokens.add(m.group().trim());
			i = m.end();
		}
		String p = s.substring(i).trim();
		if(!p.isEmpty()) {
			for(String t : p.split("\\s+"))
				tokens.add(t);
		}
		return tokens;
	}

	public static String join(List<String> chain) {
		StringBuilder sb = new StringBuilder();

		for(String word : chain) {
			if(sb.length() > 0 && word.matches(".*\\w.*"))
				sb.append(' ');
			sb.append(word);
		}

		return sb.toString();
	}

	public static List<List<String>> sentences(List<String> chain) {
		List<List<String>> sentences = new ArrayList<>();

		List<String> sentence = new ArrayList<>();

		for(String word : chain) {
			if(ELLIPSES.matcher(word).matches()) {
				sentences.add(sentence);
				sentence = new ArrayList<>();
			} else
				sentence.add(word);
		}

		if(sentence.size() > 0) {
			sentences.add(sentence);
		}
		
		return sentences;
	}

	private StringContent() {}
}
