package org.sujavabot.plugin.markov;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;

public class BerkeleyDBMarkov implements Closeable, Markov {
	private static final byte[] COUNTER = new byte[] { 0 };
	private static final byte[] HASHED_STRING = new byte[] { 1 };
	private static final byte[] LISTED_STRING = new byte[] { 2 };

	private static final long PREFIX_PID = 0;
	private static final long COUNT_SID = 0;
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private static final String SEP = " ";
	
	private byte[] longToBytes(long v, byte[] b, int off) {
		for(int i = 0; i < 8; i++) {
			b[off + i] = (byte) v;
			v = (v >>> 8);
		}
		return b;
	}
	
	private long bytesToLong(byte[] b, int off) {
		long l = 0;
		for(int i = 0; i < 8; i++)
			l |= ((b[off + i] & 0xffl) << (8*i));
		return l;
	}
	
	private long getCounter(byte[] key) throws DatabaseException {
		DatabaseEntry data = new DatabaseEntry();
		if(db.get(null, new DatabaseEntry(key), data, null) == OperationStatus.NOTFOUND)
			return 0;
		return bytesToLong(data.getData(), 0);
	}
	
	private void setCounter(byte[] key, long v) throws DatabaseException {
		DatabaseEntry k = new DatabaseEntry(key);
		DatabaseEntry data = new DatabaseEntry(longToBytes(v, new byte[8], 0));
		
		db.delete(null, k);
		db.put(null, k, data);
	}
	
	private byte[] counterKey(long pid, long sid) {
		byte[] key = new byte[COUNTER.length + 16];
		longToBytes(pid << 8, key, 0);
		longToBytes(sid << 8, key, 8);
		System.arraycopy(COUNTER, 0, key, 16, COUNTER.length);
		return key;
	}
	
	private byte[] hashedStringKey(long pid, long sid) {
		byte[] key = new byte[HASHED_STRING.length + 16];
		longToBytes(pid << 8, key, 0);
		longToBytes(sid << 8, key, 8);
		System.arraycopy(HASHED_STRING, 0, key, 16, HASHED_STRING.length);
		return key;
	}
	
	private byte[] listedStringKey(long pid, long lid) {
		byte[] key = new byte[LISTED_STRING.length + 16];
		longToBytes(pid << 8, key, 0);
		longToBytes(lid << 8, key, 8);
		System.arraycopy(LISTED_STRING, 0, key, 16, LISTED_STRING.length);
		return key;
	}
	
	private long findPID(String prefix) throws DatabaseException {
		Long pid = pids.get(prefix);
		if(pid != null)
			return pid;
		pid = (prefix.hashCode() & 0xFFFFFFFFL) << 24;
		DatabaseEntry data = new DatabaseEntry();
		for(;;) {
			if(pid == PREFIX_PID)
				pid++;
			DatabaseEntry key = new DatabaseEntry(hashedStringKey(PREFIX_PID, pid));
			if(db.get(null, key, data, null) == OperationStatus.NOTFOUND)
				return -pid;
			if(prefix.equals(new String(data.getData(), UTF8)))
				return pid;
			pid++;
		}
	}
	
	private long findSID(long pid, String suffix) throws DatabaseException {
		Long sid = sids.get(suffix);
		if(sid != null)
			return sid;
		sid = (suffix.hashCode() & 0xFFFFFFFFL) << 24;
		DatabaseEntry data = new DatabaseEntry();
		for(;;) {
			if(sid == COUNT_SID)
				sid++;
			DatabaseEntry key = new DatabaseEntry(hashedStringKey(pid, sid));
			if(db.get(null, key, data, null) == OperationStatus.NOTFOUND)
				return -sid;
			if(suffix.equals(new String(data.getData(), UTF8)))
				return sid;
			sid++;
		}
	}
	
	private long findOrAddPID(String prefix) throws DatabaseException {
		long pid = findPID(prefix);
		if(pid >= 0)
			return pid;
		pid = -pid;
		pids.put(prefix, pid);
		DatabaseEntry key = new DatabaseEntry(hashedStringKey(PREFIX_PID, pid));
		DatabaseEntry data = new DatabaseEntry(prefix.getBytes(UTF8));
		db.put(null, key, data);
		return pid;
	}
	
	private long findOrAddSID(long pid, String suffix) throws DatabaseException {
		long sid = findSID(pid, suffix);
		if(sid >= 0)
			return sid;
		sid = -sid;
		sids.put(suffix, sid);
		DatabaseEntry key = new DatabaseEntry(hashedStringKey(pid, sid));
		DatabaseEntry data = new DatabaseEntry(suffix.getBytes(UTF8));
		db.put(null, key, data);
		
		long lid = getCounter(counterKey(pid, COUNT_SID));
		lid++;
		setCounter(counterKey(pid, COUNT_SID), lid);
		
		key.setData(listedStringKey(pid, lid));
		db.put(null, key, data);
		
		return sid;
	}
	
	private long getCount(long pid, long sid) throws DatabaseException {
		return getCounter(counterKey(pid, sid));
	}
	
	private long incrementCount(long pid, long sid) throws DatabaseException {
		long count = getCount(pid, sid);
		count++;
		setCounter(counterKey(pid, sid), count);
		return count;
	}
	
	private long increment(String prefix, String suffix) throws DatabaseException {
		long pid = findOrAddPID(prefix);
		long sid = findOrAddSID(pid, suffix);
		return incrementCount(pid, sid);
	}
	
	private Map<String, Long> counts(String prefix) throws DatabaseException {
		long pid = findPID(prefix);
		if(pid < 0)
			return Collections.emptyMap();
		
		Map<String, Long> counts = new HashMap<>();
		
		long max = getCounter(counterKey(pid, COUNT_SID));
		
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		for(long lid = 1; lid <= max; lid++) {
			key.setData(listedStringKey(pid, lid));
			if(db.get(null, key, data, null) == OperationStatus.NOTFOUND)
				return counts;
			String suffix = new String(data.getData(), UTF8);
			long sid = findSID(pid, suffix);
			counts.put(suffix, getCount(pid, sid));
		}
		
		return counts;
	}
	
	private double dsum(Iterable<Double> i) {
		double v = 0;
		for(Double l : i)
			v += l;
		return v;
	}
	
	private long lsum(Iterable<Long> i) {
		long v = 0;
		for(Long l : i)
			v += l;
		return v;
	}
	
	protected Environment environment;
	protected Database db;
	protected boolean nosync;
	
	protected Map<String, Long> pids = new WeakHashMap<>();
	protected Map<String, Long> sids = new WeakHashMap<>();
	
	public BerkeleyDBMarkov() {}
	
	public BerkeleyDBMarkov(Environment environment, Database database) {
		this.environment = environment;
		this.db = database;
	}
	
	public Environment getEnvironment() {
		return environment;
	}
	
	public Database getDatabase() {
		return db;
	}
	
	public boolean isNosync() {
		return nosync;
	}
	
	public void setDatabase(Database database) {
		this.db = database;
	}
	
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	
	public void setNosync(boolean nosync) {
		this.nosync = nosync;
	}

	/* (non-Javadoc)
	 * @see org.sujavabot.plugin.markov.Markov#consume(java.util.List, int)
	 */
	@Override
	public void consume(List<String> content, int maxlen) throws DatabaseException {
		if(content.size() == 0)
			return;
		content = new ArrayList<>(content);
		content.add(0, SOT);
		content.add(EOT);
		for(int i = -maxlen + 1; i < content.size() - 1; i++) {
			List<String> prefixes = content.subList(Math.max(0, i), Math.min(content.size()-1, i + maxlen));
			String prefix = "";
			for(String p : prefixes) {
				prefix += SEP + p;
			}
			prefix = prefix.substring(SEP.length()).toLowerCase();
			String suffix = content.get(Math.min(content.size() - 1, i + maxlen));
			for(;;) {
				increment(prefix, suffix);
				int idx = prefix.indexOf(SEP);
				if(idx < 0)
					break;
				prefix = prefix.substring(idx + SEP.length());
			}
		}
		if(!nosync)
			db.sync();
	}
	
	/* (non-Javadoc)
	 * @see org.sujavabot.plugin.markov.Markov#next(java.util.List)
	 */
	@Override
	public String next(List<String> prefixes) throws DatabaseException {
		String prefix = "";
		for(String p : prefixes) {
			prefix += SEP + p;
		}
		prefix = prefix.substring(SEP.length()).toLowerCase();
		
		Map<String, Double> suffixes = suffixes(prefix);

		double smax = dsum(suffixes.values());
		double v = smax * Math.random();
		for(Entry<String, Double> e : suffixes.entrySet()) {
			if(v < e.getValue())
				return EOT.equals(e.getKey()) ? null : e.getKey();
			v -= e.getValue();
		}
		return null;
	}
	
	public Map<String, Double> suffixes(String prefix) throws DatabaseException {
		Map<String, Double> suffixes = new HashMap<>();

		for(;;) {
			Map<String, Long> psuffixes = counts(prefix);
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
		
		return suffixes;
	}

	/* (non-Javadoc)
	 * @see org.sujavabot.plugin.markov.Markov#close()
	 */
	@Override
	public void close() throws IOException {
		db.close();
		environment.close();
	}
}
