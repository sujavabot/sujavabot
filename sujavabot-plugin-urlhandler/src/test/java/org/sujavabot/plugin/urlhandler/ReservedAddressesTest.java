package org.sujavabot.plugin.urlhandler;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

public class ReservedAddressesTest {
	@Test
	public void testLocalhostReserved() throws Exception {
		Assert.assertTrue(ReservedAddresses.isReserved(InetAddress.getByName("127.0.0.1")));
		Assert.assertTrue(ReservedAddresses.isReserved(InetAddress.getByName("::1")));
	}
}
