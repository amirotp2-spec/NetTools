package com.amir.nettools.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.HttpViewModel

@Composable
fun HttpTestScreen(onBack: () -> Unit, vm: HttpViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf("https://example.com") }
    AppScaffold(title = t("تست HTTP/HTTPS", "HTTP/HTTPS Test"), onBack = onBack) {
        InfoNote(t("یک درخواست GET واقعی ارسال می‌شود (بدنه پاسخ خوانده نمی‌شود).", "Sends a real GET request (the response body is not downloaded)."))
        InputField(url, { url = it }, "URL", hint = "https://example.com")
        RunStopButton(state is UiState.Loading, { vm.start(url) }, vm::cancel, t("شروع تست", "Run test"))
        ResultSection(state)
    }
}
