package org.sujavabot.core.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.Authorization.LimitedCallable;
import org.sujavabot.core.util.Events;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class SudoCommand extends AbstractReportingCommand implements HelperConvertable<SudoCommand> {

	private String name;
	private String command;
	
	public SudoCommand() {
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("run a pre-defined command as a different pre-defined user");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		AuthorizedUser a = bot.getAuthorizedUsers().get(name);
		LimitedCallable<String, RuntimeException> task = () -> bot.getCommands().invoke(cause, command);
		return Authorization.limitedCall(a, task);
	}

	@Override
	public void configure(MarshalHelper helper, SudoCommand defaults) {
		helper.field("name", String.class, () -> name);
		helper.field("command", String.class, () -> command);
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("name", String.class, (s) -> name = s);
		helper.field("command", String.class, (c) -> command = c);
	}

}
