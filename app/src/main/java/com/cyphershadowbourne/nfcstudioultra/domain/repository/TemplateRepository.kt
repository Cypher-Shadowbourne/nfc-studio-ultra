package com.cyphershadowbourne.nfcstudioultra.domain.repository

import com.cyphershadowbourne.nfcstudioultra.domain.model.WriteTemplate
import kotlinx.coroutines.flow.StateFlow

interface TemplateRepository {
    val templates: StateFlow<List<WriteTemplate>>
    fun add(template: WriteTemplate)
    fun update(template: WriteTemplate)
    fun delete(id: String)
}
