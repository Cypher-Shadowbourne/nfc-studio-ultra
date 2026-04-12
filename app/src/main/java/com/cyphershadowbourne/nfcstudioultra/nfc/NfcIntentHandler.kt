package com.cyphershadowbourne.nfcstudioultra.nfc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast

object NfcIntentHandler {

    fun handle(context: Context, content: NdefContent) {
        when (content) {
            is NdefContent.Url -> openUrl(context, content.value)
            is NdefContent.Phone -> openDialer(context, content.value)
            is NdefContent.Email -> openEmail(context, content)
            is NdefContent.Sms -> openSms(context, content)
            is NdefContent.Location -> openLocation(context, content)
            is NdefContent.Contact -> openContact(context, content)
            is NdefContent.Text -> {
                if (content.value.isNotBlank() && content.value != "(Tag contains no readable content)") {
                    Toast.makeText(context, content.value, Toast.LENGTH_SHORT).show()
                }
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

    private fun openLocation(context: Context, value: NdefContent.Location) {
        try {
            val uri = Uri.parse("geo:${value.latitude},${value.longitude}?q=${value.latitude},${value.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No maps app found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openContact(context: Context, value: NdefContent.Contact) {
        try {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, value.name)
                putExtra(ContactsContract.Intents.Insert.PHONE, value.phone)
                putExtra(ContactsContract.Intents.Insert.EMAIL, value.email)
                putExtra(ContactsContract.Intents.Insert.COMPANY, value.organization)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No contacts app found.", Toast.LENGTH_SHORT).show()
        }
    }
}
