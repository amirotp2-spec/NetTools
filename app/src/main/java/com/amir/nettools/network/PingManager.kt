package com.amir.nettools.network

import com.amir.nettools.domain.model.PingResult
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Resolver
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PingParser {
    data class Parsed(val transmitted: Int?, val received: Int?, val rtts: List<Double>, val error: String?)

    private val rttRegex = Regex("""time[=<]\s*([0-9]+(?:\.[0-9]+)?)\s*ms""")
    private val summaryRegex = Regex("""(\d+)\s+packets transmitted,\s+(\d+)\s+(?:packets )?received""")
    private val errorKeywords = listOf(
        "unknown host", "network is unreachable", "operation not permitted",
        "bad address", "name or service not known", "permission denied"
    )

    fun parse(lines: List<String>): Parsed {
        var tx: Int? = null
        var rx: Int? = null
        val rtts = mutableListOf<Double>()
        var err: String? = null
        for (raw in lines) {
            val line = raw.trim()
            val s = summaryRegex.find(line)
            if (s != null) {
                tx = s.groupValues[1].toInt()
                rx = s.groupValues[2].toInt()
                continue
            }
            if (line.contains("bytes from")) {
                rttRegex.find(line)?.let { rtts.add(it.groupValues[1].toDouble()) }
                continue
            }
            val lower = line.lowercase()
            if (err == null && errorKeywords.any { lower.contains(it) }) err = line
        }
        return Parsed(tx, rx, rtts, err)
    }

    /** Returns (min, avg, max) or nulls when there are no samples. */
    fun stats(rtts: List<Double>): Triple<Double?, Double?, Double?> {
        if (rtts.isEmpty()) return Triple(null, null, null)
        return Triple(rtts.min(), rtts.average(), rtts.max())
    }
}

class PingManager {
    companion object {
        const val MAX_COUNT = 20
    }

    suspend fun ping(hostInput: String, count: Int): PingResult {
        val host = hostInput.trim()
        if (!Validators.isValidHost(host)) throw Errs.invalidHost()
        if (count !in 1..MAX_COUNT) throw Errs.invalidCount(MAX_COUNT)

        val addr = withContext(Dispatchers.IO) { Resolver.resolve(host) } // UnknownHostException -> mapped
        val cmd = ProcessRunner.pingCommand(addr, count, null, 2)
        val lines = ProcessRunner.run(cmd, count * 3000L + 3000L)
        val parsed = PingParser.parse(lines)

        val sent = parsed.transmitted ?: count
        val received = parsed.received ?: parsed.rtts.size
        if (received == 0) {
            throw if (parsed.error != null) Errs.pingFailed(parsed.error) else Errs.pingTimeout(host)
        }
        val (min, avg, max) = PingParser.stats(parsed.rtts)
        return PingResult(host, Resolver.ip(addr), sent, received, min, avg, max)
    }
}
