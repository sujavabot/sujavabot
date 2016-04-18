package org.sujavabot.core.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class PropertiesCommand extends AbstractReportingCommand implements HelperConvertable<PropertiesCommand> {

	protected File file;
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 2)
			return invokeHelp(bot, cause, args);
		String cmd = args.get(1);
		if(
				("entries".equals(cmd) || "keys".equals(cmd) || "values".equals(cmd)) 
				&& (args.size() == 3 || args.size() == 4)) {
			String sep = "";
			StringBuilder sb = new StringBuilder();
			int index = 0;
			for(Entry<?, ?> e : read().entrySet()) {
				sb.append(sep);
				if(args.size() == 4) {
					sb.append(args.get(3).replace("$", String.valueOf(index)));
				}
				if("keys".equals(cmd) || "entries".equals(cmd)) 
					sb.append(e.getKey());
				if("entries".equals(cmd))
					sb.append("=");
				if("values".equals(cmd) || "entries".equals(cmd))
					sb.append(e.getValue());
				sep = args.get(2);
				index++;
			}
			return sb.toString();
		}
		if("get".equals(cmd) && (args.size() == 3 || args.size() == 4)) {
			String value = read().getProperty(args.get(2));
			if(value == null && args.size() == 4)
				value = args.get(3);
			return value;
		}
		if("set".equals(cmd) && args.size() == 4) {
			Properties p = read();
			p.setProperty(args.get(2), args.get(3));
			write(p);
			return "value set";
		}
		if("remove".equals(cmd) && args.size() == 3) {
			Properties p = read();
			if(p.remove(args.get(2)) != null) {
				write(p);
				return "removed";
			}
			return "key not found";
		}
		if("clear".equals(cmd) && args.size() == 2) {
			Properties p = read();
			p.clear();
			write(p);
			return "cleared";
		}
		return invokeHelp(bot, cause, args, cmd);
	}

	protected synchronized Properties read() {
		try {
			FileInputStream fin = null;
			try {
				fin = new FileInputStream(file);
				Properties p = new Properties();
				p.load(fin);
				return p;
			} finally {
				if(fin != null)
					fin.close();
			}
		} catch(FileNotFoundException e) {
			return new Properties();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected synchronized void write(Properties p) {
		try {
			FileOutputStream fout = null;
			try {
				fout = new FileOutputStream(file);
				p.store(fout, file.getName());
			} finally {
				if(fout != null)
					fout.close();
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp(
				"commands for maps",
				"entries", "<separator> [<prefix>]: return all entries joined with separator",
				"keys", "<separator> [<prefix>]: return all keys joined with separator",
				"values", "<separator> [<prefix>]: return all values joined with separator",
				"get", "<key> [<default>]: return value for key",
				"set", "<key> <value>: set value for key",
				"remove", "<key>: remove value for key",
				"clear", ": remove all values");
	}

	@Override
	public void configure(MarshalHelper helper, PropertiesCommand defaults) {
		helper.field("file", String.class, () -> file.getPath());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("file", String.class, (s) -> file = new File(s));
	}

}
