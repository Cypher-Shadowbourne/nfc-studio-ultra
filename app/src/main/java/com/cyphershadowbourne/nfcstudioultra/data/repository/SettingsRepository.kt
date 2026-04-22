package com.cyphershadowbourne.nfcstudioultra.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val urlSafetyEnabled: StateFlow<Boolean>
    val expertModeEnabled: StateFlow<Boolean>
    fun setUrlSafetyEnabled(enabled: Boolean)
    fun setExpertModeEnabled(enabled: Boolean)
}

class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("nfc_studio_settings", Context.MODE_PRIVATE)
    
    private val _urlSafetyEnabled = MutableStateFlow(prefs.getBoolean("url_safety_enabled", true))
    override val urlSafetyEnabled: StateFlow<Boolean> = _urlSafetyEnabled

    private val _expertModeEnabled = MutableStateFlow(prefs.getBoolean("expert_mode_enabled", false))
    override val expertModeEnabled: StateFlow<Boolean> = _expertModeEnabled

    override fun setUrlSafetyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("url_safety_enabled", enabled).apply()
        _urlSafetyEnabled.value = enabled
    }

    override fun setExpertModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("expert_mode_enabled", enabled).apply()
        _expertModeEnabled.value = enabled
    }
}
