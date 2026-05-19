import Foundation
import Vision

enum AppEnvironment: String {
    case development
    case production
}

enum AppConfig {
    static var environment: AppEnvironment {
        let raw = ProcessInfo.processInfo.environment["SNAPDEX_ENV"] ?? "development"
        return AppEnvironment(rawValue: raw) ?? .development
    }

    static var backendBaseURL: URL {
        switch environment {
        case .development:
            return URL(string: "http://localhost:8000")!
        case .production:
            return URL(string: "https://api.snapdex.app")!
        }
    }

    static var visionRecognitionLevel: VNRequestTextRecognitionLevel {
        ProcessInfo.processInfo.environment["SNAPDEX_VISION_FAST"] == "1" ? .fast : .accurate
    }

    static let privacyPolicyURL = URL(string: "https://app.termly.io/policy-viewer/policy.html?policyUUID=a921da63-2d56-452d-a6a8-c8f94daa9bb2")!
}
