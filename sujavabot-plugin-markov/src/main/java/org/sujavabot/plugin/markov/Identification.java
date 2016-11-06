package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.hadoop.hbase.util.Bytes;

public class Identification {
	private static final int MIN_CONTENT_SIZE = 2;
	
	protected IdentificationTable forwardTable;
	protected IdentificationTable reverseTable;
	protected int maxlen;
	
	public void consume(long timestamp, String id, List<String> content) throws IOException {
		byte[] tableId = Bytes.toBytes(id);
		
		List<String> fContent = new ArrayList<>(content);
		List<String> rContent = new ArrayList<>(content);
		Collections.reverse(rContent);
		
		for(int start = 0; start < content.size() - MIN_CONTENT_SIZE; start++) {
			for(int stop = MIN_CONTENT_SIZE; stop < maxlen && start + stop <= content.size(); stop++) {
				List<String> fPrefix = fContent.subList(start, stop - 1);
				String fSuffix = fContent.get(stop - 1);
				
				byte[] fTablePrefix = Bytes.toBytes(StringContent.join(fPrefix));
				byte[] fTableSuffix = Bytes.toBytes(fSuffix);
				
				forwardTable.put(timestamp, fTablePrefix, fTableSuffix, tableId);
				
				List<String> rPrefix = rContent.subList(start, stop - 1);
				String rSuffix = rContent.get(stop - 1);
				
				byte[] rTablePrefix = Bytes.toBytes(StringContent.join(rPrefix));
				byte[] rTableSuffix = Bytes.toBytes(rSuffix);
				
				reverseTable.put(timestamp, rTablePrefix, rTableSuffix, tableId);
			}
		}
		
		forwardTable.flush();
		reverseTable.flush();
	}
	
	public Map<String, Double> identify(long timestamp, List<String> content) throws IOException {
		List<String> fContent = new ArrayList<>(content);
		List<String> rContent = new ArrayList<>(content);
		Collections.reverse(rContent);
		
		Map<byte[], Double> psum = new TreeMap<>(Bytes.BYTES_COMPARATOR);
		
		double tsum = 0;
		for(int start = 0; start < content.size() - MIN_CONTENT_SIZE; start++) {
			for(int stop = MIN_CONTENT_SIZE; stop < maxlen && start + stop <= content.size(); stop++) {
				List<String> fPrefix = fContent.subList(start, stop - 1);
				String fSuffix = fContent.get(stop - 1);
				
				byte[] fTablePrefix = Bytes.toBytes(StringContent.join(fPrefix));
				byte[] fTableSuffix = Bytes.toBytes(fSuffix);
				
				Map<byte[], Double> fprob = forwardTable.get(timestamp, fTablePrefix, fTableSuffix);
				for(Entry<byte[], Double> e : fprob.entrySet()) {
					double d = psum.getOrDefault(e.getKey(), 0.);
					d += e.getValue();
					tsum += e.getValue();
					psum.put(e.getKey(), d);
				}

				List<String> rPrefix = rContent.subList(start, stop - 1);
				String rSuffix = rContent.get(stop - 1);
				
				byte[] rTablePrefix = Bytes.toBytes(StringContent.join(rPrefix));
				byte[] rTableSuffix = Bytes.toBytes(rSuffix);
				
				Map<byte[], Double> rprob = reverseTable.get(timestamp, rTablePrefix, rTableSuffix);
				for(Entry<byte[], Double> e : rprob.entrySet()) {
					double d = psum.getOrDefault(e.getKey(), 0.);
					d += e.getValue();
					tsum += e.getValue();
					psum.put(e.getKey(), d);
				}
			}
		}
		
		Map<String, Double> idprobs = new LinkedHashMap<>();
		while(psum.size() > 0) {
			byte[] key = null;
			for(byte[] k : psum.keySet()) {
				if(key == null || psum.get(k) > psum.get(key))
					key = k;
			}
			double isum = psum.remove(key);
			idprobs.put(Bytes.toString(key), isum / tsum);
		}
		
		return idprobs;
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
}
