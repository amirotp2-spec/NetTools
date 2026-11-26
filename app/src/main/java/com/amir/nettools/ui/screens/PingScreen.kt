package com.amir.nettools.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.PingViewModel

@Composable
fun PingScreen(onBack: () -> Unit, vm: PingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var host by rememberSaveable { mutableStateOf("example.com") }
    var count by rememberSaveable { mutableStateOf("4") }
    AppScaffold(title = t("پینگ", "Ping"), onBack = onBack) {
        InfoNote(t("ICMP واقعی با ابزار ping خود سیستم اجرا می‌شود.", "Real ICMP echo using the system ping tool."))
        InputField(host, { host = it }, t("میزبان / IP", "Host / IP"), hint = "example.com")
        InputField(
            count, { count = it }, t("تعداد بسته (اختیاری، حداکثر ۲۰)", "Packet count (optional, max 20)"),
            keyboardType = KeyboardType.Number
        )
        RunStopButton(state is UiState.Loading, { vm.start(host, count) }, vm::cancel, t("شروع پینگ", "Start ping"))
        ResultSection(state)
    }
}
