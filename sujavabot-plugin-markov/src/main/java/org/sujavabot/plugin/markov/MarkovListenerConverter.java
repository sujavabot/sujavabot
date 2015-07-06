package org.sujavabot.plugin.markov;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.HTable;
import org.sujavabot.core.ConfigurationBuilder;
import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.AbstractConverter;
import org.sujavabot.core.xml.AuthorizedGroupConverter;
import org.sujavabot.core.xml.AuthorizedUserConverter;
import org.sujavabot.core.xml.CommandsMapConverter;
import org.sujavabot.core.xml.ConfigurationBuilderConverter;
import org.sujavabot.core.xml.XStreams;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class MarkovListenerConverter extends AbstractConverter<MarkovListener> {

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new MarkovListenerConverter(x));
		}
	}

	public MarkovListenerConverter(XStream x) {
		super(x, MarkovListener.class);
	}
	
	@Override
	protected MarkovListener createDefaults(MarkovListener current) {
		return null;
	}

	@Override
	protected void configure(MarkovListener current, MarshalHelper helper, MarkovListener defaults) {
		Configuration c = current.getMarkov().getTable().getConfiguration();
		helper.field(HConstants.ZOOKEEPER_QUORUM, String.class, () -> c.get(HConstants.ZOOKEEPER_QUORUM));
		helper.field("table", String.class, () -> current.getMarkov().getTable().getName().getNameAsString());
		if(current.getMarkov().getLifespan() != null)
			helper.field("lifespan", Long.class, () -> current.getMarkov().getLifespan());
		helper.field("maxlen", Integer.class, () -> current.getMaxlen());
		helper.field("learn", Boolean.class, () -> current.isLearn());
		for(String channel : current.getChannels())
			helper.field("channel", String.class, () -> channel);
	}

	@Override
	protected void configure(MarkovListener current, UnmarshalHelper helper) {
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			Map<String, Object> m = new HashMap<>();
			Set<String> ch = new TreeSet<>();
			
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);

			helper.field(HConstants.ZOOKEEPER_QUORUM, String.class, s -> m.put(HConstants.ZOOKEEPER_QUORUM, s));
			helper.field("table", String.class, s -> m.put("table", s));
			helper.field("lifespan", Long.class, l -> m.put("lifespan", l));
			helper.field("maxlen", Integer.class, i -> m.put("maxlen", i));
			helper.field("learn", Boolean.class, b -> m.put("learn", b));
			helper.field("channel", String.class, s -> ch.add(s));
			
			MarkovListener ml = new MarkovListener();
			
			helper.read(ml);
			
			Configuration c = HBaseConfiguration.create();
			c.set(HConstants.ZOOKEEPER_QUORUM, (String) m.get(HConstants.ZOOKEEPER_QUORUM));
			
			HTableMarkov markov = new HTableMarkov(
					c, 
					TableName.valueOf((String) m.get("table")), 
					(Long) m.get("lifespan"));
			
			ml.setChannels(ch);
			ml.setLearn((Boolean) m.getOrDefault("learn", true));
			ml.setMarkov(markov);
			ml.setMaxlen((Integer) m.getOrDefault("maxlen", 5));
			
			return ml;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
