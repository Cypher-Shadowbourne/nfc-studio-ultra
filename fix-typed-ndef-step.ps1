$ErrorActionPreference = "Stop"

$projectRoot = Get-Location
$javaRoot = Join-Path $projectRoot "app\src\main\java\com\cyphershadowbourne\nfcstudioultra"

$nfcDir = Join-Path $javaRoot "nfc"
$uiDir = Join-Path $javaRoot "ui"
$screenDir = Join-Path $uiDir "screen"

New-Item -ItemType Directory -Force -Path $nfcDir | Out-Null
New-Item -ItemType Directory -Force -Path $uiDir | Out-Null
New-Item -ItemType Directory -Force -Path $screenDir | Out-Null

# Remove stale files that are colliding with the new step
$removePaths = @(
    (Join-Path $javaRoot "ui\NfcStudioUltraApp.kt"),
    (Join-Path $javaRoot "ui\components"),
    (Join-Path $javaRoot "ui\screens"),
    (Join-Path $javaRoot "nfc\UI"),
    (Join-Path $javaRoot "nfc\NfcStudioViewModel.kt"),
    (Join-Path $javaRoot "nfc\NfcStudioUltraScreen.kt")
)

foreach ($path in $removePaths) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path
        Write-Host "Removed stale path: $path"
    }
}

# MainActivity.kt
@'
package com.cyphershadowbourne.nfcstudioultra

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteType
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
        viewModel.onTag(tag, this)
    }
}
'@ | Set-Content -Encoding UTF8 (Join-Path $javaRoot "MainActivity.kt")

# NfcMode.kt
@'
package com.cyphershadowbourne.nfcstudioultra.nfc

enum class NfcMode {
    IDLE,
    READ,
    WRITE,
    ERASE
}
'@ | Set-Content -Encoding UTF8 (Join-Path $nfcDir "NfcMode.kt")

# NdefWriteType.kt
@'
package com.cyphershadowbourne.nfcstudioultra.nfc

enum class NdefWriteType {
    TEXT,
    URL,
    PHONE,
    EMAIL,
    SMS
}
'@ | Set-Content -Encoding UTF8 (Join-Path $nfcDir "NdefWriteType.kt")

# NdefModels.kt
@'
package com.cyphershadowbourne.nfcstudioultra.nfc

sealed class NdefContent {
    data class Text(val value: String) : NdefContent()
    data class Url(val value: String) : NdefContent()
    data class Phone(val value: String) : NdefContent()
    data class Email(
        val to: String,
        val subject: String,
        val body: String
    ) : NdefContent()

    data class Sms(
        val number: String,
        val body: String
    ) : NdefContent()

    data class Unknown(val raw: String) : NdefContent()
}

data class NdefWriteData(
    val type: NdefWriteType = NdefWriteType.TEXT,
    val text: String = "",
    val url: String = "",
    val phoneNumber: String = "",
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val smsNumber: String = "",
    val smsBody: String = ""
) {
    fun describeForUi(): String {
        return when (type) {
            NdefWriteType.TEXT -> text
            NdefWriteType.URL -> url
            NdefWriteType.PHONE -> phoneNumber
            NdefWriteType.EMAIL -> buildString {
                append("To: ")
                append(emailTo)
                if (emailSubject.isNotBlank()) {
                    append("\nSubject: ")
                    append(emailSubject)
                }
                if (emailBody.isNotBlank()) {
                    append("\nBody: ")
                    append(emailBody)
                }
            }
            NdefWriteType.SMS -> buildString {
                append("Number: ")
                append(smsNumber)
                if (smsBody.isNotBlank()) {
                    append("\nMessage: ")
                    append(smsBody)
                }
            }
        }.ifBlank { "(No data entered yet)" }
    }
}
'@ | Set-Content -Encoding UTF8 (Join-Path $nfcDir "NdefModels.kt")

# NfcIntentHandler.kt
@'
package com.cyphershadowbourne.nfcstudioultra.nfc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NfcIntentHandler {

    fun handle(context: Context, content: NdefContent) {
        when (content) {
            is NdefContent.Url -> openUrl(context, content.value)
            is NdefContent.Phone -> openDialer(context, content.value)
            is NdefContent.Email -> openEmail(context, content)
            is NdefContent.Sms -> openSms(context, content)
            is NdefContent.Text -> {
                Toast.makeText(context, content.value, Toast.LENGTH_SHORT).show()
            }
            is NdefContent.Unknown -> {
                Toast.makeText(context, content.raw, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openUrl(context: Context, value: String) {
        val url = if (
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            value
        } else {
            "https://$value"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No app can open this link.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDialer(context: Context, value: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$value")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No dialer app found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmail(context: Context, value: NdefContent.Email) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${value.to}")
                putExtra(Intent.EXTRA_SUBJECT, value.subject)
                putExtra(Intent.EXTRA_TEXT, value.body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSms(context: Context, value: NdefContent.Sms) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${value.number}")
                putExtra("sms_body", value.body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No SMS app found.", Toast.LENGTH_SHORT).show()
        }
    }
}
'@ | Set-Content -Encoding UTF8 (Join-Path $nfcDir "NfcIntentHandler.kt")

# NfcManager.kt
@'
package com.cyphershadowbourne.nfcstudioultra.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class NfcManager {

    private val executor = Executors.newSingleThreadExecutor()
    private val processing = AtomicBoolean(false)

    fun processTag(
        mode: NfcMode,
        tag: Tag,
        writeData: NdefWriteData,
        callback: (NfcOperationResult) -> Unit
    ) {
        if (mode == NfcMode.IDLE) {
            callback(NfcOperationResult.Error("Select READ, WRITE, or ERASE first."))
            return
        }

        if (!processing.compareAndSet(false, true)) {
            callback(NfcOperationResult.Ignored("Another NFC action is already running."))
            return
        }

        executor.execute {
            try {
                val result = when (mode) {
                    NfcMode.READ -> readTag(tag)
                    NfcMode.WRITE -> writeTag(tag, writeData)
                    NfcMode.ERASE -> eraseTag(tag)
                    NfcMode.IDLE -> NfcOperationResult.Error("Idle mode.")
                }
                callback(result)
            } catch (t: Throwable) {
                callback(NfcOperationResult.Error("NFC operation failed: ${t.message ?: "unknown error"}"))
            } finally {
                try {
                    Thread.sleep(700)
                } catch (_: InterruptedException) {
                }
                processing.set(false)
            }
        }
    }

    private fun readTag(tag: Tag): NfcOperationResult {
        val techList = tag.techList.joinToString(", ")
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            return try {
                ndef.connect()

                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                val parsedContent = parseMessage(message)

                NfcOperationResult.ReadSuccess(
                    content = parsedContent,
                    displayText = buildDisplayText(parsedContent),
                    details = buildString {
                        appendLine("Type: ${ndef.type ?: "Unknown"}")
                        appendLine("Writable: ${ndef.isWritable}")
                        appendLine("Max size: ${ndef.maxSize} bytes")
                        append("Tech: $techList")
                    }
                )
            } catch (e: Exception) {
                NfcOperationResult.Error("Read failed: ${e.message ?: "unknown error"}")
            } finally {
                try {
                    ndef.close()
                } catch (_: IOException) {
                }
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return NfcOperationResult.Error("This tag is NDEF-formatable but not yet formatted. Tech: $techList")
        }

        return NfcOperationResult.Error("This tag is not NDEF compatible. Tech: $techList")
    }

    private fun writeTag(tag: Tag, writeData: NdefWriteData): NfcOperationResult {
        val message = createMessageForWriteData(writeData)
            ?: return NfcOperationResult.Error("Enter valid data before using WRITE.")

        return writeMessageToTag(tag, message, "Write successful.")
    }

    private fun eraseTag(tag: Tag): NfcOperationResult {
        val blankRecord = createTextRecord("")
        val blankMessage = NdefMessage(arrayOf(blankRecord))
        return writeMessageToTag(tag, blankMessage, "Erase successful. Tag overwritten with a blank record.")
    }

    private fun writeMessageToTag(
        tag: Tag,
        message: NdefMessage,
        successMessage: String
    ): NfcOperationResult {
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            return try {
                ndef.connect()

                if (!ndef.isWritable) {
                    return NfcOperationResult.Error("This tag is read-only.")
                }

                val size = message.toByteArray().size
                if (size > ndef.maxSize) {
                    return NfcOperationResult.Error("Tag is too small for this payload.")
                }

                ndef.writeNdefMessage(message)
                NfcOperationResult.WriteSuccess(successMessage)
            } catch (e: Exception) {
                NfcOperationResult.Error("Write failed: ${e.message ?: "unknown error"}")
            } finally {
                try {
                    ndef.close()
                } catch (_: IOException) {
                }
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return try {
                formatable.connect()
                formatable.format(message)
                NfcOperationResult.WriteSuccess(successMessage)
            } catch (e: Exception) {
                NfcOperationResult.Error("Format/write failed: ${e.message ?: "unknown error"}")
            } finally {
                try {
                    formatable.close()
                } catch (_: IOException) {
                }
            }
        }

        return NfcOperationResult.Error("This tag does not support NDEF writing.")
    }

    private fun createMessageForWriteData(writeData: NdefWriteData): NdefMessage? {
        val record = when (writeData.type) {
            NdefWriteType.TEXT -> {
                if (writeData.text.isBlank()) null else createTextRecord(writeData.text)
            }

            NdefWriteType.URL -> {
                val value = writeData.url.trim()
                if (value.isBlank()) {
                    null
                } else {
                    val normalized = if (
                        value.startsWith("http://", ignoreCase = true) ||
                        value.startsWith("https://", ignoreCase = true)
                    ) {
                        value
                    } else {
                        "https://$value"
                    }
                    NdefRecord.createUri(normalized)
                }
            }

            NdefWriteType.PHONE -> {
                val value = writeData.phoneNumber.trim()
                if (value.isBlank()) null else NdefRecord.createUri("tel:$value")
            }

            NdefWriteType.EMAIL -> {
                val to = writeData.emailTo.trim()
                if (to.isBlank()) {
                    null
                } else {
                    val uri = buildEmailUri(
                        to = to,
                        subject = writeData.emailSubject,
                        body = writeData.emailBody
                    )
                    NdefRecord.createUri(uri)
                }
            }

            NdefWriteType.SMS -> {
                val number = writeData.smsNumber.trim()
                if (number.isBlank()) {
                    null
                } else {
                    val uri = buildSmsUri(
                        number = number,
                        body = writeData.smsBody
                    )
                    NdefRecord.createUri(uri)
                }
            }
        }

        return record?.let { NdefMessage(arrayOf(it)) }
    }

    private fun parseMessage(message: NdefMessage?): NdefContent {
        if (message == null || message.records.isEmpty()) {
            return NdefContent.Text("(Tag contains no readable content)")
        }

        return parseRecord(message.records.first())
    }

    private fun parseRecord(record: NdefRecord): NdefContent {
        return when {
            isTextRecord(record) -> NdefContent.Text(parseTextRecord(record))
            isUriRecord(record) -> parseUriContent(parseUriRecord(record))
            else -> {
                try {
                    val bytes = record.payload
                    if (bytes.isEmpty()) {
                        NdefContent.Unknown("(Empty payload)")
                    } else {
                        NdefContent.Unknown(String(bytes, Charsets.UTF_8))
                    }
                } catch (_: Exception) {
                    NdefContent.Unknown("(Unreadable payload)")
                }
            }
        }
    }

    private fun parseUriContent(value: String): NdefContent {
        val trimmed = value.trim()

        return when {
            trimmed.startsWith("tel:", ignoreCase = true) -> {
                NdefContent.Phone(trimmed.removePrefix("tel:"))
            }

            trimmed.startsWith("mailto:", ignoreCase = true) -> {
                val withoutScheme = trimmed.substringAfter("mailto:")
                val to = withoutScheme.substringBefore("?")
                val uri = Uri.parse(trimmed)
                NdefContent.Email(
                    to = to,
                    subject = uri.getQueryParameter("subject").orEmpty(),
                    body = uri.getQueryParameter("body").orEmpty()
                )
            }

            trimmed.startsWith("smsto:", ignoreCase = true) ||
            trimmed.startsWith("sms:", ignoreCase = true) -> {
                val scheme = if (trimmed.startsWith("smsto:", ignoreCase = true)) "smsto:" else "sms:"
                val withoutScheme = trimmed.substringAfter(scheme)
                val number = withoutScheme.substringBefore("?")
                val uri = Uri.parse(trimmed)
                NdefContent.Sms(
                    number = number,
                    body = uri.getQueryParameter("body").orEmpty()
                )
            }

            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true) -> {
                NdefContent.Url(trimmed)
            }

            else -> NdefContent.Url(trimmed)
        }
    }

    private fun buildDisplayText(content: NdefContent): String {
        return when (content) {
            is NdefContent.Text -> content.value
            is NdefContent.Url -> content.value
            is NdefContent.Phone -> "Phone: ${content.value}"
            is NdefContent.Email -> buildString {
                append("Email to: ${content.to}")
                if (content.subject.isNotBlank()) append("\nSubject: ${content.subject}")
                if (content.body.isNotBlank()) append("\nBody: ${content.body}")
            }
            is NdefContent.Sms -> buildString {
                append("SMS to: ${content.number}")
                if (content.body.isNotBlank()) append("\nMessage: ${content.body}")
            }
            is NdefContent.Unknown -> content.raw
        }
    }

    private fun buildEmailUri(
        to: String,
        subject: String,
        body: String
    ): String {
        val query = mutableListOf<String>()
        if (subject.isNotBlank()) query += "subject=${Uri.encode(subject)}"
        if (body.isNotBlank()) query += "body=${Uri.encode(body)}"

        return if (query.isEmpty()) {
            "mailto:$to"
        } else {
            "mailto:$to?${query.joinToString("&")}"
        }
    }

    private fun buildSmsUri(
        number: String,
        body: String
    ): String {
        return if (body.isBlank()) {
            "smsto:$number"
        } else {
            "smsto:$number?body=${Uri.encode(body)}"
        }
    }

    private fun isTextRecord(record: NdefRecord): Boolean {
        return record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)
    }

    private fun isUriRecord(record: NdefRecord): Boolean {
        return record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_URI)
    }

    private fun parseTextRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload.isEmpty()) return ""

        val status = payload[0].toInt()
        val isUtf16 = (status and 0x80) != 0
        val languageLength = status and 0x3F
        val charset: Charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8

        if (payload.size <= 1 + languageLength) return ""

        return String(
            payload,
            1 + languageLength,
            payload.size - 1 - languageLength,
            charset
        )
    }

    private fun parseUriRecord(record: NdefRecord): String {
        return try {
            record.toUri()?.toString().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun createTextRecord(text: String): NdefRecord {
        val language = "en".toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + language.size + textBytes.size)

        payload[0] = language.size.toByte()
        System.arraycopy(language, 0, payload, 1, language.size)
        System.arraycopy(textBytes, 0, payload, 1 + language.size, textBytes.size)

        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
    }
}

sealed class NfcOperationResult {
    data class ReadSuccess(
        val content: NdefContent,
        val displayText: String,
        val details: String
    ) : NfcOperationResult()

    data class WriteSuccess(
        val message: String
    ) : NfcOperationResult()

    data class Ignored(
        val reason: String
    ) : NfcOperationResult()

    data class Error(
        val message: String
    ) : NfcOperationResult()
}
'@ | Set-Content -Encoding UTF8 (Join-Path $nfcDir "NfcManager.kt")

# NfcStudioViewModel.kt
@'
package com.cyphershadowbourne.nfcstudioultra.ui

import android.content.Context
import android.nfc.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefContent
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteData
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteType
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcIntentHandler
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcManager
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NfcStudioViewModel : ViewModel() {

    private val nfcManager = NfcManager()

    var uiState by mutableStateOf(NfcStudioUiState())
        private set

    fun setMode(mode: NfcMode) {
        uiState = uiState.copy(
            mode = mode,
            status = when (mode) {
                NfcMode.READ -> "READ selected. Next step: tap a tag to read it."
                NfcMode.WRITE -> "WRITE selected. Next step: tap a tag to write the selected type."
                NfcMode.ERASE -> "ERASE selected. Next step: tap a tag to clear it."
                NfcMode.IDLE -> "IDLE selected. NFC actions stopped."
            },
            lastActionMessage = when (mode) {
                NfcMode.READ -> "Read mode armed."
                NfcMode.WRITE -> "Write mode armed."
                NfcMode.ERASE -> "Erase mode armed."
                NfcMode.IDLE -> "NFC stopped."
            }
        )
    }

    fun setWriteType(type: NdefWriteType) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(type = type),
            lastActionMessage = "Write type set to ${type.name}."
        )
    }

    fun updateText(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(text = value),
            lastActionMessage = "Text payload updated."
        )
    }

    fun updateUrl(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(url = value),
            lastActionMessage = "URL payload updated."
        )
    }

    fun updatePhoneNumber(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(phoneNumber = value),
            lastActionMessage = "Phone payload updated."
        )
    }

    fun updateEmailTo(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(emailTo = value),
            lastActionMessage = "Email recipient updated."
        )
    }

    fun updateEmailSubject(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(emailSubject = value),
            lastActionMessage = "Email subject updated."
        )
    }

    fun updateEmailBody(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(emailBody = value),
            lastActionMessage = "Email body updated."
        )
    }

    fun updateSmsNumber(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(smsNumber = value),
            lastActionMessage = "SMS number updated."
        )
    }

    fun updateSmsBody(value: String) {
        uiState = uiState.copy(
            writeData = uiState.writeData.copy(smsBody = value),
            lastActionMessage = "SMS body updated."
        )
    }

    fun clearRead() {
        uiState = uiState.copy(
            lastRead = "",
            lastDetails = "",
            lastReadContent = null,
            status = "Read result cleared.",
            lastActionMessage = "Result cleared."
        )
    }

    fun onTag(tag: Tag, context: Context) {
        uiState = uiState.copy(
            status = when (uiState.mode) {
                NfcMode.READ -> "Reading tag..."
                NfcMode.WRITE -> "Writing tag..."
                NfcMode.ERASE -> "Erasing tag..."
                NfcMode.IDLE -> "IDLE."
            },
            lastActionMessage = "Tag detected."
        )

        nfcManager.processTag(
            mode = uiState.mode,
            tag = tag,
            writeData = uiState.writeData
        ) { result ->
            viewModelScope.launch(Dispatchers.Main) {
                when (result) {
                    is NfcOperationResult.ReadSuccess -> {
                        uiState = uiState.copy(
                            lastRead = result.displayText,
                            lastDetails = result.details,
                            lastReadContent = result.content,
                            status = "Read successful.",
                            lastActionMessage = "Read completed successfully."
                        )
                        NfcIntentHandler.handle(context, result.content)
                    }

                    is NfcOperationResult.WriteSuccess -> {
                        uiState = uiState.copy(
                            status = result.message,
                            lastActionMessage = result.message
                        )
                    }

                    is NfcOperationResult.Ignored -> {
                        uiState = uiState.copy(
                            status = result.reason,
                            lastActionMessage = result.reason
                        )
                    }

                    is NfcOperationResult.Error -> {
                        uiState = uiState.copy(
                            status = result.message,
                            lastActionMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

data class NfcStudioUiState(
    val mode: NfcMode = NfcMode.IDLE,
    val writeData: NdefWriteData = NdefWriteData(),
    val lastRead: String = "",
    val lastDetails: String = "",
    val lastReadContent: NdefContent? = null,
    val status: String = "IDLE.",
    val lastActionMessage: String = "Ready."
)
'@ | Set-Content -Encoding UTF8 (Join-Path $uiDir "NfcStudioViewModel.kt")

# NfcStudioUltraScreen.kt
@'
package com.cyphershadowbourne.nfcstudioultra.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteType
import com.cyphershadowbourne.nfcstudioultra.nfc.NfcMode
import com.cyphershadowbourne.nfcstudioultra.ui.NfcStudioUiState
import com.cyphershadowbourne.nfcstudioultra.ui.theme.DeepNavy
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NeonBlue
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NeonCyan
import com.cyphershadowbourne.nfcstudioultra.ui.theme.NeonMagenta
import com.cyphershadowbourne.nfcstudioultra.ui.theme.PanelBorder
import com.cyphershadowbourne.nfcstudioultra.ui.theme.PanelSurface
import com.cyphershadowbourne.nfcstudioultra.ui.theme.TextMuted
import com.cyphershadowbourne.nfcstudioultra.ui.theme.TextPrimary

@Composable
fun NfcStudioUltraScreen(
    state: NfcStudioUiState,
    onReadMode: () -> Unit,
    onWriteMode: () -> Unit,
    onEraseMode: () -> Unit,
    onIdleMode: () -> Unit,
    onWriteTypeSelected: (NdefWriteType) -> Unit,
    onTextChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailToChanged: (String) -> Unit,
    onEmailSubjectChanged: (String) -> Unit,
    onEmailBodyChanged: (String) -> Unit,
    onSmsNumberChanged: (String) -> Unit,
    onSmsBodyChanged: (String) -> Unit,
    onClearRead: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHeader()

            GradientActionButton(
                text = when (state.mode) {
                    NfcMode.READ -> "READY TO READ"
                    NfcMode.WRITE -> "READY TO WRITE"
                    NfcMode.ERASE -> "READY TO ERASE"
                    NfcMode.IDLE -> "NFC STUDIO ULTRA"
                },
                enabled = false,
                onClick = {}
            )

            PanelCard(title = "1. WHAT TO DO") {
                LabeledValue(
                    label = "STEP",
                    value = when (state.mode) {
                        NfcMode.IDLE -> "Choose READ TAG, WRITE TAG, or ERASE TAG."
                        NfcMode.READ -> "Read mode is selected. Tap a tag against your phone."
                        NfcMode.WRITE -> "Write mode is selected. Check the type and data below, then tap a tag."
                        NfcMode.ERASE -> "Erase mode is selected. Tap a tag against your phone to clear it."
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "BUTTON CONFIRMATION",
                    value = state.lastActionMessage
                )
            }

            PanelCard(title = "2. NFC MODE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeButton(
                        title = "READ",
                        selected = state.mode == NfcMode.READ,
                        onClick = onReadMode,
                        modifier = Modifier.weight(1f)
                    )
                    ModeButton(
                        title = "WRITE",
                        selected = state.mode == NfcMode.WRITE,
                        onClick = onWriteMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeButton(
                        title = "ERASE",
                        selected = state.mode == NfcMode.ERASE,
                        onClick = onEraseMode,
                        modifier = Modifier.weight(1f)
                    )
                    ModeButton(
                        title = "STOP",
                        selected = state.mode == NfcMode.IDLE,
                        onClick = onIdleMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LabeledValue(label = "STATUS", value = state.status)
            }

            PanelCard(title = "3. WRITE TYPE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "TEXT",
                        selected = state.writeData.type == NdefWriteType.TEXT,
                        onClick = { onWriteTypeSelected(NdefWriteType.TEXT) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "URL",
                        selected = state.writeData.type == NdefWriteType.URL,
                        onClick = { onWriteTypeSelected(NdefWriteType.URL) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "PHONE",
                        selected = state.writeData.type == NdefWriteType.PHONE,
                        onClick = { onWriteTypeSelected(NdefWriteType.PHONE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WriteTypeButton(
                        title = "EMAIL",
                        selected = state.writeData.type == NdefWriteType.EMAIL,
                        onClick = { onWriteTypeSelected(NdefWriteType.EMAIL) },
                        modifier = Modifier.weight(1f)
                    )
                    WriteTypeButton(
                        title = "SMS",
                        selected = state.writeData.type == NdefWriteType.SMS,
                        onClick = { onWriteTypeSelected(NdefWriteType.SMS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LabeledValue(
                    label = "CURRENT TYPE",
                    value = state.writeData.type.name
                )
            }

            PanelCard(title = "4. CONTENT TO WRITE") {
                when (state.writeData.type) {
                    NdefWriteType.TEXT -> {
                        UltraTextField(
                            label = "Text",
                            value = state.writeData.text,
                            placeholder = "Enter the text to write",
                            onValueChange = onTextChanged
                        )
                    }

                    NdefWriteType.URL -> {
                        UltraTextField(
                            label = "URL",
                            value = state.writeData.url,
                            placeholder = "example.com or https://example.com",
                            onValueChange = onUrlChanged
                        )
                    }

                    NdefWriteType.PHONE -> {
                        UltraTextField(
                            label = "Phone Number",
                            value = state.writeData.phoneNumber,
                            placeholder = "+441234567890",
                            onValueChange = onPhoneChanged
                        )
                    }

                    NdefWriteType.EMAIL -> {
                        UltraTextField(
                            label = "Email To",
                            value = state.writeData.emailTo,
                            placeholder = "name@example.com",
                            onValueChange = onEmailToChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Subject",
                            value = state.writeData.emailSubject,
                            placeholder = "Subject",
                            onValueChange = onEmailSubjectChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "Body",
                            value = state.writeData.emailBody,
                            placeholder = "Email body",
                            minLines = 3,
                            onValueChange = onEmailBodyChanged
                        )
                    }

                    NdefWriteType.SMS -> {
                        UltraTextField(
                            label = "SMS Number",
                            value = state.writeData.smsNumber,
                            placeholder = "+441234567890",
                            onValueChange = onSmsNumberChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        UltraTextField(
                            label = "SMS Message",
                            value = state.writeData.smsBody,
                            placeholder = "Message text",
                            minLines = 3,
                            onValueChange = onSmsBodyChanged
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LabeledValue(
                    label = "PREVIEW",
                    value = state.writeData.describeForUi()
                )
            }

            PanelCard(title = "5. ACTION BUTTONS") {
                GradientActionButton(
                    text = "READ TAG",
                    enabled = state.mode != NfcMode.READ,
                    onClick = onReadMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientActionButton(
                    text = "WRITE TAG",
                    enabled = state.mode != NfcMode.WRITE,
                    onClick = onWriteMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientActionButton(
                    text = "ERASE TAG",
                    enabled = state.mode != NfcMode.ERASE,
                    onClick = onEraseMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientActionButton(
                    text = "STOP NFC",
                    enabled = state.mode != NfcMode.IDLE,
                    onClick = onIdleMode
                )
            }

            PanelCard(title = "6. TAG RESULT") {
                LabeledValue(
                    label = "CONTENT",
                    value = if (state.lastRead.isBlank()) "No tag read yet." else state.lastRead
                )

                Spacer(modifier = Modifier.height(12.dp))

                LabeledValue(
                    label = "DETAILS",
                    value = if (state.lastDetails.isBlank()) "No details yet." else state.lastDetails
                )

                Spacer(modifier = Modifier.height(14.dp))

                GradientActionButton(
                    text = "CLEAR RESULT",
                    enabled = true,
                    onClick = onClearRead
                )
            }

            Text(
                text = "BY CYPHER SHADOWBOURNE",
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    val gradient = Brush.horizontalGradient(
        colors = listOf(NeonCyan, NeonBlue, NeonMagenta)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Text(
            text = "NFC STUDIO",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "ULTRA",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(gradient, RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "BY CYPHER SHADOWBOURNE",
            color = TextMuted,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = PanelBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun PanelCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            NeonCyan.copy(alpha = 0.55f),
                            NeonMagenta.copy(alpha = 0.55f)
                        )
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = PanelSurface),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextMuted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (selected) {
        Brush.horizontalGradient(listOf(NeonCyan, NeonBlue, NeonMagenta))
    } else {
        Brush.horizontalGradient(listOf(PanelSurface, PanelSurface))
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(18.dp))
                .border(
                    1.dp,
                    if (selected) Color.Transparent else PanelBorder,
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WriteTypeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (selected) {
        Brush.horizontalGradient(listOf(NeonMagenta, NeonBlue))
    } else {
        Brush.horizontalGradient(listOf(PanelSurface, PanelSurface))
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (selected) Color.Transparent else PanelBorder,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val brush = Brush.horizontalGradient(listOf(NeonCyan, NeonBlue, NeonMagenta))

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush, shape = RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                PanelBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = NeonCyan,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun UltraTextField(
    label: String,
    value: String,
    placeholder: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextMuted) },
        placeholder = { Text(placeholder, color = TextMuted) },
        minLines = minLines,
        maxLines = if (minLines > 1) 6 else 1,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = neonTextFieldColors()
    )
}

@Composable
private fun neonTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = PanelSurface,
    unfocusedContainerColor = PanelSurface,
    disabledContainerColor = PanelSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NeonCyan,
    focusedIndicatorColor = NeonMagenta,
    unfocusedIndicatorColor = PanelBorder,
    focusedLabelColor = NeonCyan,
    unfocusedLabelColor = TextMuted
)
'@ | Set-Content -Encoding UTF8 (Join-Path $screenDir "NfcStudioUltraScreen.kt")

Write-Host ""
Write-Host "Typed NDEF reset complete."
Write-Host "Now run:"
Write-Host "  .\gradlew.bat clean"
Write-Host "  .\gradlew.bat assembleDebug"