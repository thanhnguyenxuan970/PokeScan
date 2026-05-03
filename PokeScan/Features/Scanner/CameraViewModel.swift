import AVFoundation
import SwiftUI

enum ScanState {
    case idle, scanning, detected, loading, result
}

@MainActor
final class CameraViewModel: NSObject, ObservableObject {
    @Published var scanState: ScanState = .idle
    @Published var isAuthorized = false
    @Published var detectedCard: Card?
    @Published var presentedCard: Card?

    let session = AVCaptureSession()
    private let output = AVCaptureVideoDataOutput()
    private let sessionQueue = DispatchQueue(label: "com.pokescan.camera")
    private let visionService = VisionService()
    private let cardService = CardIdentificationService()
    private var pricingService: PricingService = {
        #if DEBUG
        if ProcessInfo.processInfo.environment["POKESCAN_USE_MOCK"] == "1" {
            return MockPricingService()
        }
        #endif
        return LivePricingService()
    }()
    private let scanCounter = ScanCounterService.shared
    @Published var showPaywall = false

    override init() {
        super.init()
        Task { await checkAuthorization() }
    }

    func checkAuthorization() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            isAuthorized = true
            setupSession()
        case .notDetermined:
            let granted = await AVCaptureDevice.requestAccess(for: .video)
            isAuthorized = granted
            if granted { setupSession() }
        default:
            isAuthorized = false
        }
    }

    // SWIFT_STRICT_CONCURRENCY=complete would flag session/output/visionService as @MainActor-isolated.
    // Safe here: non-strict mode + serial sessionQueue. Track if upgrading concurrency level.
    nonisolated private func setupSession() {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            self.session.beginConfiguration()
            self.session.sessionPreset = .high

            do {
                guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
                    self.session.commitConfiguration()
                    return
                }
                let input = try AVCaptureDeviceInput(device: device)
                guard self.session.canAddInput(input) else {
                    self.session.commitConfiguration()
                    return
                }
                self.session.addInput(input)
            } catch {
                #if DEBUG
                print("[CameraViewModel] AVCaptureDeviceInput error: \(error)")
                #endif
                self.session.commitConfiguration()
                return
            }

            let vision = self.visionService
            vision.onResult = { [weak self] result in
                Task { @MainActor [weak self] in
                    self?.handleVisionResult(result)
                }
            }
            self.output.setSampleBufferDelegate(vision, queue: self.sessionQueue)
            if self.session.canAddOutput(self.output) {
                self.session.addOutput(self.output)
            }
            self.session.commitConfiguration()
        }
    }

    func startSession() {
        sessionQueue.async { [weak self] in
            guard let self, !self.session.isRunning else { return }
            self.session.startRunning()
        }
    }

    func stopSession() {
        sessionQueue.async { [weak self] in
            guard let self, self.session.isRunning else { return }
            self.session.stopRunning()
        }
    }

    func handleVisionResult(_ result: VisionResult) {
        guard scanState == .scanning else { return }
        switch result {
        case .failure:
            return
        case .success(let observations):
            guard let card = cardService.identify(from: observations) else { return }
            detectedCard = card
            scanState = .detected
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                guard scanState == .detected else { return }
                scanState = .loading
                do {
                    let priced = try await pricingService.fetchPrice(for: card)
                    guard scanState == .loading else { return }
                    detectedCard = priced
                    presentedCard = priced
                    scanState = .result
                    scanCounter.recordScan()
                } catch {
                    scanState = .idle
                }
            }
        }
    }

    func startScan() {
        guard scanState == .idle else { return }
        guard scanCounter.canScan() else {
            showPaywall = true
            return
        }
        scanState = .scanning
    }

    func resetScan() {
        scanState = .idle
        detectedCard = nil
        presentedCard = nil
    }
}
