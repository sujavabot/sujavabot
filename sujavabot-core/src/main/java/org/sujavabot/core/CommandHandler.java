package org.sujavabot.core;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.pircbotx.hooks.Event;

public class CommandHandler {
	protected SujavaBot bot;
	protected Map<String, Command> commands = new TreeMap<>();
	protected Command defaultCommand;
	
	public CommandHandler(SujavaBot bot) {
		this.bot = bot;
	}
	
	public Map<String, Command> getCommands() {
		return commands;
	}
	
	public Command getDefaultCommand() {
		return defaultCommand;
	}
	
	public void setDefaultCommand(Command defaultCommand) {
		this.defaultCommand = defaultCommand;
	}
	
	public Command get(Event<?> cause, String name) {
		return commands.getOrDefault(name, defaultCommand);
	}
	
	public Object[] parse(String unparsed) {
		CommandParser parser = new CommandParser(new CommonTokenStream(new CommandLexer(new ANTLRInputStream(unparsed))));
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			return parser.command().c;
		} catch(RuntimeException re) {
			return new Object[] {"__parse_error", re.toString(), unparsed};
		}
	}
	
	public void perform(Event<?> cause, String unparsed) {
		perform(cause, parse(unparsed));
	}
	
	public void perform(Event<?> cause, Object[] cmd) {
		String[] args = new String[cmd.length];
		for(int i = 0; i < cmd.length; i++) {
			if(cmd[i] instanceof Object[])
				args[i] = invoke(cause, (Object[]) cmd[i]);
			else
				args[i] = (String) cmd[i];
		}
		Command reporter = get(cause, args[0]);
		String result = reporter.invoke(bot, cause, args);
		reporter.report(bot, cause, result);
	}
	
	public String invoke(Event<?> cause, Object[] cmd) {
		String[] args = new String[cmd.length];
		for(int i = 0; i < cmd.length; i++) {
			if(cmd[i] instanceof Object[])
				args[i] = invoke(cause, (Object[]) cmd[i]);
			else
				args[i] = (String) cmd[i];
		}
		return get(cause, args[0]).invoke(bot, cause, args);
	}
}
