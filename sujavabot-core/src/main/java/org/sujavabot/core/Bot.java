package org.sujavabot.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pircbotx.PircBotX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
