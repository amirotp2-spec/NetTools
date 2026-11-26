package com.amir.nettools.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amir.nettools.domain.model.ToolType
import com.amir.nettools.ui.components.*

@Composable
fun ToolsScreen(onOpenTool: (ToolType) -> Unit) {
    ScaffoldBase(title = t("ابزارها", "Tools"), onBack = null, topLevel = true, actions = {}) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ToolCatalog.all) { tool ->
                ToolCard(tool, onClick = { onOpenTool(tool.type) })
            }
        }
    }
}
