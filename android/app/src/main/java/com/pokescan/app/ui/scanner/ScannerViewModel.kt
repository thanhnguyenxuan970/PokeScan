package com.snapdex.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapdex.app.data.repository.BillingRepository
import com.snapdex.app.data.repository.CollectionRepository
import com.snapdex.app.data.service.CardIdentificationService
import com.snapdex.app.data.service.PricingService
import com.snapdex.app.data.service.ScanCounterService
import com.snapdex.app.domain.model.Card
import com.snapdex.app.domain.model.CardLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
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
    object NoInternet : ScanEvent()
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
            try {
                val card = pricingService.fetchPrice(
                    MOCK_IDENTIFIED.random(),
                    billingRepository.isPro.value,
                )
                if (_state.value !is ScanState.Scanning) return@launch
                collectionRepository.saveLocal(card)
                scanCounterService.recordScan(isPro = billingRepository.isPro.value)
                _state.value = ScanState.Result(card)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _events.emit(ScanEvent.NoInternet)
                resetScan()
            } catch (e: Exception) {
                _events.emit(ScanEvent.NoCardDetected)
                resetScan()
            } finally {
                isProcessing = false
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
                collectionRepository.saveLocal(card)
                scanCounterService.recordScan(isPro = billingRepository.isPro.value)
                _state.value = ScanState.Result(card)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _events.emit(ScanEvent.NoInternet)
                resetScan()
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
        private val MOCK_IDENTIFIED = listOf(
            CardIdentificationService.IdentifiedCard(
                cardName = "Charizard ex",
                setNumber = "125/091",
                setCode = "sv3pt5",
                language = CardLanguage.ENGLISH,
                setName = "Paldean Fates",
                setYear = 2024,
            ),
            CardIdentificationService.IdentifiedCard(
                cardName = "Pikachu ex",
                setNumber = "030/159",
                setCode = "sv1",
                language = CardLanguage.ENGLISH,
                setName = "Scarlet & Violet",
                setYear = 2023,
            ),
            CardIdentificationService.IdentifiedCard(
                cardName = "Mewtwo ex",
                setNumber = "089/165",
                setCode = "sv2a",
                language = CardLanguage.ENGLISH,
                setName = "151",
                setYear = 2023,
            ),
        )
    }
}
