import Foundation

final class SetResolver {
    private let entries: [SetEntry]

    init(entries: [SetEntry]) {
        self.entries = entries
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

        // When printedTotal is available, use it to disambiguate sets with same total.
        // Example: base1 (printedTotal=102) vs ex5/Hidden Legends (printedTotal=101).
        let withPrinted = candidates.filter { $0.printedTotal != nil }
        if withPrinted.count == candidates.count {
            let byPrinted = withPrinted.filter { $0.printedTotal == total }
            if byPrinted.count == 1 { return byPrinted[0].setCode }
        }

        // Fallback: newest set wins. Ties broken by setCode ascending for determinism.
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
