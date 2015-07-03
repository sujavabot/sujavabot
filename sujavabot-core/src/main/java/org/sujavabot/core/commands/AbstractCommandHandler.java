package org.sujavabot.core.commands;

import java.util.Arrays;
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
	
	public AbstractCommandHandler(SujavaBot bot) {
		this.bot = bot;
	}
	
	public Map<String, Command> getCommands() {
		return commands;
	}
	
	public abstract Command getDefaultCommand(Event<?> cause, String name);
	
	/* (non-Javadoc)
	 * @see org.sujavabot.core.commands.CommandHandler#get(org.pircbotx.hooks.Event, java.lang.String)
	 */
	@Override
	public Command get(Event<?> cause, String name) {
		Command c = commands.get(name);
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
		String[] args = new String[cmd.length];
		for(int i = 0; i < cmd.length; i++) {
			if(cmd[i] instanceof Object[])
				args[i] = invoke(cause, (Object[]) cmd[i]);
			else
				args[i] = (String) cmd[i];
		}
		Command reporter = get(cause, args[0]);
		String result = reporter.invoke(bot, cause, Arrays.asList(args));
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
		return get(cause, args[0]).invoke(bot, cause, Arrays.asList(args));
	}
}
