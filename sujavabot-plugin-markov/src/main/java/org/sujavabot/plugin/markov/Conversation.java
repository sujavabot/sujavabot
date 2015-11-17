package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.List;

public interface Conversation {
	public static final String SOT = Markov.SOT;
	public static final String EOT = Markov.EOT;

	public void consume(List<String> previous, List<String> next, String context, int maxlen) throws Exception;
	public String next(List<String> previous, List<String> next, String context, int maxlen) throws Exception;
	
	public void close() throws IOException;
	
	public double getPrefixPower();
	public void setPrefixPower(double prefixPower);

}
