package org.sujavabot.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.sujavabot.core.commands.DefaultCommandHandler;
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
	
	protected String name;
	protected GroupCommandHandler commands;
	protected List<AuthorizedGroup> subgroups;
	
	public AuthorizedGroup() {}
	
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
	
	public boolean contains(AuthorizedGroup other) {
		if(subgroups.contains(other))
			return true;
		for(AuthorizedGroup sub : subgroups) {
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

	public void setSubgroups(List<AuthorizedGroup> subgroups) {
		this.subgroups = subgroups;
	}
}
