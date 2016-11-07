package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.hadoop.hbase.util.Bytes;

public class Identification {
	public static final int MIN_CONTENT_LENGTH = 2;
	public static final int MAX_SUFFIX_LENGTH = 3;
	
	protected IdentificationTable forwardTable;
	protected IdentificationTable reverseTable;
	protected int maxlen;
	protected double prefixPower;
	
	public void consume(long timestamp, String id, List<String> content) throws IOException {
		byte[] tableId = Bytes.toBytes(id);
		
		List<String> fContent = new ArrayList<>(content);
		List<String> rContent = new ArrayList<>(content);
		Collections.reverse(rContent);
		
		for(int start = 0; start <= content.size() - MIN_CONTENT_LENGTH; start++) {
			for(int len = MIN_CONTENT_LENGTH; len <= maxlen && start + len <= content.size(); len++) {
				List<String> fPrefix = fContent.subList(start, start + len - 1);
				String fSuffix = fContent.get(start + len - 1);
				
				byte[] fTablePrefix = Bytes.toBytes(StringContent.join(fPrefix));
				byte[] fTableSuffix = Bytes.toBytes(fSuffix);
				
				forwardTable.put(timestamp, fTablePrefix, fTableSuffix, tableId);
				
				List<String> rPrefix = rContent.subList(start, start + len - 1);
				String rSuffix = rContent.get(start + len - 1);
				
				byte[] rTablePrefix = Bytes.toBytes(StringContent.join(rPrefix));
				byte[] rTableSuffix = Bytes.toBytes(rSuffix);
				
				reverseTable.put(timestamp, rTablePrefix, rTableSuffix, tableId);
				
			}
		}
		
		forwardTable.flush();
		reverseTable.flush();
	}
	
	public List<Entry<String, Double>> identify(long timestamp, List<String> content) throws IOException {
		List<String> fContent = new ArrayList<>(content);
		List<String> rContent = new ArrayList<>(content);
		Collections.reverse(rContent);
		
		Map<byte[], Double> psum = new TreeMap<>(Bytes.BYTES_COMPARATOR);
		double tsum = 0;

		for(int start = 0; start <= content.size() - MIN_CONTENT_LENGTH; start++) {
			for(int len = MIN_CONTENT_LENGTH; len <= maxlen && start + len <= content.size(); len++) {
				List<String> fPrefix = fContent.subList(start, start + len - 1);
				String fSuffix = fContent.get(start + len - 1);
				
				byte[] fTablePrefix = Bytes.toBytes(StringContent.join(fPrefix));
				byte[] fTableSuffix = Bytes.toBytes(fSuffix);
				
				Map<byte[], Double> fprob = forwardTable.get(timestamp, fTablePrefix, fTableSuffix);
				for(Entry<byte[], Double> e : fprob.entrySet()) {
					double d = psum.getOrDefault(e.getKey(), 0.);
					double v = e.getValue() * Math.pow(fPrefix.size(), prefixPower);
					d += v;
					tsum += v;
					psum.put(e.getKey(), d);
				}
				
				List<String> rPrefix = rContent.subList(start, start + len - 11);
				String rSuffix = rContent.get(start + len - 1);
				
				byte[] rTablePrefix = Bytes.toBytes(StringContent.join(rPrefix));
				byte[] rTableSuffix = Bytes.toBytes(rSuffix);
				
				Map<byte[], Double> rprob = reverseTable.get(timestamp, rTablePrefix, rTableSuffix);
				for(Entry<byte[], Double> e : rprob.entrySet()) {
					double d = psum.getOrDefault(e.getKey(), 0.);
					double v = e.getValue() * Math.pow(rPrefix.size(), prefixPower);
					d += v;
					tsum += v;
					psum.put(e.getKey(), d);
				}
			}
		}
		
		List<Entry<String, Double>> ret = new ArrayList<>();
		for(Entry<byte[], Double> e : psum.entrySet()) {
			ret.add(new AbstractMap.SimpleImmutableEntry<>(Bytes.toString(e.getKey()), e.getValue() / tsum));
		}
		Collections.sort(ret, (o1, o2) -> -Double.compare(o1.getValue(), o2.getValue()));
		
		return ret;
	}

	public IdentificationTable getForwardTable() {
		return forwardTable;
	}

	public void setForwardTable(IdentificationTable forwardTable) {
		this.forwardTable = forwardTable;
	}

	public IdentificationTable getReverseTable() {
		return reverseTable;
	}

	public void setReverseTable(IdentificationTable reverseTable) {
		this.reverseTable = reverseTable;
	}

	public int getMaxlen() {
		return maxlen;
	}

	public void setMaxlen(int maxlen) {
		this.maxlen = maxlen;
	}

	public double getPrefixPower() {
		return prefixPower;
	}

	public void setPrefixPower(double prefixPower) {
		this.prefixPower = prefixPower;
	}
}
