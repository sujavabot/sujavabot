package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public interface Markov {
	public static final String SOT = "\u0001";
	public static final String EOT = "\u0002";

	public void consume(String context, List<String> content, int maxlen) throws Exception;

	public String next(Pattern context, List<String> prefix) throws Exception;

	public void close() throws IOException;
	
	public double getPrefixPower();
	public void setPrefixPower(double prefixPower);

}