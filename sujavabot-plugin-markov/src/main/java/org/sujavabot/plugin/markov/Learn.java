package org.sujavabot.plugin.markov;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class Learn {
	public static void main(String[] args) throws Exception {
		File envHome = new File(args[0]);
		args = Arrays.copyOfRange(args, 1, args.length);
		
		String dbName = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);
		
		envHome.mkdirs();

		EnvironmentConfig ec = new EnvironmentConfig();
		ec.setAllowCreate(true);
		Environment e = new Environment(envHome, ec);
		
		
		DatabaseConfig dbc = new DatabaseConfig();
		dbc.setAllowCreate(true);
		dbc.setDeferredWrite(true);
		Database db = e.openDatabase(null, dbName, dbc);
		
		BerkeleyDBMarkov markov = new BerkeleyDBMarkov(db);
		
		InputStream[] inputs = new InputStream[] { System.in };
		if(args.length > 0) {
			inputs = new InputStream[args.length];
			for(int i = 0; i < args.length; i++)
				inputs[i] = new FileInputStream(args[i]);
		}
		
		for(InputStream in : inputs) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			for(String line = buf.readLine(); line != null; line = buf.readLine()) {
				line = line.replaceAll("^\\S+:", "").trim();
				List<String> words = StringContent.parse(line);
				if(words.size() == 0)
					continue;
				markov.consume(words, 5);
				System.out.println(StringContent.join(words));
			}
			buf.close();
		}
		
		db.close();
	}
}
