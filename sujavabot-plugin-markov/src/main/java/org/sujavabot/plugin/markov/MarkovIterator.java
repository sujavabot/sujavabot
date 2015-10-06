package org.sujavabot.plugin.markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Optional;
import com.sleepycat.je.DatabaseException;

public class MarkovIterator implements Iterator<String> {
	protected List<String> prefix = new ArrayList<>();
	protected Markov markov;
	protected int maxlen;

	protected List<String> init;
	protected Optional<String> next;

	public MarkovIterator(Markov markov, int maxlen, List<String> prefix) {
		this.markov = markov;
		this.maxlen = maxlen;
		this.prefix.addAll(prefix);
	}

	public List<String> toList() {
		List<String> list = new ArrayList<>();
		while(hasNext())
			list.add(next());
		return list;
	}

	@Override
	public boolean hasNext() {
		if(next == null) {
			if(init == null) {
				init = new ArrayList<>(prefix);
				if(init.size() > 0)
					next = Optional.fromNullable(init.remove(0));
				else
					next = Optional.absent();
			} else if(init.size() > 0) {
				next = Optional.fromNullable(init.remove(0));
			} else {
				String n;
				try {
					n = markov.next(prefix);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				next = (n == null ? Optional.<String>absent() : Optional.fromNullable(n));
			}
		}
		return next.isPresent();
	}

	@Override
	public String next() {
		if(!hasNext())
			throw new NoSuchElementException();
		try {
			String n = next.get();
			prefix.add(n);
			if(prefix.size() > maxlen)
				prefix.remove(0);
			return n;
		} finally {
			next = null;
		}
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
