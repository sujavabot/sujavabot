package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.ArrayList;
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
	private boolean nosync;
	private double prefixPower = 5;
	
	private transient List<Increment> rows = new ArrayList<>(); 
	
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
	public void consume(String context, List<String> content, int maxlen) throws Exception {
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
				byte[] qual = Bytes.toBytes(suffix + " " + context);
				Increment inc = new Increment(row);
				inc.addColumn(SUFFIX, qual, 1);
				if(duration != null)
					inc.setTimeRange(startTS, stopTS);
				incs.add(inc);
			}
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
	
	@Override
	public String next(String context, List<String> prefix) throws Exception {
		prefix = new ArrayList<>(prefix);

		List<Get> gets = new ArrayList<>();
		while(prefix.size() > 0) {
			byte[] row = Bytes.toBytes(StringContent.join(prefix).toUpperCase());
			Get get = new Get(row);
			get.addFamily(SUFFIX);
			gets.add(get);
			prefix.remove(0);
		}
		
		Map<byte[], Double> suffixes = new TreeMap<>(Bytes.BYTES_COMPARATOR);
		for(Result result : table.get(gets)) {
			Map<byte[], Long> counts = new TreeMap<>(Bytes.BYTES_COMPARATOR);
			if(!result.isEmpty()) {
				for(Entry<byte[], byte[]> suffix : result.getFamilyMap(SUFFIX).entrySet()) {
					String[] f = Bytes.toString(suffix.getKey()).split(" ", 2);
					if(context != null && (f.length == 1 || !context.equals(f[1])))
						continue;
					byte[] s = Bytes.toBytes(f[0]);
					if(!counts.containsKey(s))
						counts.put(s, 0L);
					counts.put(s, counts.get(s) + Bytes.toLong(suffix.getValue()));
				}
			}
			double smax = dsum(suffixes.values());
			double pmax = lsum(counts.values());
			if(smax > 0) {
				double mult = prefixPower * pmax / smax;
				for(Entry<byte[], Double> e : suffixes.entrySet())
					e.setValue(mult * e.getValue());
			}
			for(Entry<byte[], Long> e : counts.entrySet()) {
				Double v = suffixes.get(e.getKey());
				if(v == null)
					v = 0.;
				suffixes.put(e.getKey(), v + (double) (long) e.getValue());
			}
		}
		
		double smax = dsum(suffixes.values());
		double v = smax * Math.random();
		for(Entry<byte[], Double> e : suffixes.entrySet()) {
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

}
