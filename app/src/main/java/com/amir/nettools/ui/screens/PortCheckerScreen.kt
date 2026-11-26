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
import com.amir.nettools.ui.viewmodel.PortViewModel

@Composable
fun PortCheckerScreen(onBack: () -> Unit, vm: PortViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var host by rememberSaveable { mutableStateOf("example.com") }
    var port by rememberSaveable { mutableStateOf("443") }
    AppScaffold(title = t("بررسی پورت", "Port Checker"), onBack = onBack) {
        InfoNote(t("فقط همان یک پورتی که وارد می‌کنید با یک اتصال TCP واقعی تست می‌شود؛ اسکن بازه‌ای وجود ندارد.", "Only the single port you enter is tested with a real TCP connection. There is no range scanning."))
        InputField(host, { host = it }, t("میزبان", "Host"), hint = "example.com")
        InputField(port, { port = it }, t("پورت", "Port"), keyboardType = KeyboardType.Number)
        RunStopButton(state is UiState.Loading, { vm.start(host, port) }, vm::cancel, t("بررسی", "Check"))
        ResultSection(state)
    }
}
