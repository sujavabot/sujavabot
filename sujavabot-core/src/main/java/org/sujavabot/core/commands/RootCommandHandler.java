package org.sujavabot.core.commands;

import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.Command;

public class RootCommandHandler extends GroupCommandHandler {

	protected Command unrecognized = new UnrecognizedCommand();
	
	public RootCommandHandler(AuthorizedGroup root) {
		super(root);
		commands.put("_unrecognized", new UnrecognizedCommand());
		commands.put("_parse-error", new ParseErrorCommand());
	}

}
