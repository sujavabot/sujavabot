package org.sujavabot.plugin.urlhandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

public class URLTitleListener extends ListenerAdapter<PircBotX> {

	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		for(String word : event.getMessage().split("\\s+")) {
			URL url;
			try {
				url = new URL(word);
			} catch(MalformedURLException e) {
				continue;
			}
			String title;
			try {
				title = URLs.title(url);
			} catch(IOException e) {
				title = null;
			}
			if(title == null)
				continue;
			event.getChannel().send().message(title + " - " + url);
		}
	}
	
}
