package org.sujavabot.plugin.urlhandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

public abstract class URLs {
	private URLs() {}
	
	public static String title(URL url) throws IOException {
		InputStream in = url.openStream();
		try {
			Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
			long count = 16384;
			String tag = "";
			while(count-- > 0 && !"<TITLE>".equals(tag.toUpperCase())) {
				int ch = reader.read();
				if(ch < 0)
					return null;
				tag += (char) ch;
				if(!"<TITLE>".startsWith(tag))
					tag = "";
			}
			String title = null;
			while(count-- > 0 && !"</TITLE>".equals(tag)) {
				int ch = reader.read();
				if(ch < 0)
					return title;
				tag += (char) ch;
				if(!"</TITLE>".startsWith(tag.toUpperCase())) {
					if(title == null)
						title = "";
					title += tag;
					tag = "";
				}
			}
			return title;
		} finally {
			in.close();
		}
	}
}
