package org.sujavabot.core.commands;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;

public interface CommandHandler {

	Command get(Event<?> cause, String name);

	void perform(Event<?> cause, String unparsed);

}