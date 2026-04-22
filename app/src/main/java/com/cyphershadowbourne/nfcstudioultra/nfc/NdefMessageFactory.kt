package com.cyphershadowbourne.nfcstudioultra.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcPayloadType
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcWriteRequest

object NdefMessageFactory {

    fun create(request: NfcWriteRequest): NdefMessage? {
        return try {
            val record = when (request.payloadType) {
                NfcPayloadType.TEXT -> NdefRecord.createTextRecord("en", request.payloadValue)
                NfcPayloadType.URL,
                NfcPayloadType.URI -> {
                    val uri = Uri.parse(request.payloadValue)
                    if (uri.scheme == null) {
                        NdefRecord.createUri(Uri.parse("https://${request.payloadValue}"))
                    } else {
                        NdefRecord.createUri(uri)
                    }
                }
            }
            NdefMessage(arrayOf(record))
        } catch (e: Exception) {
            NfcLog.e("Failed to create NdefMessage for request: $request", e)
            null
        }
    }
}
