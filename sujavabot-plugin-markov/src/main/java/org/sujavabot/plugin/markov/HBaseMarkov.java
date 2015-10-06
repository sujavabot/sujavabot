package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseMarkov implements Markov {
	private static final byte[] SUFFIX = Bytes.toBytes("suffix");
	public static final String SOT = "\0";
	public static final String EOT = "\0";
	
	public static void createTable(Configuration conf, TableName name) throws IOException {
		Connection cxn = ConnectionFactory.createConnection(conf);
		try {
			Admin admin = cxn.getAdmin();
			try {
				HTableDescriptor htd = new HTableDescriptor(name);
				HColumnDescriptor suffix = new HColumnDescriptor(SUFFIX);
				htd.addFamily(suffix);
				admin.createTable(htd);
			} finally {
				admin.close();
			}
		} finally {
			cxn.close();
		}
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
	
	private Configuration conf;
	private Table table;
	private Long duration;
	
	public HBaseMarkov() {
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
	
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	public void setTable(Table table) {
		this.table = table;
	}
	
	public void setDuration(Long duration) {
		this.duration = duration;
	}
	
	@Override
	public void consume(List<String> content, int maxlen) throws Exception {
		content = new ArrayList<>(content);
		content.add(0, SOT);
		content.add(EOT);
		long startTS = System.currentTimeMillis();
		long stopTS = (duration != null ? startTS + duration : Long.MAX_VALUE);
		List<Increment> incs = new ArrayList<>();
		for(int i = 0; i < content.size() - 1; i++) {
			for(int j = 0; j < maxlen; j++) {
				List<String> prefix = content.subList(Math.max(0, i-j), i+1);
				String suffix = content.get(i+1);
				byte[] row = Bytes.toBytes(StringContent.join(prefix).toUpperCase());
				byte[] qual = Bytes.toBytes(suffix);
				Increment inc = new Increment(row);
				inc.addColumn(SUFFIX, qual, 1);
				if(duration != null)
					inc.setTimeRange(startTS, stopTS);
				incs.add(inc);
			}
		}
		table.batch(incs, new Object[incs.size()]);
	}

	private Map<String, Long> counts(String prefix) throws IOException {
		byte[] row = Bytes.toBytes(prefix);
		Get get = new Get(row);
		get.addFamily(SUFFIX);
		Result result = table.get(get);
		if(result.isEmpty())
			return Collections.emptyMap();
		Map<String, Long> counts = new TreeMap<>();
		for(Entry<byte[], byte[]> suffix : result.getFamilyMap(SUFFIX).entrySet())
			counts.put(Bytes.toString(suffix.getKey()), Bytes.toLong(suffix.getValue()));
		return counts;
	}
	
	@Override
	public String next(List<String> prefix) throws Exception {
		prefix = new ArrayList<>(prefix);
		Map<String, Double> suffixes = new TreeMap<>();
		while(prefix.size() > 0) {
			Map<String, Long> counts = counts(StringContent.join(prefix).toUpperCase());
			double smax = dsum(suffixes.values());
			double pmax = lsum(counts.values());
			if(smax > 0) {
				double mult = 5 * pmax / smax;
				for(Entry<String, Double> e : suffixes.entrySet())
					e.setValue(mult * e.getValue());
			}
			for(Entry<String, Long> e : counts.entrySet()) {
				Double v = suffixes.get(e.getKey());
				if(v == null)
					v = 0.;
				suffixes.put(e.getKey(), v + (double) (long) e.getValue());
			}
			prefix.remove(0);
		}
		double smax = dsum(suffixes.values());
		double v = smax * Math.random();
		for(Entry<String, Double> e : suffixes.entrySet()) {
			if(v < e.getValue())
				return EOT.equals(e.getKey()) ? null : e.getKey();
			v -= e.getValue();
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		table.close();
	}

}
