package com.pokescan.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.data.local.entity.CardRecordEntity
import com.pokescan.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    val cards: StateFlow<List<CardRecordEntity>> = collectionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (secureStorage.getToken() != null) {
            viewModelScope.launch { collectionRepository.syncAll() }
        }
    }

    fun deleteCard(entity: CardRecordEntity) {
        viewModelScope.launch { collectionRepository.delete(entity) }
    }
}
