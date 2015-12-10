package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

public class UserAdminCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() <= 1) {
			return "user <command>: list, info, create, delete, set_name, set_nick, add_alias, remove_alias, show_alias";
		}
		boolean help = "help".equals(args.get(0));
		AuthorizedUser caller = bot.getAuthorizedUser(getUser(cause));
		if("list".equals(args.get(1))) {
			if(help || args.size() != 2)
				return "user list: list the user names";
			StringBuilder sb = new StringBuilder();
			for(AuthorizedUser user : bot.getAuthorizedUsers().values()) {
				if(sb.length() > 0)
					sb.append(" ");
				sb.append(user.getName());
			}
			return sb.toString();
		}
		if("info".equals(args.get(1))) {
			if(help || args.size() != 3)
				return "user info <name>: show user info";
			String name = args.get(2);
			AuthorizedUser user = bot.getAuthorizedUsers().get(name);
			if(user == null)
				return "user " + name + " does not exist";
			
			List<String> commands = new ArrayList<>();
			List<String> groups = new ArrayList<>();
			List<String> allCommands = new ArrayList<>();
			
			for(Entry<String, Command> e : user.getCommands().getCommands().entrySet())
				commands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
			for(AuthorizedGroup parent : user.getGroups())
				groups.add(parent.getName());
			for(Entry<String, Command> e : user.getAllCommands().entrySet())
				allCommands.add(e.getKey() + ((e.getValue() instanceof AliasCommand) ? "*" : ""));
			
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("name", name);
			m.put("nick", user.getNick().pattern());
			m.put("commands", commands);
			m.put("groups", groups);
			m.put("all-commands", allCommands);
			
			return m.toString();
		}
		if("create".equals(args.get(1))) {
			if(help || args.size() < 3)
				return "user create <name> <nick> [<group>...]: create a user";
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
			if(help || args.size() != 3)
				return "user delete <name>: delete a user";
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
			if(help || args.size() != 4)
				return "user set_name <old_name> <new_name>";
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
			if(help || args.size() != 4) 
				return "user set_name <old_name> <new_name>";
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
			if(help || args.size() != 5)
				return "user add_alias <user> <name> <command>";
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
			if(help || args.size() != 4)
				return "user remove_alias <user> <name>";
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
			if(help || args.size() != 4)
				return "user show_alias <user> <name>";
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
		return "user <command>: list, info, create, delete, set_name, set_nick, add_alias, remove_alias, show_alias";
	}

}
