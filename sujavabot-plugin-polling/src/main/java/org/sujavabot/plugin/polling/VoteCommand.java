package org.sujavabot.plugin.polling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.util.Events;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class VoteCommand extends AbstractReportingCommand implements HelperConvertable<VoteCommand> {

	private File directory;
	

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args);
		String name = args.get(1);
		String option = args.get(2);
		if(!name.matches("\\w+"))
			return "invalid poll name";
		File file = new File(directory, name + ".poll");
		if(!file.exists())
			return "no such poll";
		Properties props = new Properties();
		try {
			FileInputStream fin = new FileInputStream(file);
			try {
				props.load(fin);
			} finally {
				fin.close();
			}
		} catch(IOException e) {
			return "unable to real poll";
		}
		String vote;
		if(props.getProperty("option." + option) != null)
			vote = option;
		else {
			List<String> matches = new ArrayList<String>();
			for(String p : props.stringPropertyNames()) {
				if(!p.startsWith("option."))
					continue;
				String o = p.substring("option.".length());
				if(o.toUpperCase().startsWith(option.toUpperCase()))
					matches.add(o);
			}
			if(matches.size() == 0)
				return "no matching option";
			if(matches.size() == 1)
				vote = matches.get(0);
			else
				return "multiple matching options: " + StringUtils.join(matches, " ");
		}
		String nick = Events.getUser(cause).getNick();
		String oldvote = null;
		Pattern nickp = Pattern.compile(Pattern.quote("," + nick) + "(?,|$");
		for(String p : props.stringPropertyNames()) {
			if(!p.startsWith("option."))
				continue;
			String v = props.getProperty(p);
			if(nickp.matcher(v).find()) {
				oldvote = v;
			}
		}
		if(oldvote != null) {
			String oldval = props.getProperty(oldvote);
			oldval = oldval.replaceAll(Pattern.quote("," + nick) + "(?,|$)", "");
			props.setProperty(oldvote, oldval);
		}
		String newval = props.getProperty(vote);
		newval += "," + nick;
		props.setProperty(vote, newval);
		try {
			FileOutputStream fout = new FileOutputStream(file);
			FileLock lock = fout.getChannel().lock();
			try {
				props.store(fout, props.getProperty("question"));
			} finally {
				lock.release();
				fout.close();
			}
		} catch(IOException e) {
			return "unable to write poll";
		}
		if(oldvote != null)
			return "changed vote to " + vote;
		return "voted for " + vote;
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<name> <option>: vote for a poll option");
	}

	@Override
	public void configure(MarshalHelper helper, VoteCommand defaults) {
		helper.field("directory", String.class, () -> directory.getPath());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		directory = new File("polls");
		helper.field("directory", String.class, (s) -> directory = new File(s));
	}

}
