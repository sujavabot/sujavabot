package org.sujavabot.core.xml;

import java.util.Map.Entry;
import java.util.regex.Pattern;

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
	protected AuthorizedUser createCurrent(Class<? extends AuthorizedUser> required) {
		return new AuthorizedUser();
	}

	@Override
	protected void configure(AuthorizedUser current, MarshalHelper helper, AuthorizedUser defaults) {
		helper.field("name", String.class, () -> current.getName());
		helper.field("nick", String.class, () -> current.getNick().pattern());
		for(AuthorizedGroup group : current.getGroups())
			helper.field("group", String.class, () -> group.getName());
		for(AuthorizedGroup owned : current.getOwnedGroups())
			helper.field("owns-group", String.class, () -> owned.getName());
		helper.field("commands2", CommandsMap2.class, () -> new CommandsMap2(current.getCommands().getCommands()));
		for(Entry<String, String> e : current.getProperties().entrySet())
			helper.field("property", String.class, () -> (e.getKey() + "=" + e.getValue()));
	}

	@Override
	protected void configure(AuthorizedUser current, UnmarshalHelper helper) {
		helper.field("name", String.class, s -> current.setName(s));
		helper.field("nick", String.class, s -> current.setNick(Pattern.compile(s)));
		helper.field("group", String.class, s -> {
			ConfigurationBuilder builder = (ConfigurationBuilder) helper.getContext().get(ConfigurationBuilder.class);
			if(builder.getGroups().containsKey(s))
				current.getGroups().add(builder.getGroups().get(s));
		});
		helper.field("owns-group", String.class, s -> {
			ConfigurationBuilder builder = (ConfigurationBuilder) helper.getContext().get(ConfigurationBuilder.class);
			if(builder.getGroups().containsKey(s))
				current.getOwnedGroups().add(builder.getGroups().get(s));
		});
		helper.field("commands", CommandsMap.class, m -> current.getCommands().getCommands().putAll(m));
		helper.field("commands2", CommandsMap2.class, m -> current.getCommands().getCommands().putAll(m));
		helper.field("property", String.class, (s) -> {
			String[] f = s.split("=", 2);
			current.getProperties().put(f[0], f[1]);
		});
	}

}
