package com.pokescan.app.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokescan.app.data.repository.BillingRepository
import com.pokescan.app.data.repository.CollectionRepository
import com.pokescan.app.data.service.CardIdentificationService
import com.pokescan.app.data.service.PricingService
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
import javax.inject.Inject

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Result(val card: Card) : ScanState()
}

sealed class ScanEvent {
    object ShowPaywall : ScanEvent()
    object NoCardDetected : ScanEvent()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanCounterService: ScanCounterService,
    private val billingRepository: BillingRepository,
    private val collectionRepository: CollectionRepository,
    private val cardIdentificationService: CardIdentificationService,
    private val pricingService: PricingService,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    val isPro: StateFlow<Boolean> = billingRepository.isPro
    val scansThisMonth = scanCounterService.scansThisMonth

    private val _events = MutableSharedFlow<ScanEvent>(replay = 0)
    val events = _events.asSharedFlow()

    private var scanJob: Job? = null

    @Volatile private var isProcessing = false

    fun startScan() {
        if (_state.value !is ScanState.Idle) return
        scanJob = viewModelScope.launch {
            if (!scanCounterService.canScan(isPro = billingRepository.isPro.value)) {
                _events.emit(ScanEvent.ShowPaywall)
                return@launch
            }
            isProcessing = true
            _state.value = ScanState.Scanning
            delay(1_800)
            if (_state.value !is ScanState.Scanning) { isProcessing = false; return@launch }
            val now = System.currentTimeMillis()
            val card = MOCK_CARDS.random().copy(
                id = java.util.UUID.randomUUID().toString(),
                scannedAt = now,
                priceUpdatedAt = now,
            )
            isProcessing = false
            scanCounterService.recordScan(isPro = billingRepository.isPro.value)
            _state.value = ScanState.Result(card)
            viewModelScope.launch {
                try { collectionRepository.saveLocal(card) }
                catch (e: Exception) { Log.w("ScannerViewModel", "saveLocal failed", e) }
            }
        }
    }

    fun onFrameAnalyzed(lines: List<String>) {
        if (_state.value !is ScanState.Scanning || isProcessing) return
        isProcessing = true
        val identified = cardIdentificationService.identify(lines)
        if (identified == null) {
            isProcessing = false
            return
        }
        scanJob?.cancel()
        viewModelScope.launch {
            try {
                val card = pricingService.fetchPrice(identified, billingRepository.isPro.value)
                if (_state.value !is ScanState.Scanning) return@launch
                _state.value = ScanState.Result(card)
                scanCounterService.recordScan(isPro = billingRepository.isPro.value)
                viewModelScope.launch {
                    try { collectionRepository.saveLocal(card) }
                    catch (e: Exception) { Log.w("ScannerViewModel", "saveLocal failed", e) }
                }
            } catch (e: Exception) {
                _events.emit(ScanEvent.NoCardDetected)
                resetScan()
            } finally {
                isProcessing = false
            }
        }
    }

    fun resetScan() {
        scanJob?.cancel()
        isProcessing = false
        _state.value = ScanState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }

    companion object {
        private val MOCK_CARDS = listOf(
            Card(
                id = "mock-charizard",
                name = "Charizard ex",
                setNumber = "125/091",
                setCode = "sv3pt5",
                language = CardLanguage.ENGLISH,
                marketPrice = 45.99,
                priceSource = PriceSource.AGGREGATED,
                scannedAt = 0L,
                tcgPlayerPrice = 44.50,
                ebayPrice = 47.48,
                setName = "Paldean Fates",
                setYear = 2024,
                isAuthentic = true,
                priceUpdatedAt = 0L,
            ),
            Card(
                id = "mock-pikachu",
                name = "Pikachu ex",
                setNumber = "030/159",
                setCode = "sv1",
                language = CardLanguage.ENGLISH,
                marketPrice = 12.50,
                priceSource = PriceSource.AGGREGATED,
                scannedAt = 0L,
                tcgPlayerPrice = 11.99,
                ebayPrice = 13.01,
                setName = "Scarlet & Violet",
                setYear = 2023,
                isAuthentic = true,
                priceUpdatedAt = 0L,
            ),
            Card(
                id = "mock-mewtwo",
                name = "Mewtwo ex",
                setNumber = "089/165",
                setCode = "sv2a",
                language = CardLanguage.ENGLISH,
                marketPrice = 8.25,
                priceSource = PriceSource.AGGREGATED,
                scannedAt = 0L,
                tcgPlayerPrice = 7.99,
                ebayPrice = 8.51,
                setName = "151",
                setYear = 2023,
                isAuthentic = true,
                priceUpdatedAt = 0L,
            ),
        )
    }
}
