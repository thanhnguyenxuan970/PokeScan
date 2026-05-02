import Foundation

enum CardLanguage: String, Codable {
    case english, japanese
}

enum PriceSource: String, Codable {
    case tcgplayer, ebay, cardmarket, aggregated
}

struct Card: Identifiable, Codable {
    let id: UUID
    var name: String
    var setNumber: String
    var setCode: String
    var language: CardLanguage
    var marketPrice: Double?
    var priceSource: PriceSource?
    var scannedAt: Date

    // "{setCode}-{setNumber replacing '/' with '-'}" e.g. base1-025-102
    var cardSKU: String {
        "\(setCode)-\(setNumber.replacingOccurrences(of: "/", with: "-"))"
    }
}
