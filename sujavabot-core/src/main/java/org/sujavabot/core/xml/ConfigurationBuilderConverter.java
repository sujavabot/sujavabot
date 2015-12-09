package org.sujavabot.core.xml;

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.pircbotx.ChannelModeHandler;
import org.pircbotx.cap.CapHandler;
import org.pircbotx.hooks.CoreHooks;
import org.pircbotx.hooks.Listener;
import org.sujavabot.core.AuthorizedGroup;
import org.sujavabot.core.AuthorizedUser;
import org.sujavabot.core.ConfigurationBuilder;
import org.sujavabot.core.SujavaBot.UnverifyListener;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ConfigurationBuilderConverter extends AbstractConverter<ConfigurationBuilder> {
	
	public static class SPI extends XStreams.SPI {
		@Override
		public void configure(XStream x) {
			x.registerConverter(new ConfigurationBuilderConverter(x));
			x.registerConverter(new AuthorizedGroupConverter(x));
			x.registerConverter(new AuthorizedUserConverter(x));
			x.registerConverter(new CommandsMapConverter(x));
			x.alias("sujavabot", ConfigurationBuilder.class);
		}
	}

	public ConfigurationBuilderConverter(XStream x) {
		super(x, ConfigurationBuilder.class);
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		try {
			return super.unmarshal(reader, context);
		} finally {
			context.put(ConfigurationBuilder.class, null);
		}
	}
	
	@Override
	protected ConfigurationBuilder createCurrent(Class<? extends ConfigurationBuilder> required) {
		ConfigurationBuilder current = new ConfigurationBuilder();
		current.addListener(new UnverifyListener());
		return current;
	}
	
	@Override
	protected ConfigurationBuilder createDefaults(ConfigurationBuilder current) {
		return ConfigurationBuilder.createDefault();
	}
	
	@Override
	protected void configure(ConfigurationBuilder current, MarshalHelper helper, ConfigurationBuilder defaults) {
		HierarchicalStreamWriter writer = helper.getWriter();
		MarshallingContext context = helper.getContext();
		
		helper.field("web-irc-enabled", Boolean.class, () -> current.isWebIrcEnabled(), () -> defaults.isWebIrcEnabled());
		helper.field("web-irc-username", String.class, () -> current.getWebIrcUsername(), () -> defaults.getWebIrcUsername());
		helper.field("web-irc-hostname", String.class, () -> current.getWebIrcHostname(), () -> defaults.getWebIrcHostname());
		helper.field("web-irc-address", InetAddress.class, () -> current.getWebIrcAddress(), () -> defaults.getWebIrcAddress());
		helper.field("web-irc-password", String.class, () -> current.getWebIrcPassword(), () -> defaults.getWebIrcPassword());
		helper.field("nickname", String.class, () -> current.getName());
		helper.field("username", String.class, () -> current.getLogin());
		helper.field("version", String.class, () -> current.getVersion(), () -> defaults.getVersion());
		helper.field("finger", String.class, () -> current.getFinger(), () -> defaults.getFinger());
		helper.field("realname", String.class, () -> current.getRealName());
		helper.field("channel-prefixes", String.class, () -> current.getChannelPrefixes(), () -> defaults.getChannelPrefixes());
		helper.field("dcc-filename-quotes", Boolean.class, () -> current.isDccFilenameQuotes(), () -> defaults.isDccFilenameQuotes());
		for(Integer port : current.getDccPorts())
			helper.field("dcc-port", Integer.class, () -> port);
		helper.field("dcc-local-address", InetAddress.class, () -> current.getDccLocalAddress(), () -> defaults.getDccLocalAddress());
		helper.field("dcc-accept-timeout", Integer.class, () -> current.getDccAcceptTimeout(), () -> defaults.getDccAcceptTimeout());
		helper.field("dcc-resume-accept-timeout", Integer.class, () -> current.getDccResumeAcceptTimeout(), () -> defaults.getDccResumeAcceptTimeout());
		helper.field("dcc-transfer-buffer-size", Integer.class, () -> current.getDccTransferBufferSize(), () -> defaults.getDccTransferBufferSize());
		helper.field("dcc-passive-request", Boolean.class, () -> current.isDccPassiveRequest(), () -> defaults.isDccPassiveRequest());
		helper.field("server-hostname", String.class, () -> current.getServerHostname(), () -> defaults.getServerHostname());
		helper.field("server-port", Integer.class, () -> current.getServerPort(), () -> defaults.getServerPort());
		helper.field("server-password", String.class, () -> current.getServerPassword(), () -> defaults.getServerPassword());
		helper.field("local-address", InetAddress.class, () -> current.getLocalAddress(), () -> defaults.getLocalAddress());
		helper.field("encoding", Charset.class, () -> current.getEncoding(), () -> defaults.getEncoding());
		helper.field("locale", Locale.class, () -> current.getLocale(), () -> defaults.getLocale());
		helper.field("socket-timeout", Integer.class, () -> current.getSocketTimeout(), () -> defaults.getSocketTimeout());
		helper.field("max-line-length", Integer.class, () -> current.getMaxLineLength(), () -> defaults.getMaxLineLength());
		helper.field("auto-split-message", Boolean.class, () -> current.isAutoSplitMessage(), () -> defaults.isAutoSplitMessage());
		helper.field("auto-nick-change", Boolean.class, () -> current.isAutoNickChange(), () -> defaults.isAutoNickChange());
		helper.field("message-delay", Long.class, () -> current.getMessageDelay(), () -> defaults.getMessageDelay());
		
		for(Entry<String, String> e : current.getAutoJoinChannels().entrySet()) {
			helper.handler("auto-join-channel", h2 -> {
				writer.startNode("channel-name");
				context.convertAnother(e.getKey());
				writer.endNode();
				if(!e.getValue().isEmpty()) {
					writer.startNode("channel-key");
					context.convertAnother(e.getValue());
					writer.endNode();
				}
			});
		}
		
		helper.field("ident-server-enabled", Boolean.class, () -> current.isIdentServerEnabled(), () -> defaults.isIdentServerEnabled());
		helper.field("nickserv-password", String.class, () -> current.getNickservPassword(), () -> defaults.getNickservPassword());
		helper.field("auto-reconnect", Boolean.class, () -> current.isAutoReconnect(), () -> defaults.isAutoReconnect());
		helper.field("cap-enabled", Boolean.class, () -> current.isCapEnabled(), () -> defaults.isCapEnabled());
		
		if(!"[EnableCapHandler(cap=multi-prefix, ignoreFail=true), EnableCapHandler(cap=away-notify, ignoreFail=true)]".equals(current.getCapHandlers().toString())) {
			helper.handler("cap-handlers", h2 -> {
				for(CapHandler ch : current.getCapHandlers()) {
					writer.startNode("cap-handler");
					writer.addAttribute("class", x.getMapper().serializedClass(ch.getClass()));
					context.convertAnother(ch);
					writer.endNode();
				}
			});
		}
		if(!current.getChannelModeHandlers().equals(defaults.getChannelModeHandlers())) {
			helper.handler("channel-mode-handlers", h2 -> {
				for(ChannelModeHandler cmh : current.getChannelModeHandlers()) {
					writer.startNode("channel-mode-handler");
					writer.addAttribute("class", x.getMapper().serializedClass(cmh.getClass()));
					context.convertAnother(cmh);
					writer.endNode();
				}
			});
		}
		
		for(File pluginConfig : current.getPluginConfigs())
			helper.field("plugin", File.class, () -> pluginConfig);
		
		List<AuthorizedGroup> groups = new ArrayList<>(current.getGroups().values());
		Collections.sort(groups, AuthorizedGroup.GROUP_ORDER);
		
		for(AuthorizedGroup group : groups)
			helper.field("group", AuthorizedGroup.class, () -> group);
		
		for(AuthorizedUser user : current.getUsers().values())
			helper.field("user", AuthorizedUser.class, () -> user);
		
		for(Listener<?> l : current.getBotListeners()) {
			helper.field("listener", () -> l);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void configure(ConfigurationBuilder current, UnmarshalHelper helper) {
		HierarchicalStreamReader reader = helper.getReader();
		UnmarshallingContext context = helper.getContext();
		
		context.put(ConfigurationBuilder.class, current);
		
		helper.field("web-irc-enabled", Boolean.class, b -> current.setWebIrcEnabled(b));
		helper.field("web-irc-username", String.class, s -> current.setWebIrcUsername(s));
		helper.field("web-irc-hostname", String.class, s -> current.setWebIrcHostname(s));
		helper.field("web-irc-address", InetAddress.class, a -> current.setWebIrcAddress(a));
		helper.field("web-irc-password", String.class, s -> current.setWebIrcPassword(s));
		helper.field("nickname", String.class, s -> current.setName(s));
		helper.field("username", String.class, s -> current.setLogin(s));
		helper.field("version", String.class, s -> current.setVersion(s));
		helper.field("finger", String.class, s -> current.setFinger(s));
		helper.field("realname", String.class, s -> current.setRealName(s));
		helper.field("channel-prefixes", String.class, s -> current.setChannelPrefixes(s));
		helper.field("dcc-filename-quotes", Boolean.class, b -> current.setDccFilenameQuotes(b));
		helper.field("dcc-port", Integer.class, i -> current.getDccPorts().add(i));
		helper.field("dcc-local-address", InetAddress.class, a -> current.setDccLocalAddress(a));
		helper.field("dcc-accept-timeout", Integer.class, i -> current.setDccAcceptTimeout(i));
		helper.field("dcc-resume-accept-timeout", Integer.class, i -> current.setDccResumeAcceptTimeout(i));
		helper.field("dcc-transfer-buffer-size", Integer.class, i -> current.setDccTransferBufferSize(i));
		helper.field("dcc-passive-request", Boolean.class, b -> current.setDccPassiveRequest(b));
		helper.field("server-hostname", String.class, s -> current.setServerHostname(s));
		helper.field("server-port", Integer.class, i -> current.setServerPort(i));
		helper.field("server-password", String.class, s -> current.setServerPassword(s));
		helper.field("local-address", InetAddress.class, a -> current.setLocalAddress(a));
		helper.field("encoding", Charset.class, c -> current.setEncoding(c));
		helper.field("locale", Locale.class, l -> current.setLocale(l));
		helper.field("socket-timeout", Integer.class, i -> current.setSocketTimeout(i));
		helper.field("max-line-length", Integer.class, i -> current.setMaxLineLength(i));
		helper.field("auto-split-message", Boolean.class, b -> current.setAutoSplitMessage(b));
		helper.field("auto-nick-change", Boolean.class, b -> current.setAutoNickChange(b));
		helper.field("message-delay", Integer.class, i -> current.setMessageDelay(i));
		
		helper.handler("auto-join-channel", (current2, h2) -> {
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
				channelKey = "";
			current.getAutoJoinChannels().put(channelName, channelKey);
		});
		
		
		helper.field("ident-server-enabled", Boolean.class, b -> current.setIdentServerEnabled(b));
		helper.field("nickserv-password", String.class, s -> current.setNickservPassword(s));
		helper.field("auto-reconnect", Boolean.class, b -> current.setAutoReconnect(b));
		helper.field("cap-enabled", Boolean.class, b -> current.setCapEnabled(b));
		
		helper.handler("cap-handlers", (current2, h2) -> {
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
		helper.handler("channel-mode-handlers", (current2, h2) -> {
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
		
		helper.field("cap-handler", (CapHandler ch) -> current.getCapHandlers().add(ch));
		helper.field("channel-mode-handler", (ChannelModeHandler cmh) -> current.getChannelModeHandlers().add(cmh));
		
		helper.field("plugin", File.class, f -> {
			current.addPluginConfig(f);
		});
		
		helper.field("group", AuthorizedGroup.class, g -> {
			current.getGroups().put(g.getName(), g);
		});
		
		helper.field("user", AuthorizedUser.class, u -> {
			current.getUsers().put(u.getName(), u);
		});
		
		helper.field("listener", l -> { 
			current.addListener((Listener) l);
			current.getBotListeners().add((Listener) l);
		} );
	}

}
