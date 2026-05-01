package com.cyphershadowbourne.nfcstudioultra.ui

import android.content.Context
import android.nfc.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyphershadowbourne.nfcstudioultra.data.repository.SharedPrefsHistoryRepository
import com.cyphershadowbourne.nfcstudioultra.data.repository.SharedPrefsSettingsRepository
import com.cyphershadowbourne.nfcstudioultra.data.repository.SharedPrefsTemplateRepository
import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import com.cyphershadowbourne.nfcstudioultra.domain.model.WriteTemplate
import com.cyphershadowbourne.nfcstudioultra.domain.repository.HistoryRepository
import com.cyphershadowbourne.nfcstudioultra.data.repository.SettingsRepository
import com.cyphershadowbourne.nfcstudioultra.domain.repository.TemplateRepository
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefContent
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefRecordType
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteData
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcIntentHandler
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcManager
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcOperationResult
import com.cyphershadowbourne.nfcstudioultra.nfc.ParsedNdefRecord
import com.cyphershadowbourne.nfcstudioultra.nfc.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NfcStudioViewModel(context: android.app.Application) : androidx.lifecycle.AndroidViewModel(context) {

    private val nfcManager = NfcManager()
    private val historyRepository: HistoryRepository = SharedPrefsHistoryRepository(context)
    private val templateRepository: TemplateRepository = SharedPrefsTemplateRepository(context)
    private val settingsRepository: SettingsRepository = SharedPrefsSettingsRepository(context)
    private var feedbackEventCounter = 0L

    val history: StateFlow<List<HistoryItem>> = historyRepository.history
    val templates: StateFlow<List<WriteTemplate>> = templateRepository.templates
    val urlSafetyEnabled: StateFlow<Boolean> = settingsRepository.urlSafetyEnabled
    val expertModeEnabled: StateFlow<Boolean> = settingsRepository.expertModeEnabled

    var uiState by mutableStateOf(NfcStudioUiState())
        private set

    fun addCurrentToWriteList() {
        val current = uiState.writeData
        if (isWriteDataValid(current)) {
            val newList = uiState.multiWriteList + current
            uiState = uiState.copy(
                multiWriteList = newList,
                lastActionMessage = "Added record to multi-write list"
            )
        }
    }

    fun removeFromWriteList(index: Int) {
        val newList = uiState.multiWriteList.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
            uiState = uiState.copy(
                multiWriteList = newList,
                lastActionMessage = "Removed record from multi-write list"
            )
        }
    }

    fun moveWriteListItem(fromIndex: Int, toIndex: Int) {
        val newList = uiState.multiWriteList.toMutableList()
        if (fromIndex in newList.indices && toIndex in newList.indices) {
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            uiState = uiState.copy(
                multiWriteList = newList,
                lastActionMessage = "Reordered records"
            )
        }
    }

    fun clearWriteList() {
        uiState = uiState.copy(
            multiWriteList = emptyList(),
            lastActionMessage = "Cleared multi-write list"
        )
    }

    fun clearCloneData() {
        uiState = uiState.copy(
            cloneMessage = null,
            lastActionMessage = "Clone data cleared"
        )
    }

    fun clearCompareData() {
        uiState = uiState.copy(
            compareTag1 = null,
            compareResult = null,
            lastActionMessage = "Comparison data cleared"
        )
    }

    fun setUrlSafetyEnabled(enabled: Boolean) {
        settingsRepository.setUrlSafetyEnabled(enabled)
    }

    fun setExpertModeEnabled(enabled: Boolean) {
        settingsRepository.setExpertModeEnabled(enabled)
        uiState = uiState.copy(expertMode = enabled)
    }

    fun deleteHistoryItem(id: String) {
        historyRepository.delete(id)
    }

    fun clearHistory() {
        historyRepository.clear()
    }

    fun setMode(mode: NfcMode) {
        val isValid = uiState.writeData.isValid()

        uiState = uiState.copy(
            mode = mode,
            status = modeStatusText(mode, isValid),
            statusTone = modeStatusTone(mode, isValid),
            armedMessage = modeInstructionText(mode, isValid),
            lastActionMessage = when (mode) {
                NfcMode.READ -> "Read mode selected"
                NfcMode.WRITE -> "Write mode selected"
                NfcMode.MULTI_WRITE -> "Multi-write mode selected"
                NfcMode.CLONE -> "Clone mode selected"
                NfcMode.COMPARE -> "Compare mode selected"
                NfcMode.ERASE -> "Erase mode selected"
                NfcMode.IDLE -> "NFC stopped"
            },
            pendingAction = null,
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun setWriteType(type: NdefRecordType) {
        val updatedWriteData = uiState.writeData.copy(type = type)
        val isValid = updatedWriteData.isValid()

        uiState = uiState.copy(
            writeData = updatedWriteData,
            status = if (uiState.mode == NfcMode.WRITE) {
                modeStatusText(NfcMode.WRITE, isValid)
            } else {
                uiState.status
            },
            statusTone = if (uiState.mode == NfcMode.WRITE) {
                modeStatusTone(NfcMode.WRITE, isValid)
            } else {
                uiState.statusTone
            },
            armedMessage = modeInstructionText(uiState.mode, isValid),
            lastActionMessage = "Write type set to ${type.name}",
            pendingAction = null,
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun updateText(value: String) {
        updateWriteData(uiState.writeData.copy(text = value))
    }

    fun updateUrl(value: String) {
        updateWriteData(uiState.writeData.copy(url = value))
    }

    fun updatePhoneNumber(value: String) {
        updateWriteData(uiState.writeData.copy(phoneNumber = value))
    }

    fun updateEmailTo(value: String) {
        updateWriteData(uiState.writeData.copy(emailTo = value))
    }

    fun updateEmailSubject(value: String) {
        updateWriteData(uiState.writeData.copy(emailSubject = value))
    }

    fun updateEmailBody(value: String) {
        updateWriteData(uiState.writeData.copy(emailBody = value))
    }

    fun updateSmsNumber(value: String) {
        updateWriteData(uiState.writeData.copy(smsNumber = value))
    }

    fun updateSmsBody(value: String) {
        updateWriteData(uiState.writeData.copy(smsBody = value))
    }

    fun updateLocationLatitude(value: String) {
        updateWriteData(uiState.writeData.copy(locationLatitude = value))
    }

    fun updateLocationLongitude(value: String) {
        updateWriteData(uiState.writeData.copy(locationLongitude = value))
    }

    fun updateContactName(value: String) {
        updateWriteData(uiState.writeData.copy(contactName = value))
    }

    fun updateContactPhone(value: String) {
        updateWriteData(uiState.writeData.copy(contactPhone = value))
    }

    fun updateContactEmail(value: String) {
        updateWriteData(uiState.writeData.copy(contactEmail = value))
    }

    fun updateContactOrganization(value: String) {
        updateWriteData(uiState.writeData.copy(contactOrganization = value))
    }

    fun updateWifiSsid(value: String) {
        updateWriteData(uiState.writeData.copy(wifiSsid = value))
    }

    fun updateWifiPassword(value: String) {
        updateWriteData(uiState.writeData.copy(wifiPassword = value))
    }

    fun updateWifiAuthType(value: String) {
        updateWriteData(uiState.writeData.copy(wifiAuthType = value))
    }

    fun updateCalendarTitle(value: String) {
        updateWriteData(uiState.writeData.copy(calendarTitle = value))
    }

    fun updateCalendarLocation(value: String) {
        updateWriteData(uiState.writeData.copy(calendarLocation = value))
    }

    fun updateCalendarDescription(value: String) {
        updateWriteData(uiState.writeData.copy(calendarDescription = value))
    }

    fun updateCalendarStart(value: String) {
        updateWriteData(uiState.writeData.copy(calendarStart = value))
    }

    fun updateCalendarEnd(value: String) {
        updateWriteData(uiState.writeData.copy(calendarEnd = value))
    }

    fun updateSmartPosterTitle(value: String) {
        updateWriteData(uiState.writeData.copy(smartPosterTitle = value))
    }

    fun updateAarPackageName(value: String) {
        updateWriteData(uiState.writeData.copy(aarPackageName = value))
    }

    fun updateMimeType(value: String) {
        updateWriteData(uiState.writeData.copy(mimeType = value))
    }

    fun updateMimePayload(value: String) {
        updateWriteData(uiState.writeData.copy(mimePayload = value))
    }

    fun updateMimeIsHex(value: Boolean) {
        updateWriteData(uiState.writeData.copy(mimeIsHex = value))
    }

    fun updateExternalDomain(value: String) {
        updateWriteData(uiState.writeData.copy(externalDomain = value))
    }

    fun updateExternalType(value: String) {
        updateWriteData(uiState.writeData.copy(externalType = value))
    }

    fun updateExternalPayload(value: String) {
        updateWriteData(uiState.writeData.copy(externalPayload = value))
    }

    fun updateExternalIsHex(value: Boolean) {
        updateWriteData(uiState.writeData.copy(externalIsHex = value))
    }

    fun clearRead() {
        uiState = uiState.copy(
            lastRead = "",
            lastDetails = "",
            lastReadContent = null,
            lastReadRecords = emptyList(),
            pendingAction = null,
            logs = emptyList(),
            status = "Result cleared.",
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
            lastActionMessage = "Result cleared",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun showInspector(item: HistoryItem) {
        uiState = uiState.copy(inspectorItem = item)
    }

    fun showInspectorForLastRead() {
        val lastReadItem = HistoryItem(
            id = "last_read",
            timestamp = System.currentTimeMillis(),
            recordTypes = listOf(uiState.lastReadContent?.javaClass?.simpleName ?: "Unknown"),
            previewText = uiState.lastRead,
            techList = emptyList(), // We could potentially store techList in uiState if needed
            isWritable = false,
            sizeBytes = null,
            tagIdHex = null,
            records = uiState.lastReadRecords
        )
        uiState = uiState.copy(inspectorItem = lastReadItem)
    }

    fun hideInspector() {
        uiState = uiState.copy(inspectorItem = null)
    }

    fun saveAsTemplate(name: String) {
        val template = WriteTemplate(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Template ${System.currentTimeMillis()}" },
            data = uiState.writeData
        )
        templateRepository.add(template)
        uiState = uiState.copy(
            showTemplateSaveDialog = false,
            lastActionMessage = "Template '$name' saved",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.SUCCESS
        )
    }

    fun loadTemplate(template: WriteTemplate) {
        updateWriteData(template.data)
        uiState = uiState.copy(
            lastActionMessage = "Loaded template '${template.name}'",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun deleteTemplate(id: String) {
        templateRepository.delete(id)
        uiState = uiState.copy(
            lastActionMessage = "Template deleted",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun updateTemplateName(id: String, newName: String) {
        val template = templates.value.find { it.id == id } ?: return
        templateRepository.update(template.copy(name = newName))
        uiState = uiState.copy(
            lastActionMessage = "Template renamed to '$newName'",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun showTemplateSaveDialog() {
        uiState = uiState.copy(showTemplateSaveDialog = true)
    }

    fun hideTemplateSaveDialog() {
        uiState = uiState.copy(showTemplateSaveDialog = false)
    }

    fun confirmPendingAction(context: Context) {
        val action = uiState.pendingAction
        if (action != null) {
            NfcIntentHandler.handle(context, action.content)
        }
        uiState = uiState.copy(
            pendingAction = null,
            status = "No confirmation needed.",
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
            lastActionMessage = "Confirmation removed",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun dismissPendingAction() {
        uiState = uiState.copy(
            pendingAction = null,
            status = "No dismissal needed.",
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
            lastActionMessage = "Dismiss removed",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun onTag(tag: Tag, context: Context) {
        when (uiState.mode) {
            NfcMode.IDLE -> {
                uiState = uiState.copy(
                    status = "NFC is stopped. Pick Read, Write, or Erase first.",
                    statusTone = StatusTone.ERROR,
                    armedMessage = modeInstructionText(NfcMode.IDLE, uiState.writeData.isValid()),
                    lastActionMessage = "Tag ignored because NFC is stopped",
                    feedbackEventId = nextFeedbackEventId(),
                    feedbackType = UiFeedbackType.ERROR
                )
                return
            }

            NfcMode.WRITE -> {
                if (!uiState.writeData.isValid()) {
                    uiState = uiState.copy(
                        status = "Write is blocked. Fill in the needed box first.",
                        statusTone = StatusTone.ERROR,
                        armedMessage = modeInstructionText(NfcMode.WRITE, false),
                        lastActionMessage = "Write blocked because data is missing",
                        pendingAction = null,
                        feedbackEventId = nextFeedbackEventId(),
                        feedbackType = UiFeedbackType.ERROR
                    )
                    return
                }
            }

            NfcMode.MULTI_WRITE -> {
                if (uiState.multiWriteList.isEmpty()) {
                    uiState = uiState.copy(
                        status = "Multi-write is blocked. Add records first.",
                        statusTone = StatusTone.ERROR,
                        armedMessage = modeInstructionText(NfcMode.MULTI_WRITE, false),
                        lastActionMessage = "Multi-write blocked because no records",
                        pendingAction = null,
                        feedbackEventId = nextFeedbackEventId(),
                        feedbackType = UiFeedbackType.ERROR
                    )
                    return
                }
            }

            NfcMode.CLONE -> {
                // In CLONE mode, we first read, then write.
                // If cloneMessage is present, we are in the "Write" phase of cloning.
            }
            
            NfcMode.COMPARE -> {
                // First scan stores tag1, second scan performs comparison
            }

            NfcMode.READ,
            NfcMode.ERASE -> Unit
        }

        uiState = uiState.copy(
            status = when (uiState.mode) {
                NfcMode.READ -> "Tag found. Reading..."
                NfcMode.WRITE -> "Tag found. Writing..."
                NfcMode.MULTI_WRITE -> "Tag found. Writing multiple records..."
                NfcMode.CLONE -> if (uiState.cloneMessage == null) "Tag found. Reading source..." else "Tag found. Writing clone..."
                NfcMode.COMPARE -> if (uiState.compareTag1 == null) "Tag found. Reading first tag..." else "Tag found. Comparing..."
                NfcMode.ERASE -> "Tag found. Erasing..."
                NfcMode.IDLE -> "NFC stopped."
            },
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
            lastActionMessage = "Tag detected",
            pendingAction = null,
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )

        if (uiState.mode == NfcMode.MULTI_WRITE) {
            com.cyphershadowbourne.nfcstudioultra.nfc.NfcLog.operationStarted(NfcMode.MULTI_WRITE)
            nfcManager.processMultiTag(
                tag = tag,
                writeDataList = uiState.multiWriteList
            ) { result ->
                viewModelScope.launch(Dispatchers.Main) {
                    handleNfcResult(result, context)
                }
            }
        } else if (uiState.mode == NfcMode.CLONE) {
            val cloneMsg = uiState.cloneMessage
            if (cloneMsg == null) {
                // Phase 1: Read source
                com.cyphershadowbourne.nfcstudioultra.nfc.NfcLog.operationStarted(NfcMode.CLONE)
                nfcManager.processTag(NfcMode.CLONE, tag, uiState.writeData) { result ->
                    viewModelScope.launch(Dispatchers.Main) {
                        handleNfcResult(result, context)
                    }
                }
            } else {
                // Phase 2: Write to destination
                nfcManager.processCloneWrite(tag, cloneMsg) { result ->
                    viewModelScope.launch(Dispatchers.Main) {
                        handleNfcResult(result, context)
                    }
                }
            }
        } else {
            com.cyphershadowbourne.nfcstudioultra.nfc.NfcLog.operationStarted(uiState.mode)
            nfcManager.processTag(
                mode = uiState.mode,
                tag = tag,
                writeData = uiState.writeData
            ) { result ->
                viewModelScope.launch(Dispatchers.Main) {
                    handleNfcResult(result, context)
                }
            }
        }
    }

    private fun handleNfcResult(result: NfcOperationResult, context: Context) {
        com.cyphershadowbourne.nfcstudioultra.nfc.NfcLog.operationCompleted(uiState.mode, result)
        
        when (result) {
            is NfcOperationResult.CompareSuccess -> {
                val currentTag = result.result.tag1 // From NfcManager
                val tag1 = uiState.compareTag1

                if (tag1 == null) {
                    uiState = uiState.copy(
                        compareTag1 = currentTag,
                        status = "Tag 1 scanned. Now scan Tag 2.",
                        statusTone = StatusTone.INFO,
                        armedMessage = "Scan the second tag to compare.",
                        lastActionMessage = "Tag 1 captured",
                        feedbackEventId = nextFeedbackEventId(),
                        feedbackType = UiFeedbackType.SUCCESS
                    )
                } else {
                    val comparisonResult = compareTags(tag1, currentTag)
                    uiState = uiState.copy(
                        compareResult = comparisonResult,
                        status = if (comparisonResult.areEqual) "Tags are identical." else "Tags are different.",
                        statusTone = if (comparisonResult.areEqual) StatusTone.SUCCESS else StatusTone.WARNING,
                        armedMessage = "Scan another tag to start a new comparison.",
                        lastActionMessage = "Comparison complete",
                        compareTag1 = null,
                        feedbackEventId = nextFeedbackEventId(),
                        feedbackType = if (comparisonResult.areEqual) UiFeedbackType.SUCCESS else UiFeedbackType.WARNING
                    )
                }
            }

            is NfcOperationResult.ReadSuccess -> {
                if (uiState.mode == NfcMode.CLONE && uiState.cloneMessage == null) {
                    // This was the source read for clone
                    uiState = uiState.copy(
                        cloneMessage = result.rawMessage,
                        status = "Source read. Now scan destination tag.",
                        statusTone = StatusTone.INFO,
                        armedMessage = "Scan a WRITABLE tag to complete clone.",
                        lastActionMessage = "Source tag captured",
                        feedbackEventId = nextFeedbackEventId(),
                        feedbackType = UiFeedbackType.SUCCESS
                    )
                    return
                }

                val autoActionMessage = handleReadActionIfNeeded(context, result.content)

                // Record to history
                val historyItem = HistoryItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    recordTypes = if (result.content is NdefContent.Multi) {
                        result.content.records.map { it.javaClass.simpleName }
                    } else {
                        listOf(result.content.javaClass.simpleName)
                    },
                    previewText = result.displayText,
                    techList = result.techList,
                    isWritable = result.isWritable,
                    sizeBytes = result.sizeBytes,
                    tagIdHex = result.tagIdHex,
                    records = result.records
                )
                historyRepository.add(historyItem)

                uiState = uiState.copy(
                    lastRead = result.displayText,
                    lastDetails = result.details,
                    lastReadContent = result.content,
                    lastReadRecords = result.records,
                    pendingAction = null,
                    logs = com.cyphershadowbourne.nfcstudioultra.nfc.NfcLog.getLogs(),
                    status = "Read complete.",
                    statusTone = StatusTone.SUCCESS,
                    armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
                    lastActionMessage = autoActionMessage,
                    feedbackEventId = nextFeedbackEventId(),
                    feedbackType = UiFeedbackType.SUCCESS
                )
            }

            is NfcOperationResult.WriteSuccess, is NfcOperationResult.MultiWriteSuccess, is NfcOperationResult.CloneSuccess -> {
                val verificationStatus = when (result) {
                    is NfcOperationResult.WriteSuccess -> result.verificationStatus
                    is NfcOperationResult.MultiWriteSuccess -> result.verificationStatus
                    is NfcOperationResult.CloneSuccess -> result.verificationStatus
                    else -> null
                }
                val message = when (result) {
                    is NfcOperationResult.WriteSuccess -> result.message
                    is NfcOperationResult.MultiWriteSuccess -> result.message
                    is NfcOperationResult.CloneSuccess -> result.message
                    else -> ""
                }

                if (result is NfcOperationResult.CloneSuccess) {
                    // Clone complete, maybe reset clone state?
                    // uiState = uiState.copy(cloneMessage = null)
                }

                val verificationMsg = when (verificationStatus) {
                    VerificationStatus.VERIFIED -> " (Verified)"
                    VerificationStatus.MISMATCH -> " (Verification mismatch)"
                    VerificationStatus.UNAVAILABLE -> " (Verification unavailable)"
                    null -> ""
                }
                val fullMessage = message + verificationMsg

                uiState = uiState.copy(
                    status = fullMessage,
                    statusTone = if (verificationStatus == VerificationStatus.MISMATCH) StatusTone.WARNING else StatusTone.SUCCESS,
                    armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
                    lastActionMessage = fullMessage,
                    pendingAction = null,
                    logs = com.cyphershadowbourne.nfcstudioultra.nfc.NfcLog.getLogs(),
                    feedbackEventId = nextFeedbackEventId(),
                    feedbackType = if (verificationStatus == VerificationStatus.MISMATCH) UiFeedbackType.WARNING else UiFeedbackType.SUCCESS
                )
            }

            is NfcOperationResult.Ignored -> {
                uiState = uiState.copy(
                    status = result.reason,
                    statusTone = StatusTone.WARNING,
                    armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
                    lastActionMessage = result.reason,
                    feedbackEventId = nextFeedbackEventId(),
                    feedbackType = UiFeedbackType.WARNING
                )
            }

            is NfcOperationResult.Error -> {
                uiState = uiState.copy(
                    status = result.message,
                    statusTone = StatusTone.ERROR,
                    armedMessage = modeInstructionText(uiState.mode, uiState.writeData.isValid()),
                    lastActionMessage = result.message,
                    pendingAction = null,
                    feedbackEventId = nextFeedbackEventId(),
                    feedbackType = UiFeedbackType.ERROR
                )
            }
        }
    }

    private fun compareTags(t1: com.cyphershadowbourne.nfcstudioultra.nfc.TagInfo, t2: com.cyphershadowbourne.nfcstudioultra.nfc.TagInfo): com.cyphershadowbourne.nfcstudioultra.nfc.TagComparisonResult {
        val diffs = mutableListOf<String>()
        
        if (t1.recordCount != t2.recordCount) {
            diffs.add("Record count: ${t1.recordCount} vs ${t2.recordCount}")
        }
        
        if (t1.recordTypes != t2.recordTypes) {
            diffs.add("Record types mismatch")
        }
        
        if (t1.decodedValues != t2.decodedValues) {
            diffs.add("Decoded values mismatch")
        }
        
        if (t1.isWritable != t2.isWritable) {
            diffs.add("Writability: ${t1.isWritable} vs ${t2.isWritable}")
        }
        
        if (t1.sizeBytes != t2.sizeBytes) {
            diffs.add("Capacity: ${t1.sizeBytes ?: "Unknown"} vs ${t2.sizeBytes ?: "Unknown"}")
        }
        
        if (t1.techList != t2.techList) {
            diffs.add("Supported Tech mismatch")
        }
        
        return com.cyphershadowbourne.nfcstudioultra.nfc.TagComparisonResult(
            areEqual = diffs.isEmpty(),
            tag1 = t1,
            tag2 = t2,
            differences = diffs
        )
    }

    private fun handleReadActionIfNeeded(context: Context, content: NdefContent): String {
        return when (content) {
            is NdefContent.Url -> {
                if (urlSafetyEnabled.value) {
                    val uri = try {
                        val u = if (content.value.startsWith("http://", ignoreCase = true) ||
                            content.value.startsWith("https://", ignoreCase = true)
                        ) {
                            content.value
                        } else {
                            "https://${content.value}"
                        }
                        android.net.Uri.parse(u)
                    } catch (_: Exception) {
                        null
                    }

                    if (uri != null && uri.host != null) {
                        uiState = uiState.copy(
                            pendingAction = PendingAction(
                                title = "URL Safety Preview",
                                description = "Source: ${uri.host}\nFull URL: ${content.value}",
                                content = content
                            )
                        )
                        "Review URL before opening"
                    } else {
                        NfcIntentHandler.handle(context, content)
                        "Link opened"
                    }
                } else {
                    NfcIntentHandler.handle(context, content)
                    "Link opened"
                }
            }

            is NdefContent.Phone -> {
                NfcIntentHandler.handle(context, content)
                "Phone screen opened"
            }

            is NdefContent.Email -> {
                NfcIntentHandler.handle(context, content)
                "Email screen opened"
            }

            is NdefContent.Sms -> {
                NfcIntentHandler.handle(context, content)
                "SMS screen opened"
            }

            is NdefContent.Location -> {
                NfcIntentHandler.handle(context, content)
                "Map opened"
            }

            is NdefContent.Contact -> {
                NfcIntentHandler.handle(context, content)
                "Contact screen opened"
            }

            is NdefContent.Multi -> "Multi-record tag read"
            is NdefContent.Text -> "Read complete"
            is NdefContent.Unknown -> "Read complete"
            is NdefContent.Wifi -> "Wi-Fi read"
            is NdefContent.Calendar -> "Calendar read"
            is NdefContent.SmartPoster -> "Poster read"
            is NdefContent.Aar -> "AAR read"
            is NdefContent.MimeRecord -> "MIME read"
            is NdefContent.ExternalRecord -> "External read"
        }
    }

    private fun updateWriteData(writeData: NdefWriteData) {
        val isValid = writeData.isValid()

        uiState = uiState.copy(
            writeData = writeData,
            status = if (uiState.mode == NfcMode.WRITE) {
                modeStatusText(NfcMode.WRITE, isValid)
            } else {
                uiState.status
            },
            statusTone = if (uiState.mode == NfcMode.WRITE) {
                modeStatusTone(NfcMode.WRITE, isValid)
            } else {
                uiState.statusTone
            },
            armedMessage = modeInstructionText(uiState.mode, isValid),
            lastActionMessage = when (writeData.type) {
                NdefRecordType.TEXT -> "Text updated"
                NdefRecordType.URL -> "Link updated"
                NdefRecordType.PHONE -> "Phone number updated"
                NdefRecordType.EMAIL -> "Email updated"
                NdefRecordType.SMS -> "SMS updated"
                NdefRecordType.LOCATION -> "Location updated"
                NdefRecordType.CONTACT -> "Contact updated"
                NdefRecordType.WIFI -> "Wi-Fi updated"
                NdefRecordType.CALENDAR -> "Calendar updated"
                NdefRecordType.SMART_POSTER -> "Poster updated"
                NdefRecordType.AAR -> "AAR updated"
                NdefRecordType.MIME -> "MIME updated"
                NdefRecordType.EXTERNAL -> "External updated"
            }
        )
    }

    private fun isWriteDataValid(writeData: NdefWriteData): Boolean {
        return writeData.isValid()
    }

    private fun modeStatusText(
        mode: NfcMode,
        canWrite: Boolean
    ): String {
        return when (mode) {
            NfcMode.READ -> "Read is ready. Tap a tag now."
            NfcMode.WRITE -> {
                if (canWrite) {
                    "Write is ready. Tap a tag now."
                } else {
                    "Write needs data first."
                }
            }
            NfcMode.MULTI_WRITE -> {
                if (uiState.multiWriteList.isNotEmpty()) {
                    "Multi-write is ready. Tap a tag now."
                } else {
                    "Add records to the list first."
                }
            }
            NfcMode.CLONE -> {
                if (uiState.cloneMessage == null) {
                    "Clone ready. Tap SOURCE tag."
                } else {
                    "Clone data ready. Tap DESTINATION tag."
                }
            }
            NfcMode.COMPARE -> {
                if (uiState.compareTag1 == null) {
                    "Compare ready. Tap TAG 1."
                } else {
                    "Tag 1 captured. Tap TAG 2."
                }
            }
            NfcMode.ERASE -> "Erase is ready. Tap a tag now."
            NfcMode.IDLE -> "NFC is stopped."
        }
    }

    private fun modeStatusTone(
        mode: NfcMode,
        canWrite: Boolean
    ): StatusTone {
        return when (mode) {
            NfcMode.READ -> StatusTone.INFO
            NfcMode.WRITE -> if (canWrite) StatusTone.INFO else StatusTone.WARNING
            NfcMode.MULTI_WRITE -> if (uiState.multiWriteList.isNotEmpty()) StatusTone.INFO else StatusTone.WARNING
            NfcMode.CLONE -> StatusTone.INFO
            NfcMode.COMPARE -> StatusTone.INFO
            NfcMode.ERASE -> StatusTone.WARNING
            NfcMode.IDLE -> StatusTone.INFO
        }
    }

    private fun modeInstructionText(
        mode: NfcMode,
        canWrite: Boolean
    ): String {
        return when (mode) {
            NfcMode.IDLE -> "Pick Read, Write, or Erase."
            NfcMode.READ -> "Tap the tag on the back of your phone."
            NfcMode.WRITE -> {
                if (canWrite) {
                    "Tap the tag on the back of your phone."
                } else {
                    "Fill in the needed box first. Then tap the tag."
                }
            }
            NfcMode.MULTI_WRITE -> {
                if (uiState.multiWriteList.isNotEmpty()) {
                    "Tap the tag on the back of your phone."
                } else {
                    "Add some records to your multi-write list first."
                }
            }
            NfcMode.CLONE -> {
                if (uiState.cloneMessage == null) {
                    "Tap the tag you want to copy FROM."
                } else {
                    "Tap the tag you want to copy TO."
                }
            }
            NfcMode.COMPARE -> {
                if (uiState.compareTag1 == null) {
                    "Tap the first tag you want to compare."
                } else {
                    "Tap the second tag you want to compare."
                }
            }
            NfcMode.ERASE -> "Tap the tag on the back of your phone. This will clear the tag."
        }
    }

    private fun nextFeedbackEventId(): Long {
        feedbackEventCounter += 1L
        return feedbackEventCounter
    }
}

data class PendingAction(
    val title: String,
    val description: String,
    val content: NdefContent
)

enum class UiFeedbackType {
    NONE,
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

enum class StatusTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class NfcStudioUiState(
    val mode: NfcMode = NfcMode.READ,
    val writeData: NdefWriteData = NdefWriteData(),
    val multiWriteList: List<NdefWriteData> = emptyList(),
    val cloneMessage: android.nfc.NdefMessage? = null,
    val compareTag1: com.cyphershadowbourne.nfcstudioultra.nfc.TagInfo? = null,
    val compareResult: com.cyphershadowbourne.nfcstudioultra.nfc.TagComparisonResult? = null,
    val lastRead: String = "",
    val lastDetails: String = "",
    val lastReadContent: NdefContent? = null,
    val lastReadRecords: List<ParsedNdefRecord> = emptyList(),
    val inspectorItem: HistoryItem? = null,
    val logs: List<String> = emptyList(),
    val showTemplateSaveDialog: Boolean = false,
    val pendingAction: PendingAction? = null,
    val status: String = "Read is ready. Tap a tag now.",
    val statusTone: StatusTone = StatusTone.INFO,
    val lastActionMessage: String = "Read mode selected",
    val armedMessage: String = "Tap the tag on the back of your phone.",
    val feedbackEventId: Long = 0L,
    val feedbackType: UiFeedbackType = UiFeedbackType.NONE,
    val expertMode: Boolean = false
)
