import AVFoundation
import SwiftUI

enum ScanState {
    case idle, scanning, detected, loading, result
}

@MainActor
final class CameraViewModel: NSObject, ObservableObject {
    @Published var scanState: ScanState = .idle
    @Published var isAuthorized = false

    let session = AVCaptureSession()
    private let output = AVCaptureVideoDataOutput()
    private let sessionQueue = DispatchQueue(label: "com.pokescan.camera")

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

            // Phase 1: replace nil with VisionService sample buffer delegate
            self.output.setSampleBufferDelegate(nil, queue: self.sessionQueue)
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

    func startScan() {
        guard scanState == .idle else { return }
        scanState = .scanning
    }

    func resetScan() {
        scanState = .idle
    }
}
