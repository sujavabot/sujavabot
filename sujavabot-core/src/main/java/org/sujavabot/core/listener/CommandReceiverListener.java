package org.sujavabot.core.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class CommandReceiverListener extends ListenerAdapter<PircBotX>
implements HelperConvertable<CommandReceiverListener> {
	private static final Executor exec = Executors.newCachedThreadPool();
	
	private static final Map<AuthorizedUser, AtomicInteger> runningCount = new ConcurrentHashMap<>();
	
	public static void run(SujavaBot bot, Runnable task) {
		run(bot, Authorization.getAuthorization(), task);
	}
	
	public static void run(SujavaBot bot, Authorization auth, Runnable task) {
		AuthorizedUser user = auth.getUser();
		exec.execute(() -> {
			try {
				synchronized(runningCount) {
					if(!runningCount.containsKey(user))
						runningCount.put(user, new AtomicInteger(1));
					else
						runningCount.get(user).incrementAndGet();
				}
				auth.run(task);
			} finally {
				synchronized(runningCount) {
					if(runningCount.get(user).decrementAndGet() == 0) {
						runningCount.remove(user);
						if(user.checkEmptyEphemeral())
							bot.removeAuthorizedUser(user);
					}
				}
			}
		});
	}
	
	protected String prefix;
	
	public CommandReceiverListener(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		if(!event.getMessage().startsWith(prefix))
			return;
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser(), true);
		List<AuthorizedGroup> groups = user.getAllGroups();
		List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();
		Authorization.run(bot, user, groups, ownedGroups, () -> {
			run(bot, () -> {
				String m = event.getMessage();
				m = m.substring(prefix.length());
				try {
					bot.getCommands().perform(event, m);
				} catch(Exception e) {
					bot.getCommands().perform(event, "_exception " + Throwables.message(e));
				}
			});
		});
	}
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
		if(!event.getMessage().startsWith(prefix))
			return;
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser(), true);
		List<AuthorizedGroup> groups = user.getAllGroups();
		List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();
		Authorization.run(bot, user, groups, ownedGroups, () -> {
			Authorization auth = Authorization.getAuthorization();
			exec.execute(() -> {
				auth.run(() -> {
					String m = event.getMessage();
					m = m.substring(prefix.length());
					bot.getCommands().perform(event, m);
				});
			});
		});
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void configure(MarshalHelper helper, CommandReceiverListener defaults) {
		helper.field("prefix", String.class, () -> getPrefix());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("prefix", String.class, s -> setPrefix(s));
	}
}
