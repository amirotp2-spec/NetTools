package com.amir.nettools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amir.nettools.domain.model.ConnType
import com.amir.nettools.domain.model.ToolType
import com.amir.nettools.domain.model.UiState
import com.amir.nettools.ui.components.*
import com.amir.nettools.ui.theme.NtAmber
import com.amir.nettools.ui.theme.NtGreen
import com.amir.nettools.ui.theme.NtRed
import com.amir.nettools.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenTool: (ToolType) -> Unit,
    onOpenHistory: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val fa = LocalIsFa.current
    val status by vm.netStatus.collectAsStateWithLifecycle()
    val ip by vm.publicIp.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    AppScaffold(title = "NetTools", topLevel = true) {
        // Network status
        val color: Color
        val label: String
        when {
            status.type == ConnType.NONE -> {
                color = NtRed
                label = t("🔴 بدون اینترنت", "🔴 No Internet")
            }
            status.validated -> {
                color = NtGreen
                label = t("🟢 متصل", "🟢 Connected")
            }
            else -> {
                color = NtAmber
                label = t("🟡 متصل، اینترنت تأیید نشده", "🟡 Connected, internet not validated")
            }
        }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Text(
                        t("نوع اتصال: ", "Connection: ") + status.type.label(fa),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Public IP
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(t("IP عمومی", "Public IP"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { vm.refreshPublicIp() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = t("تازه‌سازی", "Refresh"))
                }
            }
            when (val s = ip) {
                is UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is UiState.Success -> {
                    val shown = s.data.ipv4 ?: s.data.ipv6 ?: ""
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shown, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(shown)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = t("کپی", "Copy"))
                        }
                    }
                    if (s.data.ipv4 != null && s.data.ipv6 != null) {
                        Text(s.data.ipv6, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is UiState.Error -> Text(t(s.error.fa, s.error.en), color = MaterialTheme.colorScheme.error)
                is UiState.Idle -> Unit
            }
        }

        // Quick tools
        SectionTitle(t("ابزارهای سریع", "Quick tools"))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ToolCatalog.all) { tool ->
                ToolCard(tool, onClick = { onOpenTool(tool.type) }, modifier = Modifier.width(150.dp))
            }
        }

        // Recent tests
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(t("آزمون‌های اخیر", "Recent tests")) }
            TextButton(onClick = onOpenHistory) { Text(t("همه", "View all")) }
        }
        if (recent.isEmpty()) {
            InfoNote(t("هنوز آزمونی انجام نشده است.", "No tests yet."))
        } else {
            val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.US)
            AppCard {
                recent.forEach { item ->
                    val tool = ToolCatalog.of(item.tool)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenHistory() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(tool.icon)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                (if (fa) tool.fa else tool.en) + " • " + item.target,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            fmt.format(Date(item.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
