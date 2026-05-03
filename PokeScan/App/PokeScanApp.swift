import SwiftData
import SwiftUI

@main
struct PokeScanApp: App {
    @StateObject private var auth = AuthService.shared

    init() {
        Task {
            await SetDatabaseService.shared.refreshIfNeeded()
            AuthService.shared.restoreSession()
        }
    }

    var body: some Scene {
        WindowGroup {
            ScannerView()
                .environmentObject(auth)
        }
        .modelContainer(for: CardRecord.self)
    }
}
