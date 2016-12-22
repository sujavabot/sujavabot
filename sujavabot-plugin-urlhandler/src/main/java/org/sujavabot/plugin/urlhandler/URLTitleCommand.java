package org.sujavabot.plugin.urlhandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.HelperConvertable;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.plugin.urlhandler.AddressRanges.AddressRange;

public class URLTitleCommand extends AbstractReportingCommand implements HelperConvertable<URLTitleCommand> {

	private List<AddressRange> whitelist;
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 2)
			return invokeHelp(bot, cause, args);
		try {
			URL url = new URL(args.get(1));
			String title = URLs.title(url, whitelist);
			return (title == null ? "No HTML title" : title);
		} catch(Exception e) {
			return "Unable to fetch title: " + e;
		}
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<url>: returns the title of the URL");
	}

	@Override
	public void configure(MarshalHelper helper, URLTitleCommand defaults) {
		for(AddressRange r : whitelist)
			helper.field("allow", String.class, () -> r.toString());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		whitelist = new ArrayList<>();
		helper.field("allow", String.class, (s) -> whitelist.add(AddressRanges.parseAddressRange(s)));
	}
	
}
