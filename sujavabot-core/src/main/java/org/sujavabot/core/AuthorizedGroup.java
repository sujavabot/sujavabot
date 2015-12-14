package org.sujavabot.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.sujavabot.core.commands.GroupCommandHandler;

public class AuthorizedGroup {
	public static final Comparator<AuthorizedGroup> GROUP_ORDER = new Comparator<AuthorizedGroup>() {
		@Override
		public int compare(AuthorizedGroup o1, AuthorizedGroup o2) {
			if(o2.contains(o1))
				return -1;
			if(o1.contains(o2))
				return 1;
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
		}
	};
	
	public static final Pattern USER_OWNED = Pattern.compile(":([^:]+)$");
	
	protected String name;
	protected GroupCommandHandler commands;
	protected List<AuthorizedGroup> parents;
	protected Map<String, String> properties;
	
	public AuthorizedGroup() {
		commands = new GroupCommandHandler(this);
		parents = new ArrayList<>();
		properties = new TreeMap<>();
	}
	
	public AuthorizedGroup(String name) {
		this();
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public GroupCommandHandler getCommands() {
		return commands;
	}
	public List<AuthorizedGroup> getParents() {
		return parents;
	}
	
	public List<AuthorizedGroup> getAllParents() {
		List<AuthorizedGroup> all = new ArrayList<>();
		for(AuthorizedGroup p : getParents()) {
			if(!all.contains(p))
				all.add(p);
			for(AuthorizedGroup pp : p.getAllParents())
				if(!all.contains(pp))
					all.add(pp);
		}
		return all;
	}
	
	public boolean contains(AuthorizedGroup other) {
		if(parents.contains(other))
			return true;
		for(AuthorizedGroup sub : parents) {
			if(sub.contains(other))
				return true;
		}
		return false;
	}
	
	public boolean contains(AuthorizedUser user) {
		return user.isMemberOf(this);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCommands(GroupCommandHandler commands) {
		this.commands = commands;
	}

	public void setParents(List<AuthorizedGroup> subgroups) {
		this.parents = subgroups;
	}
	
	public Map<String, Command> getAllCommands() {
		Map<String, Command> all = new TreeMap<>();
		for(AuthorizedGroup parent : getParents()) {
			for(Entry<String, Command> e : parent.getAllCommands().entrySet())
				if(!all.containsKey(e.getKey()))
					all.put(e.getKey(), e.getValue());
		}
		all.putAll(getCommands().getTransientCommands());
		all.putAll(getCommands().getCommands());
		return all;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	public Map<String, String> getAllProperties() {
		Map<String, String> all = new TreeMap<>();
		for(AuthorizedGroup parent : getParents()) {
			all.putAll(parent.getAllProperties());
		}
		all.putAll(getProperties());
		return all;
	}
}
