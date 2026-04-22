package com.cyphershadowbourne.nfcstudioultra.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import java.nio.charset.Charset
import java.util.Arrays
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
                NfcLog.operationStarted(mode)
                val result = when (mode) {
                    NfcMode.READ -> readTag(tag)
                    NfcMode.WRITE -> writeTag(tag, writeData)
                    NfcMode.MULTI_WRITE -> NfcOperationResult.Error("Use processMultiTag for multi-write.")
                    NfcMode.CLONE -> readForClone(tag)
                    NfcMode.COMPARE -> readForCompare(tag)
                    NfcMode.ERASE -> eraseTag(tag)
                    NfcMode.IDLE -> NfcOperationResult.Error("Idle mode.")
                }
                NfcLog.operationCompleted(mode, result)
                callback(result)
            } catch (t: Throwable) {
                NfcLog.e("NFC operation failed during $mode", t)
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

    fun processMultiTag(
        tag: Tag,
        writeDataList: List<NdefWriteData>,
        callback: (NfcOperationResult) -> Unit
    ) {
        if (!processing.compareAndSet(false, true)) {
            callback(NfcOperationResult.Ignored("Another NFC action is already running."))
            return
        }

        executor.execute {
            try {
                NfcLog.operationStarted(NfcMode.MULTI_WRITE)
                val result = writeMultiTag(tag, writeDataList)
                NfcLog.operationCompleted(NfcMode.MULTI_WRITE, result)
                callback(result)
            } catch (t: Throwable) {
                NfcLog.e("NFC multi-write operation failed", t)
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

    fun processCloneWrite(
        tag: Tag,
        message: NdefMessage,
        callback: (NfcOperationResult) -> Unit
    ) {
        if (!processing.compareAndSet(false, true)) {
            callback(NfcOperationResult.Ignored("Another NFC action is already running."))
            return
        }

        executor.execute {
            try {
                NfcLog.operationStarted(NfcMode.CLONE)
                val result = writeCloneTag(tag, message)
                NfcLog.operationCompleted(NfcMode.CLONE, result)
                callback(result)
            } catch (t: Throwable) {
                NfcLog.e("NFC clone write operation failed", t)
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

    private fun readForCompare(tag: Tag): NfcOperationResult {
        val ndef = Ndef.get(tag)
        val techList = tag.techList.map { it.substringAfterLast('.') }
        
        val tagInfo = if (ndef != null) {
            try {
                ndef.connect()
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                
                TagInfo(
                    recordCount = message?.records?.size ?: 0,
                    recordTypes = message?.records?.map { it.type.toString(Charsets.US_ASCII) } ?: emptyList(),
                    decodedValues = message?.records?.map { parseRecord(it).toDisplayText() } ?: emptyList(),
                    isWritable = ndef.isWritable,
                    sizeBytes = ndef.maxSize,
                    techList = techList
                )
            } catch (e: Exception) {
                TagInfo(0, emptyList(), emptyList(), false, 0, techList)
            } finally {
                safeClose(ndef)
            }
        } else {
            TagInfo(0, emptyList(), emptyList(), false, 0, techList)
        }
        
        return NfcOperationResult.CompareSuccess(
            result = TagComparisonResult(
                areEqual = false,
                differences = emptyList(),
                tag1 = tagInfo,
                tag2 = tagInfo
            )
        )
    }

    private fun readTag(tag: Tag): NfcOperationResult {
        val techList = tag.techList.joinToString(", ")
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            return try {
                ndef.connect()

                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                val parsedContent = parseMessage(message)
                val parsedRecords = message?.records?.map { record ->
                    ParsedNdefRecord(
                        type = record.type.toString(Charsets.US_ASCII),
                        tnf = tnfToString(record.tnf),
                        payloadHex = record.payload.joinToString("") { "%02X".format(it) },
                        decodedValue = parseRecord(record).toDisplayText()
                    )
                } ?: emptyList()

                NfcLog.d("Read NDEF message: $message")
                NfcOperationResult.ReadSuccess(
                    content = parsedContent,
                    displayText = parsedContent.toDisplayText(),
                    details = buildString {
                        appendLine("Type: ${ndef.type ?: "Unknown"}")
                        appendLine("Writable: ${ndef.isWritable}")
                        appendLine("Max size: ${ndef.maxSize} bytes")
                        append("Tech: $techList")
                    },
                    techList = tag.techList.map { it.substringAfterLast('.') },
                    isWritable = ndef.isWritable,
                    sizeBytes = ndef.maxSize,
                    tagIdHex = tag.id.joinToString("") { "%02X".format(it) },
                    records = parsedRecords
                )
            } catch (e: Exception) {
                NfcLog.w("Read failed: ${e.message}", e)
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

    private fun readForClone(tag: Tag): NfcOperationResult {
        val ndef = Ndef.get(tag)
        if (ndef == null) return NfcOperationResult.Error("Source tag does not support NDEF.")

        return try {
            ndef.connect()
            val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
            if (message == null) {
                NfcOperationResult.Error("Source tag is empty.")
            } else {
                val content = parseMessage(message)
                NfcOperationResult.ReadSuccess(
                    content = content,
                    displayText = content.toDisplayText(),
                    details = "Payload for cloning: ${message.toByteArray().size} bytes",
                    tagIdHex = tag.id.joinToString("") { "%02X".format(it) },
                    rawMessage = message,
                    records = message.records.map { record ->
                        ParsedNdefRecord(
                            type = record.type.toString(Charsets.US_ASCII),
                            tnf = tnfToString(record.tnf),
                            payloadHex = record.payload.joinToString("") { "%02X".format(it) },
                            decodedValue = parseRecord(record).toDisplayText()
                        )
                    }
                )
            }
        } catch (e: Exception) {
            NfcOperationResult.Error("Failed to read source: ${e.message}")
        } finally {
            safeClose(ndef)
        }
    }

    private fun writeCloneTag(tag: Tag, message: NdefMessage): NfcOperationResult {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val payloadSize = message.toByteArray().size
                if (ndef.maxSize < payloadSize) {
                    return NfcOperationResult.Error("Destination tag too small. Need $payloadSize bytes, have ${ndef.maxSize} bytes.")
                }
                if (!ndef.isWritable) {
                    return NfcOperationResult.Error("Destination tag is not writable.")
                }
            } catch (e: Exception) {
                // Ignore for now, writeMessageToTag will handle it
            } finally {
                safeClose(ndef)
            }
        }

        val result = writeMessageToTag(tag, message, "Clone complete.")
        return if (result is NfcOperationResult.WriteSuccess) {
            NfcOperationResult.CloneSuccess(result.message, result.verificationStatus)
        } else {
            result
        }
    }

    private fun writeTag(tag: Tag, writeData: NdefWriteData): NfcOperationResult {
        NfcLog.d("Preparing to write: $writeData")
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

                // Post-write verification
                val verificationStatus = try {
                    val readBackMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage
                    if (readBackMessage != null && areMessagesEquivalent(message, readBackMessage)) {
                        VerificationStatus.VERIFIED
                    } else {
                        VerificationStatus.MISMATCH
                    }
                } catch (e: Exception) {
                    NfcLog.w("Verification read failed: ${e.message}")
                    VerificationStatus.UNAVAILABLE
                }

                NfcOperationResult.WriteSuccess(successMessage, verificationStatus)
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
                // Verification for formatable tags is tricky because they might need a reconnect as Ndef
                NfcOperationResult.WriteSuccess(successMessage, VerificationStatus.UNAVAILABLE)
            } catch (e: Exception) {
                NfcOperationResult.Error("Format/write failed: ${e.message ?: "unknown error"}")
            } finally {
                safeClose(formatable)
            }
        }

        return NfcOperationResult.Error("Tag does not support NDEF.")
    }

    private fun areMessagesEquivalent(m1: NdefMessage, m2: NdefMessage): Boolean {
        val b1 = m1.toByteArray()
        val b2 = m2.toByteArray()
        return b1.contentEquals(b2)
    }

    private fun createMessageForWriteData(writeData: NdefWriteData): NdefMessage? {
        return try {
            val record = createRecordForWriteData(writeData)
            record?.let { NdefMessage(arrayOf(it)) }
        } catch (e: Exception) {
            NfcLog.e("Error creating write message: ${e.message}", e)
            null
        }
    }

    private fun writeMultiTag(tag: Tag, writeDataList: List<NdefWriteData>): NfcOperationResult {
        NfcLog.d("Preparing to multi-write: ${writeDataList.size} records")
        val records = writeDataList.mapNotNull { createRecordForWriteData(it) }
        
        if (records.isEmpty()) {
            return NfcOperationResult.Error("No valid records to write.")
        }

        val message = NdefMessage(records.toTypedArray())
        val result = writeMessageToTag(tag, message, "Multi-write successful.")
        
        return if (result is NfcOperationResult.WriteSuccess) {
            NfcOperationResult.MultiWriteSuccess(result.message, result.verificationStatus)
        } else {
            result
        }
    }

    private fun createRecordForWriteData(writeData: NdefWriteData): NdefRecord? {
        return try {
            when (writeData.type) {
                NdefRecordType.TEXT -> {
                    if (writeData.text.isBlank()) null else createTextRecord(writeData.text)
                }
                NdefRecordType.URL -> {
                    val value = writeData.url.trim()
                    if (value.isBlank()) null else {
                        val normalized = if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) value else "https://$value"
                        NdefRecord.createUri(normalized)
                    }
                }
                NdefRecordType.PHONE -> {
                    val value = writeData.phoneNumber.trim()
                    if (value.isBlank()) null else NdefRecord.createUri("tel:$value")
                }
                NdefRecordType.EMAIL -> {
                    val to = writeData.emailTo.trim()
                    if (to.isBlank()) null else {
                        val uri = buildEmailUri(to = to, subject = writeData.emailSubject, body = writeData.emailBody)
                        NdefRecord.createUri(uri)
                    }
                }
                NdefRecordType.SMS -> {
                    val number = writeData.smsNumber.trim()
                    if (number.isBlank()) null else {
                        val uri = buildSmsUri(number = number, body = writeData.smsBody)
                        NdefRecord.createUri(uri)
                    }
                }
                NdefRecordType.LOCATION -> {
                    val latitude = writeData.locationLatitude.trim().toDoubleOrNull()
                    val longitude = writeData.locationLongitude.trim().toDoubleOrNull()
                    if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) null
                    else NdefRecord.createUri(String.format(Locale.US, "geo:%1$.6f,%2$.6f", latitude, longitude))
                }
                NdefRecordType.CONTACT -> createContactRecord(writeData)
                NdefRecordType.WIFI -> createWifiRecord(writeData)
                NdefRecordType.CALENDAR -> createCalendarRecord(writeData)
                NdefRecordType.SMART_POSTER -> createSmartPosterRecord(writeData)
                NdefRecordType.AAR -> createAarRecord(writeData)
                NdefRecordType.MIME -> createMimeRecord(writeData)
                NdefRecordType.EXTERNAL -> createExternalRecord(writeData)
            }
        } catch (e: Exception) {
            NfcLog.e("Error creating record: ${e.message}", e)
            null
        }
    }

    private fun createWifiRecord(data: NdefWriteData): NdefRecord? {
        return try {
            // Simplified Wi-Fi payload (Android 10+ uses NDEF for Wi-Fi but many systems use this simple text format)
            // Or use the standard Wi-Fi configuration format if available.
            // For now, let's use the standard NDEF Wi-Fi Configuration record type "application/vnd.wfa.wsc"
            // but simplified payload for compatibility.
            val ssid = "S:${data.wifiSsid};"
            val auth = "T:${data.wifiAuthType.replace("/", "")};"
            val pass = if (data.wifiPassword.isNotBlank()) "P:${data.wifiPassword};" else ""
            val hidden = "" // Could add hidden network flag
            val payload = "WIFI:$ssid$auth$pass$hidden;".toByteArray(Charsets.UTF_8)
            NdefRecord.createMime("application/vnd.wfa.wsc", payload)
        } catch (e: Exception) {
            null
        }
    }

    private fun createCalendarRecord(data: NdefWriteData): NdefRecord? {
        return try {
            val vEvent = buildString {
                append("BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\n")
                append("SUMMARY:${data.calendarTitle}\n")
                if (data.calendarLocation.isNotBlank()) append("LOCATION:${data.calendarLocation}\n")
                if (data.calendarDescription.isNotBlank()) append("DESCRIPTION:${data.calendarDescription}\n")
                // Format: YYYYMMDDTHHMMSSZ (simplified for now)
                val start = data.calendarStart.replace("-", "").replace(" ", "T").replace(":", "") + "00Z"
                append("DTSTART:$start\n")
                if (data.calendarEnd.isNotBlank()) {
                    val end = data.calendarEnd.replace("-", "").replace(" ", "T").replace(":", "") + "00Z"
                    append("DTEND:$end\n")
                }
                append("END:VEVENT\nEND:VCALENDAR")
            }.toByteArray(Charsets.UTF_8)
            NdefRecord.createMime("text/calendar", vEvent)
        } catch (e: Exception) {
            null
        }
    }

    private fun createSmartPosterRecord(data: NdefWriteData): NdefRecord? {
        return try {
            // A Smart Poster consists of a URI and a Title
            val titleRecord = createTextRecord(data.smartPosterTitle)
            val uriRecord = NdefRecord.createUri(data.url)
            val message = NdefMessage(titleRecord, uriRecord)
            NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null, message.toByteArray())
        } catch (e: Exception) {
            null
        }
    }

    private fun createAarRecord(data: NdefWriteData): NdefRecord? {
        return try {
            NdefRecord.createApplicationRecord(data.aarPackageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun createMimeRecord(data: NdefWriteData): NdefRecord? {
        return try {
            val payload = if (data.mimeIsHex) {
                data.mimePayload.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            } else {
                data.mimePayload.toByteArray(Charsets.UTF_8)
            }
            NdefRecord.createMime(data.mimeType, payload)
        } catch (e: Exception) {
            null
        }
    }

    private fun createExternalRecord(data: NdefWriteData): NdefRecord? {
        return try {
            val payload = if (data.externalIsHex) {
                data.externalPayload.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            } else {
                data.externalPayload.toByteArray(Charsets.UTF_8)
            }
            NdefRecord.createExternal(data.externalDomain, data.externalType, payload)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMessage(message: NdefMessage?): NdefContent {
        if (message == null || message.records.isEmpty()) {
            return NdefContent.Text("(Empty tag)")
        }

        if (message.records.size > 1) {
            val records = message.records.map { parseRecord(it) }
            return NdefContent.Multi(records)
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
            record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_SMART_POSTER) -> {
                try {
                    val subMessage = NdefMessage(record.payload)
                    var title = ""
                    var uri = ""
                    for (subRecord in subMessage.records) {
                        if (isTextRecord(subRecord)) title = parseTextRecord(subRecord)
                        if (isUriRecord(subRecord)) uri = parseUriRecord(subRecord)
                    }
                    NdefContent.SmartPoster(title, uri)
                } catch (e: Exception) {
                    NdefContent.Unknown("Smart Poster (Malformed)")
                }
            }
            record.tnf == NdefRecord.TNF_EXTERNAL_TYPE && Arrays.equals(record.type, "android.com:pkg".toByteArray(Charsets.US_ASCII)) -> {
                NdefContent.Aar(String(record.payload, Charsets.UTF_8))
            }
            record.tnf == NdefRecord.TNF_MIME_MEDIA -> {
                val mimeType = String(record.type, Charsets.US_ASCII)
                val payloadStr = String(record.payload, Charsets.UTF_8)
                when (mimeType) {
                    "application/vnd.wfa.wsc" -> {
                        if (payloadStr.contains("WIFI:S:")) {
                            val ssid = payloadStr.substringAfter("S:").substringBefore(";")
                            val auth = payloadStr.substringAfter("T:").substringBefore(";")
                            val pass = if (payloadStr.contains("P:")) payloadStr.substringAfter("P:").substringBefore(";") else ""
                            NdefContent.Wifi(ssid, auth, pass)
                        } else {
                            NdefContent.MimeRecord(mimeType, payloadStr)
                        }
                    }
                    "text/calendar" -> {
                        val title = payloadStr.substringAfter("SUMMARY:").substringBefore("\n")
                        val start = payloadStr.substringAfter("DTSTART:").substringBefore("\n")
                        val loc = payloadStr.substringAfter("LOCATION:").substringBefore("\n")
                        NdefContent.Calendar(title, start, loc)
                    }
                    else -> NdefContent.MimeRecord(mimeType, payloadStr)
                }
            }
            record.tnf == NdefRecord.TNF_EXTERNAL_TYPE -> {
                val typeStr = String(record.type, Charsets.US_ASCII)
                val parts = typeStr.split(":")
                val domain = parts.getOrNull(0) ?: ""
                val type = parts.getOrNull(1) ?: ""
                NdefContent.ExternalRecord(domain, type, String(record.payload, Charsets.UTF_8))
            }
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
                    NdefContent.Url(trimmed)
                }
            }

            else -> NdefContent.Url(trimmed)
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

    private fun tnfToString(tnf: Short): String {
        return when (tnf) {
            NdefRecord.TNF_EMPTY -> "Empty"
            NdefRecord.TNF_WELL_KNOWN -> "Well Known"
            NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
            NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
            NdefRecord.TNF_EXTERNAL_TYPE -> "External Type"
            NdefRecord.TNF_UNKNOWN -> "Unknown"
            NdefRecord.TNF_UNCHANGED -> "Unchanged"
            else -> "Reserved"
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
