import SwiftUI

struct CardDetailView: View {
    let card: Card

    var body: some View {
        NavigationStack {
            List {
                Section("Card") {
                    LabeledContent("Name", value: card.name)
                    LabeledContent("Set Number", value: card.setNumber)
                    LabeledContent("SKU", value: card.cardSKU)
                    LabeledContent("Language", value: card.language.rawValue.capitalized)
                }
                Section("Price") {
                    if let price = card.marketPrice {
                        LabeledContent("Market Price", value: price, format: .currency(code: "USD"))
                        LabeledContent("Source", value: card.priceSource?.rawValue ?? "—")
                    } else {
                        Text("Price unavailable").foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle(card.name)
        }
    }
}
