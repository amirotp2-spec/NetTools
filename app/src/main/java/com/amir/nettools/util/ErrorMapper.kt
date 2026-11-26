package com.amir.nettools.util

import com.amir.nettools.domain.model.NxDomainException
import com.amir.nettools.domain.model.ToolError
import com.amir.nettools.domain.model.ToolException
import java.io.IOException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object Errs {
    private fun e(fa: String, en: String) = ToolException(ToolError(fa, en))

    fun invalidHost() = e(
        "نام میزبان یا آدرس IP نامعتبر است. مثال: example.com",
        "Invalid hostname or IP address. Example: example.com"
    )
    fun invalidIp() = e(
        "آدرس IP نامعتبر است. یک آدرس IPv4 یا IPv6 معتبر وارد کنید.",
        "Invalid IP address. Enter a valid IPv4 or IPv6 address."
    )
    fun invalidUrl() = e(
        "آدرس URL نامعتبر است. مثال: https://example.com",
        "Invalid URL. Example: https://example.com"
    )
    fun invalidPort() = e(
        "شماره پورت باید عددی بین 1 تا 65535 باشد.",
        "Port must be a number between 1 and 65535."
    )
    fun invalidCount(max: Int) = e(
        "تعداد بسته باید عددی بین 1 تا $max باشد.",
        "Packet count must be a number between 1 and $max."
    )
    fun invalidCidr() = e(
        "CIDR باید عددی بین 0 تا 32 باشد.",
        "CIDR must be a number between 0 and 32."
    )
    fun pingUnavailable() = e(
        "ابزار ping سیستم در این دستگاه در دسترس نیست یا اجازه اجرا ندارد.",
        "The system ping tool is unavailable or not permitted on this device."
    )
    fun pingTimeout(host: String) = e(
        "پاسخی از $host دریافت نشد (Timeout / ۱۰۰٪ افت بسته). ممکن است میزبان ICMP را مسدود کرده باشد.",
        "No reply from $host (timeout / 100% packet loss). The host may block ICMP."
    )
    fun pingFailed(line: String) = e("اجرای ping ناموفق بود: $line", "Ping failed: $line")
    fun traceUnsupported() = e(
        "این دستگاه گزینه TTL را در ping پشتیبانی نمی‌کند؛ Traceroute در اینجا ممکن نیست.",
        "This device's ping does not support a TTL option; traceroute is not possible here."
    )
    fun badRedirect() = e("آدرس تغییر مسیر (Redirect) نامعتبر است.", "Invalid redirect location.")
    fun tooManyRedirects() = e("تعداد تغییر مسیرها بیش از حد مجاز است.", "Too many redirects.")
    fun publicIpFailed() = e(
        "دریافت IP عمومی ممکن نشد. اتصال اینترنت یا دسترسی به سرویس را بررسی کنید.",
        "Could not determine the public IP. Check your connection or service reachability."
    )
    fun dnsTimeout() = e(
        "سرور DNS پاسخ نداد (Timeout).",
        "The DNS server did not respond (timeout)."
    )
    fun dnsFailed() = e("پرس‌وجوی DNS ناموفق بود.", "The DNS query failed.")
    fun nxDomain(host: String) = NxDomainException(
        ToolError("دامنه «$host» وجود ندارد (NXDOMAIN).", "Domain \"$host\" does not exist (NXDOMAIN).")
    )
}

object ErrorMapper {
    val noInternet = ToolError(
        "اتصال اینترنت برقرار نیست. اتصال Wi-Fi یا داده همراه را بررسی کنید.",
        "No internet connection. Check your Wi-Fi or mobile data."
    )

    fun map(t: Throwable, hasNetwork: Boolean = true): ToolError {
        if (!hasNetwork && t is IOException) return noInternet
        return when (t) {
            is ToolException -> t.error
            is UnknownHostException -> ToolError(
                "میزبان پیدا نشد (خطای DNS). نام را بررسی کنید.",
                "Unknown host (DNS failure). Check the name."
            )
            is SocketTimeoutException -> ToolError(
                "زمان انتظار تمام شد (Timeout).",
                "The operation timed out."
            )
            is SSLException -> ToolError(
                "خطای SSL/TLS: ${t.message ?: ""}",
                "SSL/TLS error: ${t.message ?: ""}"
            )
            is ConnectException -> ToolError(
                "اتصال برقرار نشد: ${t.message ?: ""}",
                "Connection failed: ${t.message ?: ""}"
            )
            is NoRouteToHostException -> ToolError("مسیری به میزبان وجود ندارد.", "No route to host.")
            is SocketException -> ToolError("خطای سوکت: ${t.message ?: ""}", "Socket error: ${t.message ?: ""}")
            is MalformedURLException -> Errs.invalidUrl().error
            is SecurityException -> ToolError(
                "دسترسی لازم وجود ندارد. مجوزهای برنامه را بررسی کنید.",
                "Permission denied. Check the app permissions."
            )
            is IOException -> ToolError("خطای شبکه: ${t.message ?: ""}", "Network error: ${t.message ?: ""}")
            is IllegalArgumentException -> ToolError(
                "ورودی نامعتبر است: ${t.message ?: ""}",
                "Invalid input: ${t.message ?: ""}"
            )
            else -> ToolError("خطای غیرمنتظره: ${t.message ?: ""}", "Unexpected error: ${t.message ?: ""}")
        }
    }
}
