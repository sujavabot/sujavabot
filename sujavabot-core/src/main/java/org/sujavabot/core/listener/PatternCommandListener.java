package org.sujavabot.core.listener;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class PatternCommandListener extends ListenerAdapter<PircBotX> implements HelperConvertable<PatternCommandListener> {
	protected static final Pattern ESCAPE = Pattern.compile("\\\\\\.");
	protected static final Pattern GROUP = Pattern.compile("\\$(\\d+|nick|user)");
	
	protected Pattern user = Pattern.compile(".*");
	protected Pattern nick = Pattern.compile(".*");
	protected Pattern pattern;
	protected String command;
	
	@Override
	public void onMessage(MessageEvent<PircBotX> event) throws Exception {
		SujavaBot bot = (SujavaBot) event.getBot();
		AuthorizedUser user = bot.getAuthorizedUser(event.getUser(), false);
		Matcher mm = pattern.matcher(event.getMessage());
		while(mm.find()) {
			
			if(this.nick != null && !this.nick.matcher(event.getUser().getNick()).matches())
				return;
			
			if(this.user != null && !this.user.matcher(user.getName()).matches())
				return;
			
			StringBuilder sb = new StringBuilder();
			Matcher em = ESCAPE.matcher(command);
			int end = 0;
			while(em.find()) {
				String noescapes = command.substring(end, em.start());
				Matcher gm = GROUP.matcher(noescapes);
				int end2 = 0;
				while(gm.find()) {
					sb.append(noescapes.substring(end2, gm.start()));
					int gn = Integer.parseInt(gm.group().substring(1));
					sb.append(mm.group(gn));
					end2 = gm.end();
				}
				sb.append(noescapes.substring(end2));
				sb.append(em.group().substring(1));
				end = em.end();
			}
			String noescapes = command.substring(end);
			Matcher gm = GROUP.matcher(noescapes);
			int end2 = 0;
			while(gm.find()) {
				sb.append(noescapes.substring(end2, gm.start()));
				if("$nick".equals(gm.group()))
					sb.append(event.getUser().getNick());
				else if("$user".equals(gm.group()))
					sb.append(user.getName());
				else {
					int gn = Integer.parseInt(gm.group().substring(1));
					sb.append(mm.group(gn));
				}
				end2 = gm.end();
			}
			sb.append(noescapes.substring(end2));
			
			List<AuthorizedGroup> groups = user.getAllGroups();
			List<AuthorizedGroup> ownedGroups = user.getOwnedGroups();
			Authorization.run(bot, user, groups, ownedGroups, () -> {
				CommandReceiverListener.run(bot, () -> {
					bot.getCommands().perform(event, sb.toString());
				});
			});
		}
	}

	@Override
	public void configure(MarshalHelper helper, PatternCommandListener defaults) {
		helper.field("pattern", String.class, () -> pattern.pattern());
		helper.field("user", String.class, () -> user.pattern());
		helper.field("nick", String.class, () -> nick.pattern());
		helper.field("command", String.class, () -> command);
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("pattern", String.class, (s) -> pattern = Pattern.compile(s));
		helper.field("user", String.class, (s) -> user = Pattern.compile(s));
		helper.field("nick", String.class, (s) -> nick = Pattern.compile(s));
		helper.field("command", String.class, (s) -> command = s);
	}
	
}
