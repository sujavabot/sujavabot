package org.sujavabot.core.xml;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.pircbotx.ChannelModeHandler;
import org.pircbotx.cap.CapHandler;
import org.sujavabot.core.ConfigurationBuilder;
import org.sujavabot.core.Plugin;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ConfigurationBuilderConverter implements Converter {

	protected XStream x;
	
	public ConfigurationBuilderConverter(XStream x) {
		this.x = x;
	}
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return ConfigurationBuilder.class.equals(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		MarshalHelper h = new MarshalHelper(x, writer, context);
		ConfigurationBuilder current = (ConfigurationBuilder) source;
		
		ConfigurationBuilder def = new ConfigurationBuilder();
		
		h.field("web-irc-enabled", Boolean.class, () -> current.isWebIrcEnabled(), () -> def.isWebIrcEnabled());
		h.field("web-irc-username", String.class, () -> current.getWebIrcUsername(), () -> def.getWebIrcUsername());
		h.field("web-irc-hostname", String.class, () -> current.getWebIrcHostname(), () -> def.getWebIrcHostname());
		h.field("web-irc-address", InetAddress.class, () -> current.getWebIrcAddress(), () -> def.getWebIrcAddress());
		h.field("web-irc-password", String.class, () -> current.getWebIrcPassword(), () -> def.getWebIrcPassword());
		h.field("nickname", String.class, () -> current.getName());
		h.field("username", String.class, () -> current.getLogin());
		h.field("version", String.class, () -> current.getVersion(), () -> def.getVersion());
		h.field("finger", String.class, () -> current.getFinger(), () -> def.getFinger());
		h.field("realname", String.class, () -> current.getRealName());
		h.field("channel-prefixes", String.class, () -> current.getChannelPrefixes(), () -> def.getChannelPrefixes());
		h.field("dcc-filename-quotes", Boolean.class, () -> current.isDccFilenameQuotes(), () -> def.isDccFilenameQuotes());
		for(Integer port : current.getDccPorts())
			h.field("dcc-port", Integer.class, () -> port);
		h.field("dcc-local-address", InetAddress.class, () -> current.getDccLocalAddress(), () -> def.getDccLocalAddress());
		h.field("dcc-accept-timeout", Integer.class, () -> current.getDccAcceptTimeout(), () -> def.getDccAcceptTimeout());
		h.field("dcc-resume-accept-timeout", Integer.class, () -> current.getDccResumeAcceptTimeout(), () -> def.getDccResumeAcceptTimeout());
		h.field("dcc-transfer-buffer-size", Integer.class, () -> current.getDccTransferBufferSize(), () -> def.getDccTransferBufferSize());
		h.field("dcc-passive-request", Boolean.class, () -> current.isDccPassiveRequest(), () -> def.isDccPassiveRequest());
		h.field("server-hostname", String.class, () -> current.getServerHostname(), () -> def.getServerHostname());
		h.field("server-port", Integer.class, () -> current.getServerPort(), () -> def.getServerPort());
		h.field("server-password", String.class, () -> current.getServerPassword(), () -> def.getServerPassword());
		h.field("local-address", InetAddress.class, () -> current.getLocalAddress(), () -> def.getLocalAddress());
		h.field("encoding", Charset.class, () -> current.getEncoding(), () -> def.getEncoding());
		h.field("locale", Locale.class, () -> current.getLocale(), () -> def.getLocale());
		h.field("socket-timeout", Integer.class, () -> current.getSocketTimeout(), () -> def.getSocketTimeout());
		h.field("max-line-length", Integer.class, () -> current.getMaxLineLength(), () -> def.getMaxLineLength());
		h.field("auto-split-message", Boolean.class, () -> current.isAutoSplitMessage(), () -> def.isAutoSplitMessage());
		h.field("auto-nick-change", Boolean.class, () -> current.isAutoNickChange(), () -> def.isAutoNickChange());
		h.field("message-delay", Long.class, () -> current.getMessageDelay(), () -> def.getMessageDelay());
		
		for(Entry<String, String> e : current.getAutoJoinChannels().entrySet()) {
			h.handler("auto-join-channel", h2 -> {
				writer.startNode("channel-name");
				context.convertAnother(e.getKey());
				writer.endNode();
				if(e.getValue() != null) {
					writer.startNode("channel-key");
					context.convertAnother(e.getValue());
					writer.endNode();
				}
			});
		}
		
		h.field("ident-server-enabled", Boolean.class, () -> current.isIdentServerEnabled(), () -> def.isIdentServerEnabled());
		h.field("nickserv-password", String.class, () -> current.getNickservPassword(), () -> def.getNickservPassword());
		h.field("auto-reconnect", Boolean.class, () -> current.isAutoReconnect(), () -> def.isAutoReconnect());
		h.field("cap-enabled", Boolean.class, () -> current.isCapEnabled(), () -> def.isCapEnabled());
		
		if(!"[EnableCapHandler(cap=multi-prefix, ignoreFail=true), EnableCapHandler(cap=away-notify, ignoreFail=true)]".equals(current.getCapHandlers().toString())) {
			h.handler("cap-handlers", h2 -> {
				for(CapHandler ch : current.getCapHandlers()) {
					writer.startNode("cap-handler");
					writer.addAttribute("class", x.getMapper().serializedClass(ch.getClass()));
					context.convertAnother(ch);
					writer.endNode();
				}
			});
		}
		if(!current.getChannelModeHandlers().equals(def.getChannelModeHandlers())) {
			h.handler("channel-mode-handlers", h2 -> {
				for(ChannelModeHandler cmh : current.getChannelModeHandlers()) {
					writer.startNode("channel-mode-handler");
					writer.addAttribute("class", x.getMapper().serializedClass(cmh.getClass()));
					context.convertAnother(cmh);
					writer.endNode();
				}
			});
		}
		
		for(Plugin plugin : current.getPlugins())
			h.handler("plugin", h2 -> {
				writer.addAttribute("class", x.getMapper().serializedClass(plugin.getClass()));
				context.convertAnother(plugin, plugin.getConfigurableConverter());
			});
		
		h.write();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		ConfigurationBuilder current = new ConfigurationBuilder();
		UnmarshalHelper h = new UnmarshalHelper(x, reader, context);
		
		h.field("web-irc-enabled", Boolean.class, b -> current.setWebIrcEnabled(b));
		h.field("web-irc-username", String.class, s -> current.setWebIrcUsername(s));
		h.field("web-irc-hostname", String.class, s -> current.setWebIrcHostname(s));
		h.field("web-irc-address", InetAddress.class, a -> current.setWebIrcAddress(a));
		h.field("web-irc-password", String.class, s -> current.setWebIrcPassword(s));
		h.field("nickname", String.class, s -> current.setName(s));
		h.field("username", String.class, s -> current.setLogin(s));
		h.field("version", String.class, s -> current.setVersion(s));
		h.field("finger", String.class, s -> current.setFinger(s));
		h.field("realname", String.class, s -> current.setRealName(s));
		h.field("channel-prefixes", String.class, s -> current.setChannelPrefixes(s));
		h.field("dcc-filename-quotes", Boolean.class, b -> current.setDccFilenameQuotes(b));
		h.field("dcc-port", Integer.class, i -> current.getDccPorts().add(i));
		h.field("dcc-local-address", InetAddress.class, a -> current.setDccLocalAddress(a));
		h.field("dcc-accept-timeout", Integer.class, i -> current.setDccAcceptTimeout(i));
		h.field("dcc-resume-accept-timeout", Integer.class, i -> current.setDccResumeAcceptTimeout(i));
		h.field("dcc-transfer-buffer-size", Integer.class, i -> current.setDccTransferBufferSize(i));
		h.field("dcc-passive-request", Boolean.class, b -> current.setDccPassiveRequest(b));
		h.field("server-hostname", String.class, s -> current.setServerHostname(s));
		h.field("server-port", Integer.class, i -> current.setServerPort(i));
		h.field("server-password", String.class, s -> current.setServerPassword(s));
		h.field("local-address", InetAddress.class, a -> current.setLocalAddress(a));
		h.field("encoding", Charset.class, c -> current.setEncoding(c));
		h.field("locale", Locale.class, l -> current.setLocale(l));
		h.field("socket-timeout", Integer.class, i -> current.setSocketTimeout(i));
		h.field("max-line-length", Integer.class, i -> current.setMaxLineLength(i));
		h.field("auto-split-message", Boolean.class, b -> current.setAutoSplitMessage(b));
		h.field("auto-nick-change", Boolean.class, b -> current.setAutoNickChange(b));
		h.field("message-delay", Integer.class, i -> current.setMessageDelay(i));
		
		h.handler("auto-join-channel", (current2, h2) -> {
			String channelName;
			String channelKey;
			reader.moveDown();
			channelName = (String) context.convertAnother(current, String.class);
			reader.moveUp();
			if(reader.hasMoreChildren()) {
				reader.moveDown();
				channelKey = (String) context.convertAnother(current, String.class);
				reader.moveUp();
			} else
				channelKey = null;
			current.getAutoJoinChannels().put(channelName, channelKey);
		});
		
		
		h.field("ident-server-enabled", Boolean.class, b -> current.setIdentServerEnabled(b));
		h.field("nickserv-password", String.class, s -> current.setNickservPassword(s));
		h.field("auto-reconnect", Boolean.class, b -> current.setAutoReconnect(b));
		h.field("cap-enabled", Boolean.class, b -> current.setCapEnabled(b));
		
		h.handler("cap-handlers", (current2, h2) -> {
			List<CapHandler> capHandlers = new ArrayList<>();
			while(reader.hasMoreChildren()) {
				reader.moveDown();
				if("cap-handler".equals(reader.getNodeName())) {
					Class<?> type = x.getMapper().realClass(reader.getAttribute("class"));
					CapHandler cp = (CapHandler) context.convertAnother(current, type);
					capHandlers.add(cp);
				}
				reader.moveUp();
			}
			current.getCapHandlers().clear();
			current.getCapHandlers().addAll(capHandlers);
		});
		h.handler("channel-mode-handlers", (current2, h2) -> {
			List<ChannelModeHandler> channelModeHandlers = new ArrayList<>();
			while(reader.hasMoreChildren()) {
				reader.moveDown();
				if("channel-mode-handler".equals(reader.getNodeName())) {
					Class<?> type = x.getMapper().realClass(reader.getAttribute("class"));
					ChannelModeHandler cmh = (ChannelModeHandler) context.convertAnother(current, type);
					channelModeHandlers.add(cmh);
				}
				reader.moveUp();
			}
			current.getChannelModeHandlers().clear();
			current.getChannelModeHandlers().addAll(channelModeHandlers);
		});
		
		h.field("cap-handler", (CapHandler ch) -> current.getCapHandlers().add(ch));
		h.field("channel-mode-handler", (ChannelModeHandler cmh) -> current.getChannelModeHandlers().add(cmh));
		
		h.handler("plugin", (current2, h2) -> {
			Class<?> type = x.getMapper().realClass(reader.getAttribute("class"));
			Plugin plugin = (Plugin) x.getReflectionProvider().newInstance(type);
			plugin = (Plugin) context.convertAnother(current, type, plugin.getConfigurableConverter());
			current.getPlugins().add(plugin);
		});
		
		return h.read(current);
	}

}
