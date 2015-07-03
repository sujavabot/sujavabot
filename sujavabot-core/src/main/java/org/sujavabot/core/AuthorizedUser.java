package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;

import org.sujavabot.core.commands.DefaultCommandHandler;
import org.sujavabot.core.commands.UserCommandHandler;

public class AuthorizedUser {
	protected String name;
	protected UserCommandHandler commands;
	protected List<AuthorizedGroup> groups;
	
	public AuthorizedUser(String name) {
		this.name = name;
		commands = new UserCommandHandler(this);
		groups = new ArrayList<>();
	}
	
	public String getName() {
		return name;
	}
	public UserCommandHandler getCommands() {
		return commands;
	}
	public List<AuthorizedGroup> getGroups() {
		return groups;
	}
}
