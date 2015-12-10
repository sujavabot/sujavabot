package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class GroupAdminCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("authorization group control", 
				"list", "list groups",
				"info", "<group> [<field>]: show group info",
				"create", "<group>: create a group",
				"delete", "<group>: delete a group",
				"set_name", "<old_group> <new_group>: change a group name",
				"add_user", "<group> <user>: add a user to a group",
				"remove_user", "<group> <user>: remove a user from a group",
				"add_parent", "<child_group> <parent_group>: add a parent to a group",
				"remove_parent", "<child_group> <parent_group>: remove a parent from a group",
				"add_alias", "<group> <name> <command>: add a command alias to a group",
				"remove_alias", "<group> <name>: remove a command alias from a group",
				"show_alias", "<group> <name>: show a command alias from a group"
				);
				
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() <= 1) {
			return "group <command>: list, info, create, delete, set_name, add_user, remove_user, add_parent, remove_parent, add_alias, remove_alias, show_alias";
		}
		boolean help = "help".equals(args.get(0));
		AuthorizedUser caller = bot.getAuthorizedUser(getUser(cause));
		if("list".equals(args.get(1))) {
			if(help || args.size() != 2)
				return "group list: list the group names";
			StringBuilder sb = new StringBuilder();
			for(AuthorizedGroup group : bot.getAuthorizedGroups().values()) {
				if(sb.length() > 0)
					sb.append(" ");
				sb.append(group.getName());
			}
			return sb.toString();
		}
		if("info".equals(args.get(1))) {
			if(help || (args.size() != 3 && args.size() != 4))
				return "group info <name>: show group info";
			String name = args.get(2);
			AuthorizedGroup group = bot.getAuthorizedGroups().get(name);
			if(group == null)
				return "group " + name + " does not exist";
			List<String> commands = new ArrayList<>();
			List<String> parents = new ArrayList<>();
			List<String> allCommands = new ArrayList<>();
			
			for(Entry<String, Command> e : group.getCommands().getCommands().entrySet()) {
				if(!(e.getValue() instanceof HiddenCommand))
					commands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
			}
			for(AuthorizedGroup parent : group.getParents())
				parents.add(parent.getName());
			for(Entry<String, Command> e : group.getAllCommands().entrySet()) {
				if(!(e.getValue() instanceof HiddenCommand))
					allCommands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
			}
			
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("name", name);
			m.put("commands", commands);
			m.put("parents", parents);
			m.put("all-commands", allCommands);
			
			String info = m.toString();
			
			if(args.size() == 4) {
				Object o = m.get(args.get(3));
				if(o instanceof List<?>)
					info = StringUtils.join((List<?>) o, ", ");
				else
					info = String.valueOf(o);
			}
			
			return info;
		}
		if("create".equals(args.get(1))) {
			if(help || args.size() != 3)
				return "group create <name>: create a group";
			String name = args.get(2);
			if(bot.getAuthorizedGroups().get(name) != null)
				return "group " + name + " already exists";
			AuthorizedGroup group = new AuthorizedGroup(name);
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(root != null) {
				if(caller == null || !caller.isOwnerOf(root))
					return "permission denied";
				group.getParents().add(root);
			}
			bot.getAuthorizedGroups().put(group.getName(), group);
			bot.saveConfiguration();
			return "group created";
		}
		if("delete".equals(args.get(1))) {
			if(help || args.size() != 3)
				return "group delete <name>: delete a group";
			String name = args.get(2);
			if("@root".equals(name))
				return "cannot delete root group";
			if(bot.getAuthorizedGroups().get(name) == null)
				return "group " + name + " does not exist";
			AuthorizedGroup group = bot.getAuthorizedGroups().get(name);
			if(caller == null || !caller.isOwnerOf(group))
				return "permission denied";
			bot.getAuthorizedGroups().remove(group);
			bot.saveConfiguration();
			return "group deleted";
		}
		if("set_name".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group set_name <old_name> <new_name>";
			String oldName = args.get(2);
			AuthorizedGroup group = bot.getAuthorizedGroups().get(oldName);
			if(group == null)
				return "group with old name " + oldName + " does not exist";
			if(caller == null || !caller.isOwnerOf(group))
				return "permission denied";
			String newName = args.get(3);
			if(bot.getAuthorizedGroups().get(newName) != null)
				return "group with new name " + newName + " already exists";
			group.setName(newName);
			bot.saveConfiguration();
			return "group updated";
		}
		if("add_user".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group add_user <group> <user>";
			AuthorizedGroup group = bot.getAuthorizedGroups().get(args.get(2));
			if(group == null)
				return "group does not exist";
			if(caller == null || !caller.isOwnerOf(group))
				return "permission denied";
			AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(3));
			if(user == null)
				return "user does not exist";
			if(user.getGroups().contains(group))
				return "user " + user.getName() + " is already a member of group " + group.getName();
			user.getGroups().add(group);
			bot.saveConfiguration();
			return "user added";
		}
		if("remove_user".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group remove_user <group> <user>";
			AuthorizedGroup group = bot.getAuthorizedGroups().get(args.get(2));
			if(group == null)
				return "group does not exist";
			if(caller == null || !caller.isOwnerOf(group))
				return "permission denied";
			AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(3));
			if(user == null)
				return "user does not exist";
			if(!user.getGroups().contains(group))
				return "user " + user.getName() + " is not a member of group " + group.getName();
			user.getGroups().remove(group);
			bot.saveConfiguration();
			return "user removed";
		}
		if("add_parent".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group add_parent <child> <parent>";
			AuthorizedGroup child = bot.getAuthorizedGroups().get(args.get(2));
			if(child == null)
				return "child does not exist";
			if(caller == null || !caller.isOwnerOf(child))
				return "permission denied";
			AuthorizedGroup parent = bot.getAuthorizedGroups().get(args.get(3));
			if(parent == null)
				return "parent does not exist";
			if(child.getParents().contains(parent))
				return "group " + parent.getName() + " is already a parent of group " + child.getName();
			child.getParents().add(parent);
			bot.saveConfiguration();
			return "parent added";
		}
		if("remove_parent".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group remove_parent <child> <parent>";
			AuthorizedGroup child = bot.getAuthorizedGroups().get(args.get(2));
			if(child == null)
				return "child does not exist";
			if(caller == null || !caller.isOwnerOf(child))
				return "permission denied";
			AuthorizedGroup parent = bot.getAuthorizedGroups().get(args.get(3));
			if(parent == null)
				return "parent does not exist";
			if(!child.getParents().contains(parent))
				return "group " + parent.getName() + " is not a parent of group " + child.getName();
			child.getParents().remove(parent);
			bot.saveConfiguration();
			return "parent removed";
		}
		if("add_alias".equals(args.get(1))) {
			if(help || args.size() != 5)
				return "group add_alias <group> <name> <command>";
			AuthorizedGroup group = bot.getAuthorizedGroups().get(args.get(2));
			if(group == null)
				return "group does not exist";
			if(caller == null || !caller.isOwnerOf(group))
				return "permission denied";
			if(group.getCommands().getCommands().get(args.get(3)) != null)
				return "named command already exists";
			group.getCommands().getCommands().put(args.get(3), new AliasCommand(args.get(4)));
			bot.saveConfiguration();
			return "alias added";
		}
		if("remove_alias".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group remove_alias <group> <name>";
			AuthorizedGroup group = bot.getAuthorizedGroups().get(args.get(2));
			if(group == null)
				return "group does not exist";
			if(caller == null || !caller.isOwnerOf(group))
				return "permission denied";
			Command c = group.getCommands().getCommands().get(args.get(3));
			if(c == null)
				return "named command does not exist";
			if(!(c instanceof AliasCommand))
				return "named command not an alias";
			group.getCommands().getCommands().remove(args.get(3));
			bot.saveConfiguration();
			return "alias removed";
		}
		if("show_alias".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group show_alias <group> <name>";
			AuthorizedGroup group = bot.getAuthorizedGroups().get(args.get(2));
			if(group == null)
				return "group does not exist";
			Command c = group.getCommands().getCommands().get(args.get(3));
			if(c == null)
				return "named command does not exist";
			if(!(c instanceof AliasCommand))
				return "named command not an alias";
			return c.toString();
		}
		return "group <command>: list, info, create, delete, set_name, add_user, remove_user, add_parent, remove_parent, add_alias, remove_alias, show_alias";
	}

}
