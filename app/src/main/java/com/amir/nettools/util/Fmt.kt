package com.amir.nettools.util

import java.util.Locale

fun tr(fa: Boolean, f: String, e: String): String = if (fa) f else e

fun na(fa: Boolean): String = if (fa) "در دسترس نیست" else "Unavailable"

fun fmtMs(v: Double?, fa: Boolean): String =
    if (v == null) na(fa) else String.format(Locale.US, "%.1f ms", v)

fun fmtPct(v: Double): String = String.format(Locale.US, "%.0f%%", v)

fun fmtMbps(v: Double?, fa: Boolean): String =
    if (v == null) na(fa) else String.format(Locale.US, "%.2f Mbps", v)

fun yn(b: Boolean, fa: Boolean): String = if (b) tr(fa, "بله", "Yes") else tr(fa, "خیر", "No")

fun orNa(s: String?, fa: Boolean): String = if (s.isNullOrBlank()) na(fa) else s

fun joinOrNa(l: List<String>, fa: Boolean): String = if (l.isEmpty()) na(fa) else l.joinToString("\n")

object Fmt2 {
    fun mbps(v: Double?): String =
        if (v == null) "-" else String.format(java.util.Locale.US, "%.2f Mbps", v)
}
