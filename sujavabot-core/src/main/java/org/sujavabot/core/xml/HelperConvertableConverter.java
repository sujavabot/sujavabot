package org.sujavabot.core.xml;

import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;

@SuppressWarnings("rawtypes")
public class HelperConvertableConverter extends AbstractConverter<HelperConvertable> {
	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new HelperConvertableConverter(x));
		}
	}

	public HelperConvertableConverter(XStream x) {
		super(x, HelperConvertable.class);
	}
	
	@Override
	public boolean canConvert(Class type) {
		return this.type.isAssignableFrom(type);
	}

	@Override
	protected HelperConvertable createDefaults(HelperConvertable current) {
		return type.cast(x.getReflectionProvider().newInstance(current.getClass()));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void configure(HelperConvertable current, MarshalHelper helper, HelperConvertable defaults) {
		current.configure(helper, defaults);
	}

	@Override
	protected void configure(HelperConvertable current, UnmarshalHelper helper) {
		current.configure(helper);
	}

}
