package org.sujavabot.core.parser;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CommandParsingTest {
	private static ParsedSubstring lit(String literal) {
		return new ParsedSubstring(true, literal);
	}
	
	private static ParsedSubstring exp(String original) {
		return new ParsedSubstring(false, original);
	}
	
	private static Object[] p(String raw, ParsedSubstring... expect) {
		return new Object[] {raw, Arrays.asList(expect) };
	}
	
	@Parameters(name = "{0}")
	public static Iterable<Object[]> params() {
		return Arrays.<Object[]>asList(
				p("literal", lit("literal")),
				p("a[b]c", lit("a"), exp("b"), lit("c")),
				p("a[b][c]d", lit("a"), exp("b"), lit(""), exp("c"), lit("d")),
				p("a[b[c]]d", lit("a"), exp("b[c]"), lit("d")),
				p("a]b", lit("a]b"))
				);
	}
	
	private String raw;
	private List<ParsedSubstring> expect;

	public CommandParsingTest(String raw, List<ParsedSubstring> expect) {
		this.raw = raw;
		this.expect = expect;
	}
	
	@Test
	public void testParse() {
		List<ParsedSubstring> actual = CommandParsing.parse(raw);
		Assert.assertEquals(expect, actual);
	}
}
