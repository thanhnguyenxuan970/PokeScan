import Foundation

protocol GradeROIServiceProtocol {
    func calculate(
        cardSKU: String,
        rawPrice: Double,
        condition: CardCondition,
        service: GradingService
    ) async throws -> GradeROIResult
}

final class LiveGradeROIService: GradeROIServiceProtocol {
    private let session: URLSession
    private let baseURL: URL

    init(baseURL: URL = AppConfig.backendBaseURL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    func calculate(
        cardSKU: String,
        rawPrice: Double,
        condition: CardCondition,
        service: GradingService
    ) async throws -> GradeROIResult {
        let url = baseURL.appendingPathComponent("grading/roi")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token = AuthService.shared.readTokenFromKeychain() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let body: [String: Any] = [
            "card_sku": cardSKU,
            "raw_price": rawPrice,
            "condition": condition.rawValue,
            "service": service.rawValue,
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw GradeROIError.invalidResponse }
        if http.statusCode == 403 { throw GradeROIError.proRequired }
        guard http.statusCode == 200 else { throw GradeROIError.serverError(statusCode: http.statusCode) }

        return try JSONDecoder().decode(GradeROIResult.self, from: data)
    }
}

enum GradeROIError: Error, LocalizedError {
    case invalidResponse
    case proRequired
    case serverError(statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid server response"
        case .proRequired: return "Pro subscription required"
        case .serverError(let code): return "Server error \(code)"
        }
    }
}
