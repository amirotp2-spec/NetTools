package com.amir.nettools.data.repository

import com.amir.nettools.data.local.HistoryDao
import com.amir.nettools.data.local.HistoryEntity
import com.amir.nettools.domain.model.HistoryItem
import com.amir.nettools.domain.model.ToolType
import com.amir.nettools.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(private val dao: HistoryDao) : HistoryRepository {
    override fun observe(): Flow<List<HistoryItem>> = dao.observeAll().map { list ->
        list.map {
            HistoryItem(it.id, ToolType.fromKey(it.tool), it.target, it.summary, it.details, it.timestamp)
        }
    }

    override suspend fun add(tool: ToolType, target: String, summary: String, details: String) {
        dao.insert(
            HistoryEntity(
                tool = tool.key,
                target = target.take(300),
                summary = summary.take(500),
                details = details.take(20_000),
                timestamp = System.currentTimeMillis()
            )
        )
        dao.trim()
    }

    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun clear() = dao.clear()
}
