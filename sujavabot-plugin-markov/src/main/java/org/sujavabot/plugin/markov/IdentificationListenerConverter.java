package org.sujavabot.plugin.markov;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.AbstractConverter;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public class IdentificationListenerConverter extends AbstractConverter<IdentificationListener> {

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new IdentificationListenerConverter(x));
		}
	}

	public IdentificationListenerConverter(XStream x) {
		super(x, IdentificationListener.class);
	}
	
	@Override
	protected IdentificationListener createDefaults(IdentificationListener current) {
		return null;
	}

	@Override
	protected void configure(IdentificationListener current, MarshalHelper helper, IdentificationListener defaults) {
		for(String channel : current.getChannels())
			helper.field("channel", String.class, () -> channel);
		helper.field("maxlen", Integer.class, () -> current.getIdent().getMaxlen());
		helper.field("forward", () -> current.getIdent().getForwardTable());
		helper.field("reverse", () -> current.getIdent().getReverseTable());
	}

	@Override
	protected void configure(IdentificationListener current, UnmarshalHelper helper) {
		helper.field("channel", String.class, (s) -> {
			if(current.getChannels() == null)
				current.setChannels(new TreeSet<>());
			current.getChannels().add(s);
		});
		helper.field("maxlen", Integer.class, (i) -> {
			if(current.getIdent() == null)
				current.setIdent(new Identification());
			current.getIdent().setMaxlen(i);
		});
		helper.field("forward", (t) -> {
			if(current.getIdent() == null)
				current.setIdent(new Identification());
			current.getIdent().setForwardTable((IdentificationTable) t);
		});
		helper.field("reverse", (t) -> {
			if(current.getIdent() == null)
				current.setIdent(new Identification());
			current.getIdent().setReverseTable((IdentificationTable) t);
		});
		
	}

}