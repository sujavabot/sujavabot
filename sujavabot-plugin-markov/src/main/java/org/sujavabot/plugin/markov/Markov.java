package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.List;

import com.sleepycat.je.DatabaseException;

public interface Markov {

	public void consume(List<String> content, int maxlen) throws Exception;

	public String next(List<String> prefix) throws Exception;

	public void close() throws IOException;

}