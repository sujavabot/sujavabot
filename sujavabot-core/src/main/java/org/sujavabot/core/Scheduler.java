package org.sujavabot.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.util.SchedulerPool;
import org.sujavabot.core.xml.HelperConvertable;

public class Scheduler implements HelperConvertable<Scheduler> {
	private static Scheduler instance = new Scheduler();
	public static Scheduler get() {
		return instance;
	}
	
	private Scheduler() {}
	
	public static class ScheduledCommand implements HelperConvertable<ScheduledCommand> {

		public String name;
		public String user;
		public List<String> groups = new ArrayList<>();
		public String alias;
		public long delay;
		public String target;
		
		public SujavaBot bot;
		public ScheduledFuture<?> future;
		
		@Override
		public void configure(MarshalHelper helper, ScheduledCommand defaults) {
			helper.field("name", String.class, () -> name);
			helper.field("user", String.class, () -> user);
			for(String group : groups)
				helper.field("group", String.class, () -> group);
			helper.field("alias", String.class, () -> alias);
			helper.field("delay", Long.class, () -> delay);
			helper.field("target", String.class, () -> target);
			
		}

		@Override
		public void configure(UnmarshalHelper helper) {
			groups = new ArrayList<>();
			helper.field("name", String.class, (s) -> {name = s;});
			helper.field("user", String.class, (s) -> {user = s;});
			helper.field("group", String.class, (s) -> {groups.add(s);});
			helper.field("alias", String.class, (s) -> {alias = s;});
			helper.field("delay", Long.class, (d) -> {delay = d;});
			helper.field("target", String.class, (s) -> {target = s;});
		}

		public void schedule() {
			AuthorizedUser au = bot.getAuthorizedUsers().get(user);
			List<AuthorizedGroup> ags = new ArrayList<>();
			for(String g : groups)
				ags.add(bot.getAuthorizedGroups().get(g));
			Authorization auth = new Authorization(bot, au, ags, au.getOwnedGroups());
			Runnable task = auth.runnable(() -> {
				Event<?> e;
				if(target.startsWith("#"))
					e = new MessageEvent<>(bot, bot.getUserChannelDao().getChannel(target), bot.getUserBot(), "");
				else
					e = new PrivateMessageEvent<>(bot, bot.getUserChannelDao().getUser(target), "");
				bot.getCommands().invoke(e, alias);
			});
			future = SchedulerPool.get(au).scheduleAtFixedRate(task, delay, delay, TimeUnit.MILLISECONDS);
		}

		public void cancel() {
			future.cancel(true);
		}
		
	}

	protected Set<ScheduledCommand> commands = new LinkedHashSet<>();
	
	public Set<ScheduledCommand> getCommands() {
		return commands;
	}
	
	public synchronized boolean add(SujavaBot bot, String target, String name, String alias) {
		for(ScheduledCommand cmd : commands) {
			if(name.equals(cmd.name))
				return false;
		}
		ScheduledCommand cmd = new ScheduledCommand();
		Authorization auth = Authorization.getAuthorization();
		cmd.name = name;
		cmd.user = auth.getUser().getName();
		for(AuthorizedGroup g : auth.getGroups())
			cmd.groups.add(g.getName());
		cmd.alias = alias;
		cmd.target = target;
		cmd.bot = bot;
		cmd.schedule();
		commands.add(cmd);
		return true;
	}
	
	public synchronized boolean remove(String name) {
		boolean changed = false;
		Iterator<ScheduledCommand> sci = commands.iterator();
		while(sci.hasNext()) {
			if(sci.next().name.equals(name)) {
				sci.remove();
				changed = true;
			}
		}
		return changed;
	}
	
	@Override
	public void configure(MarshalHelper helper, Scheduler defaults) {
		for(ScheduledCommand c : commands) {
			helper.field("command", ScheduledCommand.class, () -> c);
		}
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		commands = new LinkedHashSet<>();
		helper.field("command", ScheduledCommand.class, (c) -> commands.add(c));
	}
}