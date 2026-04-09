package com.cyphershadowbourne.nfcstudioultra.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset
import java.util.Locale

object NdefReader {

    fun readPayloads(messages: Array<NdefMessage>?): List<String> {
        if (messages.isNullOrEmpty()) return emptyList()

        return messages
            .flatMap { it.records.toList() }
            .map { record -> decodeRecord(record) }
            .filter { it.isNotBlank() }
    }

    private fun decodeRecord(record: NdefRecord): String {
        return when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                decodeTextRecord(record)
            }

            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) -> {
                record.toUri()?.toString().orEmpty()
            }

            else -> record.payload.toString(Charsets.UTF_8)
        }
    }

    private fun decodeTextRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload.isEmpty()) return ""

        val status = payload[0].toInt()
        val isUtf16 = status and 0x80 != 0
        val languageCodeLength = status and 0x3F
        val textCharset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8

        return payload.copyOfRange(1 + languageCodeLength, payload.size).toString(textCharset)
    }

    private fun ByteArray.toString(charset: Charset): String = String(this, charset).trim()
}
