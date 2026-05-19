package com.snapdex.app.ui.collection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.data.local.entity.CardRecordEntity
import com.snapdex.app.data.repository.BillingRepository
import com.snapdex.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val billingRepository: BillingRepository,
) : ViewModel() {

    val cards: StateFlow<List<CardRecordEntity>> = collectionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val isPro: StateFlow<Boolean> = billingRepository.isPro

    private val _isSelectMode = MutableStateFlow(false)
    val isSelectMode: StateFlow<Boolean> = _isSelectMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _optimisticDeletedIds = MutableStateFlow<Set<String>>(emptySet())

    val displayCards: StateFlow<List<CardRecordEntity>> = combine(cards, _optimisticDeletedIds) { all, removed ->
        if (removed.isEmpty()) all else all.filter { it.id !in removed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSelected: StateFlow<Boolean> = combine(displayCards, _selectedIds) { all, selected ->
        all.isNotEmpty() && selected.size == all.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    fun enterSelectMode(firstId: String) {
        _isSelectMode.value = true
        _selectedIds.value = setOf(firstId)
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelectMode() {
        _isSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = displayCards.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        _optimisticDeletedIds.value = ids
        clearSelectMode()
        viewModelScope.launch {
            coroutineScope {
                cards.value.filter { it.id in ids }.map { async { collectionRepository.delete(it) } }.awaitAll()
            }
            _optimisticDeletedIds.value = emptySet()
        }
    }

    private companion object {
        const val TAG = "CollectionViewModel"
    }
}
