import Foundation

final class SetResolver {
    static let shared = SetResolver()

    private let entries: [SetEntry]

    private init() {
        guard let url = Bundle.main.url(forResource: "set_database", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode([SetEntry].self, from: data)
        else { entries = []; return }
        entries = decoded
    }

    /// Resolves setCode from "NNN/TTT" format and card language.
    /// Returns "unknown" if resolution fails.
    func resolve(setNumber: String, language: CardLanguage) -> String {
        guard let total = parseTotal(from: setNumber),
              let cardNum = parseCardNumber(from: setNumber)
        else { return "unknown" }

        let langStr = language.rawValue
        let candidates = entries.filter { $0.total == total && $0.language == langStr }

        guard !candidates.isEmpty else { return "unknown" }
        if candidates.count == 1 { return candidates[0].setCode }

        // Newest set wins. Ties broken by setCode ascending for determinism.
        // Phase 3 replaces this heuristic with pokemontcg.io lookup.
        let valid = candidates
            .filter { cardNum <= $0.total }
            .sorted {
                if $0.releaseYear != $1.releaseYear { return $0.releaseYear > $1.releaseYear }
                return $0.setCode < $1.setCode
            }
        return valid.first?.setCode ?? "unknown"
    }

    private func parseTotal(from setNumber: String) -> Int? {
        let parts = setNumber.split(separator: "/")
        guard parts.count == 2, let total = Int(parts[1]) else { return nil }
        return total
    }

    private func parseCardNumber(from setNumber: String) -> Int? {
        let parts = setNumber.split(separator: "/")
        guard parts.count == 2, let num = Int(parts[0]) else { return nil }
        return num
    }
}
