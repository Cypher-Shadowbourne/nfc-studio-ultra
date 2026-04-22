package com.cyphershadowbourne.nfcstudioultra.nfc

import android.util.Log
import com.cyphershadowbourne.nfcstudioultra.BuildConfig

object NfcLog {
    private const val TAG = "NfcStudioUltra"

    private val logEntries = mutableListOf<String>()
    private val timeFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
    
    fun getLogs(): List<String> = logEntries.toList()
    fun clearLogs() { logEntries.clear() }

    private fun addLog(level: String, message: String) {
        if (!BuildConfig.DEBUG && level == "DEBUG") return
        val timestamp = timeFormat.format(java.util.Date())
        val entry = "[$timestamp] $level: $message"
        logEntries.add(0, entry)
        if (logEntries.size > 100) {
            logEntries.removeAt(logEntries.size - 1)
        }
    }

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
            addLog("DEBUG", message)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
        addLog("INFO", message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
        addLog("WARN", message + (throwable?.let { " - ${it.message}" } ?: ""))
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        addLog("ERROR", message + (throwable?.let { " - ${it.message}" } ?: ""))
    }

    fun operationStarted(mode: NfcMode) {
        i("Starting NFC operation: $mode")
    }

    fun operationCompleted(mode: NfcMode, result: NfcOperationResult) {
        val status = when (result) {
            is NfcOperationResult.ReadSuccess -> "SUCCESS (Read)"
            is NfcOperationResult.WriteSuccess -> "SUCCESS (Write)"
            is NfcOperationResult.MultiWriteSuccess -> "SUCCESS (Multi-Write)"
            is NfcOperationResult.CloneSuccess -> "SUCCESS (Clone)"
            is NfcOperationResult.CompareSuccess -> "SUCCESS (Compare)"
            is NfcOperationResult.Ignored -> "IGNORED: ${result.reason}"
            is NfcOperationResult.Error -> "ERROR: ${result.message}"
        }
        i("Completed NFC operation: $mode -> $status")
    }
}
