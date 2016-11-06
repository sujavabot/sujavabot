package org.sujavabot.plugin.markov;

import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Table;
import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.AbstractConverter;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class HBaseIdentificationTableConverter extends AbstractConverter<HBaseIdentificationTable> {
	
	public static final String TABLE = "table";
	public static final String FAMILY = "family";
	public static final String DISTANCE_POWER = "distancepower";
	public static final String DURATION = "duration";
	public static final String AUTOFLUSH = "autoflush";
	public static final String BATCH = "batching";
	public static final String CACHING = "caching";

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new HBaseIdentificationTableConverter(x));
		}
	}

	public HBaseIdentificationTableConverter(XStream x) {
		super(x, HBaseIdentificationTable.class);
	}
	
	@Override
	protected HBaseIdentificationTable createDefaults(HBaseIdentificationTable current) {
		return null;
	}

	@Override
	protected void configure(HBaseIdentificationTable current, MarshalHelper helper, HBaseIdentificationTable defaults) {
		Properties props = new Properties();
		if(current.getProperties() != null)
			props.putAll(current.getProperties());
		
		props.setProperty(TABLE, current.getTable().getName().getNameAsString());
		props.setProperty(FAMILY, current.getFamily());
		props.setProperty(DISTANCE_POWER, Double.toString(current.getDistancePower()));
		if(current.getDuration() != null)
			props.setProperty(DURATION, Long.toString(current.getDuration()));
		if(current.getBatch() != null)
			props.setProperty(BATCH, Integer.toString(current.getBatch()));
		if(current.getCaching() != null)
			props.setProperty(CACHING, Integer.toString(current.getCaching()));
		
		for(String key : props.stringPropertyNames()) {
			helper.field(key, String.class, () -> current.getProperties().getProperty(key));
		}
	}

	@Override
	protected void configure(HBaseIdentificationTable current, UnmarshalHelper helper) {
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			HBaseIdentificationTable idtable = new HBaseIdentificationTable();
			Properties props = new Properties();
			
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);

			helper.setDefaultHandler((Object o, UnmarshalHelper h) -> props.setProperty(h.getReader().getNodeName(), h.getReader().getValue()));
			
			helper.read(idtable);
			
			String table = (String) props.remove(TABLE);
			String family = (String) props.remove(FAMILY);
			double distancePower = props.containsKey(DISTANCE_POWER) ? Double.parseDouble(DISTANCE_POWER) : HBaseIdentificationTable.DEFAULT_DISTANCE_POWER;
			Long duration = props.containsKey(DURATION) ? Long.parseLong((String) props.remove(DURATION)) : null;
			Integer batch = props.containsKey(BATCH) ? Integer.parseInt((String) props.remove(BATCH)) : null;
			Integer caching = props.containsKey(CACHING) ? Integer.parseInt((String) props.remove(CACHING)) : null;
			
			idtable.setProperties(props);
			
			Configuration conf = new Configuration();
			
			for(String key : props.stringPropertyNames()) {
				conf.set(key, props.getProperty(key));
			}

			TableName tableName = TableName.valueOf(table);
			
			Connection hcxn = ConnectionFactory.createConnection(conf);
			Table htable = hcxn.getTable(tableName);
			
			if(conf.get(AUTOFLUSH) != null && (htable instanceof HTable))
				((HTable) htable).setAutoFlush(conf.getBoolean(AUTOFLUSH, false), false);
			
			idtable.setTable(htable);
			idtable.setFamily(family);
			idtable.setDistancePower(distancePower);
			idtable.setDuration(duration);
			idtable.setBatch(batch);
			idtable.setCaching(caching);
			
			return idtable;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
