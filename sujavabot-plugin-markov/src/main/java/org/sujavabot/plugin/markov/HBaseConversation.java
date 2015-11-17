package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseConversation implements Conversation {
	private static final byte[] DEFAULT_FAMILY = Bytes.toBytes("conversation");
	
	private static final byte[] DELIM = new byte[1];
	
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
	
	private static List<List<String>> ngrams(List<String> strings, int maxlen) {
		strings = new ArrayList<>(strings);
		List<List<String>> ngrams = new ArrayList<>();
		for(int to = 1; to <= strings.size(); to++) {
			for(int from = Math.max(0, to - maxlen); from < to; from++) {
				ngrams.add(strings.subList(from, to));
			}
		}
		return ngrams;
	}
	
	private static List<List<String>> tgrams(List<String> strings, int maxlen) {
		strings = new ArrayList<>(strings);
		List<List<String>> tgrams = new ArrayList<>();
		for(int from = Math.max(0, strings.size() - maxlen); from < strings.size(); from++)
			tgrams.add(strings.subList(from, strings.size()));
		return tgrams;
	}
	
	private Configuration conf;
	private Table table;
	private byte[] family = DEFAULT_FAMILY;
	private Long duration;
	private boolean nosync;
	private double prefixPower = 5;
	
	private transient List<Increment> rows = new ArrayList<>(); 
	
	public HBaseConversation() {
	}

	public Configuration getConf() {
		return conf;
	}
	
	public Table getTable() {
		return table;
	}
	
	public Long getDuration() {
		return duration;
	}
	
	public boolean isNosync() {
		return nosync;
	}
	
	@Override
	public double getPrefixPower() {
		return prefixPower;
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	public void setTable(Table table) {
		this.table = table;
	}
	
	public void setDuration(Long duration) {
		this.duration = duration;
	}
	
	public void setNosync(boolean nosync) {
		this.nosync = nosync;
	}
	
	@Override
	public void setPrefixPower(double prefixPower) {
		this.prefixPower = prefixPower;
	}
	
	@Override
	public void consume(List<String> previous, List<String> next, String context, int maxlen) throws Exception {
		previous = new ArrayList<>(previous);
		next = new ArrayList<>(next);
		next.add(0, SOT);
		next.add(EOT);
		
		long startTS = System.currentTimeMillis();
		long stopTS = (duration != null ? startTS + duration : Long.MAX_VALUE);
		List<Increment> incs = new ArrayList<>();
		
		List<List<String>> pngrams = ngrams(previous, maxlen);
		List<List<String>> nngrams = ngrams(next, maxlen + 1);

		pngrams.add(new ArrayList<>());
		
		byte[] contextBytes = Bytes.toBytes(context);
		
		for(List<String> pngram : pngrams) {
			byte[] prow = Bytes.toBytes(StringContent.join(pngram).toUpperCase());
			for(List<String> nngram : nngrams) {
				if(nngram.size() <= 1)
					continue;
				byte[] nrow = Bytes.add(prow, DELIM, Bytes.toBytes(StringContent.join(nngram.subList(0, nngram.size()-1)).toUpperCase()));
				byte[] qual = Bytes.toBytes(nngram.get(nngram.size() - 1));
				
				Increment inc;
				
				inc = new Increment(Bytes.add(DELIM, nrow));
				inc.addColumn(family, qual, 1);
				incs.add(inc);
				
				inc = new Increment(Bytes.add(contextBytes, DELIM, nrow));
				inc.addColumn(family, qual, 1);
				incs.add(inc);
				
			}
		}

		if(duration != null) {
			for(Increment inc : incs)
				inc.setTimeRange(startTS, stopTS);
		}
		
		if(!nosync)
			table.batch(incs, new Object[incs.size()]);
		else
			rows.addAll(incs);
	}

	public void sync() throws IOException, InterruptedException {
		table.batch(rows, new Object[rows.size()]);
		rows.clear();
	}
	
	public String next(List<String> previous, List<String> next, String context, int maxlen) throws Exception {
		List<List<String>> pngrams = ngrams(previous, maxlen);
		List<List<String>> ntgrams = tgrams(next, maxlen);
		
		pngrams.add(new ArrayList<>());
		
		byte[] contextBytes = (context == null ? new byte[0] : Bytes.toBytes(context));
		
		List<Get> gets = new ArrayList<>();
		List<Integer> nlen = new ArrayList<>();
		for(List<String> ntgram : ntgrams) {
			for(List<String> pngram : pngrams) {
				byte[] prow = Bytes.toBytes(StringContent.join(pngram).toUpperCase());
				byte[] nrow = Bytes.add(prow, DELIM, Bytes.toBytes(StringContent.join(ntgram).toUpperCase()));
				Get get = new Get(Bytes.add(contextBytes, DELIM, nrow));
				get.addFamily(family);
				
				gets.add(get);
				nlen.add(ntgram.size());
			}
		}
		
		Object[] results = new Object[gets.size()];
		table.batch(gets, results);
		
		List<Map<byte[], Long>> suffixes = new ArrayList<>();
		suffixes.add(new TreeMap<>(Bytes.BYTES_COMPARATOR));
		
		for(int i = 0; i < results.length; i++) {
			if(i > 0 && nlen.get(i-1) > nlen.get(i)) {
				suffixes.add(new TreeMap<>(Bytes.BYTES_COMPARATOR));
			}
			Map<byte[], Long> tail = suffixes.get(suffixes.size()-1);
			Result r = (Result) results[i];
			for(Entry<byte[], byte[]> e : r.getFamilyMap(family).entrySet()) {
				byte[] key = e.getKey();
				long value = Bytes.toLong(e.getValue());
				if(!tail.containsKey(key))
					tail.put(key, value);
				else
					tail.put(key, value + tail.get(key));
			}
		}
		
		Map<byte[], Double> ns = new TreeMap<>();
		for(Map<byte[], Long> sm : suffixes) {
			double smax = dsum(ns.values());
			double pmax = lsum(sm.values());
			if(smax > 0) {
				double mult = prefixPower * pmax / smax;
				for(Entry<byte[], Double> e : ns.entrySet())
					e.setValue(mult * e.getValue());
			}
			for(Entry<byte[], Long> e : sm.entrySet()) {
				Double v = ns.get(e.getKey());
				if(v == null)
					v = 0.;
				ns.put(e.getKey(), v + (double) (long) e.getValue());
			}
		}
		
		double smax = dsum(ns.values());
		double v = smax * Math.random();
		for(Entry<byte[], Double> e : ns.entrySet()) {
			if(v < e.getValue()) {
				String sfx = Bytes.toString(e.getKey());
				return EOT.equals(sfx) ? null : sfx;
			}
			v -= e.getValue();
		}
		return null;
	}
	
	@Override
	public void close() throws IOException {
		table.close();
	}

	public byte[] getFamily() {
		return family;
	}

	public void setFamily(byte[] family) {
		this.family = family;
	}

}
