package com.cyphershadowbourne.nfcstudioultra.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cyphershadowbourne.nfcstudioultra.domain.model.WriteTemplate
import com.cyphershadowbourne.nfcstudioultra.domain.repository.TemplateRepository
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefRecordType
import com.cyphershadowbourne.nfcstudioultra.nfc.NdefWriteData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SharedPrefsTemplateRepository(context: Context) : TemplateRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("nfc_templates", Context.MODE_PRIVATE)
    private val _templates = MutableStateFlow<List<WriteTemplate>>(loadTemplates())
    override val templates: StateFlow<List<WriteTemplate>> = _templates.asStateFlow()

    override fun add(template: WriteTemplate) {
        val currentList = _templates.value.toMutableList()
        currentList.add(0, template)
        _templates.value = currentList
        saveTemplates(currentList)
    }

    override fun update(template: WriteTemplate) {
        val currentList = _templates.value.map {
            if (it.id == template.id) template else it
        }
        _templates.value = currentList
        saveTemplates(currentList)
    }

    override fun delete(id: String) {
        val currentList = _templates.value.filterNot { it.id == id }
        _templates.value = currentList
        saveTemplates(currentList)
    }

    private fun saveTemplates(items: List<WriteTemplate>) {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val data = item.data
            val dataJson = JSONObject().apply {
                put("type", data.type.name)
                put("text", data.text)
                put("url", data.url)
                put("phoneNumber", data.phoneNumber)
                put("emailTo", data.emailTo)
                put("emailSubject", data.emailSubject)
                put("emailBody", data.emailBody)
                put("smsNumber", data.smsNumber)
                put("smsBody", data.smsBody)
                put("locationLatitude", data.locationLatitude)
                put("locationLongitude", data.locationLongitude)
                put("contactName", data.contactName)
                put("contactPhone", data.contactPhone)
                put("contactEmail", data.contactEmail)
                put("contactOrganization", data.contactOrganization)
            }
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("data", dataJson)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("templates", jsonArray.toString()).apply()
    }

    private fun loadTemplates(): List<WriteTemplate> {
        val jsonString = prefs.getString("templates", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<WriteTemplate>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val dataJson = jsonObject.getJSONObject("data")
                
                val data = NdefWriteData(
                    type = NdefRecordType.valueOf(dataJson.getString("type")),
                    text = dataJson.optString("text", ""),
                    url = dataJson.optString("url", ""),
                    phoneNumber = dataJson.optString("phoneNumber", ""),
                    emailTo = dataJson.optString("emailTo", ""),
                    emailSubject = dataJson.optString("emailSubject", ""),
                    emailBody = dataJson.optString("emailBody", ""),
                    smsNumber = dataJson.optString("smsNumber", ""),
                    smsBody = dataJson.optString("smsBody", ""),
                    locationLatitude = dataJson.optString("locationLatitude", ""),
                    locationLongitude = dataJson.optString("locationLongitude", ""),
                    contactName = dataJson.optString("contactName", ""),
                    contactPhone = dataJson.optString("contactPhone", ""),
                    contactEmail = dataJson.optString("contactEmail", ""),
                    contactOrganization = dataJson.optString("contactOrganization", "")
                )

                list.add(
                    WriteTemplate(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        data = data
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
