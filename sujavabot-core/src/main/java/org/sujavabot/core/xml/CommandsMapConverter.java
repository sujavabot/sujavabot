package org.sujavabot.core.xml;

import java.util.Map.Entry;

import org.sujavabot.core.Command;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;

public class CommandsMapConverter extends AbstractConverter<CommandsMap> {

	public CommandsMapConverter(XStream x) {
		super(x, CommandsMap.class);
	}

	@Override
	protected CommandsMap createCurrent(Class<? extends CommandsMap> required) {
		return new CommandsMap();
	}
	
	@Override
	protected void configure(CommandsMap current, MarshalHelper helper, CommandsMap defaults) {
		for(Entry<String, Command> e : current.entrySet()) {
			helper.field(e.getKey(), () -> e.getValue());
		}
	}

	@Override
	protected void configure(CommandsMap current, UnmarshalHelper helper) {
		helper.setDefaultHandler((c, h) -> {
			String name = h.getReader().getNodeName();
			Command cmd;
			if("null".equals(h.getReader().getAttribute("class")))
				cmd = null;
			else {
				@SuppressWarnings("unchecked")
				Class<? extends Command> type = x.getMapper().realClass(h.getReader().getAttribute("class"));
				cmd = type.cast(h.getContext().convertAnother(current, type));
			}
			((CommandsMap) c).put(name, cmd);
		});
	}
	
	

}
