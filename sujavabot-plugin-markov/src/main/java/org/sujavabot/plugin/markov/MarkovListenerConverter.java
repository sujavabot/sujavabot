package org.sujavabot.plugin.markov;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

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

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
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
		Database db = current.getMarkov().getDatabase();
		try {
			Environment e = db.getEnvironment();
			File home = e.getHome();
			String name = db.getDatabaseName();
			helper.field("home", File.class, () -> home);
			helper.field("name", String.class, () -> name);
		} catch(DatabaseException e) {
			throw new RuntimeException(e);
		}
		helper.field("maxlen", Integer.class, () -> current.getMaxlen());
		helper.field("prefix", String.class, () -> current.getPrefix().pattern());
		helper.field("learn", Boolean.class, () -> current.isLearn());
		for(String channel : current.getChannels())
			helper.field("channel", String.class, () -> channel);
		for(Pattern p : current.getIgnore())
			helper.field("ignore", String.class, () -> p.pattern());
	}

	@Override
	protected void configure(MarkovListener current, UnmarshalHelper helper) {
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			Map<String, Object> m = new HashMap<>();
			Set<String> ch = new TreeSet<>();
			
			MarkovListener ml = new MarkovListener();
			ml.setIgnore(new ArrayList<>());
			
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);

			helper.field("home", File.class, f -> m.put("home", f));
			helper.field("name", String.class, s -> m.put("name", s));
			helper.field("maxlen", Integer.class, i -> m.put("maxlen", i));
			helper.field("prefix", String.class, s -> m.put("prefix", s));
			helper.field("learn", Boolean.class, b -> m.put("learn", b));
			helper.field("channel", String.class, s -> ch.add(s));
			helper.field("ignore", String.class, s -> ml.getIgnore().add(Pattern.compile(s)));
			
			helper.read(ml);

			EnvironmentConfig ec = new EnvironmentConfig();
			ec.setAllowCreate(true);
			Environment e = new Environment((File) m.get("home"), ec);
			
			DatabaseConfig dbc = new DatabaseConfig();
			dbc.setAllowCreate(true);
			dbc.setDeferredWrite(true);
			dbc.setReadOnly(!(Boolean) m.getOrDefault("learn", true));
			Database db = e.openDatabase(null, (String) m.get("name"), dbc);
			
			BerkeleyDBMarkov markov = new BerkeleyDBMarkov(db);
			
			ml.setChannels(ch);
			ml.setLearn((Boolean) m.getOrDefault("learn", true));
			ml.setMarkov(markov);
			ml.setMaxlen((Integer) m.getOrDefault("maxlen", 5));
			ml.setPrefix(Pattern.compile((String) m.getOrDefault("prefix", "@markov ")));
			
			return ml;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
