package org.sujavabot.plugin.markov;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

public class IdentificationListener extends ListenerAdapter<PircBotX> {

	protected Identification ident;
	protected Set<String> channels;
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(!channels.contains(event.getChannel().getName()))
			return;
		String m = event.getMessage();

		m = m.replaceAll("^(\\S+:\\s*|<\\S+>\\s*)*", "");
		List<String> content = StringContent.parse(m);
		Iterator<String> ci = content.iterator();
		while(ci.hasNext()) {
			if(StringContent.LINK.matcher(ci.next()).matches())
				ci.remove();
		}
		if(content.size() > 0) {
			ident.consume(System.currentTimeMillis(), event.getUser().getNick(), content);
		}
	}

	public Identification getIdent() {
		return ident;
	}

	public void setIdent(Identification ident) {
		this.ident = ident;
	}

	public Set<String> getChannels() {
		return channels;
	}

	public void setChannels(Set<String> channels) {
		this.channels = channels;
	}
}
