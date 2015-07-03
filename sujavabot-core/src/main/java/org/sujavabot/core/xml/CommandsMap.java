package org.sujavabot.core.xml;

import java.util.Map;
import java.util.TreeMap;

import org.sujavabot.core.Command;

public class CommandsMap extends TreeMap<String, Command> {
	private static final long serialVersionUID = 0;

	public CommandsMap() {
	}

	public CommandsMap(Map<? extends String, ? extends Command> m) {
		super(m);
	}

}
