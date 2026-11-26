package com.amir.nettools

import com.amir.nettools.domain.model.HttpTestResult
import com.amir.nettools.domain.model.PortStatus
import com.amir.nettools.network.HttpResults
import com.amir.nettools.network.PortChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class HttpAndPortTest {
    @Test fun redirectCodes() {
        listOf(301, 302, 303, 307, 308).forEach { assertTrue(HttpResults.isRedirect(it)) }
        listOf(200, 204, 304, 404, 500).forEach { assertFalse(HttpResults.isRedirect(it)) }
    }

    @Test fun locationResolution() {
        assertEquals("https://example.com/c", HttpResults.resolveLocation("https://example.com/a/b", "/c"))
        assertEquals("https://example.com/a/d", HttpResults.resolveLocation("https://example.com/a/b", "d"))
        assertEquals("http://other.org/", HttpResults.resolveLocation("https://example.com/", "http://other.org/"))
        assertNull(HttpResults.resolveLocation("https://example.com/", "ftp://other.org/"))
    }

    @Test fun statusDescriptions() {
        assertEquals("Success", HttpResults.describe(200, false))
        assertEquals("Redirection", HttpResults.describe(301, false))
        assertEquals("Client error", HttpResults.describe(404, false))
        assertEquals("Server error", HttpResults.describe(503, false))
        assertEquals("Unknown", HttpResults.describe(999, false))
    }

    @Test fun httpResultRows() {
        val r = HttpTestResult(
            url = "http://example.com", finalUrl = "https://example.com/", statusCode = 200, statusMessage = "OK",
            durationMs = 120, redirects = listOf("301 → https://example.com/ (40 ms)"),
            contentType = "text/html", server = null, isHttps = true, tlsInfo = "TLS_AES_128_GCM_SHA256", certInfo = null
        )
        val rows = r.rows(false).toMap()
        assertEquals("http://example.com", rows["URL"])
        assertEquals("https://example.com/", rows["Final URL"])
        assertTrue(rows["Status code"]!!.startsWith("200 OK"))
        assertEquals("120 ms", rows["Response time"])
        assertEquals("Unavailable", rows["Server"])
        assertEquals("Yes", rows["HTTPS"])
        assertEquals("301 → https://example.com/ (40 ms)", rows["Redirect 1"])
    }

    @Test fun portClassification() {
        assertEquals(PortStatus.CLOSED, PortChecker.classify(ConnectException("Connection refused")))
        assertEquals(
            PortStatus.CLOSED,
            PortChecker.classify(ConnectException("failed to connect to /1.2.3.4 (port 80): ECONNREFUSED (Connection refused)"))
        )
        assertEquals(PortStatus.ERROR, PortChecker.classify(ConnectException("Network is unreachable")))
        assertEquals(PortStatus.TIMEOUT, PortChecker.classify(SocketTimeoutException("connect timed out")))
        assertEquals(PortStatus.ERROR, PortChecker.classify(UnknownHostException("x")))
    }
}
