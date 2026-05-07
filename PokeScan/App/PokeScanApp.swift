import SwiftData
import SwiftUI

@main
struct PokeScanApp: App {
    @StateObject private var auth = AuthService.shared
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding = false

    init() {
        Task {
            await SetDatabaseService.shared.refreshIfNeeded()
            AuthService.shared.restoreSession()
        }
    }

    var body: some Scene {
        WindowGroup {
            if hasSeenOnboarding {
                ScannerView()
                    .environmentObject(auth)
            } else {
                OnboardingView()
                    .environmentObject(auth)
            }
        }
        .modelContainer(for: CardRecord.self)
    }
}
