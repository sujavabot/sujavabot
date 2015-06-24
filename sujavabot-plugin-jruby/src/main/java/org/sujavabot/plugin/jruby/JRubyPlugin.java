package org.sujavabot.plugin.jruby;

import java.io.File;

import org.sujavabot.core.Plugin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class JRubyPlugin implements Plugin {
	
	protected String source;
	protected File file;
	
	public JRubyPlugin(String source) {
		this.source = source;
	}
	
	public JRubyPlugin(File file) {
		this.file = file;
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
}
