package org.sujavabot.core.util;

import org.sujavabot.core.SujavaBot;

public abstract class Messages {
	private Messages() {}
	
	public static String[] splitPM(SujavaBot bot, String recipient, String message) {
		if(message == null)
			return new String[] {null, null};
		int maxlen = maxlenPM(bot, recipient);
		return split(maxlen, recipient, message);
	}
	
	public static String[] split(int maxlen, String recipient, String message) {
		if(message == null)
			return new String[] {null, null};
		message = message.trim();
		if(message.length() <= maxlen)
			return new String[] {message, null};
		String send = message.substring(0, maxlen);
		String buffer = message.substring(maxlen);
		if(!send.matches(".*\\s$") && !buffer.matches("^\\s.*") && send.matches(".*\\s.*")) {
			buffer = send.replaceAll("^.*\\s(\\S+)$", "$1") + buffer;
			send = send.replaceAll("^(.*)\\s\\S+$", "$1");
		}
		send = send.trim();
		buffer = buffer.trim();
		return new String[] {send, buffer};
	}

	public static int maxlenPM(SujavaBot bot, String recipient) {
		int maxlen = bot.getConfiguration().getMaxLineLength();
		maxlen -= ("PRIVMSG " + recipient + " :\r\n").length();
		return maxlen;
	}
	
	public static int maxlenAction(SujavaBot bot, String recipient) {
		int maxlen = bot.getConfiguration().getMaxLineLength();
		maxlen -= ("PRIVMSG " + recipient + " :\u0001ACTION \u0001\r\n").length();
		return maxlen;
	}
	
	
}
