package org.sujavabot.plugin.urlhandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringEscapeUtils;
import org.sujavabot.plugin.urlhandler.AddressRanges.AddressRange;

public abstract class URLs {
	public static int MAX_REDIRECTS = 10;
	
	private URLs() {}
	
	public static String title(URL url, Iterable<AddressRange> whitelist) throws IOException {
		for(int i = 0; i <= MAX_REDIRECTS; i++) {
			String host = url.getHost();
			for(InetAddress addr : InetAddress.getAllByName(host)) {
				if(AddressRanges.isReserved(addr) && !AddressRanges.matches(addr, whitelist)) {
					String s = addr.toString();
					s = s.startsWith("/") ? s.substring(1) : s.replace("/", " --> ");
					throw new IOException("rejecting fetch to reserved address " + s);
				}
			}
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
			c.setInstanceFollowRedirects(false);
			c.connect();
			int rc = c.getResponseCode();
			String loc = c.getHeaderField("Location");
			c.disconnect();
			if(rc < 300 || rc >= 400)
				break;
			if(i == MAX_REDIRECTS)
				throw new IOException("too many redirects");
			url = new URL(loc);
		}
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
			if(title != null)
				title = StringEscapeUtils.unescapeHtml4(title).trim();
			return title;
		} finally {
			in.close();
		}
	}
}
