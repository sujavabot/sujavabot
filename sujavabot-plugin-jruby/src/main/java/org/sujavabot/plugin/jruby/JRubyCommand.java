package org.sujavabot.plugin.jruby;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.pircbotx.hooks.Event;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class JRubyCommand extends AbstractReportingCommand implements HelperConvertable<JRubyCommand> {
	protected String source;
	protected File file;

	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		try {
			ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
			container.setArgv(args.toArray(new String[args.size()]));
			Object v;
			if(source != null)
				v = container.runScriptlet(source);
			else {
				Reader in = new FileReader(file);
				try {
					v = container.runScriptlet(in, file.getPath());
				} finally {
					in.close();
				}
			}
			return String.valueOf(v);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void configure(MarshalHelper helper, JRubyCommand defaults) {
		if(source != null)
			helper.field("source", String.class, () -> source);
		if(file != null)
			helper.field("file", File.class, () -> file);
	}

	@Override
	public void configure(UnmarshalHelper helper) {
		helper.field("source", String.class, s -> source = s);
		helper.field("file", File.class, f -> file = f);
	}

}