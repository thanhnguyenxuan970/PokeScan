import Foundation
import SwiftData

@MainActor
final class CollectionSyncService: ObservableObject {
    static let shared = CollectionSyncService()

    private let baseURL = AppConfig.backendBaseURL
    private let session = URLSession.shared

    private init() {}

    // MARK: - Public API

    func addCard(_ card: Card, context: ModelContext) async {
        let record = CardRecord(from: card)
        context.insert(record)
        try? context.save()
        await push(record: record, context: context)
    }

    func fullSync(context: ModelContext) async {
        await pushPending(context: context)
        await pullFromServer(context: context)
    }

    // MARK: - Push

    private func push(record: CardRecord, context: ModelContext) async {
        guard let token = AuthService.shared.readTokenFromKeychain() else { return }
        var request = URLRequest(url: baseURL.appendingPathComponent("collection"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        var body: [String: Any] = [
            "card_sku": record.setCode + "-" + record.setNumber.replacingOccurrences(of: "/", with: "-"),
            "name": record.name,
            "set_code": record.setCode,
            "set_number": record.setNumber,
            "language": record.language,
            "scanned_at": ISO8601DateFormatter().string(from: record.scannedAt),
        ]
        if let price = record.marketPrice { body["market_price"] = price }
        if let source = record.priceSource { body["price_source"] = source }
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, response) = try? await session.data(for: request),
              let http = response as? HTTPURLResponse,
              http.statusCode == 200,
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let serverID = json["server_id"] as? String
        else { return }

        record.serverID = serverID
        record.syncedAt = Date()
        try? context.save()
    }

    private func pushPending(context: ModelContext) async {
        let descriptor = FetchDescriptor<CardRecord>(
            predicate: #Predicate { $0.syncedAt == nil }
        )
        guard let pending = try? context.fetch(descriptor) else { return }
        for record in pending {
            await push(record: record, context: context)
        }
    }

    // MARK: - Pull

    private func pullFromServer(context: ModelContext) async {
        guard let token = AuthService.shared.readTokenFromKeychain() else { return }
        var request = URLRequest(url: baseURL.appendingPathComponent("collection"))
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        guard let (data, response) = try? await session.data(for: request),
              let http = response as? HTTPURLResponse,
              http.statusCode == 200,
              let serverCards = try? decoder.decode([ServerCardDTO].self, from: data)
        else { return }

        for dto in serverCards {
            let serverID = dto.server_id
            let descriptor = FetchDescriptor<CardRecord>(
                predicate: #Predicate { $0.serverID == serverID }
            )
            if (try? context.fetch(descriptor))?.isEmpty == true {
                let record = CardRecord(from: dto.toCard())
                record.serverID = serverID
                record.syncedAt = dto.scanned_at
                context.insert(record)
            }
        }
        try? context.save()
    }
}

// MARK: - Server DTO

private struct ServerCardDTO: Decodable {
    let server_id: String
    let card_sku: String
    let name: String
    let set_code: String?
    let set_number: String?
    let language: String
    let market_price: Double?
    let price_source: String?
    let scanned_at: Date

    func toCard() -> Card {
        Card(
            id: UUID(),
            name: name,
            setNumber: set_number ?? "",
            setCode: set_code ?? "unknown",
            language: CardLanguage(rawValue: language) ?? .english,
            marketPrice: market_price,
            priceSource: price_source.flatMap { PriceSource(rawValue: $0) },
            scannedAt: scanned_at
        )
    }
}
