package org.sujavabot.core.commands;

import java.io.File;
import java.util.List;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;

public class ConfigCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() <= 1)
			return "config <command>: save";
		boolean help = "help".equals(args.get(0));
		if("save".equals(args.get(1))) {
			if(help || args.size() < 2 || args.size() > 3)
				return "config save [<file>]: save the configuration file";
			File configFile = null;
			if(args.size() >= 3)
				configFile = new File(args.get(2));
			configFile = bot.saveConfiguration(configFile);
			return "configuration saved to " + configFile;
		}
		return "config <command>: save";
	}

}
