import Foundation
import SwiftData

@Model
final class CardRecord {
    @Attribute(.unique) var id: UUID
    var name: String
    var setCode: String
    var setNumber: String
    var language: String
    var marketPrice: Double?
    var priceSource: String?
    var scannedAt: Date
    var syncedAt: Date?    // nil = not yet synced to server
    var serverID: String?  // assigned by server after successful POST

    init(from card: Card) {
        self.id = card.id
        self.name = card.name
        self.setCode = card.setCode
        self.setNumber = card.setNumber
        self.language = card.language.rawValue
        self.marketPrice = card.marketPrice
        self.priceSource = card.priceSource?.rawValue
        self.scannedAt = card.scannedAt
        self.syncedAt = nil
        self.serverID = nil
    }

    func toCard() -> Card {
        Card(
            id: id,
            name: name,
            setNumber: setNumber,
            setCode: setCode,
            language: CardLanguage(rawValue: language) ?? .english,
            marketPrice: marketPrice,
            priceSource: priceSource.flatMap { PriceSource(rawValue: $0) },
            scannedAt: scannedAt
        )
    }
}
