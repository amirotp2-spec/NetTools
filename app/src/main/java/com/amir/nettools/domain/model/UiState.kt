package com.amir.nettools.domain.model

data class ToolError(val fa: String, val en: String)

open class ToolException(val error: ToolError) : Exception(error.en)

/** Thrown when a DNS server answers NXDOMAIN (name does not exist). */
class NxDomainException(error: ToolError) : ToolException(error)

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: ToolError) : UiState<Nothing>
}

/** Result objects expose themselves as label/value rows, localised on demand. */
interface ResultRows {
    fun rows(fa: Boolean): List<Pair<String, String>>
}
