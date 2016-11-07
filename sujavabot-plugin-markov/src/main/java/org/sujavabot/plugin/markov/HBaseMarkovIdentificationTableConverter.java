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

public class HBaseMarkovIdentificationTableConverter extends AbstractConverter<HBaseMarkovIdentificationTable> {
	
	public static final String TABLE = "table";
	public static final String FAMILY = "family";
	public static final String DISTANCE_POWER = "distance-power";

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new HBaseMarkovIdentificationTableConverter(x));
		}
	}

	public HBaseMarkovIdentificationTableConverter(XStream x) {
		super(x, HBaseMarkovIdentificationTable.class);
	}
	
	@Override
	protected HBaseMarkovIdentificationTable createDefaults(HBaseMarkovIdentificationTable current) {
		return null;
	}

	@Override
	protected void configure(HBaseMarkovIdentificationTable current, MarshalHelper helper, HBaseMarkovIdentificationTable defaults) {
		Properties props = new Properties();
		if(current.getProperties() != null)
			props.putAll(current.getProperties());
		
		props.setProperty(TABLE, current.getTable().getName().getNameAsString());
		props.setProperty(FAMILY, current.getFamily());
		props.setProperty(DISTANCE_POWER, Double.toString(current.getDistancePower()));
		
		for(String key : props.stringPropertyNames()) {
			helper.field(key, String.class, () -> props.getProperty(key));
		}
	}

	@Override
	protected void configure(HBaseMarkovIdentificationTable current, UnmarshalHelper helper) {
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			HBaseMarkovIdentificationTable idtable = new HBaseMarkovIdentificationTable();
			Properties props = new Properties();
			
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);

			helper.setDefaultHandler((Object o, UnmarshalHelper h) -> props.setProperty(h.getReader().getNodeName(), h.getReader().getValue()));
			
			helper.read(idtable);
			
			String table = (String) props.remove(TABLE);
			String family = (String) props.remove(FAMILY);
			double distancePower = props.containsKey(DISTANCE_POWER) ? Double.parseDouble((String) props.remove(DISTANCE_POWER)) : HBaseMarkovIdentificationTable.DEFAULT_DISTANCE_POWER;
			
			idtable.setProperties(props);
			
			Configuration conf = new Configuration();
			
			for(String key : props.stringPropertyNames()) {
				conf.set(key, props.getProperty(key));
			}

			TableName tableName = TableName.valueOf(table);
			
			Connection hcxn = ConnectionFactory.createConnection(conf);
			Table htable = hcxn.getTable(tableName);
			
			idtable.setTable(htable);
			idtable.setFamily(family);
			idtable.setDistancePower(distancePower);
			
			return idtable;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
