package com.pokescan.app.ui.scanner

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pokescan.app.data.service.CardIdentificationService
import com.pokescan.app.data.service.PricingService
import com.pokescan.app.data.service.ScanCounterService
import com.pokescan.app.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Detected(val card: CardIdentificationService.IdentifiedCard) : ScanState()
    data class Loading(val card: CardIdentificationService.IdentifiedCard) : ScanState()
    data class Result(val card: Card) : ScanState()
}

sealed class ScanEvent {
    object ShowPaywall : ScanEvent()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardIdentificationService: CardIdentificationService,
    private val pricingService: PricingService,
    private val scanCounterService: ScanCounterService,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>(replay = 0)
    val events = _events.asSharedFlow()

    @Volatile private var isProcessing = false
    private var isCameraStarted = false

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        if (isCameraStarted) return
        isCameraStarted = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                isCameraStarted = false
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (isProcessing) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        isProcessing = true
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                        )
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val lines = visionText.textBlocks
                                    .flatMap { it.lines }
                                    .map { it.text }
                                handleOcrResult(lines)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                                isProcessing = false
                            }
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun handleOcrResult(lines: List<String>) {
        if (_state.value !is ScanState.Scanning) return
        val identified = cardIdentificationService.identify(lines) ?: return
        _state.value = ScanState.Detected(identified)
        viewModelScope.launch {
            delay(400)
            if (_state.value !is ScanState.Detected) return@launch
            _state.value = ScanState.Loading(identified)
            try {
                val card = pricingService.fetchPrice(identified)
                _state.value = ScanState.Result(card)
                scanCounterService.recordScan(isPro = false)
            } catch (e: Exception) {
                _state.value = ScanState.Idle
            }
        }
    }

    fun startScan() {
        if (_state.value !is ScanState.Idle) return
        viewModelScope.launch {
            if (!scanCounterService.canScan(isPro = false)) {
                _events.emit(ScanEvent.ShowPaywall)
                return@launch
            }
            _state.value = ScanState.Scanning
        }
    }

    fun resetScan() {
        _state.value = ScanState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
        analysisExecutor.shutdown()
    }
}
