package org.sujavabot.core.xml;

import org.junit.Test;
import org.sujavabot.core.ConfigurationBuilder;

import com.thoughtworks.xstream.XStream;

public class ConfigurationBuilderConverterTest {
	@Test
	public void testToXML() {
		XStream x = XStreams.configure(new XStream());
		System.out.println(x.toXML(new ConfigurationBuilder()));
	}
}
