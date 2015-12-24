package org.sujavabot.core.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.util.Events;

public class KarmaCommand extends AbstractReportingCommand {

	public static final Pattern ADJUSTMENT_UP_PRE = Pattern.compile("\\+\\+([^\\s+\\-]+)");
	public static final Pattern ADJUSTMENT_UP_POST = Pattern.compile("([^\\s+\\-]+)\\+\\+");
	
	public static final Pattern ADJUSTMENT_DOWN_PRE = Pattern.compile("--([^\\s+\\-]+)");
	public static final Pattern ADJUSTMENT_DOWN_POST = Pattern.compile("([^\\s+\\-]+)--");

	public static class Listener extends ListenerAdapter<PircBotX> {
		@Override
		public void onMessage(MessageEvent<PircBotX> event) throws Exception {
			String s = event.getMessage();
			String context = event.getChannel().getName();
			Matcher m;
			
			m = ADJUSTMENT_UP_PRE.matcher(s);
			while(m.find())
				adjustUp(context, m.group(1));
			
			m = ADJUSTMENT_UP_POST.matcher(s);
			while(m.find())
				adjustUp(context, m.group(1));
			
			m = ADJUSTMENT_DOWN_PRE.matcher(s);
			while(m.find())
				adjustDown(context, m.group(1));
			
			m = ADJUSTMENT_DOWN_POST.matcher(s);
			while(m.find())
				adjustDown(context, m.group(1));
		}
	}
	
	protected static final File KARMA = new File("karma.properties");
	
	protected static synchronized Properties read() {
		Properties p = new Properties();
		try {
			InputStream in = new FileInputStream(KARMA);
			try {
				p.load(in);
			} finally {
				in.close();
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return p;
	}
	
	protected static synchronized void write(Properties p) {
		try {
			OutputStream out = new FileOutputStream(KARMA);
			try {
				p.store(out, "karma");
			} finally {
				out.close();
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static synchronized void adjustUp(String context, String nick) {
		String key = context + " " + nick;
		Properties p = read();
		p.setProperty(key, String.valueOf(getKarma(context, nick) + 1));
		write(p);
	}
	
	public static synchronized void adjustDown(String context, String nick) {
		String key = context + " " + nick;
		Properties p = read();
		p.setProperty(key, String.valueOf(getKarma(context, nick) - 1));
		write(p);
	}
	
	public static synchronized int getKarma(String context, String nick) {
		String key = context + " " + nick;
		return Integer.parseInt(read().getProperty(key, "0"));
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("<nick>: show karma for nick");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 2)
			return invokeHelp(bot, cause, args);
		return String.valueOf(getKarma(Events.getChannel(cause).getName(), args.get(1)));
	}


}
