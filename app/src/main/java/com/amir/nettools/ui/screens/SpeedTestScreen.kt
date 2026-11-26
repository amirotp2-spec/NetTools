package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.SpeedPhase
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.network.SpeedTestManager
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.SpeedViewModel
import java.util.Locale

@Composable
fun SpeedTestScreen(onBack: () -> Unit, vm: SpeedViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var server by rememberSaveable { mutableStateOf(SpeedTestManager.DEFAULT_SERVER) }
    AppScaffold(title = t("تست سرعت", "Speed Test"), onBack = onBack) {
        InfoNote(
            t(
                "نتایج تقریبی هستند. تست فقط با زدن دکمه شروع می‌شود و حداکثر حدود ۲۰ ثانیه طول می‌کشد. سرور باید API سازگار با Cloudflare (/__down و /__up) داشته باشد. تست دانلود تا ۵۰ مگابایت و آپلود تا ۲۵ مگابایت داده مصرف می‌کند.",
                "Results are approximate. The test only runs when you press Start and takes up to ~20 s. The server must offer a Cloudflare-compatible API (/__down and /__up). Download uses up to 50 MB and upload up to 25 MB of data."
            )
        )
        InputField(server, { server = it }, t("سرور تست", "Test server"), hint = SpeedTestManager.DEFAULT_SERVER)
        RunStopButton(state is UiState.Loading, { vm.start(server) }, vm::cancel, t("شروع تست", "Start test"), t("توقف تست", "Stop test"))

        if (state is UiState.Loading) {
            val p = progress
            AppCard {
                val phase = when (p?.phase) {
                    SpeedPhase.DOWNLOAD -> t("در حال دانلود…", "Downloading…")
                    SpeedPhase.UPLOAD -> t("در حال آپلود…", "Uploading…")
                    else -> t("اندازه‌گیری تأخیر…", "Measuring latency…")
                }
                Text(phase, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    p?.mbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "…",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (state !is UiState.Loading) ResultSection(state)
    }
}
