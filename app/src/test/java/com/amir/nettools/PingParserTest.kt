package com.amir.nettools

import com.amir.nettools.network.PingParser
import com.amir.nettools.network.TraceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingParserTest {
    private val ok = """
        PING example.com (93.184.216.34) 56(84) bytes of data.
        64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=12.3 ms
        64 bytes from 93.184.216.34: icmp_seq=2 ttl=56 time=11.1 ms
        64 bytes from 93.184.216.34: icmp_seq=3 ttl=56 time=13.5 ms
        64 bytes from 93.184.216.34: icmp_seq=4 ttl=56 time=12 ms

        --- example.com ping statistics ---
        4 packets transmitted, 4 received, 0% packet loss, time 3004ms
        rtt min/avg/max/mdev = 11.1/12.2/13.5/0.5 ms
    """.trimIndent().lines()

    @Test fun parsesSuccessfulRun() {
        val p = PingParser.parse(ok)
        assertEquals(4, p.transmitted)
        assertEquals(4, p.received)
        assertEquals(listOf(12.3, 11.1, 13.5, 12.0), p.rtts)
        assertNull(p.error)
    }

    @Test fun parsesTotalLoss() {
        val p = PingParser.parse(
            listOf(
                "PING 10.255.255.1 (10.255.255.1) 56(84) bytes of data.",
                "--- 10.255.255.1 ping statistics ---",
                "3 packets transmitted, 0 received, 100% packet loss, time 2000ms"
            )
        )
        assertEquals(3, p.transmitted)
        assertEquals(0, p.received)
        assertTrue(p.rtts.isEmpty())
    }

    @Test fun parsesPartialLossWithErrors() {
        val p = PingParser.parse(listOf("4 packets transmitted, 2 received, +2 errors, 50% packet loss, time 3003ms"))
        assertEquals(4, p.transmitted)
        assertEquals(2, p.received)
    }

    @Test fun detectsErrors() {
        assertNotNull(PingParser.parse(listOf("ping: unknown host foo.invalid")).error)
        assertNotNull(PingParser.parse(listOf("connect: Network is unreachable")).error)
        assertNull(PingParser.parse(ok).error)
    }

    @Test fun statistics() {
        val (min, avg, max) = PingParser.stats(listOf(10.0, 20.0, 30.0))
        assertEquals(10.0, min!!, 0.0001)
        assertEquals(20.0, avg!!, 0.0001)
        assertEquals(30.0, max!!, 0.0001)
        val empty = PingParser.stats(emptyList())
        assertNull(empty.first)
    }

    // ---- traceroute reply parsing ----
    @Test fun traceIntermediateHops() {
        val a = TraceParser.parseHop(listOf("From 192.168.1.1: icmp_seq=1 Time to live exceeded"))
        assertEquals("192.168.1.1", a.ip)
        assertFalse(a.reached)
        assertEquals("Time to live exceeded", a.note)

        val b = TraceParser.parseHop(listOf("From 10.0.0.1 icmp_seq=1 Time to live exceeded"))
        assertEquals("10.0.0.1", b.ip)

        val c = TraceParser.parseHop(listOf("From router.lan (10.1.1.1) icmp_seq=1 Time to live exceeded"))
        assertEquals("10.1.1.1", c.ip)

        val d = TraceParser.parseHop(listOf("From 2001:db8::1 icmp_seq=1 Time to live exceeded"))
        assertEquals("2001:db8::1", d.ip)
    }

    @Test fun traceDestinationReached() {
        val r = TraceParser.parseHop(listOf("64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=12.3 ms"))
        assertTrue(r.reached)
        assertEquals("93.184.216.34", r.ip)
        assertEquals(12.3, r.rttMs!!, 0.0001)

        val v6 = TraceParser.parseHop(listOf("64 bytes from 2606:4700::1111: icmp_seq=1 ttl=57 time=4.2 ms"))
        assertEquals("2606:4700::1111", v6.ip)
    }

    @Test fun traceTimeoutAndUnsupported() {
        val t = TraceParser.parseHop(listOf("PING 1.2.3.4 (1.2.3.4) 56(84) bytes of data.", "", "--- ping statistics ---"))
        assertNull(t.ip)
        assertFalse(t.reached)
        assertFalse(t.unsupported)
        assertTrue(TraceParser.parseHop(listOf("ping: invalid option -- 't'")).unsupported)
    }
}
