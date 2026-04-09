package com.cyphershadowbourne.nfcstudioultra.domain.model

data class HistoryItem(
    val id: String,
    val title: String,
    val detail: String,
    val timestampLabel: String,
    val operation: String,
    val techList: List<String> = emptyList()
)
