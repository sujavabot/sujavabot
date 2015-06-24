package org.sujavabot.core.xml;

import org.junit.Test;
import org.sujavabot.core.ConfigurationBuilder;

import com.thoughtworks.xstream.XStream;

public class ConfigurationBuilderConverterTest {
	@Test
	public void testToXML() {
		XStream x = new XStream();
		x.registerConverter(new ConfigurationBuilderConverter(x));
		System.out.println(x.toXML(new ConfigurationBuilder()));
	}
}
