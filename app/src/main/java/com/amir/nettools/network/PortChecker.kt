package com.amir.nettools.network

import com.amir.nettools.domain.model.PortResult
import com.amir.nettools.domain.model.PortStatus
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Resolver
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class PortChecker {
    companion object {
        fun classify(e: Throwable): PortStatus = when (e) {
            is SocketTimeoutException -> PortStatus.TIMEOUT
            is ConnectException -> {
                val m = (e.message ?: "").lowercase()
                if (m.contains("refused") || m.contains("econnrefused")) PortStatus.CLOSED else PortStatus.ERROR
            }
            else -> PortStatus.ERROR
        }
    }

    /** Tests exactly one TCP port. No range scanning. */
    suspend fun check(hostInput: String, port: Int, timeoutMs: Int = 5000): PortResult {
        val host = hostInput.trim()
        if (!Validators.isValidHost(host)) throw Errs.invalidHost()
        if (port !in 1..65535) throw Errs.invalidPort()
        return withContext(Dispatchers.IO) {
            val addr = Resolver.resolve(host)
            val ip = Resolver.ip(addr)
            val start = System.nanoTime()
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(addr, port), timeoutMs)
                PortResult(host, ip, port, PortStatus.OPEN, (System.nanoTime() - start) / 1_000_000, null)
            } catch (e: IOException) {
                PortResult(host, ip, port, classify(e), (System.nanoTime() - start) / 1_000_000, e.message)
            } finally {
                try {
                    socket.close()
                } catch (_: IOException) {
                }
            }
        }
    }
}
