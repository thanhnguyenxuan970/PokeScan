import Foundation
import Combine

@MainActor
final class SetDatabaseService: ObservableObject {
    static let shared = SetDatabaseService()

    @Published private(set) var sets: [SetEntry] = []

    private let apiURL = URL(string: "https://api.pokemontcg.io/v2/sets")!
    private let refreshInterval: TimeInterval = 86400  // 24h
    private let lastRefreshKey = "snapdex.setDBLastRefresh"

    private var cacheURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("set_database_cache.json")
    }

    private init() {
        sets = loadFromCacheOrBundle()
    }

    func refreshIfNeeded() async {
        if let lastRefresh = UserDefaults.standard.object(forKey: lastRefreshKey) as? Date,
           Date().timeIntervalSince(lastRefresh) < refreshInterval {
            return
        }
        await fetchFromAPI()
    }

    private func loadFromCacheOrBundle() -> [SetEntry] {
        if let cached = loadFromCache() { return cached }
        return loadFromBundle()
    }

    private func loadFromCache() -> [SetEntry]? {
        guard FileManager.default.fileExists(atPath: cacheURL.path),
              let data = try? Data(contentsOf: cacheURL),
              let entries = try? JSONDecoder().decode([SetEntry].self, from: data)
        else { return nil }
        return entries
    }

    private func loadFromBundle() -> [SetEntry] {
        guard let url = Bundle.main.url(forResource: "set_database", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let entries = try? JSONDecoder().decode([SetEntry].self, from: data)
        else { return [] }
        return entries
    }

    private func fetchFromAPI() async {
        do {
            let (data, _) = try await URLSession.shared.data(from: apiURL)
            let response = try JSONDecoder().decode(PokemonTCGSetsResponse.self, from: data)
            let entries = response.data.compactMap { SetEntry(from: $0) }
            guard !entries.isEmpty else { return }
            sets = entries
            try? data.write(to: cacheURL)
            UserDefaults.standard.set(Date(), forKey: lastRefreshKey)
        } catch {
            // Network failure is non-fatal — bundle/cache fallback already loaded.
        }
    }
}

// MARK: - pokemontcg.io API response types

private struct PokemonTCGSetsResponse: Decodable {
    let data: [PokemonTCGSet]
}

private struct PokemonTCGSet: Decodable {
    let id: String
    let name: String
    let total: Int
    let printedTotal: Int?
    let releaseDate: String  // "YYYY/MM/DD"
    let series: String
}

private extension SetEntry {
    init?(from apiSet: PokemonTCGSet) {
        let yearString = apiSet.releaseDate.prefix(4)
        guard let year = Int(yearString) else { return nil }
        let language = apiSet.id.contains("-jp") ? "japanese" : "english"
        self.init(
            setCode: apiSet.id,
            name: apiSet.name,
            total: apiSet.total,
            printedTotal: apiSet.printedTotal,
            releaseYear: year,
            series: apiSet.series,
            language: language
        )
    }
}
