package com.amir.nettools

import com.amir.nettools.calculator.SubnetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class SubnetCalculatorTest {
    @Test fun slash24() {
        val r = SubnetCalculator.calculate("192.168.1.10", 24)
        assertEquals("192.168.1.0", r.networkAddress)
        assertEquals("192.168.1.255", r.broadcastAddress)
        assertEquals("255.255.255.0", r.subnetMask)
        assertEquals("0.0.0.255", r.wildcardMask)
        assertEquals("192.168.1.1", r.firstUsable)
        assertEquals("192.168.1.254", r.lastUsable)
        assertEquals(254L, r.usableHosts)
        assertEquals(24, r.cidr)
    }

    @Test fun slash8() {
        val r = SubnetCalculator.calculate("10.20.30.40", 8)
        assertEquals("10.0.0.0", r.networkAddress)
        assertEquals("10.255.255.255", r.broadcastAddress)
        assertEquals("255.0.0.0", r.subnetMask)
        assertEquals(16_777_214L, r.usableHosts)
    }

    @Test fun slash30() {
        val r = SubnetCalculator.calculate("172.16.5.5", 30)
        assertEquals("172.16.5.4", r.networkAddress)
        assertEquals("172.16.5.7", r.broadcastAddress)
        assertEquals("172.16.5.5", r.firstUsable)
        assertEquals("172.16.5.6", r.lastUsable)
        assertEquals(2L, r.usableHosts)
    }

    @Test fun slash31() {
        val r = SubnetCalculator.calculate("192.168.0.1", 31)
        assertEquals("192.168.0.0", r.firstUsable)
        assertEquals("192.168.0.1", r.lastUsable)
        assertEquals(2L, r.usableHosts)
    }

    @Test fun slash32() {
        val r = SubnetCalculator.calculate("8.8.8.8", 32)
        assertEquals("8.8.8.8", r.networkAddress)
        assertEquals("8.8.8.8", r.broadcastAddress)
        assertEquals("8.8.8.8", r.firstUsable)
        assertEquals("8.8.8.8", r.lastUsable)
        assertEquals("255.255.255.255", r.subnetMask)
        assertEquals(1L, r.usableHosts)
    }

    @Test fun slash0() {
        val r = SubnetCalculator.calculate("1.2.3.4", 0)
        assertEquals("0.0.0.0", r.networkAddress)
        assertEquals("255.255.255.255", r.broadcastAddress)
        assertEquals("0.0.0.0", r.subnetMask)
        assertEquals("255.255.255.255", r.wildcardMask)
        assertEquals(4_294_967_294L, r.usableHosts)
    }

    @Test fun slash20() {
        val r = SubnetCalculator.calculate("10.1.17.9", 20)
        assertEquals("10.1.16.0", r.networkAddress)
        assertEquals("10.1.31.255", r.broadcastAddress)
        assertEquals("255.255.240.0", r.subnetMask)
        assertEquals(4094L, r.usableHosts)
    }

    @Test fun invalidInputs() {
        listOf("300.1.1.1" to 24, "1.2.3.4" to 33, "1.2.3.4" to -1, "abc" to 8).forEach { (ip, c) ->
            try {
                SubnetCalculator.calculate(ip, c)
                fail("expected failure for $ip/$c")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test fun parse() {
        assertEquals("192.168.1.10" to 24, SubnetCalculator.parse("192.168.1.10/24"))
        assertEquals("10.0.0.1" to 8, SubnetCalculator.parse(" 10.0.0.1 / 8 "))
        assertNull(SubnetCalculator.parse("192.168.1.10"))
        assertNull(SubnetCalculator.parse("192.168.1.10/x"))
        assertNull(SubnetCalculator.parse("1/2/3"))
    }
}
