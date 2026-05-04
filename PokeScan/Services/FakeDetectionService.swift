import Foundation

protocol FakeDetectionServiceProtocol {
    func check(
        card: Card,
        marketPrice: Double,
        listedPrice: Double,
        scanConfidence: Double
    ) async throws -> AuthenticityResult
}

@MainActor
final class LiveFakeDetectionService: ObservableObject, FakeDetectionServiceProtocol {
    @Published var result: AuthenticityResult? = nil
    @Published var isLoading = false

    private let session: URLSession
    private let baseURL: URL

    init(baseURL: URL = AppConfig.backendBaseURL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    func check(
        card: Card,
        marketPrice: Double,
        listedPrice: Double,
        scanConfidence: Double = 1.0
    ) async throws -> AuthenticityResult {
        let url = baseURL.appendingPathComponent("detection/authenticity")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token = AuthService.shared.readTokenFromKeychain() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let body: [String: Any] = [
            "card_sku": card.cardSKU,
            "card_name": card.name,
            "set_number": card.setNumber,
            "market_price": marketPrice,
            "listed_price": listedPrice,
            "scan_confidence": scanConfidence,
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw FakeDetectionError.serverError
        }
        return try JSONDecoder().decode(AuthenticityResult.self, from: data)
    }

    func checkAndStore(card: Card) async {
        guard let price = card.marketPrice else { return }
        isLoading = true
        result = try? await check(card: card, marketPrice: price, listedPrice: price, scanConfidence: 1.0)
        isLoading = false
    }
}

enum FakeDetectionError: Error {
    case serverError
}
