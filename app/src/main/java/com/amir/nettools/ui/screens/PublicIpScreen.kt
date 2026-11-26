package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.PublicIpViewModel
import com.amir.nettools.util.na

@Composable
private fun IpRow(label: String, value: String?) {
    val clipboard = LocalClipboardManager.current
    val fa = LocalIsFa.current
    AppCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value ?: na(fa),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr)
                )
            }
            if (value != null) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = t("کپی", "Copy"))
                }
            }
        }
    }
}

@Composable
fun PublicIpScreen(onBack: () -> Unit, vm: PublicIpViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh(false) }
    AppScaffold(title = t("IP عمومی", "Public IP"), onBack = onBack) {
        InfoNote(
            t(
                "از سرویس رایگان ipify (با پشتیبان icanhazip) استفاده می‌شود. فقط یک درخواست ساده HTTPS ارسال می‌شود.",
                "Uses the free ipify service (icanhazip as fallback). Only a plain HTTPS request is sent."
            )
        )
        when (val s = state) {
            is UiState.Success -> {
                IpRow(t("IPv4 عمومی", "Public IPv4"), s.data.ipv4)
                IpRow(t("IPv6 عمومی", "Public IPv6"), s.data.ipv6)
            }
            is UiState.Loading -> LoadingCard()
            is UiState.Error -> ErrorCard(s.error)
            is UiState.Idle -> Unit
        }
        RunStopButton(state is UiState.Loading, { vm.refresh(true) }, vm::cancel, t("تازه‌سازی", "Refresh"))
    }
}
