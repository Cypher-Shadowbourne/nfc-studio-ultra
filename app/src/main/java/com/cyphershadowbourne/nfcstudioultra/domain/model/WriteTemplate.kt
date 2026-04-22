package com.cyphershadowbourne.nfcstudioultra.domain.model

import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteData

data class WriteTemplate(
    val id: String,
    val name: String,
    val data: NdefWriteData
)
