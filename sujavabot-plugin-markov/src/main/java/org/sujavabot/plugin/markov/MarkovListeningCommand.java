package org.sujavabot.plugin.markov;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;

public class MarkovListeningCommand extends AbstractReportingCommand {
	protected BerkeleyDBMarkov markov;
	protected int maxlen;
	protected boolean learn;
	protected Set<String> channels;
	protected Set<Pattern> ignores;

	protected LearningListener learningListener = new LearningListener();

	protected class LearningListener extends ListenerAdapter<PircBotX> {
		@Override
		public void onMessage(MessageEvent<PircBotX> event) throws Exception {
			if(learn) {
				if(!channels.contains(event.getChannel().getName()))
					return;
				for(Pattern p : ignores) {
					if(p.matcher(event.getUser().getNick() + ": " + event.getMessage()).matches())
						return;
				}
				String m = event.getMessage();
				m = m.replaceAll("^\\S+:", "");
				List<String> content = StringContent.parse(m);
				markov.consume(content, maxlen);
				markov.getDatabase().sync();
			}
		}
	}

	@Override
	public void init(SujavaBot bot) {
		super.init(bot);
		bot.getConfiguration().getListenerManager().addListener(learningListener);
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		String m = "";
		for(String arg : args)
			m = m + arg + " ";
		m = m.trim();
		m = m.replaceAll("\\?+$", "");
		List<String> prefix = StringContent.parse(m);
		if(prefix.size() == 0)
			return null;
		MarkovIterator mi = new MarkovIterator(markov, maxlen, prefix);
		List<String> ml = mi.toList();
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
		return StringContent.join(ml);
	}

	public Markov getMarkov() {
		return markov;
	}

	public void setMarkov(BerkeleyDBMarkov markov) {
		this.markov = markov;
	}

	public int getMaxlen() {
		return maxlen;
	}

	public void setMaxlen(int maxlen) {
		this.maxlen = maxlen;
	}

	public boolean isLearn() {
		return learn;
	}

	public void setLearn(boolean learn) {
		this.learn = learn;
	}

	public Set<String> getChannels() {
		return channels;
	}

	public void setChannels(Set<String> channels) {
		this.channels = channels;
	}

	public Set<Pattern> getIgnores() {
		return ignores;
	}

	public void setIgnores(Set<Pattern> ignores) {
		this.ignores = ignores;
	}

}
