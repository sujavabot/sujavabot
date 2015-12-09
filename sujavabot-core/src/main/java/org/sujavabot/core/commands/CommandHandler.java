package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;

public interface CommandHandler {

	public void addCommand(String name, Command command, boolean isTransient);
	
	Command get(Event<?> cause, String name);

	public Object[] parse(String unparsed);

	public String invoke(Event<?> cause, String unparsed);
	public String invoke(Event<?> cause, Object[] parsed);
	void perform(Event<?> cause, String unparsed);
	void perform(Event<?> cause, Object[] parsed);

}