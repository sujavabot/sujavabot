package org.sujavabot.plugin.markov;

import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

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
	
	protected static byte[] bytesToSuffix(byte[] suffix) {
		return unescapeBytes(suffix);
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
	
	protected static byte[] tupleToRow(byte[] prefix, byte[] id) {
		return Bytes.add(prefixToStartRow(prefix), idToBytes(id));
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
	
	public HBaseIdentificationTable(Table table, String family) {
		this(table, family, DEFAULT_DISTANCE_POWER);
	}
	
	public HBaseIdentificationTable(Table table, String family, double distancePower) {
		this.table = table;
		this.family = familyToBytes(family);
		this.distancePower = distancePower;
	}
	
	protected Increment createIncrement(long timestamp, byte[] prefix, byte[] suffix, byte[] id) throws IOException {
		Increment inc = new Increment(tupleToRow(prefix, id));
		inc.addColumn(family, suffixToBytes(suffix), 1L);
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
		Map<byte[], Double> distances = new TreeMap<byte[], Double>(Bytes.BYTES_COMPARATOR);
		
		EditDistancer distancer = new EditDistancer();
		Function<byte[], Double> distanceFn = (b) -> {
			double editDistance = 0;
			byte[] edits = distancer.compute(suffix, b);
			byte op = EditDistancer.Op.NEXT;
			for(int i = 0; i < edits.length; i++) {
				if(edits[i] == EditDistancer.Op.NEXT)
					editDistance += 1. / edits.length;
				if(op != edits[i])
					editDistance -= 1. / edits.length;
				op = edits[i];
			}
			return editDistance <= 0 ? 0 : Math.pow(editDistance, distancePower);
		};
		
		double normalizedTotal = 0;
		for(Result result : table.getScanner(scan)) {
			byte[] row = result.getRow();
			byte[] id = unescapeBytes(row, rowPrefixLength, row.length - rowPrefixLength);
			
			double normalizedCount = 0;
			
			for(Entry<byte[], byte[]> e : result.getFamilyMap(family).entrySet()) {
				byte[] rowSuffix = bytesToSuffix(e.getKey());
				long count = Bytes.toLong(e.getValue());
				normalizedCount += count * distances.computeIfAbsent(rowSuffix, distanceFn);
			}
			normalizedTotal += normalizedCount;
			
			freqs.put(id, normalizedCount);
		}

		double nt = normalizedTotal;
		freqs.replaceAll((k, v) -> v / nt);
		
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
