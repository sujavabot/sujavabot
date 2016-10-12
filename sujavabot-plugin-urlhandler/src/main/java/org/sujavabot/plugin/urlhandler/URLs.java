package org.sujavabot.plugin.urlhandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringEscapeUtils;

public abstract class URLs {
	private URLs() {}
	
	public static String title(URL url) throws IOException {
		InputStream in = url.openStream();
		try {
			Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
			long count = 65536;
			String tag = "";
			while(count-- > 0 && !"<TITLE>".equals(tag.toUpperCase())) {
				int ch = reader.read();
				if(ch < 0) {
					try {
						return null;
					} finally {
						reader.close();
					}
				}
				tag += (char) ch;
				if(!"<TITLE>".startsWith(tag.toUpperCase()))
					tag = "";
			}
			if(count == 0)
				return null;
			String title = null;
			tag = "";
			while(count-- > 0 && !"</TITLE>".equals(tag.toUpperCase())) {
				int ch = reader.read();
				if(ch < 0) {
					try {
						return title;
					} finally {
						reader.close();
					}
				}
				tag += (char) ch;
				if(!"</TITLE>".startsWith(tag.toUpperCase())) {
					if(title == null)
						title = "";
					title += tag;
					tag = "";
				}
			}
			title = StringEscapeUtils.unescapeHtml4(title).trim();
			return title;
		} finally {
			in.close();
		}
	}
}
