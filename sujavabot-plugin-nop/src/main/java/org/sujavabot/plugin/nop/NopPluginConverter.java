package org.sujavabot.plugin.nop;

import org.sujavabot.core.xml.AbstractConverter;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;

public class NopPluginConverter extends AbstractConverter<NopPlugin> {

	public NopPluginConverter(XStream x) {
		super(x, NopPlugin.class);
	}

	@Override
	protected void configure(NopPlugin current, MarshalHelper helper, NopPlugin defaults) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void configure(NopPlugin current, UnmarshalHelper helper) {
		// TODO Auto-generated method stub
		
	}
	
}
