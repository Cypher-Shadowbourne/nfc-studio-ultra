package com.cyphershadowbourne.nfcstudioultra.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import com.cyphershadowbourne.nfcstudioultra.domain.model.TagScanResult

object TagParser {

    fun parse(tag: Tag): TagScanResult {
        NfcLog.d("Parsing tag: ${tag.id.joinToString("") { "%02X".format(it) }}")
        val ndef = Ndef.get(tag)
        val payloads = try {
            NdefReader.readPayloads(ndef?.cachedNdefMessage?.let { arrayOf(it) })
        } catch (e: Exception) {
            NfcLog.e("Error reading payloads from tag", e)
            emptyList()
        }

        return TagScanResult(
            tagIdHex = tag.id.joinToString(separator = "") { byte -> "%02X".format(byte) },
            payloads = payloads,
            techList = tag.techList.map { it.substringAfterLast('.') },
            isWritable = ndef?.isWritable == true,
            ndefSupported = ndef != null,
            sizeBytes = ndef?.maxSize
        )
    }
}
