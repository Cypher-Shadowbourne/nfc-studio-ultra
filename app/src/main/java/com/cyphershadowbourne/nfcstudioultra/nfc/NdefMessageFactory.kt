package com.cyphershadowbourne.nfcstudioultra.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcPayloadType
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcWriteRequest

object NdefMessageFactory {

    fun create(request: NfcWriteRequest): NdefMessage {
        val record = when (request.payloadType) {
            NfcPayloadType.TEXT -> NdefRecord.createTextRecord("en", request.payloadValue)
            NfcPayloadType.URL,
            NfcPayloadType.URI -> NdefRecord.createUri(Uri.parse(request.payloadValue))
        }

        return NdefMessage(arrayOf(record))
    }
}
