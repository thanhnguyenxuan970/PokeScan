import SwiftUI

// Phase 1: populated after OCR identifies card
struct CardDetailView: View {
    let card: Card

    var body: some View {
        Text("Card Detail — Phase 1")
            .navigationTitle(card.name)
    }
}
