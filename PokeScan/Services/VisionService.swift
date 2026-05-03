import AVFoundation
import Vision

typealias VisionResult = Result<[VNRecognizedTextObservation], Error>

final class VisionService: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    var onResult: ((VisionResult) -> Void)?
    private var isProcessing = false

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard !isProcessing else { return }
        isProcessing = true

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            isProcessing = false
            return
        }

        #if DEBUG
        let visionStart = Date()
        #endif

        let request = VNRecognizeTextRequest { [weak self] request, error in
            self?.isProcessing = false
            if let error {
                self?.onResult?(.failure(error))
                return
            }
            let observations = request.results as? [VNRecognizedTextObservation] ?? []
            #if DEBUG
            let elapsed = Date().timeIntervalSince(visionStart) * 1000
            let level = AppConfig.visionRecognitionLevel == .accurate ? "accurate" : "fast"
            print("[VisionService] OCR: \(String(format: "%.0f", elapsed))ms (\(level))")
            #endif
            self?.onResult?(.success(observations))
        }
        request.recognitionLevel = AppConfig.visionRecognitionLevel
        request.usesLanguageCorrection = false

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
        do {
            try handler.perform([request])
        } catch {
            isProcessing = false
            onResult?(.failure(error))
        }
    }
}
