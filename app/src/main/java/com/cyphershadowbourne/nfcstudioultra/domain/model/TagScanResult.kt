package com.cyphershadowbourne.nfcstudioultra.domain.model

data class TagScanResult(
    val tagIdHex: String,
    val payloads: List<String>,
    val techList: List<String>,
    val isWritable: Boolean,
    val ndefSupported: Boolean,
    val sizeBytes: Int?
)
