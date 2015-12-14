package org.sujavabot.core.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.Authorization;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.Command;
import org.sujavabot.core.SujavaBot;

import com.google.common.collect.Lists;

public class UserAdminCommand extends AbstractReportingCommand {

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("authorization user control", 
				"list", "list users",
				"whois", "<nick>: show username of a nick",
				"info", "<user> [<field>]: show user info",
				"add", "<user>: create a user",
				"remove", "<user>: delete a user",
				"set_name", "<old_user> <new_user>: change a user name",
				"set_nick", "<name> <new_nick>: change a user nick regex",
				"add_alias", "<user> <name> <command>: add a command alias to a user",
				"remove_alias", "<user> <name>: remove a command alias from a user",
				"show_alias", "<user> <name>: show a command alias from a user",
				"set_property", "<key> <value>: set a property on a user",
				"unset_property", "<key>: unset a property from a user"
				);
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if (args.size() <= 1)
			return invokeHelp(bot, cause, args);
		if ("list".equals(args.get(1)))
			return _list(bot, cause, args);
		else if("whois".equals(args.get(1)))
			return _whois(bot, cause, args);
		else if ("info".equals(args.get(1)))
			return _info(bot, cause, args);
		else if ("add".equals(args.get(1)))
			return _add(bot, cause, args);
		else if ("remove".equals(args.get(1)))
			return _remove(bot, cause, args);
		else if ("set_name".equals(args.get(1)))
			return _set_name(bot, cause, args);
		else if ("set_nick".equals(args.get(1)))
			return _set_nick(bot, cause, args);
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
		if(args.size() != 2)
			return invokeHelp(bot, cause, args, "list");
		List<String> names = Lists.transform(new ArrayList<>(bot.getAuthorizedUsers().values()), (au) -> au.getName());
		names = new ArrayList<>(names);
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return StringUtils.join(names, ", ");
	}

	protected String _whois(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args, "whois");
		String nick = args.get(2);
		User user = bot.getUserChannelDao().getUser(nick);
		if(user == null)
			return "no such nick";
		AuthorizedUser authUser = bot.getAuthorizedUser(user);
		return authUser.getName();
	}
	
	protected String _info(SujavaBot bot, Event<?> cause, List<String> args) {
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
		
		Collections.sort(commands, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(allCommands, String.CASE_INSENSITIVE_ORDER);
		
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", name);
		m.put("nick", user.getNick().pattern());
		m.put("commands", commands);
		m.put("groups", groups);
		m.put("all-commands", allCommands);
		
		Map<String, String> p = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, String> ap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		p.putAll(user.getProperties());
		ap.putAll(user.getAllProperties());
		
		m.put("properties", p);
		m.put("all-properties", ap);

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

	protected String _add(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 3)
			return invokeHelp(bot, cause, args, "add");
		String name = args.get(2);
		if(bot.getAuthorizedUsers().get(name) != null)
			return "user " + name + " already exists";
		if(!AuthorizedUser.CREATABLE_NAME.matcher(name).matches())
			return "invalid user name";
		Pattern nick;
		try {
			nick = Pattern.compile(args.get(3));
		} catch(RuntimeException e) {
			return "invalid nick pattern";
		}
		List<AuthorizedGroup> groups = new ArrayList<>();
		AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
		if(!Authorization.isCurrentRootOwner())
			return "permission denied";
		groups.add(root);
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

	protected String _remove(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args, "remove");
		AuthorizedGroup root = bot.getAuthorizedGroups().get("@root");
		if(!Authorization.isCurrentRootOwner())
			return "permission denied";
		String name = args.get(2);
		if(bot.getAuthorizedUsers().get(name) == null)
			return "user " + name + " does not exist";
		AuthorizedUser user = bot.getAuthorizedUsers().get(name);
		bot.getAuthorizedUsers().remove(user);
		bot.saveConfiguration();
		return "user deleted";
	}

	protected String _set_name(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4)
			return invokeHelp(bot, cause, args, "set_name");
		if(!Authorization.isCurrentRootOwner())
			return "permission denied";
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

	protected String _set_nick(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4) 
			return invokeHelp(bot, cause, args, "set_nick");
		String name = args.get(2);
		AuthorizedUser user = bot.getAuthorizedUsers().get(name);
		if(user == null)
			return "user with name " + name + " does not exist";
		if(!Authorization.isCurrentOwner(user))
			return "permission denied";
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

	protected String _add_alias(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 5)
			return invokeHelp(bot, cause, args, "add_alias");
		AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
		if(user == null)
			return "user with name " + args.get(2) + " does not exist";
		if(!Authorization.isCurrentOwner(user))
			return "permission denied";
		if(user.getCommands().getCommands().get(args.get(3)) != null)
			return "named command already exists";
		user.getCommands().getCommands().put(args.get(3), new AliasCommand(args.get(4)));
		bot.saveConfiguration();
		return "alias added";
	}

	protected String _remove_alias(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4)
			return invokeHelp(bot, cause, args, "remove_alias");
		AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
		if(user == null)
			return "user with name " + args.get(2) + " does not exist";
		if(!Authorization.isCurrentOwner(user))
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

	protected String _show_alias(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4)
			return invokeHelp(bot, cause, args, "show_alias");
		AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
		if(user == null)
			return "user does not exist";
		Command c = user.getAllCommands().get(args.get(3));
		if(c == null)
			return "named command does not exist";
		if(!(c instanceof AliasCommand))
			return "named command not an alias";
		return c.toString();
	}

	protected String _set_property(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 5)
			return invokeHelp(bot, cause, args, "set_property");
		AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
		if(user == null)
			return "user with name " + args.get(2) + " does not exist";
		if(!Authorization.isCurrentOwner(user))
			return "permission denied";
		String key = args.get(3);
		String value = args.get(4);
		user.getProperties().put(key, value);
		bot.saveConfiguration();
		return "property set";
	}

	protected String _unset_property(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4)
			return invokeHelp(bot, cause, args, "unset_property");
		AuthorizedUser user = bot.getAuthorizedUsers().get(args.get(2));
		if(user == null)
			return "user with name " + args.get(2) + " does not exist";
		if(!Authorization.isCurrentOwner(user))
			return "permission denied";
		String key = args.get(3);
		user.getProperties().remove(key);
		bot.saveConfiguration();
		return "alias removed";
	}

}
