package org.sujavabot.core.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class CollectionCommand extends AbstractReportingCommand implements HelperConvertable<CollectionCommand> {

	protected File file;
	protected Class<? extends Collection> type;
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		if(args.size() < 2)
			return invokeHelp(bot, cause, args);
		String cmd = args.get(1);
		if("join".equals(cmd) && (args.size() == 3 || args.size() == 4)) {
			String sep = "";
			StringBuilder sb = new StringBuilder();
			int index = 0;
			for(String s : read()) {
				sb.append(sep);
				if(args.size() == 4) {
					sb.append(args.get(3).replace("$", String.valueOf(index)));
				}
				sb.append(s);
				sep = args.get(2);
				index++;
			}
			return sb.toString();
		}
		if("get".equals(cmd) && args.size() == 3) {
			try {
				int index = Integer.parseInt(args.get(2));
				Collection<String> c = read();
				if(index >= 0 && index < c.size()) {
					Iterator<String> i = c.iterator();
					while(index-- > 0)
						i.next();
					return i.next();
				} else
					return "index out of bounds";
			} catch(NumberFormatException e) {
				return "index not an integer";
			}
		}
		if("add".equals(cmd) && args.size() == 3) {
			Collection<String> c = read();
			if(c.add(args.get(2))) {
				write(c);
				return "added";
			}
			return "already added";
		}
		if("remove".equals(cmd) && args.size() == 3) {
			Collection<String> c = read();
			if(c.remove(args.get(2))) {
				write(c);
				return "removed";
			}
			try {
				int index = Integer.parseInt(args.get(2));
				if(index >= 0 && index < c.size()) {
					Iterator<String> i = c.iterator();
					while(index-- >= 0)
						i.next();
					i.remove();
					write(c);
					return "removed";
				}
			} catch(NumberFormatException e) {
			}
			return "not found, not removed";
		}
		if("clear".equals(cmd) && args.size() == 2) {
			Collection<String> c = read();
			c.clear();
			write(c);
			return "cleared";
		}
		return invokeHelp(bot, cause, args, cmd);
	}

	@SuppressWarnings("unchecked")
	protected synchronized Collection<String> read() {
		try {
			FileInputStream fin = null;
			try {
				fin = new FileInputStream(file);
				ObjectInputStream oin = new ObjectInputStream(fin);
				try {
					return (Collection<String>) oin.readObject();
				} finally {
					oin.close();
				}
			} finally {
				if(fin != null)
					fin.close();
			}
		} catch(FileNotFoundException e) {
		} catch(IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		try {
			return type.newInstance();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected synchronized void write(Collection<String> c) {
		try {
			FileOutputStream fout = null;
			try {
				fout = new FileOutputStream(file);
				ObjectOutputStream oout = new ObjectOutputStream(fout);
				oout.writeObject(c);
				oout.close();
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
				"commands for collections",
				"join", "<separator> [<prefix>]: return all values joined with separator",
				"get", "<index>: return value at index",
				"add", "<value>: append value",
				"remove", "<value> | <index>: remove a value, or if not found, remove at index",
				"clear", ": remove all values");
	}

	@Override
	public void configure(MarshalHelper helper, CollectionCommand defaults) {
		helper.field("file", String.class, () -> file.getPath());
		helper.field("type", String.class, () -> type.getName());
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("file", String.class, (s) -> file = new File(s));
		helper.field("type", String.class, (s) -> {
			try {
				type = Class.forName(s).asSubclass(Collection.class);
			} catch(ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
