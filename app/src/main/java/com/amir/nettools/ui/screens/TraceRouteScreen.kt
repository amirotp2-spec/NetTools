package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.TraceViewModel
import com.amir.nettools.util.fmtMs

@Composable
fun TraceRouteScreen(onBack: () -> Unit, vm: TraceViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hops by vm.liveHops.collectAsStateWithLifecycle()
    var host by rememberSaveable { mutableStateOf("example.com") }
    val fa = LocalIsFa.current
    AppScaffold(title = "Traceroute", onBack = onBack) {
        AppCard {
            Text(t("محدودیت اندروید", "Android limitation"), fontWeight = FontWeight.SemiBold)
            InfoNote(
                t(
                    "اندروید به برنامه‌های عادی اجازه‌ی سوکت‌های خام ICMP نمی‌دهد. این ابزار ping خود سیستم را با TTL افزایشی (۱، ۲، ۳، …) اجرا می‌کند و پاسخ «Time to live exceeded» هر مسیریاب را می‌خواند. تأخیر هاپ‌های میانی تقریبی است (شامل زمان اجرای فرایند) و بعضی مسیریاب‌ها پاسخ نمی‌دهند (*). هاپ‌ها هرگز ساختگی نیستند.",
                    "Android does not give normal apps raw ICMP sockets. This tool runs the system ping with an increasing TTL (1, 2, 3, …) and reads each router's \"Time to live exceeded\" reply. Latency of intermediate hops is approximate (it includes process start-up) and some routers never answer (*). Hops are never fabricated."
                )
            )
        }
        InputField(host, { host = it }, t("میزبان / IP", "Host / IP"), hint = "example.com")
        RunStopButton(state is UiState.Loading, { vm.start(host) }, vm::cancel, t("شروع Traceroute", "Start traceroute"))

        if (hops.isNotEmpty()) {
            AppCard {
                hops.forEach { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${h.hop}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                h.ip ?: "*",
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                            if (h.hostname != null) {
                                Text(
                                    h.hostname,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (h.ip == null) {
                                Text(
                                    t("بدون پاسخ (Timeout)", "No reply (timeout)"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (h.latencyMs != null) (if (h.approx) "~" else "") + fmtMs(h.latencyMs, fa) else "—",
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                        )
                    }
                }
            }
        }
        when (val s = state) {
            is UiState.Loading -> LoadingCard(t("در حال ردیابی مسیر…", "Tracing route…"))
            is UiState.Error -> ErrorCard(s.error)
            is UiState.Success -> {
                Text(
                    if (s.data.reached) t("✅ به مقصد ${s.data.ip} رسید", "✅ Reached ${s.data.ip}")
                    else t("⚠️ به مقصد نرسید (ممکن است ICMP مسدود باشد)", "⚠️ Destination not reached (ICMP may be blocked)"),
                    fontWeight = FontWeight.SemiBold
                )
                CopyTextButton(s.data.rows(fa).joinToString("\n") { "${it.first}: ${it.second}" })
            }
            is UiState.Idle -> Unit
        }
    }
}
