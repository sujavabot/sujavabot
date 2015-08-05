package org.sujavabot.plugin.markov;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.StatsConfig;

public class BerkeleyDBMarkov {
	private static final byte[] COUNTER = new byte[] { 0 };
	private static final byte[] STRING = new byte[] { 1 };

	private static final long PREFIX_PID = 0;
	private static final DatabaseEntry PREFIX_COUNTER = new DatabaseEntry(counterKey(PREFIX_PID, 0));
	
	private static SequenceConfig seqc() {
		SequenceConfig sc = new SequenceConfig();
		sc.setAllowCreate(true);
		sc.setInitialValue(0);
		return sc;
	}
	
	private static StatsConfig statc() {
		return new StatsConfig();
	}
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private static final String EOF = "";
	private static final String SEP = " ";
	
	private static byte[] longToBytes(long v, byte[] b, int off) {
		for(int i = 0; i < 8; i++) {
			b[off + i] = (byte) v;
			v = (v >>> 8);
		}
		return b;
	}
	
	private static long bytesToLong(byte[] b, int off) {
		long l = 0;
		for(int i = 0; i < 8; i++)
			l |= ((b[off + i] & 0xffl) << (8*i));
		return l;
	}
	
	private static byte[] counterKey(long pid, long sid) {
		byte[] key = new byte[COUNTER.length + 16];
		System.arraycopy(COUNTER, 0, key, 0, COUNTER.length);
		longToBytes(pid, key, COUNTER.length);
		longToBytes(sid, key, COUNTER.length + 8);
		return key;
	}
	
	private static byte[] stringKey(long pid, long sid) {
		byte[] key = new byte[STRING.length + 16];
		System.arraycopy(STRING, 0, key, 0, STRING.length);
		longToBytes(pid, key, STRING.length);
		longToBytes(sid, key, STRING.length + 8);
		return key;
	}
	
	private static long findPID(Database db, String prefix) throws DatabaseException {
		Sequence seq = db.openSequence(null, PREFIX_COUNTER, seqc());
		long size = seq.getStats(statc()).getCurrent();
		seq.close();
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		for(int id = 1; id <= size; id++) {
			key.setData(stringKey(PREFIX_PID, id));
			db.get(null, key, data, null);
			if(prefix.equals(new String(data.getData(), UTF8)))
				return id;
		}
		return -1;
	}
	
	private static long findSID(Database db, long pid, String suffix) throws DatabaseException {
		Sequence seq = db.openSequence(null, new DatabaseEntry(counterKey(pid, 0)), seqc());
		long size = seq.getStats(statc()).getCurrent();
		seq.close();
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		for(int id = 1; id <= size; id++) {
			key.setData(stringKey(pid, id));
			db.get(null, key, data, null);
			if(suffix.equals(new String(data.getData(), UTF8)))
				return id;
		}
		return -1;
	}
	
	private static long findOrAddPID(Database db, String prefix) throws DatabaseException {
		long id = findPID(db, prefix);
		if(id >= 0)
			return id;
		Sequence seq = db.openSequence(null, PREFIX_COUNTER, seqc());
		id = seq.get(null, 1);
		if(id == 0)
			id = seq.get(null, 1);
		seq.close();
		DatabaseEntry key = new DatabaseEntry(stringKey(PREFIX_PID, id));
		DatabaseEntry data = new DatabaseEntry(prefix.getBytes(UTF8));
		db.put(null, key, data);
		return id;
	}
	
	private static long findOrAddSID(Database db, long pid, String suffix) throws DatabaseException {
		long id = findSID(db, pid, suffix);
		if(id >= 0)
			return id;
		Sequence seq = db.openSequence(null, new DatabaseEntry(counterKey(pid, 0)), seqc());
		id = seq.get(null, 1);
		if(id == 0)
			id = seq.get(null, 1);
		seq.close();
		DatabaseEntry key = new DatabaseEntry(stringKey(pid, id));
		DatabaseEntry data = new DatabaseEntry(suffix.getBytes(UTF8));
		db.put(null, key, data);
		return id;
	}
	
	private static long getCount(Database db, long pid, long sid) throws DatabaseException {
		Sequence seq = db.openSequence(null, new DatabaseEntry(counterKey(pid, sid)), seqc());
		long count = seq.getStats(statc()).getCurrent();
		seq.close();
		return count;
	}
	
	private static long incrementCount(Database db, long pid, long sid) throws DatabaseException {
		Sequence seq = db.openSequence(null, new DatabaseEntry(counterKey(pid, sid)), seqc());
		long count = seq.get(null, 1);
		seq.close();
		return count;
	}
	
	private static long increment(Database db, String prefix, String suffix) throws DatabaseException {
		long pid = findOrAddPID(db, prefix);
		long sid = findOrAddSID(db, pid, suffix);
		return incrementCount(db, pid, sid);
	}
	
	private static Map<String, Long> counts(Database db, String prefix) throws DatabaseException {
		long pid = findPID(db, prefix);
		if(pid < 0)
			return Collections.emptyMap();
		
		Map<String, Long> counts = new HashMap<>();
		
		Sequence seq = db.openSequence(null, new DatabaseEntry(counterKey(pid, 0)), seqc());
		long size = seq.getStats(statc()).getCurrent();
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		for(long sid = 1; sid <= size; sid++) {
			key.setData(stringKey(pid, sid));
			db.get(null, key, data, null);
			String k = new String(data.getData(), UTF8);
			counts.put(k, getCount(db, pid, sid));
		}
		
		return counts;
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

	public void consume(List<String> content, int maxlen, boolean sync) throws DatabaseException {
		if(content.size() == 0)
			return;
		content = new ArrayList<>(content);
		content.add(EOF);
		for(int i = -maxlen + 1; i < content.size() - 1; i++) {
			List<String> prefixes = content.subList(Math.max(0, i), Math.min(content.size()-1, i + maxlen));
			String prefix = "";
			for(String p : prefixes) {
				prefix += SEP + p;
			}
			prefix = prefix.substring(SEP.length()).toLowerCase();
			String suffix = content.get(Math.min(content.size() - 1, i + maxlen));
			for(;;) {
				increment(database, prefix, suffix);
				int idx = prefix.indexOf(SEP);
				if(idx < 0)
					break;
				prefix = prefix.substring(idx + SEP.length());
			}
		}

		if(sync)
			database.sync();
	}
	
	public String next(List<String> prefixes) throws DatabaseException {
		String prefix = "";
		for(String p : prefixes) {
			prefix += SEP + p;
		}
		prefix = prefix.substring(SEP.length()).toLowerCase();
		
		Map<String, Double> suffixes = new HashMap<>();
		for(;;) {
			Map<String, Long> psuffixes = counts(database, prefix);
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
