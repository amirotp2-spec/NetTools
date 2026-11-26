package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.ConnectivityViewModel

@Composable
fun ConnectivityScreen(onBack: () -> Unit, vm: ConnectivityViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.check(false) }
    AppScaffold(title = t("اتصال اینترنت", "Internet Connectivity"), onBack = onBack) {
        InfoNote(
            t(
                "علاوه بر ConnectivityManager، یک اتصال TCP واقعی به چند سرور عمومی برقرار می‌شود.",
                "Besides ConnectivityManager, a real TCP connection to public servers is attempted."
            )
        )
        val s = state
        if (s is UiState.Success) {
            AppCard {
                Text(
                    if (s.data.connected) t("🟢 متصل", "🟢 Connected") else t("🔴 بدون اینترنت", "🔴 No Internet"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        ResultSection(state)
        RunStopButton(state is UiState.Loading, { vm.check(true) }, vm::cancel, t("بررسی مجدد", "Check again"))
    }
}
