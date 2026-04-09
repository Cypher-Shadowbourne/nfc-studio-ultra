package com.cyphershadowbourne.nfcstudioultra.ui

import android.content.Context
import android.nfc.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefContent
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefRecordType
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteData
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcIntentHandler
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcManager
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NfcStudioViewModel : ViewModel() {

    private val nfcManager = NfcManager()
    private var feedbackEventCounter = 0L

    var uiState by mutableStateOf(NfcStudioUiState())
        private set

    fun setMode(mode: NfcMode) {
        val canWrite = isWriteDataValid(uiState.writeData)

        uiState = uiState.copy(
            mode = mode,
            canWrite = canWrite,
            status = modeStatusText(mode, canWrite),
            statusTone = modeStatusTone(mode, canWrite),
            armedMessage = modeInstructionText(mode, canWrite),
            lastActionMessage = when (mode) {
                NfcMode.READ -> "Read mode selected"
                NfcMode.WRITE -> "Write mode selected"
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
        val canWrite = isWriteDataValid(updatedWriteData)

        uiState = uiState.copy(
            writeData = updatedWriteData,
            canWrite = canWrite,
            status = if (uiState.mode == NfcMode.WRITE) {
                modeStatusText(NfcMode.WRITE, canWrite)
            } else {
                uiState.status
            },
            statusTone = if (uiState.mode == NfcMode.WRITE) {
                modeStatusTone(NfcMode.WRITE, canWrite)
            } else {
                uiState.statusTone
            },
            armedMessage = modeInstructionText(uiState.mode, canWrite),
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

    fun clearRead() {
        uiState = uiState.copy(
            lastRead = "",
            lastDetails = "",
            lastReadContent = null,
            pendingAction = null,
            status = "Result cleared.",
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
            lastActionMessage = "Result cleared",
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )
    }

    fun confirmPendingAction(context: Context) {
        uiState = uiState.copy(
            pendingAction = null,
            status = "No confirmation needed.",
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
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
            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
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
                    armedMessage = modeInstructionText(NfcMode.IDLE, uiState.canWrite),
                    lastActionMessage = "Tag ignored because NFC is stopped",
                    feedbackEventId = nextFeedbackEventId(),
                    feedbackType = UiFeedbackType.ERROR
                )
                return
            }

            NfcMode.WRITE -> {
                if (!uiState.canWrite) {
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

            NfcMode.READ,
            NfcMode.ERASE -> Unit
        }

        uiState = uiState.copy(
            status = when (uiState.mode) {
                NfcMode.READ -> "Tag found. Reading..."
                NfcMode.WRITE -> "Tag found. Writing..."
                NfcMode.ERASE -> "Tag found. Erasing..."
                NfcMode.IDLE -> "NFC stopped."
            },
            statusTone = StatusTone.INFO,
            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
            lastActionMessage = "Tag detected",
            pendingAction = null,
            feedbackEventId = nextFeedbackEventId(),
            feedbackType = UiFeedbackType.INFO
        )

        nfcManager.processTag(
            mode = uiState.mode,
            tag = tag,
            writeData = uiState.writeData
        ) { result ->
            viewModelScope.launch(Dispatchers.Main) {
                when (result) {
                    is NfcOperationResult.ReadSuccess -> {
                        val autoActionMessage = handleReadActionIfNeeded(context, result.content)

                        uiState = uiState.copy(
                            lastRead = result.displayText,
                            lastDetails = result.details,
                            lastReadContent = result.content,
                            pendingAction = null,
                            status = "Read complete.",
                            statusTone = StatusTone.SUCCESS,
                            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
                            lastActionMessage = autoActionMessage,
                            feedbackEventId = nextFeedbackEventId(),
                            feedbackType = UiFeedbackType.SUCCESS
                        )
                    }

                    is NfcOperationResult.WriteSuccess -> {
                        uiState = uiState.copy(
                            status = result.message,
                            statusTone = StatusTone.SUCCESS,
                            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
                            lastActionMessage = result.message,
                            pendingAction = null,
                            feedbackEventId = nextFeedbackEventId(),
                            feedbackType = UiFeedbackType.SUCCESS
                        )
                    }

                    is NfcOperationResult.Ignored -> {
                        uiState = uiState.copy(
                            status = result.reason,
                            statusTone = StatusTone.WARNING,
                            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
                            lastActionMessage = result.reason,
                            feedbackEventId = nextFeedbackEventId(),
                            feedbackType = UiFeedbackType.WARNING
                        )
                    }

                    is NfcOperationResult.Error -> {
                        uiState = uiState.copy(
                            status = result.message,
                            statusTone = StatusTone.ERROR,
                            armedMessage = modeInstructionText(uiState.mode, uiState.canWrite),
                            lastActionMessage = result.message,
                            pendingAction = null,
                            feedbackEventId = nextFeedbackEventId(),
                            feedbackType = UiFeedbackType.ERROR
                        )
                    }
                }
            }
        }
    }

    private fun handleReadActionIfNeeded(context: Context, content: NdefContent): String {
        return when (content) {
            is NdefContent.Url -> {
                NfcIntentHandler.handle(context, content)
                "Link opened"
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

            is NdefContent.Text -> "Read complete"
            is NdefContent.Unknown -> "Read complete"
        }
    }

    private fun updateWriteData(writeData: NdefWriteData) {
        val canWrite = isWriteDataValid(writeData)

        uiState = uiState.copy(
            writeData = writeData,
            canWrite = canWrite,
            status = if (uiState.mode == NfcMode.WRITE) {
                modeStatusText(NfcMode.WRITE, canWrite)
            } else {
                uiState.status
            },
            statusTone = if (uiState.mode == NfcMode.WRITE) {
                modeStatusTone(NfcMode.WRITE, canWrite)
            } else {
                uiState.statusTone
            },
            armedMessage = modeInstructionText(uiState.mode, canWrite),
            lastActionMessage = when (writeData.type) {
                NdefRecordType.TEXT -> "Text updated"
                NdefRecordType.URL -> "Link updated"
                NdefRecordType.PHONE -> "Phone number updated"
                NdefRecordType.EMAIL -> "Email updated"
                NdefRecordType.SMS -> "SMS updated"
            }
        )
    }

    private fun isWriteDataValid(writeData: NdefWriteData): Boolean {
        return when (writeData.type) {
            NdefRecordType.TEXT -> writeData.text.isNotBlank()
            NdefRecordType.URL -> writeData.url.isNotBlank()
            NdefRecordType.PHONE -> writeData.phoneNumber.isNotBlank()
            NdefRecordType.EMAIL -> writeData.emailTo.isNotBlank()
            NdefRecordType.SMS -> writeData.smsNumber.isNotBlank()
        }
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
    val mode: NfcMode = NfcMode.IDLE,
    val writeData: NdefWriteData = NdefWriteData(),
    val lastRead: String = "",
    val lastDetails: String = "",
    val lastReadContent: NdefContent? = null,
    val pendingAction: PendingAction? = null,
    val status: String = "NFC is stopped.",
    val statusTone: StatusTone = StatusTone.INFO,
    val lastActionMessage: String = "Ready",
    val armedMessage: String = "Pick Read, Write, or Erase.",
    val canWrite: Boolean = false,
    val feedbackEventId: Long = 0L,
    val feedbackType: UiFeedbackType = UiFeedbackType.NONE
)