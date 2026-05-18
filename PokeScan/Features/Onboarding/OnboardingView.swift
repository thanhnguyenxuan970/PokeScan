import SwiftUI

struct OnboardingView: View {
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding = false

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 20)

            VStack(spacing: 16) {
                Image(systemName: "camera.viewfinder")
                    .font(.system(size: 72))
                    .foregroundStyle(.yellow)

                Text("SnapDex")
                    .font(.largeTitle.bold())

                Text("Real-time prices for every card you own")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            Spacer(minLength: 20)

            VStack(alignment: .leading, spacing: 24) {
                FeatureRow(
                    icon: "viewfinder",
                    title: "Scan any Pokémon card instantly",
                    subtitle: "On-device recognition, no internet needed"
                )
                FeatureRow(
                    icon: "chart.line.uptrend.xyaxis",
                    title: "Live market prices",
                    subtitle: "TCGPlayer + eBay 30-day completed sales"
                )
                FeatureRow(
                    icon: "square.stack",
                    title: "Track your collection",
                    subtitle: "Synced across devices, never lost"
                )
            }
            .padding(.horizontal, 32)

            Spacer(minLength: 20)

            VStack(spacing: 16) {
                Button("Start Scanning") {
                    hasSeenOnboarding = true
                }
                .buttonStyle(.borderedProminent)
                .tint(.yellow)
                .foregroundStyle(.black)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 32)

                Link("Privacy Policy", destination: AppConfig.privacyPolicyURL)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.bottom, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }
}

private struct FeatureRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(.yellow)
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.bold())
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}
