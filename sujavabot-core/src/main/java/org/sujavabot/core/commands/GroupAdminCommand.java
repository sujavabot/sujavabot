package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

import com.google.common.collect.Lists;

public class GroupAdminCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("authorization group control", 
				"list", "list groups", 
				"info", "<group> [<field>]: show group info", 
				"add", "<group>: create a group", 
				"remove", "<group>: delete a group", 
				"add_user", "<group> <users...>: add a users to a group", 
				"remove_user", "<group> <users...>: remove a users from a group",
				"add_parent", "<child_group> <parent_group>: add a parent to a group", 
				"remove_parent", "<child_group> <parent_group>: remove a parent from a group", 
				"add_alias", "<group> <name> <command>: add a command alias to a group", 
				"remove_alias", "<group> <name>: remove a command alias from a group", 
				"show_alias", "<group> <name>: show a command alias from a group",
				"set_property", "<key> <value>: set a property on a group",
				"unset_property", "<key>: unset a property from a group"
				);

	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() <= 1)
			return invokeHelp(bot, cause, args);
		if ("list".equals(args.get(1)))
			return _list(bot, cause, args);
		else if ("info".equals(args.get(1)))
			return _info(bot, cause, args);
		else if ("add".equals(args.get(1)))
			return _add(bot, cause, args);
		else if ("remove".equals(args.get(1)))
			return _remove(bot, cause, args);
		else if ("add_user".equals(args.get(1)))
			return _add_user(bot, cause, args);
		else if ("remove_user".equals(args.get(1)))
			return _remove_user(bot, cause, args);
		else if ("add_parent".equals(args.get(1)))
			return _add_parent(bot, cause, args);
		else if ("remove_parent".equals(args.get(1)))
			return _remove_parent(bot, cause, args);
		else if ("add_alias".equals(args.get(1)))
			return _add_alias(bot, cause, args);
		else if ("remove_alias".equals(args.get(1)))
			return _remove_alias(bot, cause, args);
		else if ("show_alias".equals(args.get(1)))
			return _show_alias(bot, cause, args);
		else if("set_property".equals(args.get(1)))
			return _set_property(bot, cause, args);
		else if("unset_property".equals(args.get(1)))
			return _unset_property(bot, cause, args);
		else
			return invokeHelp(bot, cause, args);
	}

	protected String _list(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 2)
			return invokeHelp(bot, cause, args, "list");
		List<String> names = Lists.transform(new ArrayList<>(bot.getRawAuthorizedGroups().values()), (ag) -> ag.getName());
		names = new ArrayList<>(names);
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return StringUtils.join(names, ", ");
	}

	protected String _info(SujavaBot bot, Event<?> cause, List<String> args) {
		if ((args.size() != 3 && args.size() != 4))
			return invokeHelp(bot, cause, args, "info");
		String name = args.get(2);
		AuthorizedGroup group = bot.getAuthorizedGroupByName(name);
		if (group == null)
			return "group " + name + " does not exist";
		List<String> commands = new ArrayList<>();
		List<String> parents = new ArrayList<>();
		List<String> allCommands = new ArrayList<>();

		for (Entry<String, Command> e : group.getCommands().getCommands().entrySet()) {
			if (!(e.getValue() instanceof HiddenCommand))
				commands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
		}
		for (AuthorizedGroup parent : group.getParents())
			parents.add(parent.getName());
		for (Entry<String, Command> e : group.getAllCommands().entrySet()) {
			if (!(e.getValue() instanceof HiddenCommand))
				allCommands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
		}
		
		Collections.sort(commands, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(allCommands, String.CASE_INSENSITIVE_ORDER);

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", name);
		m.put("commands", commands);
		m.put("parents", parents);
		m.put("all-commands", allCommands);
		
		Map<String, String> p = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, String> ap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		p.putAll(group.getProperties());
		ap.putAll(group.getAllProperties());
		
		m.put("properties", p);
		m.put("all-properties", ap);

		String info = m.toString();

		if (args.size() == 4) {
			Object o = m.get(args.get(3));
			if (o instanceof List<?>)
				info = StringUtils.join((List<?>) o, ", ");
			else
				info = String.valueOf(o);
		}

		return info;
	}
	
	protected AuthorizedGroup getOrCreateGroup(SujavaBot bot, Event<?> cause, String groupName) {
		AuthorizedGroup group = bot.getAuthorizedGroupByName(groupName);
		if(group != null)
			return group;
		if(!AuthorizedGroup.CREATABLE_NAME.matcher(groupName).matches())
			return null;
		boolean permitCreate = false;
		AuthorizedGroup rootGroup = bot.getRootGroup();
		if(Authorization.isCurrentOwner(rootGroup) || Authorization.isCurrentUserOwned(groupName))
			permitCreate = true;
		if(!permitCreate)
			return null;
		group = new AuthorizedGroup(groupName);
		if(rootGroup != null)
			group.getParents().add(rootGroup);
		Authorization.getCurrentUser().getOwnedGroups().add(group);
		bot.addAuthorizedGroup(group);
		return group;
	}

	protected String _add(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 3)
			return invokeHelp(bot, cause, args, "add");
		String name = args.get(2);
		boolean userOwned = Authorization.getAuthorization().isUserOwned(name);
		if (bot.getAuthorizedGroupByName(name) != null)
			return "group " + name + " already exists";
		if(!AuthorizedGroup.CREATABLE_NAME.matcher(name).matches())
			return "invalid group name";
		AuthorizedGroup group = new AuthorizedGroup(name);
		AuthorizedGroup root = bot.getRootGroup();
		if (root != null) {
			if (!Authorization.isCurrentRootOwner() && !userOwned)
				return "permission denied";
			group.getParents().add(root);
		}
		if(Authorization.getCurrentUser() != null)
			Authorization.getCurrentUser().getOwnedGroups().add(group);
		bot.addAuthorizedGroup(group);
		bot.saveConfiguration();
		return "group created";
	}

	protected String _remove(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 3)
			return invokeHelp(bot, cause, args, "remove");
		String name = args.get(2);
		if ("@root".equals(name))
			return "cannot delete root group";
		AuthorizedGroup group = bot.getAuthorizedGroupByName(name);
		if (group == null)
			return "group " + name + " does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		for(AuthorizedUser u : bot.getAuthorizedUsersByGroup(group)) {
			u.getGroups().remove(group);
			u.getOwnedGroups().remove(group);
		}
		bot.removeAuthorizedGroup(group);
		bot.saveConfiguration();
		return "group deleted";
	}

	protected String _add_user(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() < 4)
			return invokeHelp(bot, cause, args, "add_user");
		AuthorizedGroup group = getOrCreateGroup(bot, cause, args.get(2));
		if (group == null)
			return "group does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		List<String> added = new ArrayList<>();
		List<String> notAdded = new ArrayList<>();
		for(int i = 3; i < args.size(); i++) {
			String uname = args.get(i);
			AuthorizedUser user = bot.getAuthorizedUserByName(uname);
			if(user == null || user.getGroups().contains(group)) {
				notAdded.add(uname);
				continue;
			}
			user.getGroups().add(group);
			added.add(uname);
		}
		bot.saveConfiguration();
		if(added.size() > 0) {
			if(notAdded.size() > 0)
				return "Added " + added + ", did not add " + notAdded;
			else
				return "Added " + added;
		} else {
			if(notAdded.size() > 0)
				return "Did not add " + notAdded;
			else
				return "Group unchanged";
		}
	}

	protected String _remove_user(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 4)
			return invokeHelp(bot, cause, args, "remove_user");
		AuthorizedGroup group = bot.getAuthorizedGroupByName(args.get(2));
		if (group == null)
			return "group does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		List<String> removed = new ArrayList<>();
		List<String> notRemoved = new ArrayList<>();
		for(int i = 3; i < args.size(); i++) {
			String uname = args.get(i);
			AuthorizedUser user = bot.getAuthorizedUserByName(uname);
			if(user == null || !user.getGroups().contains(group)) {
				notRemoved.add(uname);
				continue;
			}
			user.getGroups().remove(group);
			removed.add(uname);
		}
		bot.saveConfiguration();
		if(removed.size() > 0) {
			if(notRemoved.size() > 0)
				return "Removed " + removed + ", did not remove " + notRemoved;
			else
				return "Removed " + removed;
		} else {
			if(notRemoved.size() > 0)
				return "Did not remove " + notRemoved;
			else
				return "Group unchanged";
		}
	}

	protected String _add_parent(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 4)
			return invokeHelp(bot, cause, args, "add_parent");
		AuthorizedGroup child = getOrCreateGroup(bot, cause, args.get(2));
		if (child == null)
			return "child does not exist";
		if (!Authorization.isCurrentOwner(child))
			return "permission denied";
		AuthorizedGroup parent = bot.getAuthorizedGroupByName(args.get(3));
		if (parent == null)
			return "parent does not exist";
		if (child.getParents().contains(parent))
			return "group " + parent.getName() + " is already a parent of group " + child.getName();
		child.getParents().add(parent);
		bot.saveConfiguration();
		return "parent added";
	}

	protected String _remove_parent(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 4)
			return invokeHelp(bot, cause, args, "remove_parent");
		AuthorizedGroup child = bot.getAuthorizedGroupByName(args.get(2));
		if (child == null)
			return "child does not exist";
		if (!Authorization.isCurrentOwner(child))
			return "permission denied";
		AuthorizedGroup parent = bot.getAuthorizedGroupByName(args.get(3));
		if (parent == null)
			return "parent does not exist";
		if (!child.getParents().contains(parent))
			return "group " + parent.getName() + " is not a parent of group " + child.getName();
		child.getParents().remove(parent);
		bot.saveConfiguration();
		return "parent removed";
	}

	protected String _add_alias(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 5)
			return invokeHelp(bot, cause, args, "add_alias");
		AuthorizedGroup group = getOrCreateGroup(bot, cause, args.get(2));
		if (group == null)
			return "group does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		if (group.getCommands().getCommands().get(args.get(3)) != null)
			return "named command already exists";
		group.getCommands().getCommands().put(args.get(3), new AliasCommand(args.get(4)));
		bot.saveConfiguration();
		return "alias added";
	}

	protected String _remove_alias(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 4)
			return invokeHelp(bot, cause, args, "remove_alias");
		AuthorizedGroup group = bot.getAuthorizedGroupByName(args.get(2));
		if (group == null)
			return "group does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		Command c = group.getCommands().getCommands().get(args.get(3));
		if (c == null)
			return "named command does not exist";
		if (!(c instanceof AliasCommand))
			return "named command not an alias";
		group.getCommands().getCommands().remove(args.get(3));
		bot.saveConfiguration();
		return "alias removed";
	}

	protected String _show_alias(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 4)
			return invokeHelp(bot, cause, args, "show_alias");
		AuthorizedGroup group = bot.getAuthorizedGroupByName(args.get(2));
		if (group == null)
			return "group does not exist";
		Command c = group.getAllCommands().get(args.get(3));
		if (c == null)
			return "named command does not exist";
		if (!(c instanceof AliasCommand))
			return "named command not an alias";
		return c.toString();
	}

	protected String _set_property(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 5)
			return invokeHelp(bot, cause, args, "set_property");
		AuthorizedGroup group = getOrCreateGroup(bot, cause, args.get(2));
		if (group == null)
			return "group does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		String key = args.get(3);
		String value = args.get(4);
		group.getProperties().put(key, value);
		bot.saveConfiguration();
		return "property set";
	}

	protected String _unset_property(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() != 4)
			return invokeHelp(bot, cause, args, "unset_property");
		AuthorizedGroup group = bot.getAuthorizedGroupByName(args.get(2));
		if (group == null)
			return "group does not exist";
		if (!Authorization.isCurrentOwner(group))
			return "permission denied";
		String key = args.get(3);
		group.getProperties().remove(key);
		bot.saveConfiguration();
		return "property unset";
	}

}
