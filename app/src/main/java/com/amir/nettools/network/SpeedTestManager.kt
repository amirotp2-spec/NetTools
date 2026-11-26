package com.amir.nettools.network

import com.amir.nettools.domain.model.SpeedPhase
import com.amir.nettools.domain.model.SpeedProgress
import com.amir.nettools.domain.model.SpeedResult
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Validators
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

/**
 * Approximate speed test against a Cloudflare-compatible endpoint (/__down?bytes=N and /__up).
 * Nothing runs unless the user presses Start; cancelling disconnects the socket immediately.
 */
class SpeedTestManager {
    companion object {
        const val DEFAULT_SERVER = "speed.cloudflare.com"
        private const val DOWN_BYTES = 50_000_000L
        private const val UP_BYTES = 25_000_000L
        private const val DOWN_MAX_NS = 10_000_000_000L
        private const val UP_MAX_NS = 8_000_000_000L
        private const val LATENCY_SAMPLES = 5

        fun mbps(bytes: Long, ns: Long): Double = bytes * 8.0 / (ns / 1e9) / 1e6
    }

    private fun base(server: String): String =
        if (Validators.isValidIpv6(server)) "https://[$server]" else "https://$server"

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 10000
        c.useCaches = false
        c.setRequestProperty("User-Agent", "NetTools/1.0 (Android)")
        return c
    }

    /** Runs [block] on IO; on cancellation the connection is disconnected to unblock reads. */
    private suspend fun <T> withConn(conn: HttpURLConnection, block: () -> T): T = coroutineScope {
        val d = async(Dispatchers.IO) { block() }
        try {
            d.await()
        } finally {
            conn.disconnect()
        }
    }

    suspend fun run(serverInput: String, onUpdate: (SpeedProgress) -> Unit): SpeedResult {
        val server = serverInput.trim().ifEmpty { DEFAULT_SERVER }
        if (!Validators.isValidHost(server)) throw Errs.invalidHost()
        val t0 = System.nanoTime()

        onUpdate(SpeedProgress(SpeedPhase.LATENCY))
        val latency = measureLatency(server)

        var down: Double? = null
        var up: Double? = null
        var firstError: Exception? = null

        onUpdate(SpeedProgress(SpeedPhase.DOWNLOAD))
        try {
            down = download(server, onUpdate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            firstError = e
        }

        onUpdate(SpeedProgress(SpeedPhase.UPLOAD))
        try {
            up = upload(server, onUpdate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (firstError == null) firstError = e
        }

        val err = firstError
        if (down == null && up == null && err != null) throw err
        return SpeedResult(server, down, up, latency, (System.nanoTime() - t0) / 1_000_000)
    }

    private suspend fun measureLatency(server: String): Double {
        val samples = mutableListOf<Double>()
        for (i in 0..LATENCY_SAMPLES) { // first sample is a warm-up and is discarded
            val conn = open("${base(server)}/__down?bytes=0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val ms = withConn(conn) {
                val t = System.nanoTime()
                val code = conn.responseCode
                if (code !in 200..299) throw IOException("HTTP $code")
                conn.inputStream.use { it.read() }
                (System.nanoTime() - t) / 1e6
            }
            if (i > 0) samples += ms
        }
        return samples.average()
    }

    private suspend fun download(server: String, onUpdate: (SpeedProgress) -> Unit): Double {
        val conn = open("${base(server)}/__down?bytes=$DOWN_BYTES")
        return withConn(conn) {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            val buf = ByteArray(65536)
            conn.inputStream.use { ins ->
                var total = 0L
                var startNs = 0L
                var lastUpdate = 0L
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    val now = System.nanoTime()
                    if (startNs == 0L) startNs = now
                    total += n
                    val el = now - startNs
                    if (el > 0 && now - lastUpdate > 250_000_000L) {
                        onUpdate(SpeedProgress(SpeedPhase.DOWNLOAD, mbps(total, el)))
                        lastUpdate = now
                    }
                    if (el >= DOWN_MAX_NS) break
                }
                val elapsed = System.nanoTime() - startNs
                if (startNs == 0L || elapsed <= 0L) throw IOException("no data received")
                mbps(total, elapsed)
            }
        }
    }

    private suspend fun upload(server: String, onUpdate: (SpeedProgress) -> Unit): Double {
        val conn = open("${base(server)}/__up")
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setFixedLengthStreamingMode(UP_BYTES)
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        return withConn(conn) {
            val buf = ByteArray(65536).also { Random(42).nextBytes(it) }
            val out = conn.outputStream
            var total = 0L
            val startNs = System.nanoTime()
            var lastUpdate = 0L
            var el = 0L
            while (total < UP_BYTES) {
                val n = minOf(buf.size.toLong(), UP_BYTES - total).toInt()
                out.write(buf, 0, n)
                total += n
                val now = System.nanoTime()
                el = now - startNs
                if (el > 0 && now - lastUpdate > 250_000_000L) {
                    onUpdate(SpeedProgress(SpeedPhase.UPLOAD, mbps(total, el)))
                    lastUpdate = now
                }
                if (el >= UP_MAX_NS) break
            }
            if (el <= 0L) throw IOException("no data sent")
            val result = mbps(total, el)
            try {
                out.close()
            } catch (_: IOException) {
            }
            result
        }
    }
}
