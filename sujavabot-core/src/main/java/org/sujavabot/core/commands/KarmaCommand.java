package org.sujavabot.core.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
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
			
			String nick = event.getUser().getNick();
			
			m = ADJUSTMENT_UP_PRE.matcher(s);
			while(m.find() && !nick.equals(m.group(1)))
				adjustUp(context, m.group(1));
			
			m = ADJUSTMENT_UP_POST.matcher(s);
			while(m.find() && !nick.equals(m.group(1)))
				adjustUp(context, m.group(1));
			
			m = ADJUSTMENT_DOWN_PRE.matcher(s);
			while(m.find() && !nick.equals(m.group(1)))
				adjustDown(context, m.group(1));
			
			m = ADJUSTMENT_DOWN_POST.matcher(s);
			while(m.find() && !nick.equals(m.group(1)))
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
		context = context.toLowerCase();
		nick = nick.toLowerCase();
		String key = context + " " + nick;
		Properties p = read();
		p.setProperty(key, String.valueOf(getKarma(context, nick).add(BigInteger.ONE)));
		write(p);
	}
	
	public static synchronized void adjustDown(String context, String nick) {
		context = context.toLowerCase();
		nick = nick.toLowerCase();
		String key = context + " " + nick;
		Properties p = read();
		p.setProperty(key, String.valueOf(getKarma(context, nick).subtract(BigInteger.ONE)));
		write(p);
	}
	
	public static synchronized BigInteger getKarma(String context, String nick) {
		context = context.toLowerCase();
		nick = nick.toLowerCase();
		String key = context + " " + nick;
		return new BigInteger(read().getProperty(key, "0"));
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("[<channel>] <nick>: show karma for nick in channel");
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		Channel channel = Events.getChannel(cause);
		String context;
		String nick;
		if(channel == null) {
			if(args.size() != 3)
				return invokeHelp(bot, cause, args);
			context = args.get(1);
			nick = args.get(2);
		} else {
			if(args.size() == 2) {
				context = channel.getName();
				nick = args.get(1);
			} else if(args.size() == 3) {
				context = args.get(1);
				nick = args.get(2);
			} else
				return invokeHelp(bot, cause, args);
		}
		return String.valueOf(getKarma(context, nick));
	}


}
