package org.sujavabot.plugin.urlhandler;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Check whether an {@link InetAddress} is a reserved address.
 * 
 * Based on list from https://en.wikipedia.org/wiki/Reserved_IP_addresses
 * @author Robin
 *
 */
public class ReservedAddresses {
	private static class Reservation {
		public final BigInteger bits;
		public final int mask;
		public Reservation(int mask, byte[] bits) {
			this.mask = mask;
			this.bits = new BigInteger(1, bits);
		}
	}
	
	private static Reservation range(String netmask) {
		String[] f = netmask.split("/");
		int mask = Integer.parseInt(f[1]);
		try {
			return new Reservation(mask, InetAddress.getByName(f[0]).getAddress());
		} catch(UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final Reservation[] RESERVED_IPV4;
	private static final Reservation[] RESERVED_IPV6;
	
	private static boolean isReserved(Inet4Address addr) {
		BigInteger ai = new BigInteger(1, addr.getAddress());
		for(Reservation r : RESERVED_IPV4) {
			int shift = 32 - r.mask;
			if(ai.shiftRight(shift).equals(r.bits.shiftRight(shift)))
				return true;
		}
		return false;
	}
	
	public static boolean isReserved(InetAddress addr) {
		return 
				((addr instanceof Inet4Address) && isReserved((Inet4Address) addr))
				|| ((addr instanceof Inet6Address) && isReserved((Inet6Address) addr));
	}
	
	private static boolean isReserved(Inet6Address addr) {
		BigInteger ai = new BigInteger(1, addr.getAddress());
		for(Reservation r : RESERVED_IPV4) {
			int shift = 128 - r.mask;
			if(ai.shiftRight(shift).equals(r.bits.shiftRight(shift)))
				return true;
		}
		return false;
	}
	
	static {
		String[] v4 = {
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
		};
		String[] v6 = {
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
		RESERVED_IPV4 = new Reservation[v4.length];
		for(int i = 0; i < v4.length; i++)
			RESERVED_IPV4[i] = range(v4[i]);
		RESERVED_IPV6 = new Reservation[v6.length];
		for(int i = 0; i < v6.length; i++)
			RESERVED_IPV6[i] = range(v6[i]);
	}
}
