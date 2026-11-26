package com.amir.nettools.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.DnsType
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.DnsViewModel

@Composable
fun DnsScreen(onBack: () -> Unit, vm: DnsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var host by rememberSaveable { mutableStateOf("example.com") }
    var selected by rememberSaveable { mutableStateOf("A") }
    val options = DnsType.entries.map { it.name } + "ALL"
    AppScaffold(title = t("جستجوی DNS", "DNS Lookup"), onBack = onBack) {
        InfoNote(
            t(
                "پرس‌وجوی واقعی DNS روی UDP/TCP پورت ۵۳ به سرورهای DNS شبکه فعلی (و در صورت نیاز 1.1.1.1 / 8.8.8.8).",
                "Real DNS queries over UDP/TCP port 53 to the current network's DNS servers (falling back to 1.1.1.1 / 8.8.8.8)."
            )
        )
        InputField(host, { host = it }, t("دامنه", "Domain"), hint = "example.com")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { name ->
                FilterChip(
                    selected = selected == name,
                    onClick = { selected = name },
                    label = { Text(if (name == "ALL") t("همه", "All") else name) }
                )
            }
        }
        RunStopButton(
            state is UiState.Loading,
            { vm.start(host, DnsType.entries.firstOrNull { it.name == selected }) },
            vm::cancel,
            t("جستجو", "Lookup")
        )
        ResultSection(state)
    }
}
