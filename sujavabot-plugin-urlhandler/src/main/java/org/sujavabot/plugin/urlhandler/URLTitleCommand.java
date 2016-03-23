package org.sujavabot.plugin.urlhandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;

public class URLTitleCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 2)
			return invokeHelp(bot, cause, args);
		try {
			URL url = new URL(args.get(1));
			String title = URLs.title(url);
			return (title == null ? "No HTML title" : title);
		} catch(Exception e) {
			return "Unable to fetch title: " + e;
		}
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<url>: returns the title of the URL");
	}

}
