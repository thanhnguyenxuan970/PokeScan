import Foundation
import StoreKit

@MainActor
final class StoreKitService: ObservableObject {
    static let shared = StoreKitService()

    @Published private(set) var isPro: Bool = false
    @Published private(set) var proMonthly: Product? = nil
    @Published private(set) var proAnnual: Product? = nil

    static let proMonthlyID = "com.yourname.pokescan.pro.monthly"
    static let proAnnualID  = "com.yourname.pokescan.pro.annual"

    private init() {
        Task {
            await loadProducts()
            await restoreEntitlements()
            listenForTransactionUpdates()
        }
    }

    func loadProducts() async {
        do {
            let products = try await Product.products(for: [Self.proMonthlyID, Self.proAnnualID])
            for product in products {
                switch product.id {
                case Self.proMonthlyID: proMonthly = product
                case Self.proAnnualID:  proAnnual = product
                default: break
                }
            }
        } catch {
            // Products unavailable (no App Store Connect config yet) — isPro stays false.
        }
    }

    func purchase(_ product: Product) async throws {
        let result = try await product.purchase()
        switch result {
        case .success(let verification):
            let transaction = try verification.payloadValue
            await transaction.finish()
            isPro = true
            await syncProStatusToBackend(transaction: transaction)
        case .userCancelled:
            break
        case .pending:
            break
        @unknown default:
            break
        }
    }

    func restorePurchases() async {
        do {
            try await AppStore.sync()
            await restoreEntitlements()
        } catch {}
    }

    // MARK: - Private

    private func restoreEntitlements() async {
        for await result in Transaction.currentEntitlements {
            if case .verified(let tx) = result, tx.revocationDate == nil {
                isPro = true
            }
        }
    }

    private func listenForTransactionUpdates() {
        Task {
            for await result in Transaction.updates {
                if case .verified(let tx) = result {
                    if tx.revocationDate == nil {
                        isPro = true
                    } else {
                        isPro = false
                    }
                    await tx.finish()
                }
            }
        }
    }

    private func syncProStatusToBackend(transaction: Transaction) async {
        guard let token = AuthService.shared.readTokenFromKeychain() else { return }
        var request = URLRequest(
            url: AppConfig.backendBaseURL.appendingPathComponent("auth/verify-receipt")
        )
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "product_id": transaction.productID,
            "transaction_id": String(transaction.id),
        ])
        _ = try? await URLSession.shared.data(for: request)
    }
}
