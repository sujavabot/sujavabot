package org.sujavabot.core.xml;

import java.util.Map.Entry;

import org.sujavabot.core.Command;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CommandsMap2Converter extends AbstractConverter<CommandsMap2> {

	public CommandsMap2Converter(XStream x) {
		super(x, CommandsMap2.class);
	}

	@Override
	protected CommandsMap2 createCurrent(Class<? extends CommandsMap2> required) {
		return new CommandsMap2();
	}
	
	@Override
	protected void configure(CommandsMap2 current, MarshalHelper helper, CommandsMap2 defaults) {
		for(Entry<String, Command> e : current.entrySet()) {
			helper.handler("command", (h) -> {
				HierarchicalStreamWriter writer = h.getWriter();
				writer.addAttribute("name", e.getKey());
				writer.addAttribute("class", helper.getX().getMapper().serializedClass(e.getValue().getClass()));
				h.getContext().convertAnother(e.getValue());
			});
		}
	}

	@Override
	protected void configure(CommandsMap2 current, UnmarshalHelper helper) {
		helper.handler("command", (o, h) -> {
			HierarchicalStreamReader reader = h.getReader();
			String name = reader.getAttribute("name");
			Class<? extends Command> type = h.getX().getMapper().realClass(reader.getAttribute("class")).asSubclass(Command.class);
			Command command = (Command) h.getContext().convertAnother(o, type);
			((CommandsMap2) o).put(name, command);
		});
	}
	
	

}
