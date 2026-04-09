package com.cyphershadowbourne.nfcstudioultra.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

object NdefWriter {

    fun write(tag: Tag, message: NdefMessage): Result<Unit> {
        return runCatching {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    require(ndef.isWritable) { "This NFC tag is read-only." }
                    val size = message.toByteArray().size
                    require(ndef.maxSize >= size) {
                        "Tag capacity is ${ndef.maxSize} bytes, but payload requires $size bytes."
                    }
                    ndef.writeNdefMessage(message)
                } finally {
                    ndef.close()
                }
                return@runCatching
            }

            val formatable = NdefFormatable.get(tag)
                ?: error("This tag does not support NDEF formatting or writing.")

            formatable.connect()
            try {
                formatable.format(message)
            } finally {
                formatable.close()
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is FormatException -> throw IllegalStateException("The tag rejected the NDEF message format.", throwable)
                else -> throw throwable
            }
        }
    }
}
