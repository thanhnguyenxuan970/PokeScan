import Foundation

@MainActor
final class GradeROIViewModel: ObservableObject {
    @Published var condition: CardCondition = .nearMint
    @Published var gradingService: GradingService = .psa
    @Published var result: GradeROIResult? = nil
    @Published var isLoading = false
    @Published var errorMessage: String? = nil

    private let service: GradeROIServiceProtocol

    init(service: GradeROIServiceProtocol = LiveGradeROIService()) {
        self.service = service
    }

    func calculate(card: Card) async {
        guard let rawPrice = card.marketPrice else { return }
        isLoading = true
        errorMessage = nil
        result = nil
        do {
            result = try await service.calculate(
                cardSKU: card.cardSKU,
                rawPrice: rawPrice,
                condition: condition,
                service: gradingService
            )
        } catch GradeROIError.proRequired {
            errorMessage = "Pro subscription required"
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
