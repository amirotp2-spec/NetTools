package com.amir.nettools.util

enum class IpScope(val fa: String, val en: String) {
    PUBLIC("عمومی (Global)", "Public / Global"),
    PRIVATE("خصوصی (RFC 1918)", "Private (RFC 1918)"),
    CGNAT("CGNAT (100.64.0.0/10)", "Carrier-grade NAT (100.64.0.0/10)"),
    LOOPBACK("Loopback", "Loopback"),
    LINK_LOCAL("Link-local", "Link-local"),
    MULTICAST("Multicast", "Multicast"),
    UNSPECIFIED("نامشخص / این شبکه", "Unspecified / this network"),
    DOCUMENTATION("مستندات (Documentation)", "Documentation"),
    RESERVED("رزرو شده", "Reserved"),
    BROADCAST("Broadcast", "Broadcast"),
    UNIQUE_LOCAL("Unique local (fc00::/7)", "Unique local (fc00::/7)"),
    IPV4_MAPPED("IPv4-mapped", "IPv4-mapped");

    fun label(isFa: Boolean) = if (isFa) fa else en
}

object IpClassifier {
    fun ipv4ToLong(ip: String): Long? {
        if (!Validators.isValidIpv4(ip)) return null
        return ip.split(".").fold(0L) { acc, p -> (acc shl 8) or p.toLong() }
    }

    fun longToIpv4(v: Long): String =
        "${(v shr 24) and 255}.${(v shr 16) and 255}.${(v shr 8) and 255}.${v and 255}"

    private fun inCidr(ip: Long, base: String, prefix: Int): Boolean {
        val b = ipv4ToLong(base) ?: return false
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        return (ip and mask) == (b and mask)
    }

    fun scopeOfIpv4(ip: String): IpScope? {
        val v = ipv4ToLong(ip) ?: return null
        return when {
            v == 0xFFFFFFFFL -> IpScope.BROADCAST
            inCidr(v, "0.0.0.0", 8) -> IpScope.UNSPECIFIED
            inCidr(v, "10.0.0.0", 8) -> IpScope.PRIVATE
            inCidr(v, "172.16.0.0", 12) -> IpScope.PRIVATE
            inCidr(v, "192.168.0.0", 16) -> IpScope.PRIVATE
            inCidr(v, "100.64.0.0", 10) -> IpScope.CGNAT
            inCidr(v, "127.0.0.0", 8) -> IpScope.LOOPBACK
            inCidr(v, "169.254.0.0", 16) -> IpScope.LINK_LOCAL
            inCidr(v, "192.0.2.0", 24) -> IpScope.DOCUMENTATION
            inCidr(v, "198.51.100.0", 24) -> IpScope.DOCUMENTATION
            inCidr(v, "203.0.113.0", 24) -> IpScope.DOCUMENTATION
            inCidr(v, "224.0.0.0", 4) -> IpScope.MULTICAST
            inCidr(v, "240.0.0.0", 4) -> IpScope.RESERVED
            else -> IpScope.PUBLIC
        }
    }

    /** Parses a (validated) IPv6 literal into 16 bytes without any DNS lookup. */
    fun parseIpv6(ip: String): ByteArray? {
        if (!Validators.isValidIpv6(ip)) return null
        var s = ip
        val lc = s.lastIndexOf(':')
        val tail = s.substring(lc + 1)
        if (tail.contains('.')) {
            val v = ipv4ToLong(tail) ?: return null
            s = s.substring(0, lc + 1) +
                java.lang.Long.toHexString((v shr 16) and 0xFFFF) + ":" +
                java.lang.Long.toHexString(v and 0xFFFF)
        }
        val dbl = s.indexOf("::")
        val head = if (dbl >= 0) s.substring(0, dbl) else s
        val rest = if (dbl >= 0) s.substring(dbl + 2) else ""
        val hg = if (head.isEmpty()) emptyList() else head.split(":")
        val tg = if (rest.isEmpty()) emptyList() else rest.split(":")
        val groups = if (dbl >= 0) hg + List(8 - hg.size - tg.size) { "0" } + tg else hg
        if (groups.size != 8) return null
        val out = ByteArray(16)
        groups.forEachIndexed { i, g ->
            val n = g.toInt(16)
            out[i * 2] = (n shr 8).toByte()
            out[i * 2 + 1] = n.toByte()
        }
        return out
    }

    fun scopeOfIpv6(b: ByteArray): IpScope {
        val b0 = b[0].toInt() and 0xFF
        val b1 = b[1].toInt() and 0xFF
        val allZero = b.all { it.toInt() == 0 }
        val loopback = b.take(15).all { it.toInt() == 0 } && b[15].toInt() == 1
        return when {
            allZero -> IpScope.UNSPECIFIED
            loopback -> IpScope.LOOPBACK
            b0 == 0xFF -> IpScope.MULTICAST
            (b0 and 0xFE) == 0xFC -> IpScope.UNIQUE_LOCAL
            b0 == 0xFE && (b1 and 0xC0) == 0x80 -> IpScope.LINK_LOCAL
            b.take(10).all { it.toInt() == 0 } &&
                (b[10].toInt() and 0xFF) == 0xFF && (b[11].toInt() and 0xFF) == 0xFF -> IpScope.IPV4_MAPPED
            b0 == 0x20 && b1 == 0x01 && (b[2].toInt() and 0xFF) == 0x0D && (b[3].toInt() and 0xFF) == 0xB8 ->
                IpScope.DOCUMENTATION
            else -> IpScope.PUBLIC
        }
    }

    private fun groups(b: ByteArray): IntArray =
        IntArray(8) { ((b[it * 2].toInt() and 0xFF) shl 8) or (b[it * 2 + 1].toInt() and 0xFF) }

    fun ipv6Compress(b: ByteArray): String {
        val g = groups(b)
        var bestStart = -1
        var bestLen = 0
        var i = 0
        while (i < 8) {
            if (g[i] == 0) {
                var j = i
                while (j < 8 && g[j] == 0) j++
                if (j - i > bestLen) {
                    bestStart = i
                    bestLen = j - i
                }
                i = j
            } else {
                i++
            }
        }
        if (bestLen < 2) return g.joinToString(":") { Integer.toHexString(it) }
        val head = g.take(bestStart).joinToString(":") { Integer.toHexString(it) }
        val tail = g.drop(bestStart + bestLen).joinToString(":") { Integer.toHexString(it) }
        return "$head::$tail"
    }

    fun ipv6Expanded(b: ByteArray): String =
        groups(b).joinToString(":") { String.format(java.util.Locale.US, "%04x", it) }

    fun reversePointer(ip: String): String? {
        if (Validators.isValidIpv4(ip)) return ip.split(".").reversed().joinToString(".") + ".in-addr.arpa"
        val b = parseIpv6(ip) ?: return null
        val nibbles = b.flatMap { listOf((it.toInt() shr 4) and 0xF, it.toInt() and 0xF) }
        return nibbles.reversed().joinToString(".") { Integer.toHexString(it) } + ".ip6.arpa"
    }
}
