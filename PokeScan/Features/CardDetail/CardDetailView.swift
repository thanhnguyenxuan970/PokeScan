import SwiftUI

struct CardDetailView: View {
    let card: Card
    @StateObject private var fakeDetection = LiveFakeDetectionService()
    @ObservedObject private var store = StoreKitService.shared
    @State private var showAuthenticityDetail = false

    var body: some View {
        NavigationStack {
            List {
                Section("Card") {
                    LabeledContent("Name", value: card.name)
                    LabeledContent("Set Number", value: card.setNumber)
                    LabeledContent("SKU", value: card.cardSKU)
                    LabeledContent("Language", value: card.language.rawValue.capitalized)
                    if store.isPro {
                        authenticityBadge
                    }
                }
                Section("Price") {
                    if let price = card.marketPrice {
                        LabeledContent("Market Price", value: price, format: .currency(code: "USD"))
                        LabeledContent("Source", value: card.priceSource?.rawValue ?? "—")
                    } else {
                        Text("Price unavailable").foregroundStyle(.secondary)
                    }
                }
                if card.marketPrice != nil {
                    Section {
                        NavigationLink("Grade ROI") {
                            GradeROIView(card: card)
                        }
                    }
                }
            }
            .navigationTitle(card.name)
            .task {
                if store.isPro {
                    await fakeDetection.checkAndStore(card: card)
                }
            }
            .sheet(isPresented: $showAuthenticityDetail) {
                if let result = fakeDetection.result {
                    AuthenticityDetailSheet(result: result)
                }
            }
        }
    }

    @ViewBuilder
    private var authenticityBadge: some View {
        if fakeDetection.isLoading {
            HStack(spacing: 6) {
                ProgressView().scaleEffect(0.7)
                Text("Checking authenticity…").font(.caption).foregroundStyle(.secondary)
            }
        } else if let result = fakeDetection.result {
            Button { showAuthenticityDetail = true } label: {
                HStack(spacing: 6) {
                    Image(systemName: result.riskLevel.icon)
                        .foregroundStyle(result.riskLevel.color)
                    Text(result.riskLevel.label)
                        .font(.caption)
                        .foregroundStyle(result.riskLevel.color)
                }
            }
            .buttonStyle(.plain)
        }
    }
}

private struct AuthenticityDetailSheet: View {
    let result: AuthenticityResult

    var body: some View {
        NavigationStack {
            List {
                Section("Risk Assessment") {
                    LabeledContent("Risk Level", value: result.riskLevel.label)
                    LabeledContent("Risk Score") {
                        Text(result.riskScore, format: .number.precision(.fractionLength(2)))
                    }
                }
                if !result.flags.isEmpty {
                    Section("Flags") {
                        ForEach(result.flags, id: \.self) { flag in
                            Text(flag.replacingOccurrences(of: "_", with: " ").capitalized)
                                .font(.caption)
                        }
                    }
                }
                Section("Recommendation") {
                    Text(result.recommendation)
                }
            }
            .navigationTitle("Authenticity Check")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium])
    }
}
