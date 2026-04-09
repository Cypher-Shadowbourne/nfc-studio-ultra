package com.cyphershadowbourne.nfcstudioultra.domain.model

sealed class NfcOperationResult {
    data class ReadSuccess(val result: TagScanResult) : NfcOperationResult()
    data class WriteSuccess(val message: String) : NfcOperationResult()
    data class FormatSuccess(val message: String) : NfcOperationResult()
    data class Error(val message: String) : NfcOperationResult()
}
