package com.cyphershadowbourne.nfcstudioultra.nfc

enum class NdefRecordType {
    TEXT,
    URL,
    PHONE,
    EMAIL,
    SMS,
    LOCATION,
    CONTACT,
    WIFI,
    CALENDAR,
    SMART_POSTER,
    AAR,
    MIME,
    EXTERNAL
}

data class NdefWriteData(
    val type: NdefRecordType = NdefRecordType.TEXT,
    val text: String = "",
    val url: String = "",
    val phoneNumber: String = "",
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val smsNumber: String = "",
    val smsBody: String = "",
    val locationLatitude: String = "",
    val locationLongitude: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrganization: String = "",
    // New types
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiAuthType: String = "WPA/WPA2", // Open, WEP, WPA/WPA2
    val calendarTitle: String = "",
    val calendarLocation: String = "",
    val calendarDescription: String = "",
    val calendarStart: String = "", // YYYY-MM-DD HH:MM
    val calendarEnd: String = "",
    val smartPosterTitle: String = "",
    val aarPackageName: String = "",
    val mimeType: String = "",
    val mimePayload: String = "",
    val mimeIsHex: Boolean = false,
    val externalDomain: String = "",
    val externalType: String = "",
    val externalPayload: String = "",
    val externalIsHex: Boolean = false
) {
    fun describeForUi(): String {
        return when (type) {
            NdefRecordType.TEXT -> text
            NdefRecordType.URL -> url
            NdefRecordType.PHONE -> phoneNumber
            NdefRecordType.EMAIL -> buildString {
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
            NdefRecordType.SMS -> buildString {
                append("Number: ")
                append(smsNumber)
                if (smsBody.isNotBlank()) {
                    append("\nMessage: ")
                    append(smsBody)
                }
            }
            NdefRecordType.LOCATION -> buildString {
                append("Latitude: ")
                append(locationLatitude.ifBlank { "-" })
                append("\nLongitude: ")
                append(locationLongitude.ifBlank { "-" })
            }
            NdefRecordType.CONTACT -> buildString {
                append("Name: ")
                append(contactName.ifBlank { "-" })
                if (contactPhone.isNotBlank()) {
                    append("\nPhone: ")
                    append(contactPhone)
                }
                if (contactEmail.isNotBlank()) {
                    append("\nEmail: ")
                    append(contactEmail)
                }
                if (contactOrganization.isNotBlank()) {
                    append("\nOrg: ")
                    append(contactOrganization)
                }
            }
            NdefRecordType.WIFI -> buildString {
                append("SSID: ")
                append(wifiSsid)
                append("\nAuth: ")
                append(wifiAuthType)
                if (wifiPassword.isNotBlank()) {
                    append("\nPassword: ****")
                }
            }
            NdefRecordType.CALENDAR -> buildString {
                append("Event: ")
                append(calendarTitle)
                if (calendarLocation.isNotBlank()) append("\nAt: $calendarLocation")
                append("\nStart: $calendarStart")
                if (calendarEnd.isNotBlank()) append("\nEnd: $calendarEnd")
            }
            NdefRecordType.SMART_POSTER -> buildString {
                append("Title: ")
                append(smartPosterTitle)
                append("\nURI: ")
                append(url)
            }
            NdefRecordType.AAR -> "Android App: $aarPackageName"
            NdefRecordType.MIME -> "MIME: $mimeType\nPayload: ${if (mimeIsHex) "HEX data" else mimePayload}"
            NdefRecordType.EXTERNAL -> "External: $externalDomain:$externalType\nPayload: ${if (externalIsHex) "HEX data" else externalPayload}"
        }.ifBlank { "(No data entered yet)" }
    }

    fun isValid(): Boolean {
        return when (type) {
            NdefRecordType.TEXT -> text.isNotBlank()
            NdefRecordType.URL -> url.isNotBlank()
            NdefRecordType.PHONE -> phoneNumber.isNotBlank()
            NdefRecordType.EMAIL -> emailTo.isNotBlank()
            NdefRecordType.SMS -> smsNumber.isNotBlank()
            NdefRecordType.LOCATION -> {
                val lat = locationLatitude.toDoubleOrNull()
                val lon = locationLongitude.toDoubleOrNull()
                lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0
            }
            NdefRecordType.CONTACT -> {
                contactName.isNotBlank() || contactPhone.isNotBlank() ||
                        contactEmail.isNotBlank() || contactOrganization.isNotBlank()
            }
            NdefRecordType.WIFI -> wifiSsid.isNotBlank()
            NdefRecordType.CALENDAR -> calendarTitle.isNotBlank() && calendarStart.isNotBlank()
            NdefRecordType.SMART_POSTER -> smartPosterTitle.isNotBlank() && url.isNotBlank()
            NdefRecordType.AAR -> aarPackageName.isNotBlank() && aarPackageName.contains(".")
            NdefRecordType.MIME -> mimeType.isNotBlank() && mimeType.contains("/") && mimePayload.isNotBlank()
            NdefRecordType.EXTERNAL -> externalDomain.isNotBlank() && externalType.isNotBlank() && externalPayload.isNotBlank()
        }
    }
}

sealed class NdefContent {
    abstract fun toDisplayText(): String

    data class Text(val value: String) : NdefContent() {
        override fun toDisplayText() = value
    }

    data class Url(val value: String) : NdefContent() {
        override fun toDisplayText() = value
    }

    data class Phone(val value: String) : NdefContent() {
        override fun toDisplayText() = "Phone: $value"
    }

    data class Email(
        val to: String,
        val subject: String,
        val body: String
    ) : NdefContent() {
        override fun toDisplayText() = buildString {
            append("Email to: $to")
            if (subject.isNotBlank()) append("\nSubject: $subject")
            if (body.isNotBlank()) append("\nBody: $body")
        }
    }

    data class Sms(
        val number: String,
        val body: String
    ) : NdefContent() {
        override fun toDisplayText() = buildString {
            append("SMS to: $number")
            if (body.isNotBlank()) append("\nMessage: $body")
        }
    }

    data class Location(
        val latitude: Double,
        val longitude: Double
    ) : NdefContent() {
        override fun toDisplayText() = "Location: %.6f, %.6f".format(java.util.Locale.US, latitude, longitude)
    }

    data class Contact(
        val name: String,
        val phone: String,
        val email: String,
        val organization: String
    ) : NdefContent() {
        override fun toDisplayText() = buildString {
            append("Contact: ")
            append(name.ifBlank { "(No name)" })
            if (phone.isNotBlank()) append("\nPhone: $phone")
            if (email.isNotBlank()) append("\nEmail: $email")
            if (organization.isNotBlank()) append("\nOrg: $organization")
        }
    }

    data class Multi(
        val records: List<NdefContent>
    ) : NdefContent() {
        override fun toDisplayText() = buildString {
            append("Multi-record message (${records.size} records):")
            records.forEachIndexed { index, record ->
                append("\n\n[Record ${index + 1}]\n")
                append(record.toDisplayText())
            }
        }
    }

    data class Unknown(val raw: String) : NdefContent() {
        override fun toDisplayText() = raw
    }

    data class Wifi(val ssid: String, val auth: String, val password: String = "") : NdefContent() {
        override fun toDisplayText() = buildString {
            append("Wi-Fi Config\nSSID: $ssid\nAuth: $auth")
            if (password.isNotBlank()) append("\nPassword: $password")
        }
    }

    data class Calendar(val title: String, val start: String, val location: String) : NdefContent() {
        override fun toDisplayText() = "Calendar Event: $title\nStart: $start\nAt: $location"
    }

    data class SmartPoster(val title: String, val uri: String) : NdefContent() {
        override fun toDisplayText() = "Smart Poster: $title\nURI: $uri"
    }

    data class Aar(val packageName: String) : NdefContent() {
        override fun toDisplayText() = "Android App Record: $packageName"
    }

    data class MimeRecord(val type: String, val content: String) : NdefContent() {
        override fun toDisplayText() = "MIME ($type): $content"
    }

    data class ExternalRecord(val domain: String, val type: String, val content: String) : NdefContent() {
        override fun toDisplayText() = "External ($domain:$type): $content"
    }
}

data class ParsedNdefRecord(
    val type: String,
    val tnf: String,
    val payloadHex: String,
    val decodedValue: String
)

enum class VerificationStatus {
    VERIFIED,
    MISMATCH,
    UNAVAILABLE
}

data class TagComparisonResult(
    val areEqual: Boolean,
    val tag1: TagInfo,
    val tag2: TagInfo,
    val differences: List<String>
)

data class TagInfo(
    val recordCount: Int,
    val recordTypes: List<String>,
    val decodedValues: List<String>,
    val isWritable: Boolean,
    val sizeBytes: Int?,
    val techList: List<String>
)

sealed class NfcOperationResult {
    data class ReadSuccess(
        val content: NdefContent,
        val displayText: String,
        val details: String,
        val techList: List<String> = emptyList(),
        val isWritable: Boolean = false,
        val sizeBytes: Int? = null,
        val tagIdHex: String? = null,
        val records: List<ParsedNdefRecord> = emptyList(),
        val rawMessage: android.nfc.NdefMessage? = null
    ) : NfcOperationResult()

    data class CompareSuccess(
        val result: TagComparisonResult
    ) : NfcOperationResult()

    data class WriteSuccess(
        val message: String,
        val verificationStatus: VerificationStatus? = null
    ) : NfcOperationResult()

    data class MultiWriteSuccess(
        val message: String,
        val verificationStatus: VerificationStatus? = null
    ) : NfcOperationResult()

    data class CloneSuccess(
        val message: String,
        val verificationStatus: VerificationStatus? = null
    ) : NfcOperationResult()

    data class Ignored(
        val reason: String
    ) : NfcOperationResult()

    data class Error(
        val message: String
    ) : NfcOperationResult()
}
