package com.amir.nettools.network

import com.amir.nettools.domain.model.DnsRecord
import com.amir.nettools.domain.model.DnsResult
import com.amir.nettools.domain.model.DnsType
import com.amir.nettools.domain.model.NxDomainException
import com.amir.nettools.domain.model.ToolException
import com.amir.nettools.util.Errs
import com.amir.nettools.util.IpClassifier
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.IDN
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.random.Random

class DnsParseException(message: String) : Exception(message)

/** Minimal RFC 1035 wire-format encoder/decoder. Pure Kotlin/JVM so it is unit-testable. */
object DnsWire {
    data class Parsed(val id: Int, val rcode: Int, val truncated: Boolean, val answers: List<DnsRecord>)

    fun buildQuery(id: Int, name: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(id shr 8)
        out.write(id and 0xFF)
        out.write(0x01) // flags: recursion desired
        out.write(0x00)
        out.write(0)
        out.write(1) // QDCOUNT
        repeat(6) { out.write(0) } // AN/NS/AR counts
        for (label in IDN.toASCII(name).trimEnd('.').split('.')) {
            val b = label.toByteArray(Charsets.US_ASCII)
            require(b.isNotEmpty() && b.size <= 63) { "bad label" }
            out.write(b.size)
            out.write(b, 0, b.size)
        }
        out.write(0)
        out.write(type shr 8)
        out.write(type and 0xFF)
        out.write(0)
        out.write(1) // class IN
        return out.toByteArray()
    }

    fun readName(d: ByteArray, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var pos = start
        var end = -1
        var jumps = 0
        while (true) {
            if (pos >= d.size) throw DnsParseException("truncated name")
            val len = d[pos].toInt() and 0xFF
            if (len == 0) {
                pos++
                if (end < 0) end = pos
                break
            } else if ((len and 0xC0) == 0xC0) {
                if (pos + 1 >= d.size) throw DnsParseException("truncated pointer")
                val ptr = ((len and 0x3F) shl 8) or (d[pos + 1].toInt() and 0xFF)
                if (end < 0) end = pos + 2
                jumps++
                if (jumps > 64) throw DnsParseException("pointer loop")
                pos = ptr
            } else if ((len and 0xC0) != 0) {
                throw DnsParseException("bad label type")
            } else {
                if (pos + 1 + len > d.size) throw DnsParseException("truncated label")
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(String(d, pos + 1, len, Charsets.ISO_8859_1))
                pos += 1 + len
            }
        }
        return sb.toString() to end
    }

    fun parse(d: ByteArray): Parsed {
        if (d.size < 12) throw DnsParseException("short message")
        fun u16(p: Int): Int {
            if (p + 2 > d.size) throw DnsParseException("truncated")
            return ((d[p].toInt() and 0xFF) shl 8) or (d[p + 1].toInt() and 0xFF)
        }
        fun u32(p: Int): Long = (u16(p).toLong() shl 16) or u16(p + 2).toLong()

        val id = u16(0)
        val flags = u16(2)
        val qd = u16(4)
        val an = u16(6)
        var pos = 12
        repeat(qd) {
            pos = readName(d, pos).second + 4
            if (pos > d.size) throw DnsParseException("truncated question")
        }
        val answers = ArrayList<DnsRecord>()
        repeat(an) {
            pos = readName(d, pos).second
            val type = u16(pos)
            val ttl = u32(pos + 4)
            val rdlen = u16(pos + 8)
            val rd = pos + 10
            if (rd + rdlen > d.size) throw DnsParseException("truncated rdata")
            pos = rd + rdlen
            val rec: DnsRecord? = when (type) {
                1 -> if (rdlen == 4) {
                    DnsRecord(
                        "A",
                        "${d[rd].toInt() and 0xFF}.${d[rd + 1].toInt() and 0xFF}.${d[rd + 2].toInt() and 0xFF}.${d[rd + 3].toInt() and 0xFF}",
                        ttl
                    )
                } else null
                28 -> if (rdlen == 16) DnsRecord("AAAA", IpClassifier.ipv6Compress(d.copyOfRange(rd, rd + 16)), ttl) else null
                2 -> DnsRecord("NS", readName(d, rd).first, ttl)
                5 -> DnsRecord("CNAME", readName(d, rd).first, ttl)
                15 -> if (rdlen >= 3) DnsRecord("MX", "${u16(rd)} ${readName(d, rd + 2).first}", ttl) else null
                16 -> {
                    val sb = StringBuilder()
                    var p = rd
                    val end = rd + rdlen
                    while (p < end) {
                        val l = d[p].toInt() and 0xFF
                        p++
                        if (p + l > end) throw DnsParseException("bad txt")
                        sb.append(String(d, p, l, Charsets.UTF_8))
                        p += l
                    }
                    DnsRecord("TXT", sb.toString(), ttl)
                }
                else -> null
            }
            if (rec != null) answers += rec
        }
        return Parsed(id, flags and 0x0F, (flags and 0x0200) != 0, answers)
    }
}

class DnsManager(private val systemServers: () -> List<InetAddress>) {

    private fun candidateServers(): List<InetAddress> {
        val list = ArrayList<InetAddress>()
        try {
            list += systemServers().filter { !it.isAnyLocalAddress }
        } catch (_: Exception) {
        }
        list += InetAddress.getByName("1.1.1.1")
        list += InetAddress.getByName("8.8.8.8")
        return list.distinctBy { it.hostAddress }.take(4)
    }

    suspend fun lookup(hostInput: String, types: List<DnsType>): DnsResult = withContext(Dispatchers.IO) {
        val host = hostInput.trim().trimEnd('.')
        if (!Validators.isValidHostname(host)) throw Errs.invalidHost()
        val servers = candidateServers()
        val t0 = System.nanoTime()
        val records = ArrayList<DnsRecord>()
        var usedServer: String? = null
        var lastError: ToolException? = null

        for (type in types) {
            ensureActive()
            try {
                val (parsed, server) = query(host, type, servers)
                records += parsed.answers
                usedServer = server
            } catch (e: NxDomainException) {
                throw e
            } catch (e: ToolException) {
                lastError = e
            }
        }
        val failure = lastError
        if (usedServer == null && failure != null) throw failure
        DnsResult(host, usedServer, records.distinct(), (System.nanoTime() - t0) / 1_000_000)
    }

    private fun query(host: String, type: DnsType, servers: List<InetAddress>): Pair<DnsWire.Parsed, String> {
        var lastErr: Exception? = null
        for (srv in servers) {
            try {
                val id = Random.nextInt(0x10000)
                val q = DnsWire.buildQuery(id, host, type.code)
                var parsed = DnsWire.parse(udp(srv, q))
                if (parsed.id != id) continue
                if (parsed.truncated) {
                    parsed = DnsWire.parse(tcp(srv, q))
                    if (parsed.id != id) continue
                }
                when (parsed.rcode) {
                    0 -> return parsed to (srv.hostAddress ?: "")
                    3 -> throw Errs.nxDomain(host)
                    else -> lastErr = IOException("rcode ${parsed.rcode}")
                }
            } catch (e: NxDomainException) {
                throw e
            } catch (e: IOException) {
                lastErr = e
            } catch (e: DnsParseException) {
                lastErr = e
            }
        }
        throw if (lastErr is SocketTimeoutException) Errs.dnsTimeout() else Errs.dnsFailed()
    }

    private fun udp(server: InetAddress, query: ByteArray): ByteArray {
        DatagramSocket().use { s ->
            s.soTimeout = 3000
            s.send(DatagramPacket(query, query.size, server, 53))
            val buf = ByteArray(4096)
            val p = DatagramPacket(buf, buf.size)
            s.receive(p)
            return buf.copyOf(p.length)
        }
    }

    private fun tcp(server: InetAddress, query: ByteArray): ByteArray {
        Socket().use { s ->
            s.soTimeout = 3000
            s.connect(InetSocketAddress(server, 53), 3000)
            val out = DataOutputStream(s.getOutputStream())
            out.writeShort(query.size)
            out.write(query)
            out.flush()
            val inp = DataInputStream(s.getInputStream())
            val len = inp.readUnsignedShort()
            val buf = ByteArray(len)
            inp.readFully(buf)
            return buf
        }
    }
}
