package org.sujavabot.plugin.urlhandler;

import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

public class DuckDuckGoCommandTest {
	@Test
	public void testJubilinux() throws Exception {
		List<Entry<String, String>> res = DuckDuckGoCommand.search("jubilinux edison");
		for(Entry<String, String> e : res) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}
	}
}
