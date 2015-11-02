package org.sujavabot.plugin.markov;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.xml.DOMConfigurator;

public class HBaseLearn {
	public static void main(String[] args) throws Exception {
		DOMConfigurator.configure(HBaseLearn.class.getClassLoader().getResource("org/sujavabot/core/log4j.xml"));
		
		Configuration conf = HBaseConfiguration.create();
		GenericOptionsParser parser = new GenericOptionsParser(conf, args);
		conf = parser.getConfiguration();
		args = parser.getRemainingArgs();
		
		TableName name = TableName.valueOf(conf.get("table"));
		Long duration = null;
		if(conf.get("duration") != null)
			duration = conf.getLong("duration", 0);

		String context = conf.get("context");
		if(context == null)
			throw new IllegalArgumentException("must supply -Dcontext=");
		
		int maxlen = conf.getInt("maxlen", 5);
		
		String familyName = conf.get("family", "suffix");
		byte[] family = Bytes.toBytes(familyName);
		
		boolean inverse = conf.getBoolean("inverse", false);

		Connection cxn = ConnectionFactory.createConnection(conf);
		try {
			Admin admin = cxn.getAdmin();
			try {
				if(!admin.tableExists(name))
					HBaseMarkov.createTable(conf, name, family);
			} finally {
				admin.close();
			}
			
			@SuppressWarnings("deprecation")
			HTable table = new HTable(name, cxn);
			table.setAutoFlush(true, false);
			
			HBaseMarkov markov = new HBaseMarkov();
			markov.setConf(conf);
			markov.setDuration(duration);
			markov.setTable(table);
			markov.setFamily(family);
			markov.setNosync(true);

			InputStream[] inputs = new InputStream[] { System.in };
			if(args.length > 0) {
				inputs = new InputStream[args.length];
				for(int i = 0; i < args.length; i++)
					inputs[i] = new FileInputStream(args[i]);
			}
			
			for(InputStream in : inputs) {
				long total = in.available();
				long pct = -1;
				long start = System.currentTimeMillis();
				BufferedReader buf = new BufferedReader(new InputStreamReader(in, "UTF-8"), 256);
				for(String line = buf.readLine(); line != null; line = buf.readLine()) {
					line = line.replaceAll("^\\S+:", "").trim();
					List<String> words = StringContent.parse(line);
					if(words.size() == 0)
						continue;
					if(inverse)
						Collections.reverse(words);
					markov.consume(context, words, maxlen);
					long read = total - in.available();
					long rpct = read * 100 / total;
					if(pct != rpct) {
						markov.sync();
						long dur = System.currentTimeMillis() - start;
						System.out.println(String.format("%02d%% (%d bytes, %d bytes per second)", rpct, read, read * 1000 / dur));
					}
					pct = rpct;
				}
				buf.close();
			}
			markov.sync();
			markov.close();
		} finally {
			cxn.close();
		}
		
	}
}
