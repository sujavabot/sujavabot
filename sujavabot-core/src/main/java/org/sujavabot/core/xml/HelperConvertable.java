package org.sujavabot.core.xml;

import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

public interface HelperConvertable<T> {
	public default T createDefaults() { return null; }
	public void configure(MarshalHelper helper, T defaults);
	public void configure(UnmarshalHelper helper);
}
