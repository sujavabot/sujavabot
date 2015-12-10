package org.sujavabot.plugin.jython;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.pircbotx.hooks.Event;
import org.python.core.PyList;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.sujavabot.core.SujavaBot;
import org.sujavabot.core.commands.AbstractReportingCommand;
import org.sujavabot.core.xml.ConverterHelpers.MarshalHelper;
import org.sujavabot.core.xml.ConverterHelpers.UnmarshalHelper;
import org.sujavabot.core.xml.HelperConvertable;

public class JythonCommand extends AbstractReportingCommand implements HelperConvertable<JythonCommand> {
	protected String source;
	protected File file;

	@Override
	protected Map<String, String> helpTopics() {
		return buildHelp("python script: " + (source != null ? source : file.toString()));
	}
	
	@Override
	public String invoke(SujavaBot bot, Event<?> cause, List<String> args) {
		try {
			PySystemState state = new PySystemState();
			state.argv = new PyList(args);
			state.path.add(new File(".").getCanonicalPath());
			PythonInterpreter interp = new PythonInterpreter(null, state);
			
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			Writer out = new OutputStreamWriter(bytes, Charset.forName("UTF-8"));
			interp.setOut(out);
			
			interp.set("bot", bot);
			interp.set("cause", cause);
			interp.set("caller", bot.getAuthorizedUser(getUser(cause)));
			
			if(source != null)
				interp.exec(source);
			else {
				InputStream in = new FileInputStream(file);
				try {
					interp.execfile(in);
				} finally {
					in.close();
				}
			}
			
			return new String(bytes.toByteArray(), Charset.forName("UTF-8"));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void configure(MarshalHelper helper, JythonCommand defaults) {
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
