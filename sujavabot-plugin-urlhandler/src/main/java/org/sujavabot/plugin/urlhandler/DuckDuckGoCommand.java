package org.sujavabot.plugin.urlhandler;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class DuckDuckGoCommand extends AbstractReportingCommand implements HelperConvertable<DuckDuckGoCommand> {
	public static List<Entry<String, String>> search(String query) throws IOException {
		URL url = new URL("http://duckduckgo.com/html/?q=" + URLEncoder.encode(query, "UTF-8"));
		Document doc = Jsoup.connect(url.toString()).get();
		List<Entry<String, String>> ret = new ArrayList<>();
		for(Element r : doc.select("a.result__a")) {
			String rurl = r.attr("href");
			String rtitle = r.text();
			ret.add(new AbstractMap.SimpleImmutableEntry<>(rurl, rtitle));
		}
		return ret;
				
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		String query = StringUtils.join(args.subList(1, args.size()), " ");
		List<Entry<String, String>> res;
		try {
			res = search(query);
		} catch (IOException e) {
			return "error";
		}
		if(res.size() > 0)
			return res.get(0).getKey() + " - " + res.get(0).getValue();
		return "no results";
	}

	@Override
	public void configure(MarshalHelper helper, DuckDuckGoCommand defaults) {
	}

	@Override
	public void configure(UnmarshalHelper helper) {
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("search duckduckgo");
	}
}
