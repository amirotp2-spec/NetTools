package com.amir.nettools.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.NetworkInfoViewModel

@Composable
fun NetworkInfoScreen(onBack: () -> Unit, vm: NetworkInfoViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh(false) }
    AppScaffold(title = t("اطلاعات شبکه", "Network Information"), onBack = onBack) {
        InfoNote(
            t(
                "اگر اندروید مقداری را ارائه ندهد (یا مجوز لازم نباشد/داده نشده باشد) «در دسترس نیست» نمایش داده می‌شود. SSID به مجوز موقعیت مکانی نیاز دارد (صفحه Wi-Fi).",
                "Values Android does not provide show \"Unavailable\". The SSID needs the location permission (see the Wi-Fi screen)."
            )
        )
        ResultSection(state)
        RunStopButton(false, { vm.refresh(true) }, {}, t("تازه‌سازی و ذخیره", "Refresh & save"))
    }
}
