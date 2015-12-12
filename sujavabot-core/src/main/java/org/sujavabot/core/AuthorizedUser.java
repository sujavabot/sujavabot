package org.sujavabot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.sujavabot.core.commands.UserCommandHandler;

public class AuthorizedUser {
	protected String name;
	protected Pattern nick;
	protected UserCommandHandler commands;
	protected List<AuthorizedGroup> groups;
	protected List<AuthorizedGroup> ownedGroups;
	protected Map<String, String> properties;
	
	public AuthorizedUser() {
		commands = new UserCommandHandler(this);
		groups = new ArrayList<>();
		ownedGroups = new ArrayList<>();
		properties = new TreeMap<>();
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
	
	public List<AuthorizedGroup> getAllGroups() {
		List<AuthorizedGroup> all = new ArrayList<>();
		for(AuthorizedGroup g : getGroups()) {
			if(!all.contains(g))
				all.add(g);
			for(AuthorizedGroup p : g.getAllParents())
				if(!all.contains(p))
					all.add(p);
		}
		return all;
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
		for(AuthorizedGroup group : getGroups()) {
			for(Entry<String, Command> e : group.getAllCommands().entrySet())
				if(!all.containsKey(e.getKey()))
					all.put(e.getKey(), e.getValue());
		}
		all.putAll(getCommands().getTransientCommands());
		all.putAll(getCommands().getCommands());
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

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	public Map<String, String> getAllProperties() {
		Map<String, String> all = new TreeMap<>();
		for(AuthorizedGroup group : groups) {
			all.putAll(group.getAllProperties());
		}
		all.putAll(getProperties());
		return all;
	}
}
