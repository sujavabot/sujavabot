package org.sujavabot.plugin.markov;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.SujavaBot;


public class MarkovListener extends ListenerAdapter<PircBotX> {
	protected BerkeleyDBMarkov markov;
	protected int maxlen;
	protected Set<String> channels;
	protected boolean learn;
	protected Pattern prefix;
	protected List<Pattern> ignore;
	protected int shutdownPort = -1;
	
	protected List<String> responses = new ArrayList<>();
	
	protected Set<String> firstJoined = new TreeSet<>();
	
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
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		if(!channels.contains(event.getChannel().getName()))
			return;
		if(event.getUser().getNick().equals(event.getBot().getNick())) {
			if(firstJoined.add(event.getChannel().getName()))
				return;
		}
		for(Pattern p : ignore) {
			if(p.matcher(event.getUser().getNick()).matches())
				return;
		}
		String m;
		if(((SujavaBot) event.getBot()).isVerified(event.getUser()))
			m = event.getUser().getNick() + " is";
		else
			m = "fucking unidentified " + event.getUser().getNick() + " is";
		List<String> prefix = StringContent.parse(m);
		MarkovIterator mi = new MarkovIterator(markov, maxlen, prefix);
		List<String> ml = mi.toList();
		while(ml.size() == prefix.size()) {
			ml = new MarkovIterator(markov, maxlen, prefix).toList();
		}
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
			event.getChannel().send().message(r);
		}
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
		Matcher pm = prefix.matcher(m);
		if(pm.find()) {
//			m = m.substring(prefix.length()).trim();
			m = m.substring(pm.end()).trim();
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
			Iterator<String> ci = content.iterator();
			while(ci.hasNext()) {
				if(StringContent.LINK.matcher(ci.next()).matches())
					ci.remove();
			}
			markov.consume(content, maxlen);
			markov.getDatabase().sync();
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

	public Pattern getPrefix() {
		return prefix;
	}

	public void setPrefix(Pattern prefix) {
		this.prefix = prefix;
	}
	
	public List<Pattern> getIgnore() {
		return ignore;
	}
	
	public void setIgnore(List<Pattern> ignore) {
		this.ignore = ignore;
	}

	public int getShutdownPort() {
		return shutdownPort;
	}

	public void setShutdownPort(int shutdownPort) {
		this.shutdownPort = shutdownPort;
		if(shutdownPort != -1) {
			new Thread() {
				public void run() {
					try {
						ServerSocket server = new ServerSocket(shutdownPort);
						server.accept();
						markov.close();
						System.exit(0);
					} catch(Exception e) {
						throw new RuntimeException(e);
					}
				}
			}.start();
		}
	}
}
