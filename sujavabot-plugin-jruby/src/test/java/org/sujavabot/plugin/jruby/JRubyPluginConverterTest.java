package org.sujavabot.plugin.jruby;

import org.junit.Test;
import org.sujavabot.core.Plugin;
import org.sujavabot.core.xml.XStreams;

import com.thoughtworks.xstream.XStream;

public class JRubyPluginConverterTest {
	@Test
	public void testInlinePlugin() throws Exception {
		Plugin plugin = (Plugin) XStreams.configure(new XStream()).fromXML(JRubyPluginConverterTest.class.getResource("jruby-plugin-inline.xml"));
		System.out.println(plugin);
		System.out.println(plugin.getName());
		plugin.load(null);
		System.out.println(plugin.getName());
	}

	@Test
	public void testFilePlugin() throws Exception {
		Plugin plugin = (Plugin) XStreams.configure(new XStream()).fromXML(JRubyPluginConverterTest.class.getResource("jruby-plugin-file.xml"));
		System.out.println(plugin);
		System.out.println(plugin.getName());
		plugin.load(null);
		System.out.println(plugin.getName());
	}
}
