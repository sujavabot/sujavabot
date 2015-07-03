package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;

import org.sujavabot.core.commands.DefaultCommandHandler;
import org.sujavabot.core.commands.GroupCommandHandler;

public class AuthorizedGroup {
	protected String name;
	protected GroupCommandHandler commands;
	protected List<AuthorizedGroup> subgroups;
	
	public AuthorizedGroup(String name) {
		this.name = name;
		commands = new GroupCommandHandler(this);
		subgroups = new ArrayList<>();
	}
	
	public String getName() {
		return name;
	}
	public GroupCommandHandler getCommands() {
		return commands;
	}
	public List<AuthorizedGroup> getSubgroups() {
		return subgroups;
	}
}
