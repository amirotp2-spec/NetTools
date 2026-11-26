package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.SubnetViewModel

@Composable
fun SubnetScreen(onBack: () -> Unit, vm: SubnetViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var ip by rememberSaveable { mutableStateOf("192.168.1.10") }
    var cidr by rememberSaveable { mutableStateOf("24") }
    AppScaffold(title = t("ماشین‌حساب Subnet", "Subnet Calculator"), onBack = onBack) {
        InfoNote(t("تمام محاسبات به‌صورت محلی و بدون شبکه انجام می‌شود.", "All calculations are done locally, without any network access."))
        InputField(ip, { ip = it }, t("آدرس IPv4", "IPv4 address"), hint = "192.168.1.10", keyboardType = KeyboardType.Decimal)
        InputField(cidr, { cidr = it }, "CIDR", hint = "24", keyboardType = KeyboardType.Number)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(8, 16, 20, 22, 24, 26, 27, 28, 29, 30, 31, 32)) { c ->
                AssistChip(onClick = { cidr = c.toString() }, label = { Text("/$c") })
            }
        }
        RunStopButton(false, { vm.calculate(ip, cidr) }, {}, t("محاسبه", "Calculate"))
        ResultSection(state)
    }
}
