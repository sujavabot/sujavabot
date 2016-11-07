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
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseMarkovIdentificationTable implements IdentificationTable {
	public static final String DEFAULT_FAMILY = "ident-suffix";
	public static final double DEFAULT_DISTANCE_POWER = 3.;
	
	protected static byte[] escapeBytes(byte[] b) {
		return Bytes.toBytes(Bytes.toStringBinary(b));
	}
	
	protected static byte[] unescapeBytes(byte[] b) {
		return Bytes.toBytesBinary(Bytes.toString(b));
	}
	
	protected static byte[] familyToBytes(String family) {
		return escapeBytes(Bytes.toBytes(family));
	}
	
	protected static String bytesToFamily(byte[] b) {
		return Bytes.toString(unescapeBytes(b));
	}
	
	protected Properties properties;
	protected Table table;
	protected byte[] family;
	protected double distancePower;

	public HBaseMarkovIdentificationTable() {
	}
	
	public HBaseMarkovIdentificationTable(Table table, String family) {
		this(table, family, DEFAULT_DISTANCE_POWER);
	}
	
	public HBaseMarkovIdentificationTable(Table table, String family, double distancePower) {
		this.table = table;
		this.family = familyToBytes(family);
		this.distancePower = distancePower;
	}
	
	@Override
	public void put(long timestamp, byte[] prefix, byte[] suffix, byte[] id) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Map<byte[], Double> get(long timestamp, byte[] prefix, byte[] suffix) throws IOException {
		
		byte[] uprefix = Bytes.toBytes(Bytes.toString(prefix).toUpperCase());
		byte[] usuffix = Bytes.toBytes(Bytes.toString(suffix).toUpperCase());
		
		Get get = new Get(uprefix);
		get.addFamily(family);
		
		Map<byte[], Double> freqs = new TreeMap<>(Bytes.BYTES_COMPARATOR);
		Map<byte[], Double> distances = new TreeMap<>(Bytes.BYTES_COMPARATOR);
		
		EditDistancer distancer = new EditDistancer();
		Function<byte[], Double> distanceFn = (b) -> {
			double editDistance = 0;
			byte[] edits = distancer.compute(usuffix, b);
			for(int i = 0; i < edits.length; i++) {
				if(edits[i] == EditDistancer.Op.NEXT)
					editDistance += 1. / edits.length;
			}
			return Math.pow(editDistance, distancePower);
		};

		double normalizedTotal = 0;
		Result result = table.get(get);

		if(result.isEmpty())
			return freqs;
		
		for(Entry<byte[], byte[]> e : result.getFamilyMap(family).entrySet()) {
			String[] f = Bytes.toString(e.getKey()).split(" ", 2);
			byte[] resultSuffix = Bytes.toBytes(f[0].toUpperCase());
			byte[] resultId = Bytes.toBytes(f[1]);
			long count = Bytes.toLong(e.getValue());
			
			double idf = freqs.getOrDefault(resultId, 0.);
			double d = count * distances.computeIfAbsent(resultSuffix, distanceFn);
			if(d == 0)
				continue;
			normalizedTotal += d;
			freqs.put(resultId, idf + d);
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

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties conf) {
		this.properties = conf;
	}
}
