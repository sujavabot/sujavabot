package org.sujavabot.plugin.markov;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.xml.DOMConfigurator;

public class HBaseLearn {
	public static void main(String[] args) throws Exception {
		DOMConfigurator.configure(HBaseLearn.class.getClassLoader().getResource("org/sujavabot/core/log4j.xml"));
		
		Configuration conf = HBaseConfiguration.create();
		GenericOptionsParser parser = new GenericOptionsParser(conf, args);
		conf = parser.getConfiguration();
		args = parser.getRemainingArgs();
		
		System.out.println(Arrays.asList(args));
		
		TableName name = TableName.valueOf(conf.get("table"));
		Long duration = null;
		if(conf.get("duration") != null)
			duration = conf.getLong("duration", 0);

		int maxlen = conf.getInt("maxlen", 5);

		Connection cxn = ConnectionFactory.createConnection(conf);
		try {
			Admin admin = cxn.getAdmin();
			try {
				if(!admin.tableExists(name))
					HBaseMarkov.createTable(conf, name);
			} finally {
				admin.close();
			}
			Table table = cxn.getTable(name);
			
			HBaseMarkov markov = new HBaseMarkov();
			markov.setConf(conf);
			markov.setDuration(duration);
			markov.setTable(table);
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
					markov.consume(words, maxlen);
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
