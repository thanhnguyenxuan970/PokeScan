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

        let request = VNRecognizeTextRequest { [weak self] request, error in
            self?.isProcessing = false
            if let error {
                self?.onResult?(.failure(error))
                return
            }
            let observations = request.results as? [VNRecognizedTextObservation] ?? []
            self?.onResult?(.success(observations))
        }
        request.recognitionLevel = .accurate
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
