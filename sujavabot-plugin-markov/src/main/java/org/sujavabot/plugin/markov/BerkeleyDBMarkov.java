package org.sujavabot.plugin.markov;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class BerkeleyDBMarkov {
	private static final byte[] PREFIX = new byte[] { 0 };
	private static final byte[] SUFFIX = new byte[] { 1 };
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private static final String EOF = "";
	private static final String SEP = " ";
	
	private static byte[] prefixKey(String prefix) {
		byte[] prefixBytes = prefix.getBytes(UTF8);
		byte[] key = new byte[PREFIX.length + prefixBytes.length];
		System.arraycopy(PREFIX, 0, key, 0, PREFIX.length);
		System.arraycopy(prefixBytes, 0, key, PREFIX.length, prefixBytes.length);
		return key;
	}
	
	private static byte[] longToBytes(long v) {
		byte[] b = new byte[8];
		for(int i = 0; i < 8; i++) {
			b[i] = (byte) v;
			v = (v >>> 8);
		}
		return b;
	}
	
	private static long bytesToLong(byte[] b) {
		long l = 0;
		for(int i = 0; i < 8; i++)
			l |= ((b[i] & 0xffl) << (8*i));
		return l;
	}
	
	private static byte[] suffixKey(String prefix, long id) {
		byte[] prefixBytes = prefix.getBytes(UTF8);
		byte[] idBytes = longToBytes(id);
		byte[] key = new byte[SUFFIX.length + prefixBytes.length + idBytes.length];
		System.arraycopy(SUFFIX, 0, key, 0, SUFFIX.length);
		System.arraycopy(prefixBytes, 0, key, SUFFIX.length, prefixBytes.length);
		System.arraycopy(idBytes, 0, key, SUFFIX.length + prefixBytes.length, idBytes.length);
		return key;
	}
	
	private static double dsum(Iterable<Double> i) {
		double v = 0;
		for(Double l : i)
			v += l;
		return v;
	}
	
	private static long lsum(Iterable<Long> i) {
		long v = 0;
		for(Long l : i)
			v += l;
		return v;
	}
	
	protected Database database;
	
	public BerkeleyDBMarkov(Database database) {
		this.database = database;
	}
	
	public Database getDatabase() {
		return database;
	}

	private long getNextSuffixId(String prefix) throws DatabaseException {
		DatabaseEntry key = new DatabaseEntry(prefixKey(prefix));
		DatabaseEntry data = new DatabaseEntry();
		if(database.get(null, key, data, null) == OperationStatus.NOTFOUND)
			return 0;
		return bytesToLong(data.getData());
	}
	
	private void setNextSuffixId(String prefix, long id) throws DatabaseException {
		DatabaseEntry key = new DatabaseEntry(prefixKey(prefix));
		DatabaseEntry data = new DatabaseEntry(longToBytes(id));
		database.delete(null, key);
		database.put(null, key, data);
	}
	
	private void addSuffix(String prefix, String suffix) throws DatabaseException {
		long id = getNextSuffixId(prefix);
		DatabaseEntry key = new DatabaseEntry(suffixKey(prefix, id));
		DatabaseEntry data = new DatabaseEntry(suffix.getBytes(UTF8));
		database.put(null, key, data);
		setNextSuffixId(prefix, id + 1);
	}
	
	private String getSuffix(String prefix, long id) throws DatabaseException {
		DatabaseEntry key = new DatabaseEntry(suffixKey(prefix, id));
		DatabaseEntry data = new DatabaseEntry();
		if(database.get(null, key, data, null) == OperationStatus.NOTFOUND)
			return null;
		return new String(data.getData(), UTF8);
	}
	
	private Map<String, Long> getSuffixes(String prefix) throws DatabaseException {
		long maxId = getNextSuffixId(prefix);
		Map<String, Long> suffixes = new HashMap<>();
		for(long id = 0; id < maxId; id++) {
			String suffix = getSuffix(prefix, id);
			Long count = suffixes.get(suffix);
			if(count == null)
				suffixes.put(suffix, 1L);
			else
				suffixes.put(suffix, 1L + count);
		}
		return suffixes;
	}
	
	public void consume(List<String> content, int maxlen) throws DatabaseException {
		content = new ArrayList<>(content);
		content.add(EOF);
		for(int i = -maxlen + 1; i < content.size() - 1; i++) {
			List<String> prefixes = content.subList(Math.max(0, i), Math.min(content.size()-1, i + maxlen));
			String prefix = "";
			for(String p : prefixes) {
				prefix += SEP + p;
			}
			prefix = prefix.substring(SEP.length());
			String suffix = content.get(Math.min(content.size() - 1, i + maxlen));
			for(;;) {
				addSuffix(prefix, suffix);
				int idx = prefix.indexOf(SEP);
				if(idx < 0)
					break;
				prefix = prefix.substring(idx + SEP.length());
			}
		}

		database.sync();
	}
	
	public String next(List<String> prefixes) throws DatabaseException {
		String prefix = "";
		for(String p : prefixes) {
			prefix += SEP + p;
		}
		prefix = prefix.substring(SEP.length());
		
		Map<String, Double> suffixes = new HashMap<>();
		for(;;) {
			Map<String, Long> psuffixes = getSuffixes(prefix);
			double smax = dsum(suffixes.values());
			double pmax = lsum(psuffixes.values());
			if(smax > 0) {
				double mult = 5 * pmax / smax;
				for(Entry<String, Double> e : suffixes.entrySet())
					e.setValue(mult * e.getValue());
			}
			for(Entry<String, Long> e : psuffixes.entrySet()) {
				Double v = suffixes.get(e.getKey());
				if(v == null)
					v = 0.;
				suffixes.put(e.getKey(), v + (double) (long) e.getValue());
			}
			int idx = prefix.indexOf(SEP);
			if(idx < 0)
				break;
			prefix = prefix.substring(idx + SEP.length());
		}
		
		double smax = dsum(suffixes.values());
		double v = smax * Math.random();
		for(Entry<String, Double> e : suffixes.entrySet()) {
			if(v < e.getValue())
				return EOF.equals(e.getKey()) ? null : e.getKey();
			v -= e.getValue();
		}
		return null;
	}
}
