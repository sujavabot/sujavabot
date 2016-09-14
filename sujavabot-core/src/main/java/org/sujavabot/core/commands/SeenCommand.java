package org.sujavabot.core.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.SujavaBot;

public class SeenCommand extends AbstractReportingCommand {
	private static final File SEEN_DB = new File("seen.db");
	
	public static class Listener extends ListenerAdapter<PircBotX> {
		@Override
		public void onAction(ActionEvent<PircBotX> event) throws Exception {
			updateSeen(event.getBot(), event.getChannel().getName(), event.getUser(), "performing action *" + event.getUser().getNick() + " " + event.getAction());
		}
		
		@Override
		public void onJoin(JoinEvent<PircBotX> event) throws Exception {
			updateSeen(event.getBot(), event.getChannel().getName(), event.getUser(), "joining the channel");
		}
		
		@Override
		public void onPart(PartEvent<PircBotX> event) throws Exception {
			updateSeen(event.getBot(), event.getChannel().getName(), event.getUser(), "parting the channel (" + event.getReason() + ")");
		}
		
		@Override
		public void onKick(KickEvent<PircBotX> event) throws Exception {
			updateSeen(event.getBot(), event.getChannel().getName(), event.getUser(), "kicked from the channel (" + event.getReason() + ")");
		}
		
		@Override
		public void onMessage(MessageEvent<PircBotX> event) throws Exception {
			updateSeen(event.getBot(), event.getChannel().getName(), event.getUser(), "saying <" + event.getUser().getNick() + "> " + event.getMessage());
		}
		
		@Override
		public void onQuit(QuitEvent<PircBotX> event) throws Exception {
			for(Channel c : event.getDaoSnapshot().getChannels(event.getUser())) {
				updateSeen(event.getBot(), c.getName(), event.getUser(), "quiting IRC (" + event.getReason() + ")");
			}
		}
		
	}
	
	private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static {
		DF.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	protected static String parseDoingWhere(Entry<String, String> doing) {
		return doing.getKey().split("/", 3)[0];
	}
	
	protected static String parseDoingType(Entry<String, String> doing) {
		return doing.getKey().split("/", 3)[1];
	}
	
	protected static String parseDoingName(Entry<String, String> doing) {
		return doing.getKey().split("/", 3)[2];
	}
	
	protected static Date parseDoingDate(Entry<String, String> doing) {
		try {
			return DF.parse(doing.getValue().split("/", 2)[0]);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected static String parseDoingMessage(Entry<String, String> doing) {
		return doing.getValue().split("/", 2)[1];
	}
	
	protected static synchronized void updateSeen(PircBotX bot, String where, User user, String doing) {
		doing = DF.format(System.currentTimeMillis()) + "/" + doing;
		
		AuthorizedUser authUser = ((SujavaBot) bot).getAuthorizedUser(user, false);
		Map<String, String> seen = getSeen();
		seen.remove(where + "/nick/" + user.getNick());
		seen.remove(where + "/user/" + authUser.getName());
		seen.put(where + "/nick/" + user.getNick(), doing);
		seen.put(where + "/user/" + authUser.getName(), doing);
		setSeen(seen);
	}
	
	protected static synchronized Entry<String, String> getNickSeen(Pattern where, Pattern nick) {
		Map<String, String> seen = getSeen();
		Entry<String, String> doing = null;
		for(Entry<String, String> e : seen.entrySet()) {
			String[] f = e.getKey().split("/", 3);
			if(!"nick".equals(f[1]))
				continue;
			if(where.matcher(f[0]).matches() && nick.matcher(f[2]).matches()) {
				doing = e;
			}
		}
		return doing;
	}
	
	protected static synchronized Entry<String, String> getUserSeen(Pattern where, Pattern user) {
		Map<String, String> seen = getSeen();
		Entry<String, String> doing = null;
		for(Entry<String, String> e : seen.entrySet()) {
			String[] f = e.getKey().split("/", 3);
			if(!"user".equals(f[1]))
				continue;
			if(where.matcher(f[0]).matches() && user.matcher(f[2]).matches()) {
				doing = e;
			}
		}
		return doing;
	}
	
	@SuppressWarnings("unchecked")
	protected static synchronized Map<String, String> getSeen() {
		try {
			InputStream in = new FileInputStream(SEEN_DB);
			try {
				ObjectInputStream oin = new ObjectInputStream(in);
				return new LinkedHashMap<>((Map<String, String>) oin.readObject());
			} finally {
				in.close();
			}
		} catch(Exception e) {
			if(!(e instanceof FileNotFoundException))
				e.printStackTrace();
			return new LinkedHashMap<>();
		}
	}
	
	protected static synchronized void setSeen(Map<String, String> seen) {
		File tmp = new File(SEEN_DB.getParentFile(), SEEN_DB.getName() + ".tmp");
		try {
			OutputStream out = new FileOutputStream(tmp);
			try {
				ObjectOutputStream oout = new ObjectOutputStream(out);
				oout.writeObject(seen);
				oout.close();
			} finally {
				out.close();
			}
			Files.move(tmp.toPath(), SEEN_DB.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static String formatElapsed(long elapsed) {
		if(TimeUnit.SECONDS.convert(elapsed, TimeUnit.MILLISECONDS) == 0)
			return "0 seconds";
		Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(elapsed);
		List<String> s = new ArrayList<>();
		s.add(c.get(Calendar.SECOND) + " seconds");
		if(TimeUnit.MINUTES.convert(elapsed, TimeUnit.MILLISECONDS) >= 1)
			s.add(c.get(Calendar.MINUTE) + " minutes");
		if(TimeUnit.HOURS.convert(elapsed, TimeUnit.MILLISECONDS) >= 1) {
			s.remove(0);
			s.add(c.get(Calendar.HOUR_OF_DAY) + " hours");
		}
		if(TimeUnit.DAYS.convert(elapsed, TimeUnit.MILLISECONDS) >= 1) {
			s.remove(0);
			s.remove(0);
			s.add((c.get(Calendar.DAY_OF_YEAR) - 1) + " days");
		}
		Collections.reverse(s);
		return StringUtils.join(s, ", ");
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp(
				"info about user activity",
				"nick", "<where_pattern> <nick_pattern>: when a nick was last active",
				"user", "<where_pattern> <name_pattern>: when a bot user was last active"
				);
	}

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 2)
			return invokeHelp(bot, cause, args);
		String cmd = args.get(1);
		if("nick".equals(cmd))
			return _nick(bot, cause, args);
		else if("user".equals(cmd))
			return _user(bot, cause, args);
		else
			return invokeHelp(bot, cause, args);
		
	}

	protected String _nick(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4)
			return invokeHelp(bot, cause, args, "nick");
		Pattern where;
		Pattern nick;
		try {
			where = Pattern.compile(args.get(2), Pattern.CASE_INSENSITIVE);
		} catch(Exception e) {
			return "invalid where_pattern: " + e.getMessage();
		}
		try {
			nick = Pattern.compile(args.get(3), Pattern.CASE_INSENSITIVE);
		} catch(Exception e) {
			return "invalid nick_pattern: " + e.getMessage();
		}
		Entry<String, String> doing = getNickSeen(where, nick);
		if(doing == null)
			return "nick not seen";
		String dwhere = parseDoingWhere(doing);
		String dname = parseDoingName(doing);
		String dmessage = parseDoingMessage(doing);
		long delapsed = System.currentTimeMillis() - parseDoingDate(doing).getTime();
		
		return dname + " was last seen in " + dwhere + ", " + formatElapsed(delapsed) + " ago, " + dmessage;
	}
	
	protected String _user(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() != 4)
			return invokeHelp(bot, cause, args, "user");
		Pattern where;
		Pattern nick;
		try {
			where = Pattern.compile(args.get(2));
		} catch(Exception e) {
			return "invalid where_pattern: " + e.getMessage();
		}
		try {
			nick = Pattern.compile(args.get(3));
		} catch(Exception e) {
			return "invalid name_pattern: " + e.getMessage();
		}
		Entry<String, String> doing = getUserSeen(where, nick);
		if(doing == null)
			return "user not seen";
		String dwhere = parseDoingWhere(doing);
		String dname = parseDoingName(doing);
		String dmessage = parseDoingMessage(doing);
		long delapsed = System.currentTimeMillis() - parseDoingDate(doing).getTime();
		
		return dname + " was last seen in " + dwhere + ", " + formatElapsed(delapsed) + " ago, " + dmessage;
	}
	
}
