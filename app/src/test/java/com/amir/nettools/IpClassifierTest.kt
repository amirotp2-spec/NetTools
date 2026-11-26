package com.amir.nettools

import com.amir.nettools.util.IpClassifier
import com.amir.nettools.util.IpScope
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IpClassifierTest {
    @Test fun ipv4Scopes() {
        assertEquals(IpScope.PRIVATE, IpClassifier.scopeOfIpv4("192.168.1.1"))
        assertEquals(IpScope.PRIVATE, IpClassifier.scopeOfIpv4("10.1.2.3"))
        assertEquals(IpScope.PRIVATE, IpClassifier.scopeOfIpv4("172.16.0.1"))
        assertEquals(IpScope.PUBLIC, IpClassifier.scopeOfIpv4("172.32.0.1"))
        assertEquals(IpScope.PUBLIC, IpClassifier.scopeOfIpv4("8.8.8.8"))
        assertEquals(IpScope.CGNAT, IpClassifier.scopeOfIpv4("100.64.0.1"))
        assertEquals(IpScope.LOOPBACK, IpClassifier.scopeOfIpv4("127.0.0.1"))
        assertEquals(IpScope.LINK_LOCAL, IpClassifier.scopeOfIpv4("169.254.10.10"))
        assertEquals(IpScope.MULTICAST, IpClassifier.scopeOfIpv4("224.0.0.1"))
        assertEquals(IpScope.DOCUMENTATION, IpClassifier.scopeOfIpv4("203.0.113.5"))
        assertEquals(IpScope.BROADCAST, IpClassifier.scopeOfIpv4("255.255.255.255"))
        assertNull(IpClassifier.scopeOfIpv4("300.1.1.1"))
    }

    @Test fun ipv6Scopes() {
        fun scope(s: String) = IpClassifier.scopeOfIpv6(IpClassifier.parseIpv6(s)!!)
        assertEquals(IpScope.LOOPBACK, scope("::1"))
        assertEquals(IpScope.UNSPECIFIED, scope("::"))
        assertEquals(IpScope.LINK_LOCAL, scope("fe80::1"))
        assertEquals(IpScope.UNIQUE_LOCAL, scope("fd12:3456::1"))
        assertEquals(IpScope.MULTICAST, scope("ff02::1"))
        assertEquals(IpScope.DOCUMENTATION, scope("2001:db8::1"))
        assertEquals(IpScope.IPV4_MAPPED, scope("::ffff:192.168.1.1"))
        assertEquals(IpScope.PUBLIC, scope("2606:4700:4700::1111"))
    }

    @Test fun ipv6ParseAndFormat() {
        val b = IpClassifier.parseIpv6("2001:db8::1")!!
        assertEquals(16, b.size)
        assertEquals("2001:db8::1", IpClassifier.ipv6Compress(b))
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001", IpClassifier.ipv6Expanded(b))
        assertEquals("::", IpClassifier.ipv6Compress(ByteArray(16)))
        assertEquals("::1", IpClassifier.ipv6Compress(IpClassifier.parseIpv6("::1")!!))
        assertEquals("1::", IpClassifier.ipv6Compress(IpClassifier.parseIpv6("1::")!!))
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 2, 3, 4),
            IpClassifier.parseIpv6("::ffff:1.2.3.4")
        )
        assertNull(IpClassifier.parseIpv6("not-an-ip"))
    }

    @Test fun conversionsAndPointers() {
        assertEquals(3232235786L, IpClassifier.ipv4ToLong("192.168.1.10"))
        assertEquals("192.168.1.10", IpClassifier.longToIpv4(3232235786L))
        assertEquals("4.3.2.1.in-addr.arpa", IpClassifier.reversePointer("1.2.3.4"))
        assertEquals(
            "1" + ".0".repeat(23) + ".8.b.d.0.1.0.0.2.ip6.arpa",
            IpClassifier.reversePointer("2001:db8::1")
        )
    }
}
