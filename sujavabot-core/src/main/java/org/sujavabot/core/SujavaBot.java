package org.sujavabot.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
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
	
	protected List<AuthorizedUser> authorizedUsers = new ArrayList<>();
	protected List<AuthorizedGroup> authorizedGroups = new ArrayList<>();
	
	protected CommandHandler commands;

	public SujavaBot(Configuration configuration) {
		super(configuration);
		for(File pluginConfig : configuration.getPluginConfigs()) {
			plugins.put(pluginConfig, null);
		}
		commands = new DefaultCommandHandler(this);
	}

	public Map<File, Plugin> getPlugins() {
		return plugins;
	}
	
	public CommandHandler getCommands() {
		return commands;
	}
	
	public CommandHandler getRootCommands() {
		return getAuthorizedGroup("root").getCommands();
	}

	public List<AuthorizedUser> getAuthorizedUsers() {
		return authorizedUsers;
	}
	
	public List<AuthorizedGroup> getAuthorizedGroups() {
		return authorizedGroups;
	}
	
	public AuthorizedGroup getAuthorizedGroup(String name) {
		for(AuthorizedGroup g : authorizedGroups)
			if(name.equals(g.getName()))
				return g;
		return null;
	}
	
	public AuthorizedUser getAuthorizedUser(User user) {
		for(AuthorizedUser u : authorizedUsers) {
			if(u.getNick().matcher(user.getNick()).matches()) {
				if(!user.isVerified())
					return null;
				return u;
			}
		}
		return null;
	}
	
	@Override
	public Configuration getConfiguration() {
		return (Configuration) super.getConfiguration();
	}
	
	public void saveConfiguration() {
		try {
			File configFile = getConfiguration().getConfigFile();
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
	}

	public void initializePlugins() {
		Iterator<Entry<File, Plugin>> pi = plugins.entrySet().iterator();

		while(pi.hasNext()) {
			Entry<File, Plugin> e = pi.next();
			Plugin plugin = null;
			try {
				plugin = (Plugin) XStreams.configure(new XStream()).fromXML(e.getKey());
				e.setValue(plugin);
				plugin.initializePlugin();
			} catch(Exception ex) {
				LOG.error("Unable to initialize plugin {} ({}), removing from plugins list. {}", e.getKey(), plugin, ex);
				pi.remove();
			}
		}
	}

	public void initializeBot() {
		Iterator<Plugin> pi = plugins.values().iterator();

		while(pi.hasNext()) {
			Plugin p = pi.next();
			try {
				p.initializeBot(this);
			} catch(Exception e) {
				LOG.error("Unable to initialize plugin {} ({}), removing from plugins list. {}", p.getName(), p, e);
				pi.remove();
			}
		}
	}
}
