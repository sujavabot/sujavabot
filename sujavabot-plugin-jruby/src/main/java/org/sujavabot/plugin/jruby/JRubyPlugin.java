package org.sujavabot.plugin.jruby;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.script.ScriptEngine;
import org.jruby.embed.jsr223.JRubyEngineFactory;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.Plugin;

public class JRubyPlugin implements Plugin {
	
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
		ScriptEngine engine = new JRubyEngineFactory().getScriptEngine();
		if(file == null) {
			plugin = (Plugin) engine.eval(source);
		} else {
			Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
			try {
				plugin = (Plugin) engine.eval(r);
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
