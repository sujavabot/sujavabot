package org.sujavabot.plugin.markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
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
		if(args.size() < 4)
			return invokeHelp(bot, cause, args);
		if("who".equals(args.get(1))) {
			int count;
			try {
				count = Integer.parseInt(args.get(2));
			} catch(RuntimeException e) {
				return invokeHelp(bot, cause, args, "who");
			}
			if(count <= 0)
				return invokeHelp(bot, cause, args, "who");
			List<String> prompt = StringContent.parse(StringUtils.join(args.subList(3, args.size()), " "));
			if(prompt.size() < Identification.MIN_CONTENT_SIZE)
				return "minimum prompt size is " + Identification.MIN_CONTENT_SIZE;
			Map<String, Double> ids;
			try {
				ids = ident.identify(System.currentTimeMillis(), prompt);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			List<String> ret = new ArrayList<String>();
			for(Entry<String, Double> e : ids.entrySet()) {
				String nick = e.getKey();
				double prob = e.getValue();
				ret.add(String.format("%s (probability %d%%)", nick, Math.round(prob * 100)));
				if(--count == 0)
					break;
			}
			if(ret.size() == 0)
				return "no identification match";
			return StringUtils.join(ret, ", ");
		}
		if("did".equals(args.get(1))) {
			Pattern npat;
			try {
				npat = Pattern.compile(args.get(2), Pattern.CASE_INSENSITIVE);
			} catch(RuntimeException e) {
				return invokeHelp(bot, cause, args, "did");
			}
			List<String> prompt = StringContent.parse(StringUtils.join(args.subList(3, args.size()), " "));
			if(prompt.size() < Identification.MIN_CONTENT_SIZE)
				return "minimum prompt size is " + Identification.MIN_CONTENT_SIZE;
			Map<String, Double> ids;
			try {
				ids = ident.identify(System.currentTimeMillis(), prompt);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			double prob = 0;
			for(Entry<String, Double> e : ids.entrySet()) {
				String nick = e.getKey();
				if(!npat.matcher(nick).matches())
					continue;
				prob += e.getValue();
			}
			return String.format("%s (probability %d%%)", prob >= 0.5 ? "Yes" : "No", Math.round(prob * 100));
		}
		return invokeHelp(bot, cause, args);
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("identify text using frequency analysis",
				"who", "<count> <prompt>: identify nicks most likely to have said the prompt",
				"did", "<pattern> <prompt>: identify whether a nick would likely say a prompt");
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
