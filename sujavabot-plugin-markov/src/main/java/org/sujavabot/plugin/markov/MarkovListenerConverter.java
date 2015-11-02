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
		Markov m = current.getMarkov();
		Markov im = current.getInverseMarkov();
		helper.field("markov", () -> m);
		if(im != null)
			helper.field("inverse-markov", () -> im);
		helper.field("prefix-power", Double.class, () -> m.getPrefixPower());
		helper.field("maxlen", Integer.class, () -> current.getMaxlen());
		helper.field("prefix", String.class, () -> current.getPrefix().pattern());
		helper.field("learn", Boolean.class, () -> current.isLearn());
		for(String channel : current.getChannels())
			helper.field("channel", String.class, () -> channel);
		for(Pattern p : current.getIgnore())
			helper.field("ignore", String.class, () -> p.pattern());
		helper.field("shutdown-port", Integer.class, () -> current.getShutdownPort());
		for(Entry<Pattern, String> e : current.getContexts().entrySet()) {
			if(e.getKey().equals(current.getPrefix()))
				continue;
			helper.handler("context", (h) -> {
				h.getWriter().addAttribute("pattern", e.getKey().pattern());
				if(e.getValue() != null)
					h.getWriter().addAttribute("context", e.getValue());
			});
		}
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

			helper.field("markov", (o) -> m.put("markov", o));
			helper.field("inverse-markov", (o) -> m.put("inverse-markov", o));
			helper.field("prefix-power", Double.class, (d) -> m.put("prefix-power", d));
			helper.field("maxlen", Integer.class, i -> m.put("maxlen", i));
			helper.field("prefix", String.class, s -> m.put("prefix", s));
			helper.field("learn", Boolean.class, b -> m.put("learn", b));
			helper.field("channel", String.class, s -> ch.add(s));
			helper.field("ignore", String.class, s -> ml.getIgnore().add(Pattern.compile(s)));
			helper.field("thesaurus", Boolean.class, b -> m.put("thesaurus", b));
			helper.field("shutdown-port", Integer.class, i -> m.put("shutdown-port", i));
			
			helper.handler("context", (o, h) -> {
				Pattern p = Pattern.compile(h.getReader().getAttribute("pattern"));
				String c = h.getReader().getAttribute("context");
				ml.getContexts().put(p, c);
			});
			
			helper.read(ml);

			Markov markov = (Markov) m.get("markov");
			markov.setPrefixPower((Double) m.getOrDefault("prefix-power", 5.));
			Markov im = (Markov) m.get("inverse-markov");
			if(im != null)
				im.setPrefixPower(markov.getPrefixPower());
			
			ml.setChannels(ch);
			ml.setLearn((Boolean) m.getOrDefault("learn", true));
			ml.setMarkov(markov);
			ml.setInverseMarkov(im);
			ml.setMaxlen((Integer) m.getOrDefault("maxlen", 5));
			ml.setPrefix(Pattern.compile((String) m.getOrDefault("prefix", "@markov ")));
			
			ml.setShutdownPort((Integer) m.getOrDefault("shutdown-port", -1));
			
			return ml;
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}
}
