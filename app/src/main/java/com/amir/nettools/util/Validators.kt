package com.amir.nettools.util

import java.net.URI

object Validators {
    private val LABEL = Regex("[A-Za-z0-9_]([A-Za-z0-9_-]{0,61}[A-Za-z0-9_])?")

    fun isValidIpv4(s: String): Boolean {
        val parts = s.split(".")
        if (parts.size != 4) return false
        return parts.all { p ->
            p.isNotEmpty() && p.length <= 3 &&
                p.all { it in '0'..'9' } &&
                (p.length == 1 || p[0] != '0') &&
                p.toInt() in 0..255
        }
    }

    private fun groupCount(part: String): Int? {
        if (part.isEmpty()) return 0
        val groups = part.split(":")
        for (g in groups) {
            if (g.isEmpty() || g.length > 4) return null
            if (!g.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        }
        return groups.size
    }

    /** Strict IPv6 literal validation. Zone identifiers (%eth0) are not accepted. */
    fun isValidIpv6(input: String): Boolean {
        if (input.isEmpty() || input.contains('%') || !input.contains(':')) return false
        var s = input
        val lastColon = s.lastIndexOf(':')
        val tail = s.substring(lastColon + 1)
        if (tail.contains('.')) {
            if (!isValidIpv4(tail)) return false
            s = s.substring(0, lastColon + 1) + "0:0"
        }
        val dbl = s.indexOf("::")
        if (dbl != s.lastIndexOf("::")) return false
        if (dbl >= 0) {
            val left = groupCount(s.substring(0, dbl)) ?: return false
            val right = groupCount(s.substring(dbl + 2)) ?: return false
            return left + right <= 7
        }
        return groupCount(s) == 8
    }

    fun isValidHostname(input: String): Boolean {
        val h = input.trimEnd('.')
        if (h.isEmpty() || h.length > 253) return false
        val labels = h.split(".")
        if (!labels.all { LABEL.matches(it) }) return false
        // All-numeric names look like (invalid) IPv4 addresses.
        if (labels.all { l -> l.all { it in '0'..'9' } }) return false
        return true
    }

    fun isValidHost(input: String): Boolean =
        isValidIpv4(input) || isValidIpv6(input) || isValidHostname(input)

    fun parsePort(text: String): Int? {
        val t = text.trim()
        if (t.isEmpty() || t.length > 5 || !t.all { it in '0'..'9' }) return null
        return t.toInt().takeIf { it in 1..65535 }
    }

    fun parseCount(text: String, default: Int, max: Int): Int? {
        val t = text.trim()
        if (t.isEmpty()) return default
        if (t.length > 3 || !t.all { it.isDigit() }) return null
        val n = t.toInt()
        return if (n in 1..max) n else null
    }

    /** Returns a normalized http(s) URL or null if the input is not acceptable. */
    fun normalizeUrl(input: String): String? {
        val t = input.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() }) return null
        val withScheme = if (t.contains("://")) t else "https://$t"
        val uri = try {
            URI(withScheme)
        } catch (e: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        val bare = host.removePrefix("[").removeSuffix("]")
        if (!isValidHost(bare)) return null
        if (uri.port != -1 && uri.port !in 1..65535) return null
        return withScheme
    }
}
