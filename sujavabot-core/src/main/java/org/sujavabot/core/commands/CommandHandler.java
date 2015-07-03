package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;

public interface CommandHandler {

	Command get(Event<?> cause, String name);

	public Object[] parse(String unparsed);

	public String invoke(Event<?> cause, String unparsed);
	void perform(Event<?> cause, String unparsed);

}