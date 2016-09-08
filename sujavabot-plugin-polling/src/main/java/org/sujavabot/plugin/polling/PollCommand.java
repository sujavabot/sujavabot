package org.sujavabot.plugin.polling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class PollCommand extends AbstractReportingCommand implements HelperConvertable<PollCommand> {

	private File directory;
	
	private transient boolean listing;
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		listing = false;
		if(args.size() <= 1)
			return invokeHelp(bot, cause, args);
		if("create".equals(args.get(1)))
			return createPoll(bot, cause, args);
		else if("list".equals(args.get(1)))
			return listPolls(bot, cause, args);
		else if("question".equals(args.get(1)))
			return pollQuestion(bot, cause, args);
		else if("results".equals(args.get(1)))
			return pollResults(bot, cause, args);
		else if("delete".equals(args.get(1)))
			return deletePoll(bot, cause, args);
		else
			return invokeHelp(bot, cause, args);
	}
	
	@Override
	protected void reportMessage(SujavaBot bot, Event<?> cause, String result, boolean isChannelMessage) {
		super.reportMessage(bot, cause, result, isChannelMessage && !listing);
	}
	
	private String createPoll(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 5)
			return invokeHelp(bot, cause, args, "create");
		String name = args.get(2);
		String question = args.get(3);
		List<String> options = args.subList(4, args.size());
		if(!name.matches("\\w+"))
			return "invalid poll name";
		File file = new File(directory, name + ".poll");
		if(file.exists())
			return "poll already exists";
		Properties props = new Properties();
		props.setProperty("question", question);
		for(String o : options) 
			props.setProperty("option." + o, "");
		try {
			directory.mkdirs();
			OutputStream out = new FileOutputStream(file);
			try {
				props.store(out, question);
			} finally {
				out.close();
			}
		} catch(IOException e) {
			return "unable to write poll";
		}
		return "poll created";
	}

	private String listPolls(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 2)
			return invokeHelp(bot, cause, args, "list");
		directory.mkdirs();
		List<String> polls = new ArrayList<String>();
		for(File f : directory.listFiles()) {
			if(f.getName().matches("\\w+\\.poll"))
				polls.add(f.getName().substring(0, f.getName().length() - 5));
		}
		if(polls.size() == 0)
			return "<no polls>";
		Collections.sort(polls);
		for(int i = 0; i < polls.size(); i++) {
			String name = polls.get(i);
			File file = new File(directory, name + ".poll");
			Properties props = new Properties();
			try {
				FileInputStream fin = new FileInputStream(file);
				try {
					props.load(fin);
				} finally {
					fin.close();
				}
			} catch(IOException e) {
				continue;
			}
			polls.set(i, name + ": " + props.getProperty("question"));
		}
		listing = true;
		return StringUtils.join(polls, "\n");
	}

	private String pollResults(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args, "results");
		String name = args.get(2);
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
		List<String> results = new ArrayList<String>();
		for(String p : props.stringPropertyNames()) {
			if(!p.startsWith("option."))
				continue;
			String option = p.substring("option.".length());
			int count = props.getProperty(p).split(",").length - 1;
			results.add(option + ": " + count);
		}
		Collections.sort(results);
		return StringUtils.join(results, " \n");
	}

	private String pollQuestion(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args, "question");
		String name = args.get(2);
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
		return props.getProperty("question");
	}

	
	private String deletePoll(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 3)
			return invokeHelp(bot, cause, args, "delete");
		String name = args.get(2);
		if(!name.matches("\\w+"))
			return "invalid poll name";
		File file = new File(directory, name + ".poll");
		if(!file.exists())
			return "no such poll";
		if(!file.delete())
			return "unable to delete poll";
		return "poll deleted";
	}

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("polling",
				"create", "<name> <question> <options>...: create a poll",
				"list", ": list existing polls",
				"question", "<name>: return the poll question",
				"results", "<name>: show poll results",
				"delete", "<name>: delete a poll");
	}

	@Override
	public void configure(MarshalHelper helper, PollCommand defaults) {
		helper.field("directory", String.class, () -> directory.getPath());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		directory = new File("polls");
		helper.field("directory", String.class, (s) -> directory = new File(s));
	}

}
