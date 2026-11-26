package com.amir.nettools.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.NetToolsApp
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.HistoryViewModel

@Composable
fun SettingsScreen(vm: HistoryViewModel = viewModel()) {
    val ctx = LocalContext.current
    val settings = (ctx.applicationContext as NetToolsApp).settings
    var confirmClear by remember { mutableStateOf(false) }
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    AppScaffold(title = t("تنظیمات", "Settings"), topLevel = true) {
        AppCard {
            Text(t("حالت تم (Dark mode)", "Dark mode"), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    1 to t("تیره", "Dark"),
                    2 to t("روشن", "Light"),
                    0 to t("سیستم", "System")
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { settings.updateTheme(mode) },
                        label = { Text(label) }
                    )
                }
            }
        }
        AppCard {
            Text(t("زبان", "Language"), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = settings.language == "fa", onClick = { settings.updateLanguage("fa") }, label = { Text("فارسی") })
                FilterChip(selected = settings.language == "en", onClick = { settings.updateLanguage("en") }, label = { Text("English") })
            }
        }
        AppCard {
            Text(t("تاریخچه", "History"), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirmClear = true }) { Text(t("پاک کردن تاریخچه", "Clear history")) }
        }
        AppCard {
            Text(t("درباره", "About"), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("NetTools")
            InfoNote(
                t(
                    "مجموعه ابزارهای شبکه با عملیات واقعی روی دستگاه. بدون هوش مصنوعی، بدون ردیابی، بدون ارسال داده به سرور شخصی.",
                    "Network tools performing real operations on your device. No AI, no tracking, no data sent to any private server."
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(t("نسخه برنامه: ", "App version: ") + version)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(t("پاک کردن تاریخچه؟", "Clear history?")) },
            text = { Text(t("همه نتایج ذخیره‌شده حذف می‌شوند.", "All stored results will be deleted.")) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clear()
                    confirmClear = false
                }) { Text(t("پاک کردن", "Clear")) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(t("انصراف", "Cancel")) } }
        )
    }
}
