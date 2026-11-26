package com.amir.nettools.network

import com.amir.nettools.domain.model.PublicIpResult
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class PublicIpManager {
    companion object {
        val V4_URLS = listOf("https://api.ipify.org", "https://ipv4.icanhazip.com")
        val V6_URLS = listOf("https://api6.ipify.org", "https://ipv6.icanhazip.com")
    }

    suspend fun fetch(): PublicIpResult = coroutineScope {
        val v4 = async(Dispatchers.IO) { first(V4_URLS) { Validators.isValidIpv4(it) } }
        val v6 = async(Dispatchers.IO) { first(V6_URLS) { Validators.isValidIpv6(it) } }
        val r = PublicIpResult(v4.await(), v6.await())
        if (r.ipv4 == null && r.ipv6 == null) throw Errs.publicIpFailed()
        r
    }

    private fun first(urls: List<String>, ok: (String) -> Boolean): String? {
        for (u in urls) {
            try {
                val s = get(u)
                if (ok(s)) return s
            } catch (_: IOException) {
            }
        }
        return null
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "NetTools/1.0 (Android)")
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText().take(100).trim() }
        } finally {
            conn.disconnect()
        }
    }
}
