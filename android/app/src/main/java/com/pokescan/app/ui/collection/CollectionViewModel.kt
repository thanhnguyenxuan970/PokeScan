package com.snapdex.app.ui.collection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.data.local.entity.CardRecordEntity
import com.snapdex.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    val cards: StateFlow<List<CardRecordEntity>> = collectionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        if (secureStorage.getToken() != null) refresh()
    }

    fun refresh() {
        if (_syncState.value is SyncState.Loading) return
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            try {
                collectionRepository.syncAll()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                Log.w(TAG, "Sync failed: ${e.message}")
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    fun deleteCard(entity: CardRecordEntity) {
        viewModelScope.launch { collectionRepository.delete(entity) }
    }

    private companion object {
        const val TAG = "CollectionViewModel"
    }
}
