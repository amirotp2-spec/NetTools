package com.amir.nettools

import com.amir.nettools.network.DnsParseException
import com.amir.nettools.network.DnsWire
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DnsWireTest {
    private fun nameBytes(vararg labels: String): List<Int> =
        labels.flatMap { l -> listOf(l.length) + l.map { it.code } } + 0

    private fun header(id: Int, flags: Int, an: Int): List<Int> =
        listOf(id shr 8, id and 0xFF, flags shr 8, flags and 0xFF, 0, 1, an shr 8, an and 0xFF, 0, 0, 0, 0)

    private fun response(flags: Int, an: Int, answers: List<Int>): ByteArray {
        val all = header(0x1234, flags, an) + nameBytes("example", "com") + listOf(0, 1, 0, 1) + answers
        return ByteArray(all.size) { all[it].toByte() }
    }

    /** Answer with a compressed name (pointer to the question name at offset 12). */
    private fun answer(type: Int, ttl: Int, rdata: List<Int>): List<Int> =
        listOf(0xC0, 0x0C, type shr 8, type and 0xFF, 0, 1) +
            listOf(ttl shr 24, (ttl shr 16) and 0xFF, (ttl shr 8) and 0xFF, ttl and 0xFF) +
            listOf(rdata.size shr 8, rdata.size and 0xFF) + rdata

    @Test fun buildQueryLayout() {
        val q = DnsWire.buildQuery(0x1234, "example.com", 1)
        assertEquals(29, q.size)
        assertEquals(0x12, q[0].toInt())
        assertEquals(0x34, q[1].toInt())
        assertEquals(1, q[2].toInt()) // RD flag
        assertEquals(1, q[5].toInt()) // QDCOUNT
        assertEquals(7, q[12].toInt())
        assertEquals('e'.code, q[13].toInt())
        assertEquals(0, q[24].toInt())
        assertEquals(1, q[26].toInt()) // type A
        assertEquals(1, q[28].toInt()) // class IN
    }

    @Test fun parseA() {
        val p = DnsWire.parse(response(0x8180, 1, answer(1, 300, listOf(93, 184, 216, 34))))
        assertEquals(0x1234, p.id)
        assertEquals(0, p.rcode)
        assertFalse(p.truncated)
        assertEquals(1, p.answers.size)
        assertEquals("A", p.answers[0].type)
        assertEquals("93.184.216.34", p.answers[0].value)
        assertEquals(300L, p.answers[0].ttl)
    }

    @Test fun parseAaaaCompressed() {
        val rdata = listOf(0x20, 0x01, 0x0d, 0xb8) + List(11) { 0 } + listOf(1)
        val p = DnsWire.parse(response(0x8180, 1, answer(28, 60, rdata)))
        assertEquals("AAAA", p.answers[0].type)
        assertEquals("2001:db8::1", p.answers[0].value)
        assertEquals(60L, p.answers[0].ttl)
    }

    @Test fun parseMxWithPointer() {
        val rdata = listOf(0, 10, 4, 'm'.code, 'a'.code, 'i'.code, 'l'.code, 0xC0, 0x0C)
        val p = DnsWire.parse(response(0x8180, 1, answer(15, 3600, rdata)))
        assertEquals("MX", p.answers[0].type)
        assertEquals("10 mail.example.com", p.answers[0].value)
    }

    @Test fun parseCnameAndNs() {
        val ns = nameBytes("ns1", "example", "net")
        val p = DnsWire.parse(
            response(0x8180, 2, answer(5, 10, listOf(3, 'w'.code, 'w'.code, 'w'.code, 0xC0, 0x0C)) + answer(2, 20, ns))
        )
        assertEquals("CNAME", p.answers[0].type)
        assertEquals("www.example.com", p.answers[0].value)
        assertEquals("NS", p.answers[1].type)
        assertEquals("ns1.example.net", p.answers[1].value)
    }

    @Test fun parseTxtJoinsStrings() {
        val rdata = listOf(5) + "hello".map { it.code } + listOf(3) + "abc".map { it.code }
        val p = DnsWire.parse(response(0x8180, 1, answer(16, 5, rdata)))
        assertEquals("TXT", p.answers[0].type)
        assertEquals("helloabc", p.answers[0].value)
    }

    @Test fun flagsAndRcode() {
        assertTrue(DnsWire.parse(response(0x8380, 0, emptyList())).truncated)
        assertEquals(3, DnsWire.parse(response(0x8183, 0, emptyList())).rcode)
    }

    @Test fun malformedMessagesThrow() {
        try {
            DnsWire.parse(ByteArray(5))
            fail("short message must fail")
        } catch (_: DnsParseException) {
        }
        // Question name is a pointer to itself -> loop must be detected.
        val loop = header(1, 0x8180, 0) + listOf(0xC0, 0x0C, 0, 1, 0, 1)
        try {
            DnsWire.parse(ByteArray(loop.size) { loop[it].toByte() })
            fail("pointer loop must fail")
        } catch (_: DnsParseException) {
        }
        // Declared rdata longer than the message.
        val bad = response(0x8180, 1, listOf(0xC0, 0x0C, 0, 1, 0, 1, 0, 0, 0, 1, 0, 50, 1, 2))
        try {
            DnsWire.parse(bad)
            fail("truncated rdata must fail")
        } catch (_: DnsParseException) {
        }
    }
}
