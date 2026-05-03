import Foundation
import Vision

enum AppEnvironment: String {
    case development
    case production
}

enum AppConfig {
    static var environment: AppEnvironment {
        let raw = ProcessInfo.processInfo.environment["POKESCAN_ENV"] ?? "development"
        return AppEnvironment(rawValue: raw) ?? .development
    }

    static var backendBaseURL: URL {
        switch environment {
        case .development:
            return URL(string: "http://localhost:8000")!
        case .production:
            return URL(string: "https://api.pokescan.app")!
        }
    }

    static var visionRecognitionLevel: VNRequestTextRecognitionLevel {
        ProcessInfo.processInfo.environment["POKESCAN_VISION_FAST"] == "1" ? .fast : .accurate
    }
}
