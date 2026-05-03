import StoreKit
import SwiftUI

struct PurchaseButton: View {
    let product: Product
    @State private var isLoading = false

    var body: some View {
        Button {
            guard !isLoading else { return }
            isLoading = true
            Task {
                defer { isLoading = false }
                try? await StoreKitService.shared.purchase(product)
            }
        } label: {
            Group {
                if isLoading {
                    ProgressView()
                        .tint(.black)
                } else {
                    Text(product.displayPrice + "/mo")
                        .font(.system(size: 17, weight: .semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
        }
        .buttonStyle(.borderedProminent)
        .tint(.yellow)
        .foregroundStyle(.black)
        .disabled(isLoading)
    }
}
