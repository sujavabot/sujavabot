package org.sujavabot.core.xml;

import java.util.Map;
import java.util.TreeMap;

import org.sujavabot.core.Command;

public class CommandsMap2 extends TreeMap<String, Command> {
	private static final long serialVersionUID = 0;

	public CommandsMap2() {
	}

	public CommandsMap2(Map<? extends String, ? extends Command> m) {
		super(m);
	}

}
