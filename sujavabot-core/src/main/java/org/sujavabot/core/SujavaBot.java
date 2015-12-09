package org.sujavabot.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sujavabot.core.commands.CommandHandler;
import org.sujavabot.core.commands.DefaultCommandHandler;
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
		
	}

	public Map<File, Plugin> getPlugins() {
		return plugins;
	}
	
	public CommandHandler getCommands() {
		return commands;
	}
	
	public CommandHandler getRootCommands() {
		return getAuthorizedGroups().get("@root").getCommands();
	}

	public Map<String, AuthorizedUser> getAuthorizedUsers() {
		return authorizedUsers;
	}
	
	public Map<String, AuthorizedGroup> getAuthorizedGroups() {
		return authorizedGroups;
	}
	
	public AuthorizedUser getAuthorizedUser(User user) {
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
	
	public boolean isVerified(User user) {
		if(verified.contains(user))
			return true;
		if(user.isVerified()) {
			verified.add(user);
			return true;
		}
		try {
			sendRaw().rawLine("WHOIS " + user.getNick() + " " + user.getNick());
			WaitForQueue waitForQueue = new WaitForQueue(this);
			while (true) {
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
		} catch (InterruptedException ex) {
			throw new RuntimeException("Couldn't finish querying user for verified status", ex);
		}
	}

	public Set<User> getVerified() {
		return verified;
	}
	
	@Override
	public Configuration getConfiguration() {
		return (Configuration) super.getConfiguration();
	}
	
	public Map<Channel, Map<User, String>> getOutputBuffers() {
		return outputBuffers;
	}
	
	public String buffer(Channel channel, User user, String result) {
		if(!outputBuffers.containsKey(channel))
			outputBuffers.put(channel, Collections.synchronizedMap(new WeakHashMap<>()));
		Map<User, String> channelBuffer = outputBuffers.get(channel);
		channelBuffer.remove(user);
		result = user.getNick() + ": " + result;
		if(result.length() > getConfiguration().getMaxLineLength()) {
			int stop = getConfiguration().getMaxLineLength() - " (!more)".length();
			channelBuffer.put(user, result.substring(stop));
			return result.substring(0, stop) + " (!more)";
		}
		return result;
	}
	
	public String continueBuffer(Channel channel, User user) {
		if(!outputBuffers.containsKey(channel))
			outputBuffers.put(channel, Collections.synchronizedMap(new WeakHashMap<>()));
		Map<User, String> channelBuffer = outputBuffers.get(channel);
		String result = channelBuffer.remove(user);
		if(result == null)
			return null;
		result = user.getNick() + ": " + result;
		if(result.length() > getConfiguration().getMaxLineLength()) {
			int stop = getConfiguration().getMaxLineLength() - " (!more)".length();
			channelBuffer.put(user, result.substring(stop));
			return result.substring(0, stop) + " (!more)";
		}
		return result;
	}
	
	public void saveConfiguration() {
		saveConfiguration(null);
	}
	
	public File saveConfiguration(File configFile) {
		if(configFile == null)
			configFile = getConfiguration().getConfigFile();
		if(configFile == null) {
			ConfigurationBuilder builder = getConfiguration().createBuilder();
			XStream x = XStreams.configure(new XStream());
			x.toXML(builder, System.out);
			System.out.println();
			return null;
		}
		try {
			File configFileTmp = new File(configFile.getParent(), configFile.getName() + ".tmp");
			File configFileOld = new File(configFile.getParent(), configFile.getName() + ".old");
			for(int i = 2; configFileOld.exists(); i++)
				configFileOld = new File(configFile.getParent(), configFile.getName() + ".old." + i);
			OutputStream out = new FileOutputStream(configFileTmp);
			try {
				ConfigurationBuilder builder = getConfiguration().createBuilder();
				XStream x = XStreams.configure(new XStream());
				x.toXML(builder, out);
			} finally {
				out.close();
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
	
	public static class UnverifyListener extends ListenerAdapter<PircBotX> {
		protected Set<User> verified(Event<?> event) {
			return ((SujavaBot) event.getBot()).getVerified();
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
