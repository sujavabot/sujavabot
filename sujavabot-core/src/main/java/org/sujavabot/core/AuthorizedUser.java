package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.sujavabot.core.commands.UserCommandHandler;

public class AuthorizedUser {
	protected String name;
	protected Pattern nick;
	protected UserCommandHandler commands;
	protected List<AuthorizedGroup> groups;
	protected List<AuthorizedGroup> ownedGroups;
	
	public AuthorizedUser() {
		commands = new UserCommandHandler(this);
		groups = new ArrayList<>();
		ownedGroups = new ArrayList<>();
	}
	
	public AuthorizedUser(String name) {
		this();
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public Pattern getNick() {
		return nick;
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
	public void setNick(Pattern nick) {
		this.nick = nick;
	}
	public void setCommands(UserCommandHandler commands) {
		this.commands = commands;
	}

	public void setGroups(List<AuthorizedGroup> groups) {
		this.groups = groups;
	}

	public Map<String, Command> getAllCommands() {
		Map<String, Command> all = new TreeMap<>();
		all.putAll(getCommands().getTransientCommands());
		all.putAll(getCommands().getCommands());
		for(AuthorizedGroup group : getGroups()) {
			for(Entry<String, Command> e : group.getAllCommands().entrySet())
				if(!all.containsKey(e.getKey()))
					all.put(e.getKey(), e.getValue());
		}
		return all;
	}

	public List<AuthorizedGroup> getOwnedGroups() {
		return ownedGroups;
	}

	public void setOwnedGroups(List<AuthorizedGroup> ownedGroups) {
		this.ownedGroups = ownedGroups;
	}
	
	public boolean isOwnerOf(AuthorizedGroup group) {
		if(ownedGroups.contains(group))
			return true;
		for(AuthorizedGroup g : ownedGroups) {
			if(group.contains(g))
				return true;
		}
		return false;
	}
}
