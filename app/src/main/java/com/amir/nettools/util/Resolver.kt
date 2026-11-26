package com.amir.nettools.util

import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object Resolver {
    /** Blocking. Prefers IPv4 if available. Throws UnknownHostException. */
    fun resolve(host: String): InetAddress {
        val all = InetAddress.getAllByName(host)
        return all.firstOrNull { it is Inet4Address } ?: all.first()
    }

    fun ip(addr: InetAddress): String = addr.hostAddress?.substringBefore('%') ?: ""
}

object ReverseDns {
    /** Returns a PTR host name or null. Never blocks longer than [timeoutMs]. */
    fun lookup(ip: String, timeoutMs: Long): String? {
        val future = CompletableFuture.supplyAsync {
            try {
                InetAddress.getByName(ip).canonicalHostName
            } catch (e: Exception) {
                null
            }
        }
        return try {
            val r = future.get(timeoutMs, TimeUnit.MILLISECONDS)
            if (r == null || Validators.isValidIpv4(r) || Validators.isValidIpv6(r)) null else r
        } catch (e: Exception) {
            null
        }
    }
}
