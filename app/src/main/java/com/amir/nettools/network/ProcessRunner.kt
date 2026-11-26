package com.amir.nettools.network

import com.amir.nettools.util.Errs
import com.amir.nettools.util.Resolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Collections

/**
 * Runs the device's own ping binary with a FIXED argument list built from validated values.
 * No shell is involved and no user text is ever interpreted as a command.
 */
object ProcessRunner {
    private fun pingBinary(addr: InetAddress): String {
        if (addr is Inet6Address && File("/system/bin/ping6").exists()) return "/system/bin/ping6"
        return if (File("/system/bin/ping").exists()) "/system/bin/ping" else "ping"
    }

    fun pingCommand(addr: InetAddress, count: Int, ttl: Int?, waitSec: Int): List<String> {
        val cmd = mutableListOf(pingBinary(addr), "-c", count.toString(), "-W", waitSec.toString())
        if (ttl != null) {
            cmd += "-t"
            cmd += ttl.toString()
        }
        cmd += Resolver.ip(addr)
        return cmd
    }

    suspend fun run(cmd: List<String>, timeoutMs: Long): List<String> = coroutineScope {
        val proc = withContext(Dispatchers.IO) {
            try {
                ProcessBuilder(cmd).redirectErrorStream(true).start()
            } catch (e: IOException) {
                throw Errs.pingUnavailable()
            }
        }
        val lines = Collections.synchronizedList(ArrayList<String>())
        val reader = async(Dispatchers.IO) {
            try {
                proc.inputStream.bufferedReader().forEachLine { lines.add(it) }
            } catch (_: IOException) {
            }
        }
        try {
            withTimeoutOrNull(timeoutMs) { reader.await() }
        } finally {
            proc.destroy()
        }
        synchronized(lines) { ArrayList(lines) }
    }
}
