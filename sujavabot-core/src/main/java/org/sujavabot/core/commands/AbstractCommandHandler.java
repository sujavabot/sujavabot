package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Command;
import org.sujavabot.core.CommandLexer;
import org.sujavabot.core.CommandParser;
import org.sujavabot.core.SujavaBot;

public abstract class AbstractCommandHandler implements CommandHandler {
	protected static User getUser(Event<?> event) {
		try {
			return (User) event.getClass().getMethod("getUser").invoke(event);
		} catch(Exception e) {
			return null;
		}
	}
	
	protected static Channel getChannel(Event<?> event) {
		try {
			return (Channel) event.getClass().getMethod("getChannel").invoke(event);
		} catch(Exception e) {
			return null;
		}
	}
	
	protected SujavaBot bot;
	protected Map<String, Command> commands = new TreeMap<>();
	
	protected transient Map<String, Command> transientCommands = new TreeMap<>();
	
	public AbstractCommandHandler(SujavaBot bot) {
		this.bot = bot;
	}
	
	public Map<String, Command> getCommands() {
		return commands;
	}
	
	public Map<String, Command> getTransientCommands() {
		return transientCommands;
	}
	
	public abstract Command getDefaultCommand(Event<?> cause, String name);
	
	@Override
	public void addCommand(String name, Command command, boolean isTransient) {
		(isTransient ? commands : transientCommands).remove(name);
		(isTransient ? transientCommands : commands).put(name, command);
	}
	
	@Override
	public Command get(Event<?> cause, String name) {
		Command c = commands.get(name);
		if(c == null)
			c = transientCommands.get(name);
		if(c == null)
			c = getDefaultCommand(cause, name);
		return c;
	}
	
	public Object[] parse(String unparsed) {
		CommandParser parser = new CommandParser(new CommonTokenStream(new CommandLexer(new ANTLRInputStream(unparsed))));
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			return parser.command().c;
		} catch(RuntimeException re) {
			return new Object[] {"_parse-error", unparsed};
		}
	}
	
	@Override
	public void perform(Event<?> cause, String unparsed) {
		perform(cause, parse(unparsed));
	}
	
	@Override
	public String invoke(Event<?> cause, String unparsed) {
		return invoke(cause, parse(unparsed));
	}
	
	public void perform(Event<?> cause, Object[] cmd) {
		List<String> args = new ArrayList<>();
		for(int i = 0; i < cmd.length; i++) {
			String arg;
			if(cmd[i] instanceof Object[])
				arg = invoke(cause, (Object[]) cmd[i]);
			else
				arg = (String) cmd[i];
			if(arg != null)
				args.add(arg);
		}
		if(args.size() > 0) {
			Command reporter = get(cause, args.get(0));
			String result = reporter.invoke(bot, cause, args);
			if(result != null)
				reporter.report(bot, cause, result);
		}
	}
	
	public String invoke(Event<?> cause, Object[] cmd) {
		List<String> args = new ArrayList<>();
		for(int i = 0; i < cmd.length; i++) {
			String arg;
			if(cmd[i] instanceof Object[])
				arg = invoke(cause, (Object[]) cmd[i]);
			else
				arg = (String) cmd[i];
			if(arg != null)
				args.add(arg);
		}
		if(args.size() == 0)
			return null;
		return get(cause, args.get(0)).invoke(bot, cause, args);
	}
}
