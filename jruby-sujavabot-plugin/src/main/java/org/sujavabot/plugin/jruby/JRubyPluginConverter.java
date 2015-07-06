package org.sujavabot.plugin.jruby;

import java.io.File;

import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class JRubyPluginConverter implements Converter {

	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new JRubyPluginConverter());
			x.alias("jruby-plugin", JRubyPlugin.class);
		}
	}
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return JRubyPlugin.class.equals(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		JRubyPlugin plugin = (JRubyPlugin) source;
		if(plugin.getFile() != null)
			writer.addAttribute("file", plugin.getFile().getPath());
		else
			writer.setValue(plugin.getSource());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		if(reader.getAttribute("file") != null)
			return new JRubyPlugin(new File(reader.getAttribute("file")));
		else
			return new JRubyPlugin(reader.getValue());
	}

}
