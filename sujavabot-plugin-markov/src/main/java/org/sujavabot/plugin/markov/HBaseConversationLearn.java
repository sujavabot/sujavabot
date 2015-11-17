package org.sujavabot.plugin.markov;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

public class HBaseConversationLearn {
	public static void main(String[] args) throws Exception {
		DOMConfigurator.configure(HBaseConversationLearn.class.getClassLoader().getResource("org/sujavabot/core/log4j.xml"));
		
		Configuration conf = HBaseConfiguration.create();
		GenericOptionsParser parser = new GenericOptionsParser(conf, args);
		conf = parser.getConfiguration();
		args = parser.getRemainingArgs();
		
		TableName name = TableName.valueOf(conf.get("table"));
		Long duration = null;
		if(conf.get("duration") != null)
			duration = conf.getLong("duration", 0);

		int maxlen = conf.getInt("maxlen", 5);
		
		String familyName = conf.get("family", "conversation");
		byte[] family = Bytes.toBytes(familyName);
		
		boolean inverse = conf.getBoolean("inverse", false);
		
		boolean buffer = conf.getBoolean("buffer", true);

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
			
			HBaseConversation conversation = new HBaseConversation();
			conversation.setConf(conf);
			conversation.setDuration(duration);
			conversation.setTable(table);
			conversation.setFamily(family);
			conversation.setNosync(buffer);

			InputStream[] inputs = new InputStream[] { System.in };
			if(args.length > 0) {
				inputs = new InputStream[args.length];
				for(int i = 0; i < args.length; i++)
					inputs[i] = new FileInputStream(args[i]);
			}
			
			List<String> previous = new ArrayList<>();
			
			for(InputStream in : inputs) {
				long total = in.available();
				long pct = -1;
				long start = System.currentTimeMillis();
				BufferedReader buf = new BufferedReader(new InputStreamReader(in, "UTF-8"), 256);
				for(String line = buf.readLine(); line != null; line = buf.readLine()) {
					String context = line.replaceAll("^<(\\S+)>.*", "$1");
					line = line.replaceAll("^(\\S+:\\s*|<\\S+>\\s*)*", "").trim();
					List<String> next = StringContent.parse(line);
					if(next.size() == 0)
						continue;
					if(inverse)
						Collections.reverse(next);
					conversation.consume(previous, next, context, maxlen);
					if(buffer)
						conversation.sync();
					long read = total - in.available();
					long rpct = read * 1000 / total;
					if(pct != rpct) {
						long dur = System.currentTimeMillis() - start;
						System.out.println(String.format("%03.1f%% (%d bytes, %d bytes per second)", rpct / 10., read, read * 1000 / dur));
					}
					pct = rpct;
					previous = next;
				}
				buf.close();
			}
			conversation.close();
		} finally {
			cxn.close();
		}
		
	}
}
