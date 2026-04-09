package com.cyphershadowbourne.nfcstudioultra

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefRecordType
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.ui.NfcStudioViewModel
import com.cyphershadowbourne.nfcstudioultra.ui.screen.NfcStudioUltraScreen
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NfcStudioUltraTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: NfcStudioViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            NfcStudioUltraTheme {
                val state = viewModel.uiState

                NfcStudioUltraScreen(
                    state = state,
                    onReadMode = { viewModel.setMode(NfcMode.READ) },
                    onWriteMode = { viewModel.setMode(NfcMode.WRITE) },
                    onEraseMode = { viewModel.setMode(NfcMode.ERASE) },
                    onIdleMode = { viewModel.setMode(NfcMode.IDLE) },
                    onWriteTypeSelected = viewModel::setWriteType,
                    onTextChanged = viewModel::updateText,
                    onUrlChanged = viewModel::updateUrl,
                    onPhoneChanged = viewModel::updatePhoneNumber,
                    onEmailToChanged = viewModel::updateEmailTo,
                    onEmailSubjectChanged = viewModel::updateEmailSubject,
                    onEmailBodyChanged = viewModel::updateEmailBody,
                    onSmsNumberChanged = viewModel::updateSmsNumber,
                    onSmsBodyChanged = viewModel::updateSmsBody,
                    onConfirmAction = { viewModel.confirmPendingAction(this) },
                    onDismissAction = viewModel::dismissPendingAction,
                    onClearRead = viewModel::clearRead
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V

        nfcAdapter?.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        runOnUiThread {
            viewModel.onTag(tag, this)
        }
    }
}