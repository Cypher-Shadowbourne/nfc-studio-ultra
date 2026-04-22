package com.cyphershadowbourne.nfcstudioultra.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.NdefFormatable

object TagFormatter {

    fun format(tag: Tag): Result<Unit> {
        return runCatching {
            NfcLog.d("Attempting to format tag...")
            val formatable = NdefFormatable.get(tag)
                ?: error("This tag cannot be NDEF formatted.")

            val emptyMessage = NdefMessage(arrayOf(NdefRecord.createTextRecord("en", "")))
            formatable.connect()
            try {
                formatable.format(emptyMessage)
                NfcLog.i("Successfully formatted tag.")
            } finally {
                formatable.close()
            }
        }.onFailure { throwable ->
            NfcLog.e("TagFormatter error: ${throwable.message}", throwable)
        }
    }
}
