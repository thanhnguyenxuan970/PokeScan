package com.pokescan.app.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.app.data.repository.BillingRepository
import com.pokescan.app.data.repository.CollectionRepository
import com.pokescan.app.data.service.ScanCounterService
import com.pokescan.app.domain.model.Card
import com.pokescan.app.domain.model.CardLanguage
import com.pokescan.app.domain.model.PriceSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Result(val card: Card) : ScanState()
}

sealed class ScanEvent {
    object ShowPaywall : ScanEvent()
    // TODO: emit on OCR timeout when real scan pipeline is restored
    object NoCardDetected : ScanEvent()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanCounterService: ScanCounterService,
    private val billingRepository: BillingRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    val isPro: StateFlow<Boolean> = billingRepository.isPro
    val scansThisMonth = scanCounterService.scansThisMonth

    private val _events = MutableSharedFlow<ScanEvent>(replay = 0)
    val events = _events.asSharedFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (_state.value !is ScanState.Idle) return
        scanJob = viewModelScope.launch {
            if (!scanCounterService.canScan(isPro = billingRepository.isPro.value)) {
                _events.emit(ScanEvent.ShowPaywall)
                return@launch
            }
            _state.value = ScanState.Scanning
            delay(1_800)
            if (_state.value !is ScanState.Scanning) return@launch
            val mockCard = Card(
                id = UUID.randomUUID().toString(),
                name = "Charizard",
                setNumber = "4/102",
                setCode = "base1",
                language = CardLanguage.ENGLISH,
                marketPrice = 450.00,
                priceSource = PriceSource.AGGREGATED,
                scannedAt = System.currentTimeMillis(),
                tcgPlayerPrice = 425.00,
                ebayPrice = 472.00,
                variant = "Holo",
                setName = "Base Set",
                setYear = 1999,
                isAuthentic = true,
                priceUpdatedAt = System.currentTimeMillis(),
                gradeRoiPsaGrade = 9,
                gradeRoiSellValue = 1200.00,
                gradeRoiNetProfit = 725.00,
            )
            _state.value = ScanState.Result(mockCard)
            scanCounterService.recordScan(isPro = billingRepository.isPro.value)
            viewModelScope.launch {
                try { collectionRepository.saveLocal(mockCard) }
                catch (e: Exception) { Log.w("ScannerViewModel", "saveLocal failed", e) }
            }
        }
    }

    fun resetScan() {
        scanJob?.cancel()
        _state.value = ScanState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
