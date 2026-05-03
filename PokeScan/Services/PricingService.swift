import Foundation

protocol PricingService {
    func fetchPrice(for card: Card) async throws -> Card
}

final class MockPricingService: PricingService {
    func fetchPrice(for card: Card) async throws -> Card {
        // Simulates latency of backend proxy call (real: URLSession → FastAPI /price/{card_sku})
        try await Task.sleep(for: .seconds(0.8))
        var result = card
        result.marketPrice = Double.random(in: 0.5...150.0)
        result.priceSource = .tcgplayer
        return result
    }
}

// MARK: - Live

final class LivePricingService: PricingService {
    private let session: URLSession
    private let baseURL: URL

    init(baseURL: URL = AppConfig.backendBaseURL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    func fetchPrice(for card: Card) async throws -> Card {
        let url = baseURL
            .appending(path: "price")
            .appending(path: card.cardSKU)

        let (data, response) = try await session.data(from: url)

        guard let http = response as? HTTPURLResponse else {
            throw PricingError.invalidResponse
        }
        guard http.statusCode == 200 else {
            throw PricingError.serverError(statusCode: http.statusCode)
        }

        let dto = try JSONDecoder().decode(PriceResponseDTO.self, from: data)

        var result = card
        result.marketPrice = dto.marketPrice
        result.priceSource = .tcgplayer
        return result
    }
}

// MARK: - DTO

private struct PriceResponseDTO: Decodable {
    let cardSku: String
    let marketPrice: Double?
    let priceSource: String
    let isCompletedSale: Bool
    let fetchedAt: String

    enum CodingKeys: String, CodingKey {
        case cardSku = "card_sku"
        case marketPrice = "market_price"
        case priceSource = "price_source"
        case isCompletedSale = "is_completed_sale"
        case fetchedAt = "fetched_at"
    }
}

// MARK: - Errors

enum PricingError: Error, LocalizedError {
    case invalidResponse
    case serverError(statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid server response"
        case .serverError(let code): return "Server error \(code)"
        }
    }
}
