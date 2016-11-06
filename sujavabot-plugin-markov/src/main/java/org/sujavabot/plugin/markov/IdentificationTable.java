package org.sujavabot.plugin.markov;

import java.io.Flushable;
import java.io.IOException;
import java.util.Map;

public interface IdentificationTable extends Flushable {
	public void put(long timestamp, byte[] prefix, byte[] suffix, byte[] id) throws IOException;
	public Map<byte[], Double> get(long timestamp, byte[] prefix, byte[] suffix) throws IOException;
}
