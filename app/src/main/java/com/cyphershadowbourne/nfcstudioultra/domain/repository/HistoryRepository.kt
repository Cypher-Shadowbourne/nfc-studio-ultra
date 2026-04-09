package com.cyphershadowbourne.nfcstudioultra.domain.repository

import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import kotlinx.coroutines.flow.StateFlow

interface HistoryRepository {
    val history: StateFlow<List<HistoryItem>>
    fun add(item: HistoryItem)
    fun delete(id: String)
    fun clear()
}
