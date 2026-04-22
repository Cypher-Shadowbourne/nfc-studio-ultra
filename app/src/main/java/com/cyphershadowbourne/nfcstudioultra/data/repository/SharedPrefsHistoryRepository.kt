package com.cyphershadowbourne.nfcstudioultra.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cyphershadowbourne.nfcstudioultra.domain.model.HistoryItem
import com.cyphershadowbourne.nfcstudioultra.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SharedPrefsHistoryRepository(context: Context) : HistoryRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("nfc_history", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow<List<HistoryItem>>(loadHistory())
    override val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    override fun add(item: HistoryItem) {
        val currentList = _history.value.toMutableList()
        currentList.add(0, item)
        _history.value = currentList
        saveHistory(currentList)
    }

    override fun delete(id: String) {
        val currentList = _history.value.filterNot { it.id == id }
        _history.value = currentList
        saveHistory(currentList)
    }

    override fun clear() {
        _history.value = emptyList()
        prefs.edit().remove("history_items").apply()
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val jsonArray = JSONArray()
        items.take(100).forEach { item ->
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("recordTypes", JSONArray(item.recordTypes))
                put("previewText", item.previewText)
                put("techList", JSONArray(item.techList))
                put("isWritable", item.isWritable)
                put("sizeBytes", item.sizeBytes ?: -1)
                put("tagIdHex", item.tagIdHex ?: "")
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("history_items", jsonArray.toString()).apply()
    }

    private fun loadHistory(): List<HistoryItem> {
        val jsonString = prefs.getString("history_items", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val recordTypesArray = jsonObject.getJSONArray("recordTypes")
                val recordTypes = mutableListOf<String>()
                for (j in 0 until recordTypesArray.length()) {
                    recordTypes.add(recordTypesArray.getString(j))
                }
                val techListArray = jsonObject.getJSONArray("techList")
                val techList = mutableListOf<String>()
                for (j in 0 until techListArray.length()) {
                    techList.add(techListArray.getString(j))
                }
                
                val sizeBytes = jsonObject.optInt("sizeBytes", -1)
                val tagIdHex = jsonObject.optString("tagIdHex", "")

                list.add(
                    HistoryItem(
                        id = jsonObject.getString("id"),
                        timestamp = jsonObject.getLong("timestamp"),
                        recordTypes = recordTypes,
                        previewText = jsonObject.getString("previewText"),
                        techList = techList,
                        isWritable = jsonObject.getBoolean("isWritable"),
                        sizeBytes = if (sizeBytes == -1) null else sizeBytes,
                        tagIdHex = if (tagIdHex.isEmpty()) null else tagIdHex
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
