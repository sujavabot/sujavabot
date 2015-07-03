package org.sujavabot.core.xml;

import java.util.TreeMap;

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
	protected void configure(AuthorizedGroup current, MarshalHelper helper, AuthorizedGroup defaults) {
		helper.field("name", String.class, () -> current.getName());
		for(AuthorizedGroup subgroup : current.getSubgroups())
			helper.field("subgroup", String.class, () -> subgroup.getName());
		helper.field("commands", CommandsMap.class, () -> new CommandsMap(current.getCommands().getCommands()));
	}

	@Override
	protected void configure(AuthorizedGroup current, UnmarshalHelper helper) {
		helper.field("name", String.class, s -> current.setName(s));
		helper.field("subgroup", String.class, s -> {
			ConfigurationBuilder builder = (ConfigurationBuilder) helper.getContext().get(ConfigurationBuilder.class);
			for(AuthorizedGroup g : builder.getGroups())
				if(s.equals(g.getName()))
					current.getSubgroups().add(g);
		});
		helper.field("commands", CommandsMap.class, m -> current.getCommands().getCommands().putAll(m));
	}

}
