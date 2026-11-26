package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.HistoryItem
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: HistoryViewModel = viewModel()) {
    val list by vm.items.collectAsStateWithLifecycle()
    val fa = LocalIsFa.current
    var viewing by remember { mutableStateOf<HistoryItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    ScaffoldBase(
        title = t("تاریخچه", "History"),
        onBack = null,
        topLevel = true,
        actions = {
            if (list.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = t("پاک کردن همه", "Clear all"))
                }
            }
        }
    ) { padding ->
        if (list.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
                InfoNote(t("تاریخچه خالی است. نتیجه آزمون‌ها پس از اجرای موفق اینجا ذخیره می‌شود.", "History is empty. Results of successful tests are stored here."))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    val tool = ToolCatalog.of(item.tool)
                    AppCard(onClick = { viewing = item }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(tool.icon)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (fa) tool.fa else tool.en, fontWeight = FontWeight.SemiBold)
                                Text(item.target, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    item.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    fmt.format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewing = item }) {
                                Icon(Icons.Filled.Visibility, contentDescription = t("مشاهده", "View"))
                            }
                            IconButton(onClick = { vm.delete(item.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = t("حذف", "Delete"))
                            }
                        }
                    }
                }
            }
        }
    }

    viewing?.let { item ->
        val tool = ToolCatalog.of(item.tool)
        AlertDialog(
            onDismissRequest = { viewing = null },
            title = { Text((if (fa) tool.fa else tool.en) + " • " + item.target, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text(item.details, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text(t("بستن", "Close")) } },
            dismissButton = {
                TextButton(onClick = {
                    vm.delete(item.id)
                    viewing = null
                }) { Text(t("حذف", "Delete")) }
            }
        )
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
