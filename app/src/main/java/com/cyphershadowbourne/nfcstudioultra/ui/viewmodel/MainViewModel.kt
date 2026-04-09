package com.cyphershadowbourne.nfcstudioultra.ui.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyphershadowbourne.nfcstudioultra.domain.model.AppMode
import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcPayloadType
import com.cyphershadowbourne.nfcstudioultra.domain.model.NfcWriteRequest
import com.cyphershadowbourne.nfcstudioultra.domain.repository.HistoryRepository
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefMessageFactory
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriter
import com.cyphershadowbourne.nfcstudioultra.nfc.TagFormatter
import com.cyphershadowbourne.nfcstudioultra.nfc.TagParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val internalState = MutableStateFlow(MainUiState())
    private var lastDiscoveredTag: Tag? = null

    val uiState: StateFlow<MainUiState> = combine(
        internalState,
        historyRepository.history
    ) { state, history ->
        state.copy(history = history)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun setMode(mode: AppMode) {
        internalState.update {
            it.copy(
                mode = mode,
                bannerMessage = if (mode == AppMode.READ) {
                    "Read mode active. Tap an NFC tag."
                } else {
                    "Write mode active. Configure payload, then tap Prepare Write."
                }
            )
        }
    }

    fun setPayloadType(type: NfcPayloadType) {
        internalState.update { it.copy(payloadType = type) }
    }

    fun setPayloadValue(value: String) {
        internalState.update { it.copy(payloadValue = value) }
    }

    fun setRecordLabel(value: String) {
        internalState.update { it.copy(recordLabel = value) }
    }

    fun prepareWriteOperation() {
        val state = internalState.value
        if (state.payloadValue.isBlank()) {
            internalState.update { it.copy(bannerMessage = "Payload is empty. Enter text or a URI first.") }
            return
        }

        internalState.update {
            it.copy(
                mode = AppMode.WRITE,
                pendingWrite = true,
                bannerMessage = "Write armed. Tap the NFC tag now."
            )
        }
    }

    fun cancelWriteOperation() {
        internalState.update {
            it.copy(
                pendingWrite = false,
                bannerMessage = "Write cancelled."
            )
        }
    }

    fun handleTagDiscovered(tag: Tag) {
        lastDiscoveredTag = tag
        val state = internalState.value

        if (state.pendingWrite) {
            writeToTag(tag, state)
        } else {
            readTag(tag)
        }
    }

    fun formatLastTag() {
        val tag = lastDiscoveredTag
        if (tag == null) {
            internalState.update { it.copy(bannerMessage = "No tag available yet. Tap a tag first.") }
            return
        }

        TagFormatter.format(tag)
            .onSuccess {
                internalState.update { it.copy(bannerMessage = "Tag formatted for NDEF successfully.") }
                addHistory(
                    title = "Tag formatted",
                    detail = "Tag ${TagParser.parse(tag).tagIdHex}",
                    operation = "FORMAT",
                    techList = tag.techList.map { tech -> tech.substringAfterLast('.') }
                )
            }
            .onFailure { error ->
                internalState.update { it.copy(bannerMessage = error.message ?: "Format failed.") }
            }
    }

    fun deleteHistoryItem(id: String) {
        historyRepository.delete(id)
    }

    fun clearHistory() {
        historyRepository.clear()
        internalState.update { it.copy(bannerMessage = "History cleared.") }
    }

    fun clearBanner() {
        internalState.update { it.copy(bannerMessage = null) }
    }

    private fun readTag(tag: Tag) {
        val result = TagParser.parse(tag)
        val payloadSummary = result.payloads.ifEmpty { listOf("No NDEF payload found") }.joinToString(" • ")

        internalState.update {
            it.copy(
                lastScan = result,
                bannerMessage = "Tag read successfully."
            )
        }

        addHistory(
            title = stateLabelForRead(result),
            detail = payloadSummary,
            operation = "READ",
            techList = result.techList
        )
    }

    private fun writeToTag(tag: Tag, state: MainUiState) {
        val request = NfcWriteRequest(
            label = state.recordLabel.ifBlank { state.payloadType.label },
            payloadType = state.payloadType,
            payloadValue = state.payloadValue.trim()
        )
        val message = NdefMessageFactory.create(request)

        NdefWriter.write(tag, message)
            .onSuccess {
                val scan = TagParser.parse(tag)
                internalState.update {
                    it.copy(
                        pendingWrite = false,
                        lastScan = scan,
                        bannerMessage = "Write completed successfully.",
                        payloadValue = "",
                        recordLabel = ""
                    )
                }
                addHistory(
                    title = "${request.payloadType.label} written",
                    detail = request.payloadValue,
                    operation = "WRITE",
                    techList = scan.techList
                )
            }
            .onFailure { error ->
                internalState.update {
                    it.copy(
                        pendingWrite = false,
                        bannerMessage = error.message ?: "Write failed."
                    )
                }
            }
    }

    private fun stateLabelForRead(result: com.cyphershadowbourne.nfcstudioultra.domain.model.TagScanResult): String {
        return when {
            result.payloads.isNotEmpty() -> "Payload captured"
            result.ndefSupported -> "Blank NDEF tag"
            else -> "Non-NDEF tag detected"
        }
    }

    private fun addHistory(
        title: String,
        detail: String,
        operation: String,
        techList: List<String>
    ) {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm")
        historyRepository.add(
            HistoryItem(
                id = UUID.randomUUID().toString(),
                title = title,
                detail = detail,
                timestampLabel = LocalDateTime.now().format(formatter),
                operation = operation,
                techList = techList
            )
        )
    }
}
