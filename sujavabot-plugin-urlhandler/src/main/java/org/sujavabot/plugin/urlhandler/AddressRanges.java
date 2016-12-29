package org.sujavabot.plugin.urlhandler;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Check whether an {@link InetAddress} is a reserved address.
 * 
 * Based on list from https://en.wikipedia.org/wiki/Reserved_IP_addresses
 * @author Robin
 *
 */
public class AddressRanges {
	public static class AddressRange {
		private final InetAddress addr;
		private final int mask;
		private AddressRange(InetAddress addr, int mask) {
			this.addr = addr;
			this.mask = mask;
		}
		@Override
		public String toString() {
			String a = addr.toString();
			return a.substring(a.indexOf('/') + 1) + "/" + mask;
		}
	}
	
	public static AddressRange parseAddressRange(String netmask) {
		String[] f = netmask.split("/");
		int mask = Integer.parseInt(f[1]);
		try {
			InetAddress addr = InetAddress.getByName(f[0]);
			return new AddressRange(addr, mask);
		} catch(UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final List<AddressRange> RESERVED;
	
	public static boolean matches(InetAddress addr, Iterable<AddressRange> reserved) {
		int size = addr.getAddress().length * 8;
		BigInteger ai = new BigInteger(1, addr.getAddress());
		for(AddressRange r : reserved) {
			if(!addr.getClass().equals(r.addr.getClass()))
				continue;
			int shift = size - r.mask;
			BigInteger ri = new BigInteger(1, r.addr.getAddress());
			if(ai.shiftRight(shift).equals(ri.shiftRight(shift)))
				return true;
		}
		return false;
	}
	
	public static boolean isReserved(InetAddress addr) {
		return matches(addr, RESERVED);
	}
	
	static {
		String[] ranges = {
				"0.0.0.0/8",
				"10.0.0.0/8",
				"100.64.0.0/10",
				"127.0.0.0/8",
				"169.254.0.0/16",
				"172.16.0.0/12",
				"192.0.0.0/24",
				"192.0.2.0/24",
				"192.88.99.0/24",
				"192.168.0.0/16",
				"198.18.0.0/15",
				"198.51.100.0/24",
				"203.0.113.0/24",
				"224.0.0.0/4",
				"240.0.0.0/4",
				"255.255.255.255/32",
				"::/128",
				"::1/128",
				"::ffff:0:0/96",
				"100::/64",
				"64:ff9b::/96",
				"2001::/32",
				"2001:10::/28",
				"2001:20::/28",
				"2001:db8::/32",
				"2002::/16",
				"fc00::/7",
				"fe80::/10",
				"ff00::/8",
		};
		List<AddressRange> rr = new ArrayList<>();
		for(String r : ranges) {
			rr.add(parseAddressRange(r));
		}
		RESERVED = Collections.unmodifiableList(rr);
	}
}
