import SwiftUI

struct PaywallView: View {
    let onDismiss: () -> Void
    @StateObject private var store = StoreKitService.shared

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundStyle(.yellow)

            Text("You've used your 20 free scans")
                .font(.title2.bold())
                .multilineTextAlignment(.center)

            Text("Upgrade to Pro for unlimited scans, multi-market prices, and Grade ROI.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            VStack(spacing: 12) {
                if let monthly = store.proMonthly {
                    PurchaseButton(product: monthly)
                } else {
                    Button("Upgrade to Pro — $4.99/mo") {}
                        .buttonStyle(.borderedProminent)
                        .tint(.yellow)
                        .foregroundStyle(.black)
                        .disabled(true)
                }

                if let annual = store.proAnnual {
                    Button("$39/yr — save 35%") {
                        Task { try? await store.purchase(annual) }
                    }
                    .foregroundStyle(.secondary)
                }

                Button("Restore Purchases") {
                    Task { await store.restorePurchases() }
                }
                .font(.footnote)
                .foregroundStyle(.tertiary)

                Button("Maybe Later") { onDismiss() }
                    .foregroundStyle(.secondary)
            }
        }
        .padding(32)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .onChange(of: store.isPro) { _, isPro in
            if isPro { onDismiss() }
        }
    }
}
