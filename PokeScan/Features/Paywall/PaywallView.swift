import SwiftUI

struct PaywallView: View {
    let onDismiss: () -> Void

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
                Button("Upgrade to Pro — $4.99/mo") {
                    // Phase 3: StoreKit purchase
                }
                .buttonStyle(.borderedProminent)
                .tint(.yellow)
                .foregroundStyle(.black)
                .disabled(true)

                Button("Maybe Later") { onDismiss() }
                    .foregroundStyle(.secondary)
            }
        }
        .padding(32)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
}
