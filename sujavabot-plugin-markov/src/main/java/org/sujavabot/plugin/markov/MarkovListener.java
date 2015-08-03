package org.sujavabot.plugin.markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;


public class MarkovListener extends ListenerAdapter<PircBotX> {
	protected BerkeleyDBMarkov markov;
	protected int maxlen;
	protected Set<String> channels;
	protected boolean learn;
	protected String prefix;
	protected List<Pattern> ignore;
	
	protected List<String> responses = new ArrayList<>();
	
	public MarkovListener() {}
	
	public BerkeleyDBMarkov getMarkov() {
		return markov;
	}
	
	public int getMaxlen() {
		return maxlen;
	}
	
	public Set<String> getChannels() {
		return channels;
	}
	
	public boolean isLearn() {
		return learn;
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(!channels.contains(event.getChannel().getName()))
			return;
		for(Pattern p : ignore) {
			if(p.matcher(event.getUser().getNick()).matches())
				return;
		}
		String m = event.getMessage();
		if(m.startsWith(prefix)) {
			m = m.substring(prefix.length()).trim();
			m = m.replaceAll("\\?+$", "");
			List<String> prefix = StringContent.parse(m);
			if(prefix.size() == 0)
				return;
			MarkovIterator mi = new MarkovIterator(markov, maxlen, prefix);
			List<String> ml = mi.toList();
//			ml.subList(0, prefix.size()).clear();
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
			while(ml.size() > 0 && !ml.get(0).matches(".*\\w.*"))
				ml.remove(0);
			if(ml.size() > 0) {
				String r = StringContent.join(ml);
				responses.add(r);
				while(responses.size() > 3)	
					responses.remove(0);
				if(Collections.frequency(responses, r) < 3)
					event.getChannel().send().message(event.getUser().getNick() + ": " + r);
			}
		} else if(learn) {
			m = m.replaceAll("^\\S+:", "");
			List<String> content = StringContent.parse(m);
			markov.consume(content, maxlen);
		}
	}

	public void setMarkov(BerkeleyDBMarkov markov) {
		this.markov = markov;
	}

	public void setMaxlen(int maxlen) {
		this.maxlen = maxlen;
	}

	public void setChannels(Set<String> channels) {
		this.channels = channels;
	}

	public void setLearn(boolean learn) {
		this.learn = learn;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public List<Pattern> getIgnore() {
		return ignore;
	}
	
	public void setIgnore(List<Pattern> ignore) {
		this.ignore = ignore;
	}
}
