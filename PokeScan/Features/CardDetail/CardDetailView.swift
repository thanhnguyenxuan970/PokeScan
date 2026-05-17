import SwiftUI

struct CardDetailView: View {
    let card: Card
    @ObservedObject private var store = StoreKitService.shared
    @State private var showChecklist = false

    var body: some View {
        NavigationStack {
            List {
                Section("Card") {
                    LabeledContent("Name", value: card.name)
                    LabeledContent("Set Number", value: card.setNumber)
                    LabeledContent("SKU", value: card.cardSKU)
                    LabeledContent("Language", value: card.language.rawValue.capitalized)
                    if store.isPro {
                        verifyCardSection
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
        }
    }

    @ViewBuilder
    private var verifyCardSection: some View {
        DisclosureGroup("Verify Card", isExpanded: $showChecklist) {
            ForEach(ChecklistItem.standard) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title).font(.subheadline).fontWeight(.medium)
                    Text(item.description).font(.caption).foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }
        }
    }
}
