package org.sujavabot.core.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public abstract class MessageQueueCommand extends AbstractReportingCommand {
	protected File file;

	@SuppressWarnings("unchecked")
	private Map<String, List<String>> readFile() throws IOException {
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				ObjectInputStream oin = new ObjectInputStream(in);
				try {
					return (Map<String, List<String>>) oin.readObject();
				} finally {
					oin.close();
				}
			} finally {
				in.close();
			}
		} catch(FileNotFoundException | ClassNotFoundException e) {
			return new TreeMap<>();
		}
	}
	
	private void writeFile(Map<String, List<String>> map) throws IOException {
		File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
		FileOutputStream out = new FileOutputStream(tmp);
		try {
			ObjectOutputStream oout = new ObjectOutputStream(out);
			oout.writeObject(map);
			oout.close();
		} finally {
			out.close();
		}
		Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
	
	protected void offer(String dest, String message) throws IOException {
		synchronized(MessageQueueCommand.class) {
			Map<String, List<String>> map = readFile();
			List<String> queue = map.getOrDefault(dest, new ArrayList<>());
			queue.add(message);
			map.put(dest, queue);
			writeFile(map);
		}
	}
	
	protected List<String> poll(String dest) throws IOException {
		List<String> messages = new ArrayList<>();
		synchronized(MessageQueueCommand.class) {
			Map<String, List<String>> map = readFile();
			Iterator<Entry<String, List<String>>> i = map.entrySet().iterator();
			while (i.hasNext()) {
				Entry<String, List<String>> e = i.next();
				Pattern p = null;
				try {
					p = Pattern.compile(e.getKey());
				} catch(RuntimeException ex) {
				}
				if (e.getKey().equals(dest) || p != null && p.matcher(dest).matches()) {
					messages.addAll(e.getValue());
					i.remove();
				}
			}
			writeFile(map);
		}
		return messages;
	}
	
	public static class OfferMessageCommand extends MessageQueueCommand implements HelperConvertable<OfferMessageCommand> {

		@Override
		public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
			if (args.size() < 2) return invokeHelp(bot, cause, args);
			String dest = args.get(1);
			String message = StringUtils.join(args.subList(2, args.size()), " ");
			try {
				offer(dest, message);
			} catch(IOException ex) {
				throw new RuntimeException(ex);
			}
			return "Message offered.";
		}

		@Override
		public void configure(MarshalHelper helper, OfferMessageCommand defaults) {
			helper.field("file", String.class, () -> file.getPath());
		}

		@Override
		public void configure(UnmarshalHelper helper) {
			helper.field("file", String.class, (s) -> file = new File(s));
		}

		@Override
		protected Map<String, String> helpTopics() {
			return buildHelp("<dest> <message>: offer a message");
		}

	}

	public static class PollMessageCommand extends MessageQueueCommand implements HelperConvertable<PollMessageCommand> {

		@Override
		public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
			if (args.size() != 2) return invokeHelp(bot, cause, args);
			try {
				return StringUtils.join(poll(args.get(1)), "\n");
			} catch(IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void configure(MarshalHelper helper, PollMessageCommand defaults) {
			helper.field("file", String.class, () -> file.getPath());
		}

		@Override
		public void configure(UnmarshalHelper helper) {
			helper.field("file", String.class, (s) -> file = new File(s));
		}

		@Override
		protected Map<String, String> helpTopics() {
			return buildHelp("<dest> poll all messages");
		}

	}
}
