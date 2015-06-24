package org.sujavabot.plugin.jruby;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.jruby.embed.jsr223.JRubyEngine;
import org.jruby.embed.jsr223.JRubyEngineFactory;
import org.jruby.embed.jsr223.JRubyScriptEngineManager;
import org.sujavabot.Bot;
import org.sujavabot.core.Plugin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class JRubyPlugin implements Plugin {
	
	protected String source;
	protected File file;
	
	protected org.sujavabot.Plugin plugin;
	
	public JRubyPlugin(String source) {
		this.source = source;
	}
	
	public JRubyPlugin(File file) {
		this.file = file;
	}
	
	@Override
	public String getName() {
		if(file != null)
			return "jruby:" + file.getPath();
		return "jruby:<inline>";
	}

	@Override
	public Converter getConfigurableConverter(XStream x) {
		return new JRubyPluginConverter();
	}

	public String getSource() {
		return source;
	}
	
	public File getFile() {
		return file;
	}

	@Override
	public void initializePlugin() throws Exception {
		ScriptEngine engine = new JRubyEngineFactory().getScriptEngine();
		if(file == null) {
			plugin = (org.sujavabot.Plugin) engine.eval(source);
		} else {
			Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
			try {
				plugin = (org.sujavabot.Plugin) engine.eval(r);
			} finally {
				r.close();
			}
		}
		plugin.initializePlugin();
	}

	@Override
	public void initializeBot(Bot bot) throws Exception {
		plugin.initializeBot(bot);
	}
}
