package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.Authorization.LimitedCallable;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class SudoCommand extends AbstractReportingCommand {

	public SudoCommand() {
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<nick> <command>: run a command as another nick");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args);
		String name = args.get(1);
		String command = args.get(2);
		AuthorizedUser a = bot.getAuthorizedUserByNick(name, true);
		if(a == null)
			return "invalid user";
		LimitedCallable<String, RuntimeException> task = new LimitedCallable<String, RuntimeException>() {
			@Override
			public String call() throws RuntimeException {
				return bot.getCommands().invoke(cause, command);
			}
		}; 
		return Authorization.limitedCall(a, task);
	}
}
