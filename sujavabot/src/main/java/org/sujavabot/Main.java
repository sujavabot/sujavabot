package org.sujavabot;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.sujavabot.core.Configuration;
import org.sujavabot.core.ConfigurationBuilder;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;

public class Main {
	private static final Options OPT = new Options();
	static {
		OPT.addOption("c", "config-file", true, "config file");
		OPT.addOption(null, "config-resource", true, "config classpath resource");
	}
	
	public static void main(String[] args) throws Throwable {
		CommandLine cli = new DefaultParser().parse(OPT, args);
		if(!cli.hasOption("config-file") && !cli.hasOption("config-resource"))
			throw new IllegalArgumentException("Must supply argument --config-file");
		File configFile = null;
		InputStream cin;
		if(cli.hasOption("config-file"))
			cin = new FileInputStream(configFile = new File(cli.getOptionValue("config-file")));
		else
			cin = Main.class.getClassLoader().getResourceAsStream(cli.getOptionValue("config-resource"));
		XStream x = XStreams.configure(new XStream());
		ConfigurationBuilder builder = (ConfigurationBuilder) x.fromXML(cin);
		Configuration config = builder.buildConfiguration(configFile);
		SujavaBot bot = new SujavaBot(config);
		bot.initializePlugins();
		bot.initializeBot();
		bot.startBot();
	}
}
