package org.sujavabot.plugin.markov;

import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.AbstractConverter;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class HBaseMarkovConverter extends AbstractConverter<HBaseMarkov> {

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new HBaseMarkovConverter(x));
		}
	}

	public HBaseMarkovConverter(XStream x) {
		super(x, HBaseMarkov.class);
	}
	
	@Override
	protected HBaseMarkov createDefaults(HBaseMarkov current) {
		return null;
	}

	@Override
	protected void configure(HBaseMarkov current, MarshalHelper helper, HBaseMarkov defaults) {
		helper.field("table", String.class, () -> current.getTable().getName().getNameAsString());
		helper.field("family", String.class, () -> Bytes.toString(current.getFamily()));
		if(current.getDuration() != null)
			helper.field("duration", Long.class, () -> current.getDuration());
		for(Entry<String, String> e : current.getConf()) {
			helper.field(e.getKey(), String.class, () -> e.getValue());
		}
	}

	@Override
	protected void configure(HBaseMarkov current, UnmarshalHelper helper) {
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			HBaseMarkov ml = new HBaseMarkov();
			Configuration conf = new Configuration();
			
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);

			helper.setDefaultHandler((Object o, UnmarshalHelper h) -> {
				String key = h.getReader().getNodeName();
				String val = h.getReader().getValue();
				System.out.println(key + " => " + val);
				conf.set(key, val);
			});
			
			helper.read(ml);

			TableName name = TableName.valueOf(conf.get("table"));
			
			ml.setConf(conf);
			if(conf.get("family") != null)
				ml.setFamily(Bytes.toBytes(conf.get("family")));
			if(conf.get("duration") != null)
				ml.setDuration(conf.getLong("duration", 0));
			Connection cxn = ConnectionFactory.createConnection(conf);
			Admin admin = cxn.getAdmin();
			try {
				if(!admin.tableExists(name))
					HBaseMarkov.createTable(conf, name);
			} finally {
				admin.close();
			}
			Table table = cxn.getTable(name);
			ml.setTable(table);
			
			return ml;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
