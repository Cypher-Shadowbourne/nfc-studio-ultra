package com.cyphershadowbourne.nfcstudioultra.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale
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
                    Thread.sleep(500)
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
                safeClose(ndef)
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return NfcOperationResult.Error(
                "Tag is formatable but not formatted (empty NFC tag)."
            )
        }

        return NfcOperationResult.Error(
            "Not an NDEF-compatible tag. Tech: $techList"
        )
    }

    private fun writeTag(tag: Tag, writeData: NdefWriteData): NfcOperationResult {
        val message = createMessageForWriteData(writeData)
            ?: return NfcOperationResult.Error("Enter valid data before WRITE.")

        return writeMessageToTag(tag, message, "Write successful.")
    }

    private fun eraseTag(tag: Tag): NfcOperationResult {
        val emptyRecordMessage = NdefMessage(
            arrayOf(
                NdefRecord(
                    NdefRecord.TNF_EMPTY,
                    ByteArray(0),
                    ByteArray(0),
                    ByteArray(0)
                )
            )
        )

        val result = writeMessageToTag(
            tag = tag,
            message = emptyRecordMessage,
            successMessage = "Erase successful."
        )

        return if (result is NfcOperationResult.WriteSuccess) {
            result
        } else {
            NfcOperationResult.Error(
                "Erase failed. Tag may be read-only or unsupported."
            )
        }
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
                    return NfcOperationResult.Error("Tag is read-only.")
                }

                val size = message.toByteArray().size
                if (size > ndef.maxSize) {
                    return NfcOperationResult.Error("Tag too small for data.")
                }

                ndef.writeNdefMessage(message)
                NfcOperationResult.WriteSuccess(successMessage)
            } catch (e: Exception) {
                NfcOperationResult.Error("Write failed: ${e.message ?: "unknown error"}")
            } finally {
                safeClose(ndef)
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
                safeClose(formatable)
            }
        }

        return NfcOperationResult.Error("Tag does not support NDEF.")
    }

    private fun createMessageForWriteData(writeData: NdefWriteData): NdefMessage? {
        val record = when (writeData.type) {
            NdefRecordType.TEXT -> {
                if (writeData.text.isBlank()) {
                    null
                } else {
                    createTextRecord(writeData.text)
                }
            }

            NdefRecordType.URL -> {
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

            NdefRecordType.PHONE -> {
                val value = writeData.phoneNumber.trim()
                if (value.isBlank()) {
                    null
                } else {
                    NdefRecord.createUri("tel:$value")
                }
            }

            NdefRecordType.EMAIL -> {
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

            NdefRecordType.SMS -> {
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

            NdefRecordType.LOCATION -> {
                val latitude = writeData.locationLatitude.trim().toDoubleOrNull()
                val longitude = writeData.locationLongitude.trim().toDoubleOrNull()
                if (
                    latitude == null ||
                    longitude == null ||
                    latitude !in -90.0..90.0 ||
                    longitude !in -180.0..180.0
                ) {
                    null
                } else {
                    NdefRecord.createUri(
                        String.format(
                            Locale.US,
                            "geo:%1$.6f,%2$.6f",
                            latitude,
                            longitude
                        )
                    )
                }
            }

            NdefRecordType.CONTACT -> createContactRecord(writeData)
        }

        return record?.let { NdefMessage(arrayOf(it)) }
    }

    private fun parseMessage(message: NdefMessage?): NdefContent {
        if (message == null || message.records.isEmpty()) {
            return NdefContent.Text("(Empty tag)")
        }

        val firstRecord = message.records.first()

        if (isEmptyRecord(firstRecord)) {
            return NdefContent.Text("(Empty tag)")
        }

        return parseRecord(firstRecord)
    }

    private fun parseRecord(record: NdefRecord): NdefContent {
        return when {
            isEmptyRecord(record) -> NdefContent.Text("(Empty tag)")
            isTextRecord(record) -> NdefContent.Text(parseTextRecord(record))
            isUriRecord(record) -> parseUriContent(parseUriRecord(record))
            isVCardRecord(record) -> parseVCardContent(record)
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
            trimmed.startsWith("tel:", ignoreCase = true) ->
                NdefContent.Phone(trimmed.removePrefix("tel:"))

            trimmed.startsWith("mailto:", ignoreCase = true) -> {
                val to = trimmed.substringAfter("mailto:").substringBefore("?")
                val uri = Uri.parse(trimmed)
                NdefContent.Email(
                    to = to,
                    subject = uri.getQueryParameter("subject").orEmpty(),
                    body = uri.getQueryParameter("body").orEmpty()
                )
            }

            trimmed.startsWith("smsto:", ignoreCase = true) ||
                trimmed.startsWith("sms:", ignoreCase = true) -> {
                val scheme = if (trimmed.startsWith("smsto:", ignoreCase = true)) {
                    "smsto:"
                } else {
                    "sms:"
                }

                val number = trimmed.substringAfter(scheme).substringBefore("?")
                val uri = Uri.parse(trimmed)
                NdefContent.Sms(
                    number = number,
                    body = uri.getQueryParameter("body").orEmpty()
                )
            }

            trimmed.startsWith("geo:", ignoreCase = true) -> {
                val coordinates = trimmed.substringAfter("geo:").substringBefore("?")
                val latitude = coordinates.substringBefore(",").toDoubleOrNull()
                val longitude = coordinates.substringAfter(",", "").toDoubleOrNull()

                if (latitude != null && longitude != null) {
                    NdefContent.Location(latitude = latitude, longitude = longitude)
                } else {
                    NdefContent.Unknown(trimmed)
                }
            }

            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("www.", ignoreCase = true) ->
                NdefContent.Url(trimmed)

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
                if (content.subject.isNotBlank()) {
                    append("\nSubject: ${content.subject}")
                }
                if (content.body.isNotBlank()) {
                    append("\nBody: ${content.body}")
                }
            }
            is NdefContent.Sms -> buildString {
                append("SMS to: ${content.number}")
                if (content.body.isNotBlank()) {
                    append("\nMessage: ${content.body}")
                }
            }
            is NdefContent.Location -> String.format(
                Locale.US,
                "Location: %.6f, %.6f",
                content.latitude,
                content.longitude
            )
            is NdefContent.Contact -> buildString {
                append("Contact: ")
                append(content.name.ifBlank { "(No name)" })
                if (content.phone.isNotBlank()) {
                    append("\nPhone: ${content.phone}")
                }
                if (content.email.isNotBlank()) {
                    append("\nEmail: ${content.email}")
                }
                if (content.organization.isNotBlank()) {
                    append("\nOrg: ${content.organization}")
                }
            }
            is NdefContent.Unknown -> content.raw
        }
    }

    private fun createTextRecord(text: String): NdefRecord {
        val lang = "en"
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val langBytes = lang.toByteArray(Charset.forName("US-ASCII"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)

        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
    }

    private fun buildEmailUri(to: String, subject: String, body: String): String {
        val uri = Uri.Builder()
            .scheme("mailto")
            .opaquePart(to)

        if (subject.isNotBlank()) {
            uri.appendQueryParameter("subject", subject)
        }

        if (body.isNotBlank()) {
            uri.appendQueryParameter("body", body)
        }

        return uri.build().toString()
    }

    private fun buildSmsUri(number: String, body: String): String {
        val uri = Uri.Builder()
            .scheme("smsto")
            .opaquePart(number)

        if (body.isNotBlank()) {
            uri.appendQueryParameter("body", body)
        }

        return uri.build().toString()
    }

    private fun createContactRecord(writeData: NdefWriteData): NdefRecord? {
        val name = writeData.contactName.trim()
        val phone = writeData.contactPhone.trim()
        val email = writeData.contactEmail.trim()
        val organization = writeData.contactOrganization.trim()

        if (
            name.isBlank() &&
            phone.isBlank() &&
            email.isBlank() &&
            organization.isBlank()
        ) {
            return null
        }

        val payload = buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (name.isNotBlank()) appendLine("FN:${escapeVCardValue(name)}")
            if (organization.isNotBlank()) appendLine("ORG:${escapeVCardValue(organization)}")
            if (phone.isNotBlank()) appendLine("TEL:${escapeVCardValue(phone)}")
            if (email.isNotBlank()) appendLine("EMAIL:${escapeVCardValue(email)}")
            append("END:VCARD")
        }.toByteArray(Charsets.UTF_8)

        return NdefRecord.createMime("text/x-vCard", payload)
    }

    private fun parseVCardContent(record: NdefRecord): NdefContent {
        val text = record.payload.toString(Charsets.UTF_8)
        val fields = mutableMapOf<String, String>()

        text.lineSequence()
            .map(String::trim)
            .filter { it.contains(':') }
            .forEach { line ->
                val rawKey = line.substringBefore(':')
                val key = rawKey.substringBefore(';').uppercase(Locale.US)
                val value = line.substringAfter(':').trim()
                if (value.isNotBlank() && key !in fields) {
                    fields[key] = value
                }
            }

        return NdefContent.Contact(
            name = fields["FN"].orEmpty(),
            phone = fields["TEL"].orEmpty(),
            email = fields["EMAIL"].orEmpty(),
            organization = fields["ORG"].orEmpty()
        )
    }

    private fun escapeVCardValue(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    private fun isEmptyRecord(record: NdefRecord): Boolean {
        return record.tnf == NdefRecord.TNF_EMPTY &&
            record.type.isEmpty() &&
            record.id.isEmpty() &&
            record.payload.isEmpty()
    }

    private fun isTextRecord(record: NdefRecord): Boolean {
        return record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)
    }

    private fun isUriRecord(record: NdefRecord): Boolean {
        return record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_URI)
    }

    private fun isVCardRecord(record: NdefRecord): Boolean {
        return record.tnf == NdefRecord.TNF_MIME_MEDIA &&
            record.type.toString(Charsets.US_ASCII).equals("text/x-vcard", ignoreCase = true)
    }

    private fun parseTextRecord(record: NdefRecord): String {
        return try {
            val payload = record.payload
            val langLength = payload[0].toInt() and 0x3F
            String(
                payload,
                1 + langLength,
                payload.size - 1 - langLength,
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            "(Unreadable text)"
        }
    }

    private fun parseUriRecord(record: NdefRecord): String {
        return try {
            val payload = record.payload
            val prefixIndex = payload[0].toInt() and 0xFF
            val prefix = URI_PREFIX_MAP.getOrElse(prefixIndex) { "" }
            val uri = String(payload, 1, payload.size - 1, Charsets.UTF_8)
            prefix + uri
        } catch (_: Exception) {
            "(Unreadable URI)"
        }
    }

    private fun safeClose(closeable: Any?) {
        try {
            when (closeable) {
                is Ndef -> closeable.close()
                is NdefFormatable -> closeable.close()
            }
        } catch (_: IOException) {
        }
    }

    companion object {
        private val URI_PREFIX_MAP = arrayOf(
            "",
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://",
            "sftp://",
            "smb://",
            "nfs://",
            "ftp://",
            "dav://",
            "news:",
            "telnet://",
            "imap:",
            "rtsp://",
            "urn:",
            "pop:",
            "sip:",
            "sips:",
            "tftp:",
            "btspp://",
            "btl2cap://",
            "btgoep://",
            "tcpobex://",
            "irdaobex://",
            "file://",
            "urn:epc:id:",
            "urn:epc:tag:",
            "urn:epc:pat:",
            "urn:epc:raw:",
            "urn:epc:",
            "urn:nfc:"
        )
    }
}
