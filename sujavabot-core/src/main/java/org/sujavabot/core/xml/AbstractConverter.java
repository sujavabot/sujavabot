package org.sujavabot.core.xml;

import java.util.Objects;

import org.sujavabot.core.util.Throwables;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public abstract class AbstractConverter<T> implements Converter {
	
	protected Class<? extends T> type;
	protected XStream x;
	
	public AbstractConverter(XStream x, Class<? extends T> type) {
		this.x = Objects.requireNonNull(x);
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return this.type.equals(type);
	}

	protected T createDefaults() {
		return type.cast(x.getReflectionProvider().newInstance(type));
	}
	
	protected T createCurrent() {
		return type.cast(x.getReflectionProvider().newInstance(type));
	}
	
	protected abstract void configure(T current, MarshalHelper helper, T defaults);
	
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		try {
			MarshalHelper helper = new MarshalHelper(x, writer, context);
			T current = type.cast(source);
			T defaults = createDefaults();

			configure(current, helper, defaults);

			helper.write();
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}

	protected abstract void configure(T current, UnmarshalHelper helper);
	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			UnmarshalHelper helper = new UnmarshalHelper(x, reader, context);
			T current = createCurrent();
			
			configure(current, helper);
			
			return helper.read(current);
		} catch(Exception e) {
			throw Throwables.as(ConversionException.class, e);
		}
	}

}
