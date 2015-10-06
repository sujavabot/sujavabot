package org.sujavabot.plugin.markov;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.AbstractConverter;
import org.sujavabot.core.xml.XStreams;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class BerkeleyDBMarkovConverter extends AbstractConverter<BerkeleyDBMarkov> {

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new BerkeleyDBMarkovConverter(x));
		}
	}

	public BerkeleyDBMarkovConverter(XStream x) {
		super(x, BerkeleyDBMarkov.class);
	}
	
	@Override
	protected BerkeleyDBMarkov createDefaults(BerkeleyDBMarkov current) {
		return null;
	}

	@Override
	protected void configure(BerkeleyDBMarkov current, MarshalHelper helper, BerkeleyDBMarkov defaults) {
		Database db = current.getDatabase();
		try {
			Environment e = db.getEnvironment();
			File home = e.getHome();
			String name = db.getDatabaseName();
			helper.field("home", File.class, () -> home);
			helper.field("name", String.class, () -> name);
		} catch(DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void configure(BerkeleyDBMarkov current, UnmarshalHelper helper) {
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			Map<String, Object> m = new HashMap<>();
			
			BerkeleyDBMarkov ml = new BerkeleyDBMarkov();
			
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);

			helper.field("home", File.class, f -> m.put("home", f));
			helper.field("name", String.class, s -> m.put("name", s));
			
			helper.read(ml);

			EnvironmentConfig ec = new EnvironmentConfig();
			ec.setAllowCreate(true);
			Environment e = new Environment((File) m.get("home"), ec);
			
			DatabaseConfig dbc = new DatabaseConfig();
			dbc.setAllowCreate(true);
			dbc.setDeferredWrite(true);
			Database db = e.openDatabase(null, (String) m.get("name"), dbc);

			ml.setEnvironment(e);
			ml.setDatabase(db);
			
			return ml;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
