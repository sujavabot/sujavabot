package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class UserAdminCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("authorization user control", 
				"list", "list users",
				"info", "<user> [<field>]: show user info",
				"create", "<user>: create a user",
				"delete", "<user>: delete a user",
				"set_name", "<old_user> <new_user>: change a user name",
				"set_nick", "<name> <new_nick>: change a user nick",
				"add_alias", "<user> <name> <command>: add a command alias to a user",
				"remove_alias", "<user> <name>: remove a command alias from a user",
				"show_alias", "<user> <name>: show a command alias from a user"
				);
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() <= 1) {
			return invokeHelp(bot, cause, args);
		}
		AuthorizedUser caller = bot.getAuthorizedUser(getUser(cause));
		if("list".equals(args.get(1))) {
			if(args.size() != 2)
				return invokeHelp(bot, cause, args, "list");
			StringBuilder sb = new StringBuilder();
			for(AuthorizedUser user : bot.getAuthorizedUsers().values()) {
				if(sb.length() > 0)
					sb.append(" ");
				sb.append(user.getName());
			}
			return sb.toString();
		}
		if("info".equals(args.get(1))) {
			if((args.size() != 3 && args.size() != 4))
				return invokeHelp(bot, cause, args, "info");
			String name = args.get(2);
			AuthorizedUser user = bot.getAuthorizedUsers().get(name);
			if(user == null)
				return "user " + name + " does not exist";
			
			List<String> commands = new ArrayList<>();
			List<String> groups = new ArrayList<>();
			List<String> allCommands = new ArrayList<>();
			
			for(Entry<String, Command> e : user.getCommands().getCommands().entrySet()) {
				if(!(e.getValue() instanceof HiddenCommand))
					commands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
			}
			for(AuthorizedGroup parent : user.getGroups())
				groups.add(parent.getName());
			for(Entry<String, Command> e : user.getAllCommands().entrySet()) {
				if(!(e.getValue() instanceof HiddenCommand))
					allCommands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
			}
			
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("name", name);
			m.put("nick", user.getNick().pattern());
			m.put("commands", commands);
			m.put("groups", groups);
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
			if(args.size() < 3)
				return invokeHelp(bot, cause, args, "create");
			String name = args.get(2);
			if(bot.getAuthorizedUsers().get(name) != null)
				return "user " + name + " already exists";
			Pattern nick;
			try {
				nick = Pattern.compile(args.get(3));
			} catch(RuntimeException e) {
				return "invalid nick pattern";
			}
			List<AuthorizedGroup> groups = new ArrayList<>();
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(root != null) {
				if(caller == null || !caller.isOwnerOf(root))
					return "permission denied";
				groups.add(root);
			}
			for(int i = 4; i < args.size(); i++) {
				AuthorizedGroup group = bot.getAuthorizedGroups().get(args.get(i));
				if(group == null)
					return "no such group " + args.get(i);
				groups.add(group);
			}
			AuthorizedUser user = new AuthorizedUser(name);
			user.setNick(nick);
			user.setGroups(groups);
			bot.getAuthorizedUsers().put(user.getName(), user);
			bot.saveConfiguration();
			return "user created";
		}
		if("delete".equals(args.get(1))) {
			if(args.size() != 3)
				return invokeHelp(bot, cause, args, "delete");
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(root != null) {
				if(caller == null || !caller.isOwnerOf(root))
					return "permission denied";
			}
			String name = args.get(2);
			if(bot.getAuthorizedUsers().get(name) == null)
				return "user " + name + " does not exist";
			AuthorizedUser user = bot.getAuthorizedUsers().get(name);
			bot.getAuthorizedUsers().remove(user);
			bot.saveConfiguration();
			return "user deleted";
		}
		if("set_name".equals(args.get(1))) {
			if(args.size() != 4)
				return invokeHelp(bot, cause, args, "set_name");
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(root != null) {
				if(caller == null || !caller.isOwnerOf(root))
					return "permission denied";
			}
			String oldName = args.get(2);
			AuthorizedUser user = bot.getAuthorizedUsers().get(oldName);
			if(user == null)
				return "user with old name " + oldName + " does not exist";
			String newName = args.get(3);
			if(bot.getAuthorizedUsers().get(newName) != null)
				return "user with new name " + newName + " already exists";
			user.setName(newName);
			bot.saveConfiguration();
			return "user updated";
		}
		if("set_nick".equals(args.get(1))) {
			if(args.size() != 4) 
				return invokeHelp(bot, cause, args, "set_nick");
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(root != null) {
				if(caller == null || !caller.isOwnerOf(root))
					return "permission denied";
			}
			String name = args.get(2);
			AuthorizedUser user = bot.getAuthorizedUsers().get(name);
			if(user == null)
				return "user with name " + name + " does not exist";
			String nick = args.get(3);
			Pattern p;
			try {
				p = Pattern.compile(nick);
			} catch(RuntimeException e) {
				return "invalid regex";
			}
			user.setNick(p);
			bot.saveConfiguration();
			return "user updated";
		}
		if("add_alias".equals(args.get(1))) {
			if(args.size() != 5)
				return invokeHelp(bot, cause, args, "add_alias");
			AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
			if(user == null)
				return "user does not exist";
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(caller == null || (caller != user && (root == null || !caller.isOwnerOf(root))))
				return "permission denied";
			if(user.getCommands().getCommands().get(args.get(3)) != null)
				return "named command already exists";
			user.getCommands().getCommands().put(args.get(3), new AliasCommand(args.get(4)));
			bot.saveConfiguration();
			return "alias added";
		}
		if("remove_alias".equals(args.get(1))) {
			if(args.size() != 4)
				return invokeHelp(bot, cause, args, "remove_alias");
			AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
			if(user == null)
				return "user does not exist";
			AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
			if(caller == null || (caller != user && (root == null || !caller.isOwnerOf(root))))
				return "permission denied";
			Command c = user.getCommands().getCommands().get(args.get(3));
			if(c == null)
				return "named command does not exist";
			if(!(c instanceof AliasCommand))
				return "named command not an alias";
			user.getCommands().getCommands().remove(args.get(3));
			bot.saveConfiguration();
			return "alias removed";
		}
		if("show_alias".equals(args.get(1))) {
			if(args.size() != 4)
				return invokeHelp(bot, cause, args, "show_alias");
			AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
			if(user == null)
				return "user does not exist";
			Command c = user.getCommands().getCommands().get(args.get(3));
			if(c == null)
				return "named command does not exist";
			if(!(c instanceof AliasCommand))
				return "named command not an alias";
			return c.toString();
		}
		return invokeHelp(bot, cause, args);
	}

}
