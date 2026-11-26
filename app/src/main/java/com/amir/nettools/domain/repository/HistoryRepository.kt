package com.amir.nettools.domain.repository

import com.amir.nettools.domain.model.HistoryItem
import com.amir.nettools.domain.model.ToolType
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observe(): Flow<List<HistoryItem>>
    suspend fun add(tool: ToolType, target: String, summary: String, details: String)
    suspend fun delete(id: Long)
    suspend fun clear()
}
