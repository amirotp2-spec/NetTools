package com.amir.nettools

import com.amir.nettools.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun ipv4Valid() {
        listOf("192.168.1.1", "0.0.0.0", "255.255.255.255", "8.8.8.8").forEach { assertTrue(it, Validators.isValidIpv4(it)) }
    }

    @Test fun ipv4Invalid() {
        listOf("256.1.1.1", "1.2.3", "1.2.3.4.5", "01.2.3.4", "a.b.c.d", "", "1.2.3.4 ", "1..2.3").forEach {
            assertFalse(it, Validators.isValidIpv4(it))
        }
    }

    @Test fun ipv6Valid() {
        listOf("::1", "::", "2001:db8::1", "fe80::1", "::ffff:192.168.1.1", "1:2:3:4:5:6:7:8", "1::", "1:2:3:4:5:6:7::")
            .forEach { assertTrue(it, Validators.isValidIpv6(it)) }
    }

    @Test fun ipv6Invalid() {
        listOf("1:2:3:4:5:6:7", "1::2::3", "12345::1", "g::1", "", "::1%eth0", ":::", "1:2:3:4:5:6:7:8:9", ":1:2:3:4:5:6:7", "1.2.3.4")
            .forEach { assertFalse(it, Validators.isValidIpv6(it)) }
    }

    @Test fun hostnames() {
        listOf("example.com", "localhost", "sub.example.co.uk", "example.com.", "_dmarc.example.com", "a-b.com")
            .forEach { assertTrue(it, Validators.isValidHostname(it)) }
        listOf("-bad.com", "a..b", "exa mple.com", "", "999.999.1.1", "a".repeat(64) + ".com", "bad-.com", "ex;rm -rf.com")
            .forEach { assertFalse(it, Validators.isValidHostname(it)) }
    }

    @Test fun hostCombined() {
        assertTrue(Validators.isValidHost("1.1.1.1"))
        assertTrue(Validators.isValidHost("2001:db8::1"))
        assertTrue(Validators.isValidHost("example.com"))
        assertFalse(Validators.isValidHost("999.1.1.1"))
        assertFalse(Validators.isValidHost("-c 100 example.com"))
    }

    @Test fun ports() {
        assertEquals(443, Validators.parsePort("443"))
        assertEquals(80, Validators.parsePort(" 80 "))
        assertEquals(65535, Validators.parsePort("65535"))
        assertEquals(1, Validators.parsePort("1"))
        listOf("0", "65536", "abc", "", "-1", "+80", "80.5", "100000").forEach { assertNull(it, Validators.parsePort(it)) }
    }

    @Test fun packetCount() {
        assertEquals(4, Validators.parseCount("", 4, 20))
        assertEquals(10, Validators.parseCount("10", 4, 20))
        assertNull(Validators.parseCount("0", 4, 20))
        assertNull(Validators.parseCount("21", 4, 20))
        assertNull(Validators.parseCount("abc", 4, 20))
        assertNull(Validators.parseCount("-3", 4, 20))
    }

    @Test fun urls() {
        assertEquals("https://example.com", Validators.normalizeUrl("example.com"))
        assertEquals("http://example.com/path?q=1", Validators.normalizeUrl("http://example.com/path?q=1"))
        assertEquals("https://[2001:db8::1]/", Validators.normalizeUrl("https://[2001:db8::1]/"))
        assertNull(Validators.normalizeUrl(""))
        assertNull(Validators.normalizeUrl("ftp://example.com"))
        assertNull(Validators.normalizeUrl("https://exa mple.com"))
        assertNull(Validators.normalizeUrl("https://example.com:99999"))
        assertNull(Validators.normalizeUrl("javascript:alert(1)"))
    }
}
