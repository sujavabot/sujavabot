package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;

public class UserAdminCommand extends AbstractReportingCommand {

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() <= 1) {
			return "user <command>: list, info, create, delete, set_name, set_nick";
		}
		boolean help = "help".equals(args.get(0));
		if("list".equals(args.get(1))) {
			if(help || args.size() != 2)
				return "user list: list the user names";
			StringBuilder sb = new StringBuilder();
			for(AuthorizedUser user : bot.getAuthorizedUsers()) {
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
			AuthorizedUser user = bot.getAuthorizedUser(name);
			if(user == null)
				return "user " + name + " does not exist";
			
			List<String> commands = new ArrayList<>();
			List<String> groups = new ArrayList<>();
			List<String> allCommands = new ArrayList<>();
			
			commands.addAll(user.getCommands().getCommands().keySet());
			for(AuthorizedGroup parent : user.getGroups())
				groups.add(parent.getName());
			allCommands.addAll(user.getAllCommands().keySet());
			
			return String.format("user %s: nick:%s commands: %s groups:%s all-commands:%s", name, user.getNick().pattern(), commands, groups, allCommands);
		}
		if("create".equals(args.get(1))) {
			if(help || args.size() < 3)
				return "user create <name> <nick> [<group>...]: create a user";
			String name = args.get(2);
			if(bot.getAuthorizedUser(name) != null)
				return "user " + name + " already exists";
			Pattern nick;
			try {
				nick = Pattern.compile(args.get(3));
			} catch(RuntimeException e) {
				return "invalid nick pattern";
			}
			List<AuthorizedGroup> groups = new ArrayList<>();
			for(int i = 4; i < args.size(); i++) {
				AuthorizedGroup group = bot.getAuthorizedGroup(args.get(i));
				if(group == null)
					return "no such group " + args.get(i);
				groups.add(group);
			}
			AuthorizedUser user = new AuthorizedUser(name);
			user.setNick(nick);
			user.setGroups(groups);
			bot.getAuthorizedUsers().add(user);
			return "user created";
		}
		if("delete".equals(args.get(1))) {
			if(help || args.size() != 3)
				return "user delete <name>: delete a user";
			String name = args.get(2);
			if(bot.getAuthorizedUser(name) == null)
				return "user " + name + " does not exist";
			AuthorizedUser user = bot.getAuthorizedUser(name);
			bot.getAuthorizedUsers().remove(user);
			return "user deleted";
		}
		if("set_name".equals(args.get(1))) {
			if(help || args.size() != 4)
				return "user set_name <old_name> <new_name>";
			String oldName = args.get(2);
			AuthorizedUser user = bot.getAuthorizedUser(oldName);
			if(user == null)
				return "user with old name " + oldName + " does not exist";
			String newName = args.get(3);
			if(bot.getAuthorizedUser(newName) != null)
				return "user with new name " + newName + " already exists";
			user.setName(newName);
			return "user updated";
		}
		if("set_nick".equals(args.get(1))) {
			if(help || args.size() != 4) 
				return "user set_name <old_name> <new_name>";
			String name = args.get(2);
			AuthorizedUser user = bot.getAuthorizedUser(name);
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
			return "user updated";
		}
		return "user <command>: list, info, create, delete, set_name, set_nick";
	}

}
