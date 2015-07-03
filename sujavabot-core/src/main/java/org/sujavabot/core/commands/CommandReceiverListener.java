package org.sujavabot.core.commands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class CommandReceiverListener extends ListenerAdapter<PircBotX>
implements HelperConvertable<CommandReceiverListener> {
	protected String prefix;
	
	public CommandReceiverListener(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		String m = event.getMessage();
		if(!m.startsWith(prefix))
			return;
		m = m.substring(prefix.length());
		((SujavaBot) event.getBot()).getCommands().perform(event, m);
	}
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
		String m = event.getMessage();
		if(!m.startsWith(prefix))
			return;
		m = m.substring(prefix.length());
		((SujavaBot) event.getBot()).getCommands().perform(event, m);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void configure(MarshalHelper helper, CommandReceiverListener defaults) {
		helper.field("prefix", String.class, () -> getPrefix());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("prefix", String.class, s -> setPrefix(s));
	}
}
