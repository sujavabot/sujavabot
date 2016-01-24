package org.sujavabot.plugin.google;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;

public class GoogleSearchCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args);
		String query = args.get(1);
		int max = Integer.parseInt(args.get(2));
		
		String google = "http://www.google.com/search?q=";
		String charset = "UTF-8";
		String userAgent = "SujavaBot 0.0.1-SNAPSHOT (+http://github.com/sujavabot/sujavabot)";

		String sep = "";
		StringBuilder sb = new StringBuilder();
		
		try {
			Elements links = Jsoup.connect(google + URLEncoder.encode(query, charset)).userAgent(userAgent).get().select("li.g>h3>a");
	
			for (Element link : links) {
				if(--max < 0)
					break;
				
			    String title = link.text();
			    String url = link.absUrl("href"); // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
			    url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");
	
			    if (!url.startsWith("http")) {
			        continue; // Ads/news/etc.
			    }
			    
			    sb.append(sep);
			    sb.append(url);
			    sb.append(" ");
			    sb.append(title);
			    sep = " :: ";
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		
		return sb.toString();
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<query_string> <num_results>: perform a google search");
	}

}
