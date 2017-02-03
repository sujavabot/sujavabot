package org.sujavabot.core.xml;

import java.util.Map.Entry;

import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.ConfigurationBuilder;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;

public class AuthorizedGroupConverter extends AbstractConverter<AuthorizedGroup> {

	public AuthorizedGroupConverter(XStream x) {
		super(x, AuthorizedGroup.class);
	}

	@Override
	protected AuthorizedGroup createCurrent(Class<? extends AuthorizedGroup> required) {
		return new AuthorizedGroup();
	}
	
	@Override
	protected void configure(AuthorizedGroup current, MarshalHelper helper, AuthorizedGroup defaults) {
		helper.field("name", String.class, () -> current.getName());
		for(AuthorizedGroup subgroup : current.getParents())
			helper.field("parent", String.class, () -> subgroup.getName());
		helper.field("commands2", CommandsMap2.class, () -> new CommandsMap2(current.getCommands().getCommands()));
		for(Entry<String, String> e : current.getProperties().entrySet())
			helper.field("property", String.class, () -> (e.getKey() + "=" + e.getValue()));
	}

	@Override
	protected void configure(AuthorizedGroup current, UnmarshalHelper helper) {
		helper.field("name", String.class, s -> current.setName(s));
		helper.field("parent", String.class, s -> {
			ConfigurationBuilder builder = (ConfigurationBuilder) helper.getContext().get(ConfigurationBuilder.class);
			if(builder.getGroups().containsKey(s))
				current.getParents().add(builder.getGroups().get(s));
		});
		helper.field("commands", CommandsMap.class, m -> current.getCommands().getCommands().putAll(m));
		helper.field("commands2", CommandsMap2.class, m -> current.getCommands().getCommands().putAll(m));
		helper.field("property", String.class, (s) -> {
			String[] f = s.split("=", 2);
			current.getProperties().put(f[0], f[1]);
		});
	}

}
