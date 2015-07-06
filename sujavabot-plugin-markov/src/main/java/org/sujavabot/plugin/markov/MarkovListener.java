package org.sujavabot.plugin.markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;


public class MarkovListener extends ListenerAdapter<PircBotX> {
	protected HTableMarkov markov;
	protected int maxlen;
	
	protected List<String> responses = new ArrayList<>();
	
	public MarkovListener(HTableMarkov markov, int maxlen) {
		this.markov = markov;
		this.maxlen = maxlen;
	}
	
	@Override
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		MessageEvent<PircBotX> e = new MessageEvent<PircBotX>(event.getBot(), event.getChannel(), event.getUser(), event.getBot().getNick() + ": " + event.getUser().getNick());
//		onMessage(e);
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		String m = event.getMessage();
		if(m.matches(event.getBot().getNick() + "[,:].*")) {
			m = m.split("[,:]", 2)[1];
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
		} else if("snacky".equals(event.getUser().getNick()) && event.getUser().isVerified()) {
			m = m.replaceAll("^\\S+:", "");
			List<String> content = StringContent.parse(m);
			markov.consume(content, maxlen);
			markov.flush();
		}
	}
}
