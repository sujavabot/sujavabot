package org.sujavabot.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pircbotx.PircBotX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.ConfigurationBuilderConverter;

import com.thoughtworks.xstream.XStream;

public class Bot extends PircBotX {
	private static final Logger LOG = LoggerFactory.getLogger(Bot.class);

	protected List<Plugin> plugins = new ArrayList<>();

	public Bot(Configuration configuration) {
		super(configuration);
		plugins.addAll(configuration.getPlugins());
	}

	public List<Plugin> getPlugins() {
		return plugins;
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
				XStream x = ConfigurationBuilderConverter.createXStream();
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
		Iterator<Plugin> pi = plugins.iterator();

		while(pi.hasNext()) {
			Plugin p = pi.next();
			try {
				p.initializePlugin();
			} catch(Exception e) {
				LOG.error("Unable to initialize plugin {} ({}), removing from plugins list. {}", p.getName(), p, e);
				pi.remove();
			}
		}
	}

	public void initializeBot() {
		Iterator<Plugin> pi = plugins.iterator();

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
