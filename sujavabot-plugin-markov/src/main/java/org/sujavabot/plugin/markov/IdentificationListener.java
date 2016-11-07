package org.sujavabot.plugin.markov;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

public class IdentificationListener extends ListenerAdapter<PircBotX> {

	protected Identification ident;
	protected Set<String> channels;
	protected Set<Pattern> ignore;
	
	@Override
	public void onAction(ActionEvent<PircBotX> event) throws Exception {
		if(channels != null && !channels.contains(event.getChannel().getName()))
			return;
		String m = event.getMessage();
		m = event.getUser().getNick() + " " + m;
		
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
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(channels != null && !channels.contains(event.getChannel().getName()))
			return;
		String m = event.getMessage();
		if(ignore != null) {
			for(Pattern p : ignore) {
				if(p.matcher(m).matches())
					return;
			}
		}

		m = m.replaceAll("^(\\S+:\\s*|<\\S+>\\s*)*", "").toUpperCase();
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

	public Set<Pattern> getIgnore() {
		return ignore;
	}

	public void setIgnore(Set<Pattern> ignore) {
		this.ignore = ignore;
	}
}
