package com.amir.nettools.network

import com.amir.nettools.domain.model.TraceHop
import com.amir.nettools.domain.model.TraceResult
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Resolver
import com.amir.nettools.util.ReverseDns
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object TraceParser {
    data class Reply(
        val ip: String?,
        val reached: Boolean,
        val rttMs: Double?,
        val note: String?,
        val unsupported: Boolean
    )

    private val bytesFrom = Regex(
        """(?i)bytes from\s+(?:\S+\s+\(([0-9A-Fa-f:.]+)\)|([0-9A-Fa-f:.]+?)):?\s+icmp_seq"""
    )
    private val fromRegex = Regex(
        """(?i)\bFrom\s+(?:\S+\s+\(([0-9A-Fa-f:.]+)\)|([0-9A-Fa-f:.]+?)):?\s+icmp_seq=\d+\s*(.*)"""
    )
    private val rttRegex = Regex("""time[=<]\s*([0-9]+(?:\.[0-9]+)?)\s*ms""")

    fun parseHop(lines: List<String>): Reply {
        var unsupported = false
        var intermediate: Reply? = null
        for (raw in lines) {
            val line = raw.trim()
            val b = bytesFrom.find(line)
            if (b != null) {
                val ip = b.groupValues[1].ifEmpty { b.groupValues[2] }
                val rtt = rttRegex.find(line)?.groupValues?.get(1)?.toDouble()
                return Reply(ip, true, rtt, null, false)
            }
            val f = fromRegex.find(line)
            if (f != null && intermediate == null) {
                val ip = f.groupValues[1].ifEmpty { f.groupValues[2] }
                intermediate = Reply(ip, false, null, f.groupValues[3].trim().ifEmpty { null }, false)
            }
            val lower = line.lowercase()
            if (lower.contains("invalid option") || lower.contains("unrecognized option") || lower.startsWith("usage")) {
                unsupported = true
            }
        }
        return intermediate ?: Reply(null, false, null, null, unsupported)
    }
}

class TraceRouteManager {
    companion object {
        const val MAX_HOPS = 30
        const val MAX_CONSECUTIVE_TIMEOUTS = 8
    }

    suspend fun trace(hostInput: String, onHop: (TraceHop) -> Unit): TraceResult {
        val host = hostInput.trim()
        if (!Validators.isValidHost(host)) throw Errs.invalidHost()
        val addr = withContext(Dispatchers.IO) { Resolver.resolve(host) }
        val destIp = Resolver.ip(addr)

        val hops = mutableListOf<TraceHop>()
        var reached = false
        var timeouts = 0

        for (ttl in 1..MAX_HOPS) {
            currentCoroutineContext().ensureActive()
            val start = System.nanoTime()
            val lines = ProcessRunner.run(ProcessRunner.pingCommand(addr, 1, ttl, 2), 6000L)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
            val reply = TraceParser.parseHop(lines)
            if (ttl == 1 && reply.ip == null && reply.unsupported) throw Errs.traceUnsupported()

            val hop = if (reply.ip == null) {
                timeouts++
                TraceHop(ttl, null, null, null, false, "timeout")
            } else {
                timeouts = 0
                val name = withContext(Dispatchers.IO) { ReverseDns.lookup(reply.ip, 1500) }
                val latency = reply.rttMs ?: elapsedMs
                TraceHop(ttl, reply.ip, name, latency, reply.rttMs == null, reply.note)
            }
            hops += hop
            onHop(hop)
            if (reply.reached) {
                reached = true
                break
            }
            if (timeouts >= MAX_CONSECUTIVE_TIMEOUTS) break
        }
        return TraceResult(host, destIp, hops, reached)
    }
}
