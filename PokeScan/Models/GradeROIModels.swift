import Foundation

enum CardCondition: String, CaseIterable, Codable {
    case nearMint = "near_mint"
    case lightlyPlayed = "lightly_played"
    case moderatelyPlayed = "moderately_played"
    case heavilyPlayed = "heavily_played"

    var shortLabel: String {
        switch self {
        case .nearMint: return "NM"
        case .lightlyPlayed: return "LP"
        case .moderatelyPlayed: return "MP"
        case .heavilyPlayed: return "HP"
        }
    }

    var label: String {
        switch self {
        case .nearMint: return "Near Mint"
        case .lightlyPlayed: return "Lightly Played"
        case .moderatelyPlayed: return "Moderately Played"
        case .heavilyPlayed: return "Heavily Played"
        }
    }
}

enum GradingService: String, CaseIterable, Codable {
    case psa, bgs, cgc

    var label: String { rawValue.uppercased() }

    var fee: Double {
        switch self {
        case .psa: return 25.0
        case .bgs: return 20.0
        case .cgc: return 18.0
        }
    }
}

struct GradeROIResult: Codable {
    let expectedGrade: Int
    let gradedMarketValue: Double
    let gradingFee: Double
    let netROI: Double
    let roiPct: Double
    let breakEvenGrade: Int?
    let confidence: String

    enum CodingKeys: String, CodingKey {
        case expectedGrade = "expected_grade"
        case gradedMarketValue = "graded_market_value"
        case gradingFee = "grading_fee"
        case netROI = "net_roi"
        case roiPct = "roi_pct"
        case breakEvenGrade = "break_even_grade"
        case confidence
    }
}
