package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;
import org.sujavabot.core.CommandComponent;

public interface CommandHandler {

	public void addCommand(String name, Command command, boolean isTransient);
	
	Command get(Event<?> cause, String name);

	public CommandComponent.Expression parse(String unparsed);

	public String invoke(Event<?> cause, String unparsed);
	public String invoke(Event<?> cause, CommandComponent.Expression parsed);
	void perform(Event<?> cause, String unparsed);
	void perform(Event<?> cause, CommandComponent.Expression parsed);

}