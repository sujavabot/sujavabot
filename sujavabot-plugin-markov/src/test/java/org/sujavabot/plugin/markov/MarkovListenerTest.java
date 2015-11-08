package org.sujavabot.plugin.markov;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MarkovListenerTest {
	@Test
	public void testMerge() {
		List<String> words = Arrays.asList("i should really test this code abcdefghijklmnopqrstuvwxyz".split("\\s+"));
		System.out.println(MarkovListener.merge(15, "hi: ", words));
	}
}
