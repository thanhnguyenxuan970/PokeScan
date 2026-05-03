import Vision
import Foundation

final class CardIdentificationService {
    private static let setNumberRegex = try! NSRegularExpression(pattern: #"\b\d{1,3}/\d{1,3}\b"#)
    private static let numericOnlyRegex = try! NSRegularExpression(pattern: #"^\d+(/\d+)?$"#)

    private let setResolver: SetResolver

    init(setResolver: SetResolver) {
        self.setResolver = setResolver
    }

    func identify(from observations: [VNRecognizedTextObservation]) -> Card? {
        // Vision Y-axis: 0 = bottom, 1 = top — sort descending to get top of card first
        let sorted = observations.sorted { $0.boundingBox.midY > $1.boundingBox.midY }
        let strings = sorted.compactMap { $0.topCandidates(1).first?.string }

        guard let setNumber = extractSetNumber(from: strings) else { return nil }

        let name = extractName(from: strings, excluding: setNumber)
        let language = detectLanguage(from: strings)

        return Card(
            id: UUID(),
            name: name,
            setNumber: setNumber,
            setCode: setResolver.resolve(setNumber: setNumber, language: language),
            language: language,
            marketPrice: nil,
            priceSource: nil,
            scannedAt: Date()
        )
    }

    private func extractSetNumber(from strings: [String]) -> String? {
        for string in strings {
            let range = NSRange(string.startIndex..., in: string)
            if let match = Self.setNumberRegex.firstMatch(in: string, range: range),
               let swiftRange = Range(match.range, in: string) {
                return String(string[swiftRange])
            }
        }
        return nil
    }

    private func extractName(from strings: [String], excluding setNumber: String) -> String {
        for string in strings {
            let trimmed = string.trimmingCharacters(in: .whitespaces)
            guard !trimmed.isEmpty,
                  !trimmed.contains(setNumber),
                  !isNumericOrCode(trimmed),
                  trimmed.count > 2 else { continue }
            return trimmed
        }
        return "Unknown Card"
    }

    private func isNumericOrCode(_ string: String) -> Bool {
        let range = NSRange(string.startIndex..., in: string)
        return Self.numericOnlyRegex.firstMatch(in: string, range: range) != nil
    }

    private func detectLanguage(from strings: [String]) -> CardLanguage {
        for string in strings {
            for scalar in string.unicodeScalars {
                // Covers CJK Symbols, Hiragana, Katakana, CJK Unified Ideographs
                if scalar.value >= 0x3000 && scalar.value <= 0x9FFF {
                    return .japanese
                }
            }
        }
        return .english
    }
}
