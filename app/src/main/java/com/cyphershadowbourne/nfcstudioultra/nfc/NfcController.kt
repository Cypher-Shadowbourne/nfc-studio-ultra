package com.cyphershadowbourne.nfcstudioultra.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle

class NfcController(
    private val activity: Activity,
    private val onTagDiscovered: (Tag) -> Unit
) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun enableReaderMode() {
        val flags =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE

        adapter?.enableReaderMode(
            activity,
            { tag -> onTagDiscovered(tag) },
            flags,
            Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 150)
            }
        )
    }

    fun disableReaderMode() {
        adapter?.disableReaderMode(activity)
    }
}
