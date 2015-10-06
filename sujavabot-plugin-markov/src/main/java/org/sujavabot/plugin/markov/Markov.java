package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.List;

public interface Markov {
	public static final String SOT = "\u0001";
	public static final String EOT = "\u0002";

	public void consume(List<String> content, int maxlen) throws Exception;

	public String next(List<String> prefix) throws Exception;

	public void close() throws IOException;

}