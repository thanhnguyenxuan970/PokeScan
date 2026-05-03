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
