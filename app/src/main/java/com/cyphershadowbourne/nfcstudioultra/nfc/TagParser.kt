package com.cyphershadowbourne.nfcstudioultra.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import com.cyphershadowbourne.nfcstudioultra.domain.model.TagScanResult

object TagParser {

    fun parse(tag: Tag): TagScanResult {
        val ndef = Ndef.get(tag)
        val payloads = NdefReader.readPayloads(ndef?.cachedNdefMessage?.let { arrayOf(it) })

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
