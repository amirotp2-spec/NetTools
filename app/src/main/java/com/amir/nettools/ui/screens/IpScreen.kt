package com.amir.nettools.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.IpViewModel

@Composable
fun IpScreen(onBack: () -> Unit, vm: IpViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val local by vm.local.collectAsStateWithLifecycle()
    var ip by rememberSaveable { mutableStateOf("") }
    val fa = LocalIsFa.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshLocal() }
    AppScaffold(title = t("اطلاعات IP", "IP Information"), onBack = onBack) {
        SectionTitle(t("IP های این دستگاه", "This device's IPs"))
        ResultTable(local.rows(fa))
        SectionTitle(t("تحلیل یک آدرس IP", "Analyse an IP address"))
        InfoNote(
            t(
                "تحلیل به‌صورت محلی انجام می‌شود؛ فقط DNS معکوس (PTR) از شبکه پرسیده می‌شود. موقعیت جغرافیایی یا ISP نمایش داده نمی‌شود.",
                "Analysis is local; only the reverse DNS (PTR) lookup uses the network. No geolocation or ISP data is shown."
            )
        )
        InputField(ip, { ip = it }, t("آدرس IPv4 یا IPv6", "IPv4 or IPv6 address"), hint = "8.8.8.8 / 2001:db8::1")
        RunStopButton(state is UiState.Loading, { vm.analyze(ip) }, vm::cancel, t("تحلیل", "Analyse"))
        ResultSection(state)
    }
}
