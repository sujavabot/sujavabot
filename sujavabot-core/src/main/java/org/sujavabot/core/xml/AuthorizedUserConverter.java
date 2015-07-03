package org.sujavabot.core.xml;

import java.util.TreeMap;

import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.ConfigurationBuilder;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;

public class AuthorizedUserConverter extends AbstractConverter<AuthorizedUser> {

	public AuthorizedUserConverter(XStream x) {
		super(x, AuthorizedUser.class);
	}
	
	@Override
	protected AuthorizedUser createCurrent() {
		return new AuthorizedUser();
	}

	@Override
	protected void configure(AuthorizedUser current, MarshalHelper helper, AuthorizedUser defaults) {
		helper.field("name", String.class, () -> current.getName());
		for(AuthorizedGroup group : current.getGroups())
			helper.field("group", String.class, () -> group.getName());
		helper.field("commands", CommandsMap.class, () -> new CommandsMap(current.getCommands().getCommands()));
	}

	@Override
	protected void configure(AuthorizedUser current, UnmarshalHelper helper) {
		helper.field("name", String.class, s -> current.setName(s));
		helper.field("group", String.class, s -> {
			ConfigurationBuilder builder = (ConfigurationBuilder) helper.getContext().get(ConfigurationBuilder.class);
			for(AuthorizedGroup g : builder.getGroups())
				if(s.equals(g.getName()))
					current.getGroups().add(g);
		});
		helper.field("commands", CommandsMap.class, m -> current.getCommands().getCommands().putAll(m));
	}

}
