package com.cyphershadowbourne.nfcstudioultra.data.repository

import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import com.cyphershadowbourne.nfcstudioultra.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryHistoryRepository : HistoryRepository {

    private val items = MutableStateFlow<List<HistoryItem>>(emptyList())

    override val history: StateFlow<List<HistoryItem>> = items.asStateFlow()

    override fun add(item: HistoryItem) {
        items.value = listOf(item) + items.value
    }

    override fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override fun clear() {
        items.value = emptyList()
    }
}
