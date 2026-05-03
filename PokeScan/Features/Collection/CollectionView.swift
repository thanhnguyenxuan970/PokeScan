import SwiftData
import SwiftUI

struct CollectionView: View {
    @EnvironmentObject var auth: AuthService

    var body: some View {
        Group {
            if auth.isSignedIn {
                SignedInCollectionView()
            } else {
                SignInView()
            }
        }
        .navigationTitle("Collection")
    }
}

private struct SignedInCollectionView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \CardRecord.scannedAt, order: .reverse) var cards: [CardRecord]

    var body: some View {
        List {
            ForEach(cards) { record in
                CardRowView(record: record)
            }
            .onDelete(perform: deleteCards)
        }
        .overlay {
            if cards.isEmpty {
                ContentUnavailableView(
                    "No cards yet",
                    systemImage: "camera.viewfinder",
                    description: Text("Scan a card to add it here.")
                )
            }
        }
        .task {
            await CollectionSyncService.shared.fullSync(context: context)
        }
        .toolbar {
            EditButton()
        }
    }

    private func deleteCards(at offsets: IndexSet) {
        for index in offsets {
            context.delete(cards[index])
        }
        try? context.save()
    }
}

private struct CardRowView: View {
    let record: CardRecord

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(record.name).font(.headline)
                Text("\(record.setCode) · \(record.setNumber)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if let price = record.marketPrice {
                Text(price, format: .currency(code: "USD"))
                    .font(.subheadline)
                    .foregroundStyle(.green)
            }
        }
    }
}
