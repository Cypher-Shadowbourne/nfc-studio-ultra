package com.cyphershadowbourne.nfcstudioultra.domain.model

data class NfcWriteRequest(
    val label: String,
    val payloadType: NfcPayloadType,
    val payloadValue: String
)
