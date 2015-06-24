package org.sujavabot.core.xml;

import java.util.ServiceLoader;

import com.thoughtworks.xstream.XStream;

public abstract class XStreams {
	
	public static abstract class SPI {
		public abstract void configure(XStream x);
	}

	public static XStream configure(XStream x) {
		for(SPI spi : ServiceLoader.load(SPI.class, XStreams.class.getClassLoader())) {
			spi.configure(x);
		}
		return x;
	}
	
	private XStreams() {}
}
