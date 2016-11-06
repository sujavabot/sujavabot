package org.sujavabot.plugin.markov;

import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseIdentificationTable implements IdentificationTable {
	public static final String DEFAULT_FAMILY = "ident-suffix";
	public static final double DEFAULT_DISTANCE_POWER = 3.;
	
	protected static byte[] escapeBytes(byte[] b) {
		return Bytes.toBytes(Bytes.toStringBinary(b));
	}
	
	protected static byte[] unescapeBytes(byte[] b) {
		return Bytes.toBytesBinary(Bytes.toString(b));
	}
	
	protected static byte[] unescapeBytes(byte[] b, int off, int len) {
		return Bytes.toBytesBinary(Bytes.toString(b, off, len));
	}
	
	protected static byte[] familyToBytes(String family) {
		return escapeBytes(Bytes.toBytes(family));
	}
	
	protected static String bytesToFamily(byte[] b) {
		return Bytes.toString(unescapeBytes(b));
	}
	
	protected static byte[] prefixToBytes(byte[] prefix) {
		return escapeBytes(prefix);
	}
	
	protected static byte[] suffixToBytes(byte[] suffix) {
		return escapeBytes(suffix);
	}
	
	protected static byte[] prefixToStartRow(byte[] prefix) {
		byte[] b = prefixToBytes(prefix);
		return Arrays.copyOf(b, b.length + 1);
	}
	
	protected static byte[] prefixToStopRow(byte[] prefix) {
		byte[] b = prefixToStartRow(prefix);
		b[b.length - 1] = 1;
		return b;
	}
	
	protected static byte[] tupleToRow(byte[] prefix, byte[] suffix) {
		return Bytes.add(prefixToStartRow(prefix), suffixToBytes(suffix));
	}
	
	protected static byte[] idToBytes(byte[] id) {
		return escapeBytes(id);
	}
	
	protected static byte[] bytesToId(byte[] b) {
		return unescapeBytes(b);
	}
	
	protected Properties properties;
	protected Table table;
	protected byte[] family;
	protected double distancePower;

	protected Long duration;
	protected Integer batch;
	protected Integer caching;
	
	public HBaseIdentificationTable() {
	}
	
	public HBaseIdentificationTable(Table table) {
		this(table, DEFAULT_FAMILY);
	}
	
	public HBaseIdentificationTable(Table table, String family) {
		this(table, family, DEFAULT_DISTANCE_POWER);
	}
	
	public HBaseIdentificationTable(Table table, String family, double distancePower) {
		this.table = table;
		this.family = familyToBytes(family);
		this.distancePower = distancePower;
	}
	
	protected Increment createIncrement(long timestamp, byte[] prefix, byte[] suffix, byte[] id) throws IOException {
		Increment inc = new Increment(tupleToRow(prefix, suffix));
		inc.addColumn(family, idToBytes(id), 1L);
		return inc;
	}
	
	protected Increment configureIncrement(long timestamp, byte[] prefix, byte[] suffix, byte[] id, Increment inc) throws IOException {
		if(duration != null)
			inc.setTimeRange(timestamp, timestamp + duration);
		return inc;
	}
	
	@Override
	public void put(long timestamp, byte[] prefix, byte[] suffix, byte[] id) throws IOException {
		Increment inc = createIncrement(timestamp, prefix, suffix, id);
		inc = configureIncrement(timestamp, prefix, suffix, id, inc);
		table.increment(inc);
	}
	
	protected Scan createScan(long timestamp, byte[] prefix, byte[] suffix) throws IOException {
		byte[] startRow = prefixToStartRow(prefix);
		byte[] stopRow = prefixToStopRow(prefix);
		Scan scan = new Scan(startRow, stopRow);
		scan.addFamily(family);
		return scan;
	}
	
	protected Scan configureScan(long timestamp, byte[] prefix, byte[] suffix, Scan scan) throws IOException {
		if(duration != null)
			scan.setTimeRange(timestamp - duration, timestamp);
		if(batch != null)
			scan.setBatch(batch);
		if(caching != null)
			scan.setCaching(caching);
		return scan;
	}
	
	@Override
	public Map<byte[], Double> get(long timestamp, byte[] prefix, byte[] suffix) throws IOException {
		Scan scan = createScan(timestamp, prefix, suffix);
		scan = configureScan(timestamp, prefix, suffix, scan);
		
		int rowPrefixLength = scan.getStartRow().length;
		
		Map<byte[], Double> freqs = new TreeMap<>(Bytes.BYTES_COMPARATOR);
		EditDistancer distancer = new EditDistancer();
		
		for(Result result : table.getScanner(scan)) {
			byte[] row = result.getRow();
			byte[] rowSuffix = unescapeBytes(row, rowPrefixLength, row.length - rowPrefixLength);
			double editDistance = 0;
			
			byte[] edits = distancer.compute(suffix, rowSuffix);
			for(int i = 0; i < edits.length; i++) {
				if(edits[i] == EditDistancer.Op.NEXT)
					editDistance += 1. / edits.length;
			}
			
			editDistance = Math.pow(editDistance, distancePower);
			
			long resultCount = 0;
			for(Entry<byte[], byte[]> e : result.getFamilyMap(family).entrySet()) {
				resultCount += Bytes.toLong(e.getValue());
			}
			
			if(resultCount == 0)
				continue;
			
			for(Entry<byte[], byte[]> e : result.getFamilyMap(family).entrySet()) {
				byte[] id = bytesToId(e.getKey());
				long count = Bytes.toLong(e.getValue());
				
				double idfreq;
				if(!freqs.containsKey(id))
					freqs.put(id, idfreq = 0.);
				else
					idfreq = freqs.get(id);
				
				idfreq += (count * editDistance) / resultCount;
				freqs.put(id, idfreq);
			}
		}
		
		return freqs;
	}

	public Table getTable() {
		return table;
	}

	@Override
	public void flush() throws IOException {
		if(table instanceof HTable)
			((HTable) table).flushCommits();
		else if(table instanceof Flushable)
			((Flushable) table).flush();
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public String getFamily() {
		return bytesToFamily(family);
	}

	public void setFamily(String family) {
		this.family = familyToBytes(family);
	}

	public double getDistancePower() {
		return distancePower;
	}

	public void setDistancePower(double distancePower) {
		this.distancePower = distancePower;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long expiration) {
		this.duration = expiration;
	}

	public Integer getBatch() {
		return batch;
	}

	public void setBatch(Integer batch) {
		this.batch = batch;
	}

	public Integer getCaching() {
		return caching;
	}

	public void setCaching(Integer caching) {
		this.caching = caching;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties conf) {
		this.properties = conf;
	}
}
