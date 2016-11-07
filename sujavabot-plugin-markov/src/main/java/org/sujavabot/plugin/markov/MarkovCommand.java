package org.sujavabot.plugin.markov;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;


public class MarkovCommand extends AbstractReportingCommand implements HelperConvertable<MarkovCommand> {
	
	protected Markov markov;
	protected Markov inverseMarkov;
	protected int maxlen;
	
	protected int extensions = 10;
	
	public MarkovCommand() {}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<context-pattern> [<prompt>]: build a markov chain");
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 2)
			return invokeHelp(bot, cause, args);
		String context = args.get(1);
		String m = StringUtils.join(args.subList(2, args.size()), " ");
		List<String> prefix = StringContent.parse(m);
		List<String> ml = new MarkovIterator(context, markov, maxlen, prefix).toList();
		int size = ml.size();
		for(int i = 0; i < extensions; i++) {
			List<String> ml2 = new MarkovIterator(context, markov, maxlen, prefix).toList();
			if(ml2.size() > size) {
				size = ml2.size();
				ml = ml2;
			}
		}
		if(inverseMarkov != null) {
			Collections.reverse(ml);
			List<String> l = ml;
			size = l.size();
			for(int i = 0; i < extensions; i++) {
				List<String> l2 = new MarkovIterator(context, inverseMarkov, maxlen, ml).toList();
				if(l2.size() > size) {
					size = l2.size();
					l = l2;
				}
			}
			ml = l;
			Collections.reverse(ml);
		}
		if(ml.size() == prefix.size()) {
			ml = new MarkovIterator(context, markov, maxlen, Arrays.asList(Markov.SOT)).toList();
			ml.remove(0);
			size = ml.size();
			for(int i = 0; i < extensions; i++) {
				List<String> ml2 = new MarkovIterator(context, markov, maxlen, Arrays.asList(Markov.SOT)).toList();
				ml2.remove(0);
				if(ml2.size() > size) {
					size = ml2.size();
					ml = ml2;
				}
			}
		}
		if(ml.size() == 0)
			ml = Arrays.asList("i have nothing to say to that");
		for(int i = ml.size() - 3; i >= 0; i--) {
			int j = i+3;
			List<String> sub = ml.subList(i, j);
			if(Collections.frequency(sub, sub.get(0)) == j - i) {
				while(i >= 0 && Collections.frequency(sub, sub.get(0)) == j - i) { 
					i--;
					if(i >= 0)
						sub = ml.subList(i, j);
				}
				i++;
				ml.subList(i, j-3).clear();
			}
		}
		
		return StringContent.join(ml);
	}
	
	@Override
	public void configure(MarshalHelper helper, MarkovCommand defaults) {
		Markov m = getMarkov();
		Markov im = getInverseMarkov();
		helper.field("markov", () -> m);
		if(im != null)
			helper.field("inverse-markov", () -> im);
		helper.field("prefix-power", Double.class, () -> m.getPrefixPower());
		helper.field("maxlen", Integer.class, () -> getMaxlen());
		helper.field("extensions", Integer.class, () -> getExtensions());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("markov", (Markov m) -> this.markov = m);
		helper.field("inverse-markov", (Markov im) -> this.inverseMarkov = im);
		helper.field("prefix-power", Double.class, (p) -> {
			if(this.markov != null)
				this.markov.setPrefixPower(p);
			if(this.inverseMarkov != null)
				this.inverseMarkov.setPrefixPower(p);
		});
		helper.field("maxlen", Integer.class, (len) -> this.maxlen = len);
		helper.field("extensions", Integer.class, (e) -> this.extensions = e);
	}

	public Markov getMarkov() {
		return markov;
	}

	public int getMaxlen() {
		return maxlen;
	}

	public void setMarkov(Markov markov) {
		this.markov = markov;
	}

	public void setMaxlen(int maxlen) {
		this.maxlen = maxlen;
	}

	public Markov getInverseMarkov() {
		return inverseMarkov;
	}

	public void setInverseMarkov(Markov inverseMarkov) {
		this.inverseMarkov = inverseMarkov;
	}

	public int getExtensions() {
		return extensions;
	}

	public void setExtensions(int extensions) {
		this.extensions = extensions;
	}
}
