package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class IdentificationCommand extends AbstractReportingCommand implements HelperConvertable<IdentificationCommand> {

	protected Identification ident;
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		List<String> prompt = StringContent.parse(StringUtils.join(args.subList(1, args.size()), " "));
		if(prompt.size() < Identification.MIN_CONTENT_SIZE)
			return "minimum prompt size is " + Identification.MIN_CONTENT_SIZE;
		Map<String, Double> ids;
		try {
			ids = ident.identify(System.currentTimeMillis(), prompt);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for(Entry<String, Double> e : ids.entrySet()) {
			String nick = e.getKey();
			double prob = e.getValue();
			return String.format("%s (probability %3.0f%%)", nick, prob * 100);
		}
		return "no identification match";
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<prompt>: attempt to identify a prompt");
	}

	@Override
	public void configure(MarshalHelper helper, IdentificationCommand defaults) {
		IdentificationCommand current = this;
		
		helper.field("maxlen", Integer.class, () -> current.getIdent().getMaxlen());
		helper.field("forward", () -> current.getIdent().getForwardTable());
		helper.field("reverse", () -> current.getIdent().getReverseTable());
		helper.field("prefix-power", Double.class, () -> current.getIdent().getPrefixPower());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		IdentificationCommand current = this;
		
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
		helper.field("prefix-power", Double.class, (d) -> {
			if(current.getIdent() == null)
				current.setIdent(new Identification());
			current.getIdent().setPrefixPower(d);
		});
	}

	public Identification getIdent() {
		return ident;
	}

	public void setIdent(Identification ident) {
		this.ident = ident;
	}

}
