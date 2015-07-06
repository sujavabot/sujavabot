package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.util.Bytes;

public class HTableMarkov {
	public static final byte[] SUFFIX_FAMILY =  Bytes.toBytes("suffix");
	
	private HTable table;
	
	private Long lifespan;
	
	private Map<byte[], Map<byte[], Long>> incs = new TreeMap<>(Bytes.BYTES_COMPARATOR);
	
	public HTableMarkov(HTable table, Long lifespan) {
		this.table = table;
		this.lifespan = lifespan;
	}
	
	public HTableMarkov(Configuration conf, TableName tn, Long lifespan) throws IOException {
		HBaseAdmin admin = new HBaseAdmin(conf);
		if(!admin.tableExists(tn)) {
			HTableDescriptor td = new HTableDescriptor(tn);
			td.addFamily(new HColumnDescriptor(SUFFIX_FAMILY));
			admin.createTable(td);
		}
		admin.close();
		table = new HTable(conf, tn);
		this.lifespan = lifespan;
	}
	
	private void insert(List<String> prefix, String suffix) {
		if(suffix == null)
			suffix = "\0";
		byte[] row = Bytes.toBytes(StringContent.join(prefix).toUpperCase());
		if(row.length == 0)
			return;
		byte[] q = Bytes.toBytes(suffix);
		
		synchronized(incs) {
			Map<byte[], Long> rowmap = incs.get(row);
			if(rowmap == null)
				incs.put(row, rowmap = new TreeMap<>(Bytes.BYTES_COMPARATOR));
			Long inc = rowmap.get(q);
			inc = (inc == null ? 1 : inc + 1);
			rowmap.put(q, inc);
		}
	}
	
	public void consume(List<String> content, int maxlen) {
		List<String> prefix = new ArrayList<>();
		for(String suffix : content) {
			List<String> p = new ArrayList<>(prefix);
			while(p.size() > 0) {
				insert(p, suffix);
				p.remove(0);
			}
			prefix.add(suffix);
			if(prefix.size() > maxlen)
				prefix.remove(0);
		}
		insert(prefix, null);
	}

	public void flush() {
		try {
			List<Increment> l = new ArrayList<>();
			synchronized(incs) {
				for(Entry<byte[], Map<byte[], Long>> e : incs.entrySet()) {
					Increment inc = new Increment(e.getKey());
					for(Entry<byte[], Long> i : e.getValue().entrySet()) {
						inc.addColumn(SUFFIX_FAMILY, i.getKey(), i.getValue());
					}
					if(lifespan != null)
						inc.setTimeRange(System.currentTimeMillis(), System.currentTimeMillis() + lifespan);
					l.add(inc);
				}
				incs.clear();
				table.batch(l);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String next(List<String> prefix) {
		prefix = new ArrayList<>(prefix);
		Map<String, Double> suffixes = new TreeMap<>();
		double max = 0;
		for(; prefix.size() > 0; prefix.remove(0)) {
			byte[] row = Bytes.toBytes(StringContent.join(prefix).toUpperCase());
			if(row.length == 0)
				return null;
			Get get = new Get(row);
			get.addFamily(SUFFIX_FAMILY);
			Result result;
			try {
				result = table.get(get);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if(result.isEmpty()) {
				continue;
			}
			
			double smax = 0;
			for(byte[] v : result.getFamilyMap(SUFFIX_FAMILY).values())
				smax += Bytes.toLong(v);
			
			for(Entry<String, Double> e : suffixes.entrySet()) {
				suffixes.put(e.getKey(), suffixes.get(e.getKey()) * 5 * smax / max);
			}
			
			for(Entry<byte[], byte[]> e : result.getFamilyMap(SUFFIX_FAMILY).entrySet()) {
				String suffix = Bytes.toString(e.getKey());
				long count = Bytes.toLong(e.getValue());
				if(!suffixes.containsKey(suffix))
					suffixes.put(suffix, 0.);
				suffixes.put(suffix, count + suffixes.get(suffix));
			}

			max = 0;
			for(Entry<String, Double> e : suffixes.entrySet()) {
				max += suffixes.get(e.getKey());
			}
		}
		double smax = 0;
		for(double v : suffixes.values())
			smax += v;
		long idx = (long)(smax * Math.random());
		for(Entry<String, Double> e : suffixes.entrySet()) {
			if(idx < e.getValue()) {
				String s = e.getKey();
				if("\0".equals(s))
					return null;
				return s;
			}
			idx -= e.getValue();
		}
		return null;
	}

	public HTable getTable() {
		return table;
	}

	public Long getLifespan() {
		return lifespan;
	}

}
