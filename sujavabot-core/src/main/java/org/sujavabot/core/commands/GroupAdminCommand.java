package org.sujavabot.core.commands;

import java.util.List;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;

public class GroupAdminCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() <= 1) {
			return "group <command>: create, delete, set_name, add_user, remove_user, add_parent, remove_parent";
		}
		boolean help = "help".equals(args.get(0));
		if("create".equals(args.get(1))) {
			if(help || args.size() != 3)
				return "group create <name>: create a group";
			String name = args.get(2);
			if(bot.getAuthorizedGroup(name) != null)
				return "group " + name + " already exists";
			AuthorizedGroup group = new AuthorizedGroup(name);
			bot.getAuthorizedGroups().add(group);
			return "group created";
		}
		if("delete".equals(args.get(1))) {
			if(help || args.size() != 3)
				return "group delete <name>: delete a group";
			String name = args.get(2);
			if("root".equals(name))
				return "cannot delete root group";
			if(bot.getAuthorizedGroup(name) == null)
				return "group " + name + " does not exist";
			AuthorizedGroup group = bot.getAuthorizedGroup(name);
			bot.getAuthorizedGroups().remove(group);
			return "group deleted";
		}
		if("set_name".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group set_name <old_name> <new_name>";
			String oldName = args.get(2);
			AuthorizedGroup group = bot.getAuthorizedGroup(oldName);
			if(group == null)
				return "group with old name " + oldName + " does not exist";
			String newName = args.get(3);
			if(bot.getAuthorizedGroup(newName) != null)
				return "group with new name " + newName + " already exists";
			group.setName(newName);
			return "group updated";
		}
		if("add_user".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group add_user <group> <user>";
			AuthorizedGroup group = bot.getAuthorizedGroup(args.get(2));
			if(group == null)
				return "group does not exist";
			AuthorizedUser user = bot.getAuthorizedUser(args.get(3));
			if(user == null)
				return "user does not exist";
			if(user.getGroups().contains(group))
				return "user " + user.getName() + " is already a member of group " + group.getName();
			user.getGroups().add(group);
			return "user added";
		}
		if("remove_user".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group remove_user <group> <user>";
			AuthorizedGroup group = bot.getAuthorizedGroup(args.get(2));
			if(group == null)
				return "group does not exist";
			AuthorizedUser user = bot.getAuthorizedUser(args.get(3));
			if(user == null)
				return "user does not exist";
			if(!user.getGroups().contains(group))
				return "user " + user.getName() + " is not a member of group " + group.getName();
			user.getGroups().remove(group);
			return "user removed";
		}
		if("add_parent".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group add_parent <child> <parent>";
			AuthorizedGroup child = bot.getAuthorizedGroup(args.get(2));
			if(child == null)
				return "child does not exist";
			AuthorizedGroup parent = bot.getAuthorizedGroup(args.get(3));
			if(parent == null)
				return "parent does not exist";
			if(child.getParents().contains(parent))
				return "group " + parent.getName() + " is already a parent of group " + child.getName();
			child.getParents().add(parent);
			return "parent added";
		}
		if("remove_parent".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "group remove_parent <child> <parent>";
			AuthorizedGroup child = bot.getAuthorizedGroup(args.get(2));
			if(child == null)
				return "child does not exist";
			AuthorizedGroup parent = bot.getAuthorizedGroup(args.get(3));
			if(parent == null)
				return "parent does not exist";
			if(!child.getParents().contains(parent))
				return "group " + parent.getName() + " is not a parent of group " + child.getName();
			child.getParents().remove(parent);
			return "parent removed";
		}
		return "group <command>: create, delete, set_name, add_user, remove_user, add_parent, remove_parent";
	}

}
