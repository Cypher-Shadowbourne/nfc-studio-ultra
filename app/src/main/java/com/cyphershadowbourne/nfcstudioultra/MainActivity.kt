package com.cyphershadowbourne.nfcstudioultra

import android.Manifest
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.ui.NfcStudioViewModel
import com.cyphershadowbourne.nfcstudioultra.ui.screen.NfcStudioUltraScreen
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NfcStudioUltraTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: NfcStudioViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        requestWifiPermissions()

        setContent {
            NfcStudioUltraTheme {
                val state = viewModel.uiState

                NfcStudioUltraScreen(
                    state = state,
                    onReadMode = { setModeAndUpdateReader(NfcMode.READ) },
                    onWriteMode = { setModeAndUpdateReader(NfcMode.WRITE) },
                    onEraseMode = { setModeAndUpdateReader(NfcMode.ERASE) },
                    onIdleMode = { setModeAndUpdateReader(NfcMode.IDLE) },
                    onWriteTypeSelected = viewModel::setWriteType,
                    onTextChanged = viewModel::updateText,
                    onUrlChanged = viewModel::updateUrl,
                    onPhoneChanged = viewModel::updatePhoneNumber,
                    onEmailToChanged = viewModel::updateEmailTo,
                    onEmailSubjectChanged = viewModel::updateEmailSubject,
                    onEmailBodyChanged = viewModel::updateEmailBody,
                    onSmsNumberChanged = viewModel::updateSmsNumber,
                    onSmsBodyChanged = viewModel::updateSmsBody,
                    onLocationLatitudeChanged = viewModel::updateLocationLatitude,
                    onLocationLongitudeChanged = viewModel::updateLocationLongitude,
                    onContactNameChanged = viewModel::updateContactName,
                    onContactPhoneChanged = viewModel::updateContactPhone,
                    onContactEmailChanged = viewModel::updateContactEmail,
                    onContactOrganizationChanged = viewModel::updateContactOrganization,
                    onWifiSsidChanged = viewModel::updateWifiSsid,
                    onWifiPasswordChanged = viewModel::updateWifiPassword,
                    onWifiAuthTypeChanged = viewModel::updateWifiAuthType,
                    onCalendarTitleChanged = viewModel::updateCalendarTitle,
                    onCalendarLocationChanged = viewModel::updateCalendarLocation,
                    onCalendarDescriptionChanged = viewModel::updateCalendarDescription,
                    onCalendarStartChanged = viewModel::updateCalendarStart,
                    onCalendarEndChanged = viewModel::updateCalendarEnd,
                    onSmartPosterTitleChanged = viewModel::updateSmartPosterTitle,
                    onAarPackageNameChanged = viewModel::updateAarPackageName,
                    onMimeTypeChanged = viewModel::updateMimeType,
                    onMimePayloadChanged = viewModel::updateMimePayload,
                    onMimeIsHexChanged = viewModel::updateMimeIsHex,
                    onExternalDomainChanged = viewModel::updateExternalDomain,
                    onExternalTypeChanged = viewModel::updateExternalType,
                    onExternalPayloadChanged = viewModel::updateExternalPayload,
                    onExternalIsHexChanged = viewModel::updateExternalIsHex,
                    onConfirmAction = { viewModel.confirmPendingAction(this) },
                    onDismissAction = viewModel::dismissPendingAction,
                    onClearRead = viewModel::clearRead,
                    onShowInspector = viewModel::showInspector,
                    onShowInspectorForLastRead = viewModel::showInspectorForLastRead,
                    onHideInspector = viewModel::hideInspector,
                    history = viewModel.history,
                    onDeleteHistoryItem = viewModel::deleteHistoryItem,
                    onClearHistory = viewModel::clearHistory,
                    templates = viewModel.templates,
                    onSaveTemplate = viewModel::saveAsTemplate,
                    onLoadTemplate = viewModel::loadTemplate,
                    onDeleteTemplate = viewModel::deleteTemplate,
                    onRenameTemplate = viewModel::updateTemplateName,
                    onShowSaveTemplateDialog = viewModel::showTemplateSaveDialog,
                    onHideSaveTemplateDialog = viewModel::hideTemplateSaveDialog,
                    onMultiWriteMode = { setModeAndUpdateReader(NfcMode.MULTI_WRITE) },
                    onAddToMultiWriteList = viewModel::addCurrentToWriteList,
                    onRemoveFromMultiWriteList = viewModel::removeFromWriteList,
                    onMoveMultiWriteItem = viewModel::moveWriteListItem,
                    onClearMultiWriteList = viewModel::clearWriteList,
                    onCloneMode = { setModeAndUpdateReader(NfcMode.CLONE) },
                    onClearCloneData = viewModel::clearCloneData,
                    onCompareMode = { setModeAndUpdateReader(NfcMode.COMPARE) },
                    onClearCompareData = viewModel::clearCompareData,
                    urlSafetyEnabled = viewModel.urlSafetyEnabled.collectAsState().value,
                    onUrlSafetyToggle = viewModel::setUrlSafetyEnabled,
                    expertModeEnabled = viewModel.expertModeEnabled.collectAsState().value,
                    onExpertModeToggle = viewModel::setExpertModeEnabled
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateReaderMode()
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

    private fun setModeAndUpdateReader(mode: NfcMode) {
        viewModel.setMode(mode)
        updateReaderMode()
    }

    private fun updateReaderMode() {
        val adapter = nfcAdapter ?: return

        if (viewModel.uiState.mode == NfcMode.IDLE) {
            adapter.disableReaderMode(this)
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V

        adapter.enableReaderMode(this, this, flags, null)
    }

    private fun requestWifiPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}
