package com.cyphershadowbourne.nfcstudioultra.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyphershadowbourne.nfcstudioultra.domain.repository.HistoryRepository

class MainViewModelFactory(
    private val historyRepository: HistoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MainViewModel(historyRepository) as T
    }
}
