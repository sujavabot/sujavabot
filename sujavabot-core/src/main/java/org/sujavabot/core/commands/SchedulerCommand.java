package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Scheduler;
import org.sujavabot.core.Scheduler.ScheduledCommand;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Events;

public class SchedulerCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		String target;
		if(Events.getChannel(cause) != null)
			target = Events.getChannel(cause).getName();
		else
			target = Events.getUser(cause).getNick();
		if("list".equals(args.get(1))) {
			if(args.size() != 2)
				return invokeHelp(bot, cause, args, "list");
			for(ScheduledCommand sc : Scheduler.get().getCommands()) {
				if(!target.equals(sc.target))
					continue;
				report(bot, cause, sc.name + " " + sc.delay + ":" + sc.alias);
			}
			return null;
		} else if("add".equals(args.get(1))) {
			if(args.size() != 5)
				return invokeHelp(bot, cause, args, "add");
			String name = args.get(2);
			long delay = Long.parseLong(args.get(3));
			String alias = args.get(4);
			boolean ok = Scheduler.get().add(bot, target, name, alias, delay);
			return (ok ? "OK" : "name already exists");
		} else if("remove".equals(args.get(1))) {
			if(args.size() != 3)
				return invokeHelp(bot, cause, args, "remove");
			String name = args.get(2);
			boolean ok = Scheduler.get().remove(name);
			return (ok ? "OK" : "no such name");
		} else
			return invokeHelp(bot, cause, args);
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("scheduler control", 
				"list", "lists the currently scheduled commands",
				"add", "<name> <delay> <command>: schedules a command to be run as the invoking user",
				"remove", "<name>: unschedules a command");
	}

}
