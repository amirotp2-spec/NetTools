package com.amir.nettools.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.WifiViewModel

@Composable
fun WifiInfoScreen(onBack: () -> Unit, vm: WifiViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh(false) }

    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    AppScaffold(title = t("اطلاعات Wi-Fi", "Wi-Fi Information"), onBack = onBack) {
        if (!granted) {
            AppCard {
                Text(
                    t(
                        "اندروید برای نمایش نام شبکه (SSID) و BSSID مجوز موقعیت مکانی دقیق را لازم می‌داند. این برنامه از موقعیت شما برای هیچ کار دیگری استفاده نمی‌کند و آن را ذخیره یا ارسال نمی‌کند.",
                        "Android requires the precise location permission to reveal the network name (SSID) and BSSID. NetTools uses it for nothing else and never stores or sends your location."
                    )
                )
                Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text(t("اعطای مجوز", "Grant permission"))
                }
                OutlinedButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                    )
                }) { Text(t("باز کردن تنظیمات برنامه", "Open app settings")) }
            }
        }
        ResultSection(state)
        val s = state
        if (granted && s is UiState.Success && s.data.connected && s.data.ssid == null) {
            InfoNote(
                t(
                    "SSID در دسترس نیست. مطمئن شوید سرویس موقعیت مکانی (GPS) دستگاه روشن است.",
                    "SSID is unavailable. Make sure the device's location service is turned on."
                )
            )
        }
        RunStopButton(false, { vm.refresh(true) }, {}, t("تازه‌سازی و ذخیره", "Refresh & save"))
    }
}
