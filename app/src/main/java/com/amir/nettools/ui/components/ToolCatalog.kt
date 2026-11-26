package com.amir.nettools.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.amir.nettools.domain.model.ToolType

data class ToolUi(
    val type: ToolType,
    val fa: String,
    val en: String,
    val descFa: String,
    val descEn: String,
    val icon: ImageVector
)

object ToolCatalog {
    val all: List<ToolUi> = listOf(
        ToolUi(ToolType.PING, "پینگ", "Ping", "تأخیر و افت بسته", "Latency & packet loss", Icons.Filled.SettingsInputAntenna),
        ToolUi(ToolType.DNS, "جستجوی DNS", "DNS Lookup", "A, AAAA, CNAME, MX, NS, TXT", "A, AAAA, CNAME, MX, NS, TXT", Icons.Filled.Dns),
        ToolUi(ToolType.IP_INFO, "اطلاعات IP", "IP Information", "تحلیل آدرس و IP دستگاه", "Analyse an IP & device IPs", Icons.Filled.Info),
        ToolUi(ToolType.HTTP, "تست HTTP/HTTPS", "HTTP/HTTPS Test", "وضعیت، ریدایرکت، TLS", "Status, redirects, TLS", Icons.Filled.Http),
        ToolUi(ToolType.CONNECTIVITY, "اتصال اینترنت", "Internet Connectivity", "بررسی واقعی دسترسی", "Real reachability check", Icons.Filled.NetworkCheck),
        ToolUi(ToolType.NETWORK_INFO, "اطلاعات شبکه", "Network Information", "گیت‌وی، DNS، قابلیت‌ها", "Gateway, DNS, capabilities", Icons.Filled.Router),
        ToolUi(ToolType.PORT, "بررسی پورت", "Port Checker", "تست یک پورت TCP", "Test a single TCP port", Icons.Filled.SettingsEthernet),
        ToolUi(ToolType.TRACEROUTE, "Traceroute", "Traceroute", "مسیر بسته‌ها تا مقصد", "Path to destination", Icons.Filled.Timeline),
        ToolUi(ToolType.SPEED, "تست سرعت", "Speed Test", "دانلود، آپلود، تأخیر", "Download, upload, latency", Icons.Filled.Speed),
        ToolUi(ToolType.WIFI, "اطلاعات Wi-Fi", "Wi-Fi Information", "SSID، سیگنال، فرکانس", "SSID, signal, frequency", Icons.Filled.Wifi),
        ToolUi(ToolType.PUBLIC_IP, "IP عمومی", "Public IP", "IPv4 و IPv6 عمومی", "Public IPv4 & IPv6", Icons.Filled.Public),
        ToolUi(ToolType.SUBNET, "ماشین‌حساب Subnet", "Subnet Calculator", "محاسبه کاملاً محلی", "Fully local calculation", Icons.Filled.Calculate)
    )

    fun of(type: ToolType): ToolUi = all.first { it.type == type }
}
