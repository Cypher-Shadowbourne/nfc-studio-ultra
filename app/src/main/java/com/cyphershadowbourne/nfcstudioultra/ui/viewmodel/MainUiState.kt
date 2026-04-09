package com.cyphershadowbourne.nfcstudioultra.ui.viewmodel

import com.cyphershadowbourne.nfcstudioultra.domain.model.AppMode
import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcPayloadType
import com.cyphershadowbourne.nfcstudioultra.domain.model.TagScanResult

data class MainUiState(
    val authorName: String = "Cypher Shadowbourne",
    val appTitle: String = "NFC Studio Ultra",
    val mode: AppMode = AppMode.READ,
    val payloadType: NfcPayloadType = NfcPayloadType.TEXT,
    val payloadValue: String = "",
    val recordLabel: String = "",
    val pendingWrite: Boolean = false,
    val bannerMessage: String? = "Ready. Tap a tag to read it.",
    val lastScan: TagScanResult? = null,
    val history: List<HistoryItem> = emptyList()
)
