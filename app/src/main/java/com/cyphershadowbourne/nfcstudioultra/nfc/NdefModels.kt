package com.cyphershadowbourne.nfcstudioultra.nfc

enum class NdefRecordType {
    TEXT,
    URL,
    PHONE,
    EMAIL,
    SMS
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
    val smsBody: String = ""
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
        }.ifBlank { "(No data entered yet)" }
    }
}

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