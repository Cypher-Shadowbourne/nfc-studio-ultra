package com.cyphershadowbourne.nfcstudioultra.nfc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import java.util.ArrayList

object NfcIntentHandler {

    fun handle(context: Context, content: NdefContent) {
        when (content) {
            is NdefContent.Url -> openUrl(context, content.value)
            is NdefContent.Phone -> openDialer(context, content.value)
            is NdefContent.Email -> openEmail(context, content)
            is NdefContent.Sms -> openSms(context, content)
            is NdefContent.Location -> openLocation(context, content)
            is NdefContent.Contact -> openContact(context, content)
            is NdefContent.Multi -> {
                // For multi-record, we just show a toast or nothing as it's a composite
                Toast.makeText(context, "Multi-record tag detected", Toast.LENGTH_SHORT).show()
            }
            is NdefContent.Text -> {
                if (content.value.isNotBlank() && content.value != "(Tag contains no readable content)") {
                    Toast.makeText(context, content.value, Toast.LENGTH_SHORT).show()
                }
            }
            is NdefContent.Unknown -> {
                Toast.makeText(context, content.raw, Toast.LENGTH_SHORT).show()
            }
            is NdefContent.Wifi -> {
                connectToWifi(context, content)
            }
            is NdefContent.Calendar -> {
                Toast.makeText(context, "Calendar Event detected: ${content.title}", Toast.LENGTH_SHORT).show()
            }
            is NdefContent.SmartPoster -> {
                openUrl(context, content.uri)
            }
            is NdefContent.Aar -> {
                Toast.makeText(context, "AAR: ${content.packageName}", Toast.LENGTH_SHORT).show()
            }
            is NdefContent.MimeRecord -> {
                Toast.makeText(context, "MIME Record: ${content.type}", Toast.LENGTH_SHORT).show()
            }
            is NdefContent.ExternalRecord -> {
                Toast.makeText(context, "External Record: ${content.domain}:${content.type}", Toast.LENGTH_SHORT).show()
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

    private fun connectToWifi(context: Context, wifi: NdefContent.Wifi) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // On Android 10+, apps cannot toggle Wi-Fi directly. We show the Wi-Fi panel instead.
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                    panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(panelIntent)
                    Toast.makeText(context, "Please enable Wi-Fi to connect", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Please enable Wi-Fi manually", Toast.LENGTH_SHORT).show()
                }
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+: Use ACTION_WIFI_ADD_NETWORKS for a system-managed prompt
                val suggestion = createWifiSuggestion(wifi)
                val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                    val list = ArrayList<WifiNetworkSuggestion>()
                    list.add(suggestion)
                    putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, list)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to suggestions API
                    addWifiSuggestion(context, wifiManager, suggestion, wifi.ssid)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10: Use Network Suggestions
                val suggestion = createWifiSuggestion(wifi)
                addWifiSuggestion(context, wifiManager, suggestion, wifi.ssid)
            }
            else -> {
                // Legacy Wi-Fi connection (pre-Android 10)
                connectToWifiLegacy(context, wifi, wifiManager)
            }
        }
    }

    private fun createWifiSuggestion(wifi: NdefContent.Wifi): WifiNetworkSuggestion {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw IllegalStateException("WifiNetworkSuggestion requires API 29+")
        }
        val builder = WifiNetworkSuggestion.Builder()
            .setSsid(wifi.ssid)
            .setIsAppInteractionRequired(false)

        when (wifi.auth.uppercase()) {
            "WPA", "WPA2", "WPA/WPA2" -> builder.setWpa2Passphrase(wifi.password)
            "WPA3" -> builder.setWpa3Passphrase(wifi.password)
        }
        return builder.build()
    }

    private fun addWifiSuggestion(context: Context, wifiManager: WifiManager, suggestion: WifiNetworkSuggestion, ssid: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Toast.makeText(context, "Wi-Fi suggestion added. System will connect to $ssid when in range.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to add Wi-Fi suggestion", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun connectToWifiLegacy(context: Context, wifi: NdefContent.Wifi, wifiManager: WifiManager) {
        try {
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = "\"${wifi.ssid}\""
                when (wifi.auth.uppercase()) {
                    "WPA", "WPA2", "WPA/WPA2" -> {
                        preSharedKey = "\"${wifi.password}\""
                    }
                    "OPEN" -> {
                        allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                    }
                }
            }
            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(netId, true)
                wifiManager.reconnect()
                Toast.makeText(context, "Connecting to ${wifi.ssid}...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add Wi-Fi network", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error connecting to Wi-Fi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
