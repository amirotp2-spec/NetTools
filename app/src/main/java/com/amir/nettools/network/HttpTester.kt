package com.amir.nettools.network

import com.amir.nettools.domain.model.HttpTestResult
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection

object HttpResults {
    fun isRedirect(code: Int): Boolean = code == 301 || code == 302 || code == 303 || code == 307 || code == 308

    fun resolveLocation(base: String, location: String): String? = try {
        val r = URI(base).resolve(location.trim())
        val scheme = r.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") r.toString() else null
    } catch (e: Exception) {
        null
    }

    fun describe(code: Int, fa: Boolean): String = when (code) {
        in 100..199 -> if (fa) "اطلاعاتی" else "Informational"
        in 200..299 -> if (fa) "موفق" else "Success"
        in 300..399 -> if (fa) "تغییر مسیر" else "Redirection"
        in 400..499 -> if (fa) "خطای کلاینت" else "Client error"
        in 500..599 -> if (fa) "خطای سرور" else "Server error"
        else -> if (fa) "نامشخص" else "Unknown"
    }
}

class HttpTester {
    companion object {
        const val MAX_REDIRECTS = 10
    }

    suspend fun test(rawUrl: String): HttpTestResult {
        val normalized = Validators.normalizeUrl(rawUrl) ?: throw Errs.invalidUrl()
        return withContext(Dispatchers.IO) { execute(normalized) }
    }

    private fun execute(startUrl: String): HttpTestResult {
        var current = startUrl
        val redirects = ArrayList<String>()
        val t0 = System.nanoTime()

        for (attempt in 0..MAX_REDIRECTS) {
            val hopStart = System.nanoTime()
            val conn = URL(current).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.instanceFollowRedirects = false
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "NetTools/1.0 (Android)")
                conn.setRequestProperty("Accept-Encoding", "identity")

                val code = conn.responseCode
                val hopMs = (System.nanoTime() - hopStart) / 1_000_000
                val location = conn.getHeaderField("Location")

                if (HttpResults.isRedirect(code) && location != null) {
                    val next = HttpResults.resolveLocation(current, location) ?: throw Errs.badRedirect()
                    redirects.add("$code → $next ($hopMs ms)")
                    current = next
                    continue
                }

                var cipher: String? = null
                var cert: String? = null
                if (conn is HttpsURLConnection) {
                    try {
                        cipher = conn.cipherSuite
                        val c = conn.serverCertificates.firstOrNull() as? X509Certificate
                        if (c != null) cert = "${c.subjectX500Principal.name} • exp ${c.notAfter}"
                    } catch (_: Exception) {
                    }
                }
                return HttpTestResult(
                    url = startUrl,
                    finalUrl = current,
                    statusCode = code,
                    statusMessage = conn.responseMessage,
                    durationMs = (System.nanoTime() - t0) / 1_000_000,
                    redirects = redirects,
                    contentType = conn.contentType,
                    server = conn.getHeaderField("Server"),
                    isHttps = current.startsWith("https://", ignoreCase = true),
                    tlsInfo = cipher,
                    certInfo = cert
                )
            } finally {
                conn.disconnect()
            }
        }
        throw Errs.tooManyRedirects()
    }
}
