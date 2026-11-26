package com.amir.nettools.domain.model

import com.amir.nettools.network.HttpResults
import com.amir.nettools.util.*

enum class ToolType(val key: String) {
    PING("ping"), DNS("dns"), IP_INFO("ip"), HTTP("http"), CONNECTIVITY("connectivity"),
    NETWORK_INFO("network"), PORT("port"), TRACEROUTE("traceroute"), SPEED("speed"),
    WIFI("wifi"), PUBLIC_IP("publicip"), SUBNET("subnet");

    val route: String get() = "tool/$key"

    companion object {
        fun fromKey(k: String): ToolType = entries.firstOrNull { it.key == k } ?: PING
    }
}

enum class ConnType(val fa: String, val en: String) {
    WIFI("Wi-Fi", "Wi-Fi"),
    MOBILE("داده همراه", "Mobile data"),
    ETHERNET("اترنت", "Ethernet"),
    VPN("VPN", "VPN"),
    BLUETOOTH("بلوتوث", "Bluetooth"),
    OTHER("سایر", "Other"),
    NONE("بدون اتصال", "No connection");

    fun label(isFa: Boolean) = if (isFa) fa else en
}

data class NetStatus(val type: ConnType, val validated: Boolean)

data class HistoryItem(
    val id: Long,
    val tool: ToolType,
    val target: String,
    val summary: String,
    val details: String,
    val timestamp: Long
)

data class PingResult(
    val host: String,
    val ip: String,
    val sent: Int,
    val received: Int,
    val minMs: Double?,
    val avgMs: Double?,
    val maxMs: Double?
) : ResultRows {
    val lossPercent: Double get() = if (sent <= 0) 0.0 else (sent - received) * 100.0 / sent

    override fun rows(fa: Boolean) = listOf(
        tr(fa, "میزبان", "Host") to host,
        tr(fa, "IP مقصد", "Resolved IP") to ip,
        tr(fa, "بسته‌های ارسالی", "Packets sent") to sent.toString(),
        tr(fa, "بسته‌های دریافتی", "Packets received") to received.toString(),
        tr(fa, "افت بسته", "Packet loss") to fmtPct(lossPercent),
        tr(fa, "کمترین تأخیر", "Minimum latency") to fmtMs(minMs, fa),
        tr(fa, "میانگین تأخیر", "Average latency") to fmtMs(avgMs, fa),
        tr(fa, "بیشترین تأخیر", "Maximum latency") to fmtMs(maxMs, fa)
    )
}

data class DnsRecord(val type: String, val value: String, val ttl: Long?)

enum class DnsType(val code: Int) {
    A(1), AAAA(28), CNAME(5), MX(15), NS(2), TXT(16)
}

data class DnsResult(
    val host: String,
    val server: String?,
    val records: List<DnsRecord>,
    val durationMs: Long
) : ResultRows {
    override fun rows(fa: Boolean): List<Pair<String, String>> {
        val out = mutableListOf(
            tr(fa, "دامنه", "Domain") to host,
            tr(fa, "سرور DNS", "DNS server") to orNa(server, fa),
            tr(fa, "مدت", "Duration") to "$durationMs ms"
        )
        if (records.isEmpty()) {
            out += tr(fa, "نتیجه", "Result") to tr(fa, "رکوردی یافت نشد", "No records found")
        } else {
            records.forEach { r ->
                out += r.type to (r.value + (r.ttl?.let { "  (TTL ${it}s)" } ?: ""))
            }
        }
        return out
    }
}

data class LocalIpInfo(
    val ipv4: List<String>,
    val ipv6: List<String>,
    val privateIp: String?,
    val iface: String?,
    val connType: ConnType
) : ResultRows {
    override fun rows(fa: Boolean) = listOf(
        "IPv4" to joinOrNa(ipv4, fa),
        "IPv6" to joinOrNa(ipv6, fa),
        tr(fa, "IP خصوصی", "Private IP") to orNa(privateIp, fa),
        tr(fa, "رابط شبکه", "Network interface") to orNa(iface, fa),
        tr(fa, "نوع اتصال", "Connection type") to connType.label(fa)
    )
}

data class IpAnalysis(
    val address: String,
    val version: Int,
    val scope: IpScope,
    val normalized: String,
    val expanded: String?,
    val integerValue: Long?,
    val reversePointer: String?,
    val reverseDns: String?
) : ResultRows {
    override fun rows(fa: Boolean): List<Pair<String, String>> {
        val out = mutableListOf(
            tr(fa, "آدرس", "Address") to address,
            tr(fa, "نسخه", "Version") to "IPv$version",
            tr(fa, "محدوده", "Scope") to scope.label(fa),
            tr(fa, "فرم استاندارد", "Normalized") to normalized
        )
        if (expanded != null) out += tr(fa, "فرم کامل", "Expanded") to expanded
        if (integerValue != null) out += tr(fa, "مقدار عددی", "Integer value") to integerValue.toString()
        out += tr(fa, "رکورد PTR", "PTR name") to orNa(reversePointer, fa)
        out += tr(fa, "DNS معکوس", "Reverse DNS") to orNa(reverseDns, fa)
        return out
    }
}

data class HttpTestResult(
    val url: String,
    val finalUrl: String,
    val statusCode: Int,
    val statusMessage: String?,
    val durationMs: Long,
    val redirects: List<String>,
    val contentType: String?,
    val server: String?,
    val isHttps: Boolean,
    val tlsInfo: String?,
    val certInfo: String?
) : ResultRows {
    override fun rows(fa: Boolean): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        out += "URL" to url
        if (finalUrl != url) out += tr(fa, "URL نهایی", "Final URL") to finalUrl
        out += tr(fa, "کد وضعیت", "Status code") to
            "$statusCode ${statusMessage ?: ""}".trim() + " • " + HttpResults.describe(statusCode, fa)
        out += tr(fa, "زمان پاسخ", "Response time") to "$durationMs ms"
        if (redirects.isEmpty()) {
            out += tr(fa, "تغییر مسیر", "Redirects") to tr(fa, "ندارد", "None")
        } else {
            redirects.forEachIndexed { i, r -> out += tr(fa, "تغییر مسیر ${i + 1}", "Redirect ${i + 1}") to r }
        }
        out += "Content-Type" to orNa(contentType, fa)
        out += "Server" to orNa(server, fa)
        out += "HTTPS" to yn(isHttps, fa)
        if (isHttps) {
            out += tr(fa, "وضعیت TLS", "TLS status") to
                tr(fa, "برقرار (گواهی توسط سیستم معتبر شناخته شد)", "Established (certificate trusted by system)")
            out += tr(fa, "مجموعه رمز", "Cipher suite") to orNa(tlsInfo, fa)
            out += tr(fa, "گواهی", "Certificate") to orNa(certInfo, fa)
        }
        return out
    }
}

enum class PortStatus(val fa: String, val en: String) {
    OPEN("باز (Open)", "Open"),
    CLOSED("بسته (Closed)", "Closed"),
    TIMEOUT("Timeout", "Timeout"),
    ERROR("خطا", "Error")
}

data class PortResult(
    val host: String,
    val ip: String,
    val port: Int,
    val status: PortStatus,
    val timeMs: Long,
    val message: String?
) : ResultRows {
    override fun rows(fa: Boolean): List<Pair<String, String>> {
        val out = mutableListOf(
            tr(fa, "میزبان", "Host") to host,
            "IP" to ip,
            tr(fa, "پورت", "Port") to port.toString(),
            tr(fa, "وضعیت", "Status") to (if (fa) status.fa else status.en),
            tr(fa, "زمان اتصال", "Connection time") to "$timeMs ms"
        )
        if (!message.isNullOrBlank() && status != PortStatus.OPEN) out += tr(fa, "جزئیات", "Details") to message
        return out
    }
}

data class TraceHop(
    val hop: Int,
    val ip: String?,
    val hostname: String?,
    val latencyMs: Double?,
    val approx: Boolean,
    val note: String?
)

data class TraceResult(
    val host: String,
    val ip: String,
    val hops: List<TraceHop>,
    val reached: Boolean
) : ResultRows {
    override fun rows(fa: Boolean): List<Pair<String, String>> {
        val out = mutableListOf(
            tr(fa, "میزبان", "Host") to host,
            tr(fa, "IP مقصد", "Destination IP") to ip,
            tr(fa, "وضعیت", "Status") to
                if (reached) tr(fa, "به مقصد رسید", "Destination reached")
                else tr(fa, "به مقصد نرسید", "Destination not reached")
        )
        hops.forEach { h ->
            val v = if (h.ip == null) {
                "*  " + tr(fa, "بدون پاسخ", "no reply")
            } else {
                h.ip + (h.hostname?.let { " ($it)" } ?: "") +
                    (h.latencyMs?.let { " · " + (if (h.approx) "~" else "") + fmtMs(it, fa) } ?: "")
            }
            out += tr(fa, "هاپ ${h.hop}", "Hop ${h.hop}") to v
        }
        return out
    }
}

enum class SpeedPhase { LATENCY, DOWNLOAD, UPLOAD }

data class SpeedProgress(val phase: SpeedPhase, val mbps: Double? = null)

data class SpeedResult(
    val server: String,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latencyMs: Double?,
    val durationMs: Long
) : ResultRows {
    override fun rows(fa: Boolean) = listOf(
        tr(fa, "سرور", "Server") to server,
        tr(fa, "سرعت دانلود (تقریبی)", "Download (approx.)") to fmtMbps(downloadMbps, fa),
        tr(fa, "سرعت آپلود (تقریبی)", "Upload (approx.)") to fmtMbps(uploadMbps, fa),
        tr(fa, "تأخیر", "Latency") to fmtMs(latencyMs, fa),
        tr(fa, "مدت تست", "Test duration") to String.format(java.util.Locale.US, "%.1f s", durationMs / 1000.0)
    )
}

data class NetworkInfoData(
    val connType: ConnType,
    val hasInternet: Boolean,
    val validated: Boolean,
    val ssid: String?,
    val linkSpeed: String?,
    val capabilities: List<String>,
    val localIps: List<String>,
    val gateway: String?,
    val dnsServers: List<String>,
    val privateDns: String?,
    val iface: String?
) : ResultRows {
    override fun rows(fa: Boolean) = listOf(
        tr(fa, "نوع اتصال", "Connection type") to connType.label(fa),
        tr(fa, "قابلیت اینترنت", "Internet capability") to yn(hasInternet, fa),
        tr(fa, "اینترنت تأییدشده", "Validated internet") to yn(validated, fa),
        "SSID" to orNa(ssid, fa),
        tr(fa, "سرعت لینک", "Link speed") to orNa(linkSpeed, fa),
        tr(fa, "قابلیت‌های شبکه", "Network capabilities") to joinOrNa(capabilities, fa),
        tr(fa, "رابط شبکه", "Interface") to orNa(iface, fa),
        tr(fa, "IP محلی", "Local IP") to joinOrNa(localIps, fa),
        tr(fa, "گیت‌وی", "Gateway") to orNa(gateway, fa),
        tr(fa, "سرورهای DNS", "DNS servers") to joinOrNa(dnsServers, fa),
        "Private DNS" to orNa(privateDns, fa)
    )
}

data class WifiData(
    val connected: Boolean,
    val permissionGranted: Boolean,
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    val band: String?,
    val ip: String?
) : ResultRows {
    override fun rows(fa: Boolean): List<Pair<String, String>> {
        if (!connected) {
            return listOf(tr(fa, "وضعیت", "Status") to tr(fa, "به Wi-Fi متصل نیستید", "Not connected to Wi-Fi"))
        }
        return listOf(
            tr(fa, "وضعیت", "Status") to tr(fa, "متصل", "Connected"),
            "SSID" to orNa(ssid, fa),
            "BSSID" to orNa(bssid, fa),
            tr(fa, "قدرت سیگنال", "Signal strength") to (rssiDbm?.let { "$it dBm" } ?: na(fa)),
            tr(fa, "سرعت لینک", "Link speed") to (linkSpeedMbps?.let { "$it Mbps" } ?: na(fa)),
            tr(fa, "فرکانس", "Frequency") to (frequencyMhz?.let { "$it MHz" } ?: na(fa)),
            tr(fa, "باند", "Band") to orNa(band, fa),
            tr(fa, "IP", "IP address") to orNa(ip, fa)
        )
    }
}

data class ConnectivityResult(
    val networkAvailable: Boolean,
    val validated: Boolean,
    val type: ConnType,
    val reachable: Boolean,
    val reachMs: Long?,
    val reachTarget: String?
) : ResultRows {
    val connected: Boolean get() = networkAvailable && reachable

    override fun rows(fa: Boolean) = listOf(
        tr(fa, "وضعیت", "Status") to
            if (connected) tr(fa, "🟢 متصل", "🟢 Connected") else tr(fa, "🔴 بدون اینترنت", "🔴 No Internet"),
        tr(fa, "شبکه فعال", "Network available") to yn(networkAvailable, fa),
        tr(fa, "اینترنت تأییدشده (Android)", "Validated internet (Android)") to yn(validated, fa),
        tr(fa, "نوع اتصال", "Connection type") to type.label(fa),
        tr(fa, "دسترسی واقعی (TCP)", "Reachability (TCP)") to
            if (reachable) "${reachTarget ?: ""} • ${reachMs ?: 0} ms" else tr(fa, "ناموفق", "Failed")
    )
}

data class PublicIpResult(val ipv4: String?, val ipv6: String?) : ResultRows {
    override fun rows(fa: Boolean) = listOf(
        tr(fa, "IPv4 عمومی", "Public IPv4") to orNa(ipv4, fa),
        tr(fa, "IPv6 عمومی", "Public IPv6") to orNa(ipv6, fa)
    )
}
