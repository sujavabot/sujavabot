package org.sujavabot.core.xml;

import java.util.ArrayList;
import java.util.List;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

public class ClassLoadersMapper extends MapperWrapper {

	protected List<Mapper> loaderMappers = new ArrayList<>();
	
	public ClassLoadersMapper(Mapper wrapped, List<ClassLoader> loaders) {
		super(wrapped);
		for(ClassLoader loader : loaders) {
			loaderMappers.add(new DefaultMapper(new ClassLoaderReference(loader)));
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class realClass(String elementName) {
		try {
			return super.realClass(elementName);
		} catch(CannotResolveClassException e) {
			for(Mapper mapper : loaderMappers) {
				try {
					return mapper.realClass(elementName);
				} catch(CannotResolveClassException e2) {
				}
			}
			throw e;
		}
	}
}
