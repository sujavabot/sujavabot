package org.sujavabot.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.WeakHashMap;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sujavabot.core.Scheduler.ScheduledCommand;
import org.sujavabot.core.commands.CommandHandler;
import org.sujavabot.core.commands.DefaultCommandHandler;
import org.sujavabot.core.util.Messages;
import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;

public class SujavaBot extends PircBotX {
	private static final Logger LOG = LoggerFactory.getLogger(SujavaBot.class);

	protected Map<File, Plugin> plugins = new LinkedHashMap<>();
	
	protected Map<String, AuthorizedGroup> authorizedGroups = new LinkedHashMap<>();
	protected Map<String, AuthorizedUser> authorizedUsers = new LinkedHashMap<>();
	
	protected CommandHandler commands;
	
	protected Set<User> verified = Collections.synchronizedSet(new HashSet<>());
	
	protected Map<Channel, Map<User, String>> outputBuffers = Collections.synchronizedMap(new WeakHashMap<>());

	public SujavaBot(Configuration configuration) {
		super(configuration);
		for(File pluginConfig : configuration.getPluginConfigs()) {
			plugins.put(pluginConfig, null);
		}
		commands = new DefaultCommandHandler(this);
		authorizedGroups.putAll(configuration.getGroups());
		authorizedUsers.putAll(configuration.getUsers());
		configuration.getListenerManager().addListener(new ListenerAdapter<PircBotX>() {
			@Override
			public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
				configuration.getListenerManager().removeListener(this);
				schedule();
			}
		});
	}

	public Map<File, Plugin> getPlugins() {
		return plugins;
	}
	
	public CommandHandler getCommands() {
		return commands;
	}
	
	public AuthorizedGroup getRootGroup() {
		return getAuthorizedGroups().get("@root");
	}
	
	public CommandHandler getRootCommands() {
		return getRootGroup().getCommands();
	}

	public Map<String, AuthorizedUser> getAuthorizedUsers() {
		return authorizedUsers;
	}
	
	public Map<String, AuthorizedGroup> getAuthorizedGroups() {
		return authorizedGroups;
	}
	
	public AuthorizedUser getAuthorizedUser(User user) {
		synchronized(verified) {
			if(user == null)
				return null;
			for(AuthorizedUser u : authorizedUsers.values()) {
				if(u.getNick().matcher(user.getNick()).matches()) {
					if(!isVerified(user)) {
						LOG.info("nick {} not verified", user.getNick());
						return getAuthorizedUsers().get("@nobody");
					}
					return u;
				}
			}
			return getAuthorizedUsers().get("@nobody");
		}
	}
	
	public boolean isVerified(User user) {
		synchronized(verified) {
			if(verified.contains(user))
				return true;
			if(user.isVerified()) {
				verified.add(user);
				return true;
			}
			try {
				WaitForQueue waitForQueue = new WaitForQueue(this);
				sendRaw().rawLine("WHOIS " + user.getNick() + " " + user.getNick());
				long timeout = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
				while (System.currentTimeMillis() < timeout) {
					ServerResponseEvent<?> event = waitForQueue.waitFor(ServerResponseEvent.class);
					if (!event.getParsedResponse().get(1).equals(user.getNick()))
						continue;
	
					if(event.getCode() == 318 || event.getCode() == 307) {
						waitForQueue.close();
						if(event.getCode() == 307)
							verified.add(user);
						return event.getCode() == 307;
					}
				}
				waitForQueue.close();
				return false;
			} catch (InterruptedException ex) {
				throw new RuntimeException("Couldn't finish querying user for verified status", ex);
			}
		}
	}

	public Set<User> getVerified() {
		return verified;
	}
	
	@Override
	public Configuration getConfiguration() {
		Configuration c = (Configuration) super.getConfiguration();
		return c;
	}
	
	public Configuration updateConfiguration() {
		Configuration c = (Configuration) super.getConfiguration();

		c.getUsers().clear();
		c.getUsers().putAll(authorizedUsers);
		c.getGroups().clear();
		c.getGroups().putAll(authorizedGroups);

		return c;
	}
	
	public Map<Channel, Map<User, String>> getOutputBuffers() {
		return outputBuffers;
	}
	
	public synchronized String buffer(Channel channel, User user, String prefix, String result) {
		if(!outputBuffers.containsKey(channel))
			outputBuffers.put(channel, Collections.synchronizedMap(new WeakHashMap<>()));
		Map<User, String> channelBuffer = outputBuffers.get(channel);
		int maxlen = Messages.maxlenPM(this, channel.getName());
		String[] sb = Messages.split(maxlen, channel.getName(), prefix + result);
		if(sb[1] != null) {
			sb = Messages.split(maxlen - " (more)".length(), channel.getName(), prefix + result);
			sb[0] += " (more)";
			channelBuffer.put(user, sb[1]);
		} else
			channelBuffer.put(user, null);
		return sb[0];
	}
	
	public synchronized String continueBuffer(Channel channel, User user, String prefix) {
		if(!outputBuffers.containsKey(channel))
			outputBuffers.put(channel, Collections.synchronizedMap(new WeakHashMap<>()));
		Map<User, String> channelBuffer = outputBuffers.get(channel);
		String pb = channelBuffer.get(user);
		if(pb == null) {
			return null;
		}
		int maxlen = Messages.maxlenPM(this, channel.getName());
		String[] sb = Messages.split(maxlen, channel.getName(), prefix + pb);
		if(sb[1] != null) {
			sb = Messages.split(maxlen - " (more)".length(), channel.getName(), prefix + pb);
			sb[0] += " (more)";
			channelBuffer.put(user, sb[1]);
		} else
			channelBuffer.put(user, null);
		return sb[0];
	}
	
	public void saveConfiguration() {
		saveConfiguration(null);
	}
	
	public File saveConfiguration(File configFile) {
		if(configFile == null)
			configFile = getConfiguration().getConfigFile();
		if(configFile == null) {
			ConfigurationBuilder builder = updateConfiguration().createBuilder();
			XStream x = XStreams.configure(new XStream());
			x.toXML(builder, System.out);
			System.out.println();
			return null;
		}
		try {
			File configFileTmp = new File(configFile.getParentFile(), configFile.getName() + ".tmp");

			OutputStream out = new FileOutputStream(configFileTmp);
			try {
				ConfigurationBuilder builder = updateConfiguration().createBuilder();
				XStream x = XStreams.configure(new XStream());
				x.toXML(builder, out);
			} finally {
				out.close();
			}
			
			File configFileOld = new File(configFile.getParentFile(), configFile.getName() + ".old");
			List<File> backups = new ArrayList<>();
			backups.add(configFileOld);
			if(configFileOld.exists()) {
				File f = configFileOld;
				for(int i = 1; f.exists(); i++)
					backups.add(f = new File(configFile.getParentFile(), configFile.getName() + ".old." + i));
				ListIterator<File> bi = backups.listIterator(backups.size());
				File next = bi.previous();
				while(bi.hasPrevious()) {
					File prev = bi.previous();
					Files.move(prev.toPath(), next.toPath(), StandardCopyOption.REPLACE_EXISTING);
					next = prev;
				}
			}
			Files.move(configFile.toPath(), configFileOld.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.move(configFileTmp.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch(IOException e) {
			throw Throwables.as(RuntimeException.class, e);
		}
		return configFile;
	}

	public void configurePlugins() {
		Iterator<Entry<File, Plugin>> pi = plugins.entrySet().iterator();

		while(pi.hasNext()) {
			Entry<File, Plugin> e = pi.next();
			Plugin plugin = null;
			try {
				plugin = (Plugin) XStreams.configure(new XStream()).fromXML(e.getKey());
				e.setValue(plugin);
			} catch(Exception ex) {
				LOG.error("Unable to configure plugin {} ({}), removing from plugins list. {}", e.getKey(), plugin, ex);
				pi.remove();
			}
		}
	}

	public void initializeBot() {
		Iterator<Plugin> pi = plugins.values().iterator();

		while(pi.hasNext()) {
			Plugin p = pi.next();
			try {
				p.load(this);
			} catch(Exception e) {
				LOG.error("Unable to load plugin {} ({}), removing from plugins list. {}", p.getName(), p, e);
				pi.remove();
			}
		}

		// initialize all the commands
		Set<Command> commands = new HashSet<>();
		for(AuthorizedGroup group : authorizedGroups.values())
			commands.addAll(group.getAllCommands().values());
		for(AuthorizedUser user : authorizedUsers.values())
			commands.addAll(user.getAllCommands().values());
		for(Command c : commands)
			c.init(this);
	}
	
	
	
	public void schedule() {
		for(ScheduledCommand sc : ((Configuration) configuration).getSchedule()) {
			AuthorizedUser user = getAuthorizedUsers().get(sc.user);
			Authorization auth = new Authorization(this, user, user.getAllGroups(), user.getOwnedGroups());
			auth.run(() -> Scheduler.get().add(this, sc.target, sc.name, sc.alias, sc.delay));
		}
	}
	
	public static class UnverifyListener extends ListenerAdapter<PircBotX> {
		protected Set<User> verified(Event<?> event) {
			return ((SujavaBot) event.getBot()).getVerified();
		}
		
		@Override
		public void onJoin(JoinEvent<PircBotX> event) throws Exception {
			((SujavaBot) event.getBot()).getAuthorizedUser(event.getUser());
		}
		
		@Override
		public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
			verified(event).clear();
		}

		@Override
		public void onNickChange(NickChangeEvent<PircBotX> event) throws Exception {
			verified(event).remove(event.getUser());
		}

		@Override
		public void onPart(PartEvent<PircBotX> event) throws Exception {
			if(event.getUser().getNick().equals(event.getBot().getNick()))
				verified(event).clear();
			else
				verified(event).remove(event.getUser());
		}

		@Override
		public void onQuit(QuitEvent<PircBotX> event) throws Exception {
			if(event.getUser().getNick().equals(event.getBot().getNick()))
				verified(event).clear();
			else
				verified(event).remove(event.getUser());
		}
		
	}
}
