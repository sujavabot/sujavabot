package org.sujavabot.plugin.markov;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;


public class MarkovListener extends ListenerAdapter<PircBotX> {
	protected static List<String> merge(int maxlen, String prefix, List<String> words) {
		List<String> merged = new ArrayList<>();
		merged.add(prefix + StringContent.join(words));
		while(merged.get(merged.size()-1).length() > maxlen) {
			String last = merged.get(merged.size() - 1);
			String head = last.substring(prefix.length(), maxlen);
			String tail = last.substring(maxlen);
			if(head.matches(".*\\s+\\S+") && tail.matches("\\S.*")) {
				String move = head.replaceAll("^.*\\s+(\\S+)$", "$1");
				head = head.substring(0, head.length() - move.length());
				tail = move + tail;
			}
			merged.set(merged.size() - 1, prefix + head.trim());
			merged.add(tail.trim());
			prefix = "";
		}
		return merged;
	}
	
	protected Markov markov;
	protected Markov inverseMarkov;
	protected int maxlen;
	protected Set<String> channels;
	protected boolean learn;
	protected Pattern prefix;
	protected List<Pattern> ignore;
	protected int shutdownPort = -1;
	
	protected int extensions = 10;
	
	protected Map<Pattern, String> contexts = new LinkedHashMap<>();
	
	protected List<String> responses = new ArrayList<>();
	
	protected Set<String> firstJoined = new TreeSet<>();
	
	public MarkovListener() {}
	
	public Markov getMarkov() {
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
		String context = null;
		boolean found = false;
		Matcher matcher = null;
		for(Entry<Pattern, String> e : contexts.entrySet()) {
			matcher = e.getKey().matcher(m);
			if(matcher.find()) {
				found = true;
				String v = e.getValue();
				if(v != null) {
					Matcher cm = Pattern.compile("\\$\\d+").matcher(v);
					int prev = 0;
					context = "";
					while(cm.find()) {
						context += v.substring(prev, cm.start());
						int i = Integer.parseInt(cm.group().substring(1));
						String mg = matcher.group(i);
						if(mg != null)
							context += mg;
						prev = cm.end();
					}
					context += v.substring(prev);
				}
				break;
			}
		}
		if(found) {
			if(matcher.start() == 0)
				m = m.substring(matcher.end()).trim();
			m = m.replaceAll("\\?+$", "");
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
			
			int maxlen = event.getBot().getConfiguration().getMaxLineLength();
			maxlen -= ("PRIVMSG " + event.getChannel().getName() + " :\r\n").length();
			
			List<String> lines = merge(
					maxlen,
					event.getUser().getNick() + ": ",
					ml);
			for(String line : lines) {
				event.getChannel().send().message(line);
			}
		} else if(learn) {
			m = m.replaceAll("^(\\S+:\\s*|<\\S+>\\s*)*", "");
			List<String> content = StringContent.parse(m);
			Iterator<String> ci = content.iterator();
			while(ci.hasNext()) {
				if(StringContent.LINK.matcher(ci.next()).matches())
					ci.remove();
			}
			if(content.size() > 0) {
				markov.consume(event.getUser().getNick(), content, maxlen);
				if(inverseMarkov != null) {
					Collections.reverse(content);
					inverseMarkov.consume(event.getUser().getNick(), content, maxlen);
				}
			}
		}
	}
	
	@Override
	public void onAction(ActionEvent<PircBotX> event) throws Exception {
		if(learn) {
			if(!channels.contains(event.getChannel().getName()))
				return;
			for(Pattern p : ignore) {
				if(p.matcher(event.getUser().getNick()).matches())
					return;
			}
			String m = event.getUser().getNick() + " " + event.getAction();

			m = m.replaceAll("^\\S+:", "");
			List<String> content = StringContent.parse(m);
			Iterator<String> ci = content.iterator();
			while(ci.hasNext()) {
				if(StringContent.LINK.matcher(ci.next()).matches())
					ci.remove();
			}
			if(content.size() > 0) {
				markov.consume(event.getUser().getNick(), content, maxlen);
				if(inverseMarkov != null) {
					Collections.reverse(content);
					inverseMarkov.consume(event.getUser().getNick(), content, maxlen);
				}
			}
		}
	}

	public void setMarkov(Markov markov) {
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
		contexts.put(prefix, null);
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
						server.close();
						markov.close();
						System.exit(0);
					} catch(Exception e) {
						throw new RuntimeException(e);
					}
				}
			}.start();
		}
	}

	public Map<Pattern, String> getContexts() {
		return contexts;
	}

	public void setContexts(Map<Pattern, String> contexts) {
		this.contexts = contexts;
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
