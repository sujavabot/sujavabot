package org.sujavabot.plugin.jruby;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import javax.script.ScriptEngine;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.jsr223.JRubyEngineFactory;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.Plugin;

public class JRubyPlugin implements Plugin {
	public static final ScriptingContainer container = new ScriptingContainer(LocalContextScope.CONCURRENT);
	
	protected String source;
	protected File file;
	
	protected Plugin plugin;
	
	public JRubyPlugin(String source) {
		this.source = source;
	}
	
	public JRubyPlugin(File file) {
		this.file = file;
	}
	
	@Override
	public String getName() {
		if(plugin != null)
			return plugin.getName();
		if(file != null)
			return "jruby:" + file.getPath();
		return "jruby:<inline>";
	}

	public String getSource() {
		return source;
	}
	
	public File getFile() {
		return file;
	}

	@Override
	public void load(SujavaBot bot) throws Exception {
		ScriptingContainer container = JRubyPlugin.container;
		container.put("bot", bot);

		if(file == null) {
			plugin = (Plugin) container.runScriptlet(source);
		} else {
			Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
			try {
				plugin = (Plugin) container.runScriptlet(r, file.getName());
			} catch(Exception e) {
				e.printStackTrace();
				throw e;
			} finally {
				r.close();
			}
		}
		plugin.load(bot);
	}
	
	@Override
	public void unload(SujavaBot bot) throws Exception {
		plugin.unload(bot);
	}
	
}
