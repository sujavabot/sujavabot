package org.sujavabot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;

public class Authorization {
	
	@FunctionalInterface
	public static interface LimitedCallable<E, T extends Exception> extends Callable<E> {
		public E call() throws T;
	}
	
	public static <E, T extends Exception> E limitedCall(
			AuthorizedUser asUser, 
			LimitedCallable<E, T> task) throws T {
		return limitedCall(getAuthorization().getBot(), asUser, null, null, task);
	}
	
	public static <E, T extends Exception> E limitedCall(
			SujavaBot bot,
			AuthorizedUser asUser, 
			List<AuthorizedGroup> asGroups,
			List<AuthorizedGroup> asOwnedGroups,
			LimitedCallable<E, T> task) throws T {
		Authorization old = getAuthorization();
		try {
			setAuthorization(new Authorization(bot, asUser, asGroups, asOwnedGroups));
			return task.call();
		} finally {
			setAuthorization(old);
		}
	}
	
	public static <E, T extends Exception> LimitedCallable<E, T> limitedCallable(
			SujavaBot bot,
			AuthorizedUser asUser, 
			List<AuthorizedGroup> asGroups,
			List<AuthorizedGroup> asOwnedGroups,
			LimitedCallable<E, T> task) {
		return (() -> limitedCall(bot, asUser, asGroups, asOwnedGroups, task));
	}
	
	public static <E> E call(
			AuthorizedUser asUser, 
			Callable<E> task) throws Exception {
		return call(getAuthorization().getBot(), asUser, null, null, task);
	}
	
	public static <E> E call(
			SujavaBot bot,
			AuthorizedUser asUser, 
			List<AuthorizedGroup> asGroups,
			List<AuthorizedGroup> asOwnedGroups,
			Callable<E> task) throws Exception {
		Authorization old = getAuthorization();
		try {
			setAuthorization(new Authorization(bot, asUser, asGroups, asOwnedGroups));
			return task.call();
		} finally {
			setAuthorization(old);
		}
	}
	
	public static <E> Callable<E> callable(
			SujavaBot bot,
			AuthorizedUser asUser, 
			List<AuthorizedGroup> asGroups,
			List<AuthorizedGroup> asOwnedGroups,
			Callable<E> task) {
		return (() -> call(bot, asUser, asGroups, asOwnedGroups, task));
	}

	public static void run(
			AuthorizedUser asUser,
			Runnable task) {
		run(getAuthorization().getBot(), asUser, null, null, task);
	}
	
	public static void run(
			SujavaBot bot,
			AuthorizedUser asUser,
			List<AuthorizedGroup> asGroups,
			List<AuthorizedGroup> asOwnedGroups,
			Runnable task) {
		Authorization old = getAuthorization();
		try {
			setAuthorization(new Authorization(bot, asUser, asGroups, asOwnedGroups));
			task.run();
		} finally {
			setAuthorization(old);
		}
	}
	
	public static Runnable runnable(
			SujavaBot bot,
			AuthorizedUser asUser,
			List<AuthorizedGroup> asGroups,
			List<AuthorizedGroup> asOwnedGroups,
			Runnable task) {
		return (() -> run(bot, asUser, asGroups, asOwnedGroups, task));
	}
	
	private static ThreadLocal<Authorization> authorization = new ThreadLocal<Authorization>() {
		protected Authorization initialValue() {
			return new Authorization(null, null, null, null);
		}
	};
	
	public static Authorization getAuthorization() {
		return authorization.get();
	}
	
	private static void setAuthorization(Authorization auth) {
		authorization.set(auth);
	}
	
	public static AuthorizedUser getCurrentUser() {
		return getAuthorization().getUser();
	}
	
	public static List<AuthorizedGroup> getCurrentGroups() {
		return getAuthorization().getGroups();
	}
	
	public static List<AuthorizedGroup> getCurrentOwnedGroups() {
		return getAuthorization().getOwnedGroups();
	}
	
	public static boolean isCurrentRootOwner() {
		return getAuthorization().isRootOwner();
	}
	
	public static boolean isCurrentOwner(AuthorizedUser u) {
		return getAuthorization().isOwner(u);
	}
	
	public static boolean isCurrentOwner(AuthorizedGroup g) {
		return getAuthorization().isOwner(g);
	}
	
	private SujavaBot bot;
	private AuthorizedUser user;
	private List<AuthorizedGroup> groups;
	private List<AuthorizedGroup> ownedGroups;
	
	public Authorization(
			SujavaBot bot, 
			AuthorizedUser user, 
			List<AuthorizedGroup> groups, 
			List<AuthorizedGroup> ownedGroups) {
		this.bot = bot;
		this.user = user;
		this.groups = new ArrayList<>();
		this.ownedGroups = new ArrayList<>();
		if(user != null) {
			this.groups.addAll(user.getAllGroups());
			this.ownedGroups.addAll(user.getOwnedGroups());
		}
		if(groups != null) {
			for(AuthorizedGroup g : groups) {
				if(!this.groups.contains(g))
					this.groups.add(g);
				for(AuthorizedGroup p : g.getAllParents())
					if(!this.groups.contains(p))
						this.groups.add(p);
			}
		}
		if(ownedGroups != null) {
			for(AuthorizedGroup g : ownedGroups)
				if(!this.ownedGroups.contains(g))
					this.ownedGroups.add(g);
		}
		if(bot != null) {
			for(AuthorizedGroup g : bot.getAuthorizedGroups().values()) {
				if(!this.ownedGroups.contains(g)) {
					if(!Collections.disjoint(g.getAllParents(), this.ownedGroups))
						this.ownedGroups.add(g);
				}
			}
		}
	}
	
	public <E, T extends Exception> E limitedCall(LimitedCallable<E, T> task) throws T {
		Authorization old = getAuthorization();
		try {
			setAuthorization(this);
			return task.call();
		} finally {
			setAuthorization(old);
		}
	}
	
	public <E> E call(Callable<E> task) throws Exception {
		Authorization old = getAuthorization();
		try {
			setAuthorization(this);
			return task.call();
		} finally {
			setAuthorization(old);
		}
	}
	
	public void run(Runnable task) {
		Authorization old = getAuthorization();
		try {
			setAuthorization(this);
			task.run();
		} finally {
			setAuthorization(old);
		}
	}
	
	public <E, T extends Exception> LimitedCallable<E, T> limitedCallable(LimitedCallable<E, T> task) {
		return (() -> limitedCall(task));
	}
	
	public <E> Callable<E> callable(Callable<E> task) {
		return (() -> call(task));
	}
	
	public Runnable runnable(Runnable task) {
		return (() -> run(task));
	}
	
	public SujavaBot getBot() {
		return bot;
	}
	
	public AuthorizedUser getUser() {
		return user;
	}
	
	public List<AuthorizedGroup> getGroups() {
		return groups;
	}
	
	public List<AuthorizedGroup> getOwnedGroups() {
		return ownedGroups;
	}
	
	public boolean isRootOwner() {
		return isOwner(getBot().getRootGroup());
	}
	
	public boolean isOwner(AuthorizedUser u) {
		if(u == null || getUser() == null)
			return false;
		return getUser().equals(u) || isRootOwner();
	}
	
	public boolean isOwner(AuthorizedGroup g) {
		if(g == null)
			return false;
		List<AuthorizedGroup> ownables = new ArrayList<>();
		ownables.add(g);
		ownables.addAll(g.getAllParents());
		for(AuthorizedGroup gg : ownables) {
			Matcher m = AuthorizedGroup.USER_OWNED.matcher(gg.getName());
			if(m.find()) {
				String groupOwner = m.group(1);
				if(getUser().getName().equals(groupOwner))
					return true;
			}
		}
		return !Collections.disjoint(ownables, getOwnedGroups());
	}
}
