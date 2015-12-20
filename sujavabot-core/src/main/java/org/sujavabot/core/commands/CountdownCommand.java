package org.sujavabot.core.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.SchedulerPool;

public class CountdownCommand extends AbstractReportingCommand {
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<from> [<finished_command> [<counting_command>]]: start a countdown");
	}
	
	private transient Map<AuthorizedUser, Future<?>> countdowns = new HashMap<>();

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 2 || args.size() > 4)
			return invokeHelp(bot, cause, args);
		int from;
		try {
			from = Integer.parseInt(args.get(1));
			if(from <= 0 || from > 10)
				return "invalid from";
		} catch(NumberFormatException e) {
			return "invalid from";
		}
		String finished = (args.size() >= 3 ? args.get(2) : "echo GO");
		String counting = (args.size() >= 4 ? args.get(3) : "echo %1");
		
		Authorization auth = Authorization.getAuthorization();
		
		Runnable countingTask = new Runnable() {
			private int rc = from;
			@Override
			public void run() {
				if(rc == 0) {
					String processed = AliasCommand.applyAlias(bot, cause, Arrays.asList(args.get(0)), finished);
					bot.getCommands().perform(cause, processed);
					Future<?> f = countdowns.get(auth.getUser());
					if(f != null)
						f.cancel(true);
				} else {
					String processed = AliasCommand.applyAlias(bot, cause, Arrays.asList(args.get(0), "" + rc), counting);
					bot.getCommands().perform(cause, processed);
					rc--;
				}
			}
		};

		if(countdowns == null)
			countdowns = new HashMap<>();
		
		Future<?> f = countdowns.get(auth.getUser());
		if(f != null)
			f.cancel(true);
		countdowns.put(auth.getUser(), SchedulerPool.get().scheduleAtFixedRate(() -> auth.run(countingTask), 1, 1, TimeUnit.SECONDS));
		
		return null;
	}


}
