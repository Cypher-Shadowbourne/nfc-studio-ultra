package com.cyphershadowbourne.nfcstudioultra.domain.model

import com.cyphershadowbourne.nfcstudioultra.nfc.ParsedNdefRecord

data class HistoryItem(
    val id: String,
    val timestamp: Long,
    val recordTypes: List<String>,
    val previewText: String,
    val techList: List<String>,
    val isWritable: Boolean,
    val sizeBytes: Int?,
    val tagIdHex: String?,
    val records: List<ParsedNdefRecord> = emptyList()
)
