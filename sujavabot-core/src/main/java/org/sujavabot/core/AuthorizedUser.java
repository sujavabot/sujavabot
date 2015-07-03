package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;

import org.sujavabot.core.commands.DefaultCommandHandler;
import org.sujavabot.core.commands.UserCommandHandler;

public class AuthorizedUser {
	protected String name;
	protected UserCommandHandler commands;
	protected List<AuthorizedGroup> groups;
	
	public AuthorizedUser() {
		commands = new UserCommandHandler(this);
		groups = new ArrayList<>();
	}
	
	public AuthorizedUser(String name) {
		this();
		this.name = name;
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
	
	public boolean isMemberOf(AuthorizedGroup group) {
		if(groups.contains(group))
			return true;
		for(AuthorizedGroup g : groups) {
			if(g.contains(group))
				return true;
		}
		return false;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCommands(UserCommandHandler commands) {
		this.commands = commands;
	}

	public void setGroups(List<AuthorizedGroup> groups) {
		this.groups = groups;
	}
}
