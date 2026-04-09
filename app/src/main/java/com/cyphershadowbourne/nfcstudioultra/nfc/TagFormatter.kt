package com.cyphershadowbourne.nfcstudioultra.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.NdefFormatable

object TagFormatter {

    fun format(tag: Tag): Result<Unit> {
        return runCatching {
            val formatable = NdefFormatable.get(tag)
                ?: error("This tag cannot be NDEF formatted.")

            val emptyMessage = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", "")))
            formatable.connect()
            try {
                formatable.format(emptyMessage)
            } finally {
                formatable.close()
            }
        }
    }
}
