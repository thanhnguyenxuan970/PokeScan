import Foundation

final class ScanCounterService {
    static let shared = ScanCounterService()

    static let freeMonthlyLimit = 20

    private let defaults: UserDefaults
    private let countKey = "snapdex.scanCount"
    private let resetDateKey = "snapdex.scanCountResetDate"

    private(set) var scansThisMonth: Int = 0

    private init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        resetIfNewMonth()
        scansThisMonth = defaults.integer(forKey: countKey)
    }

    /// Returns true if user can start a new scan. Pro users always can.
    @MainActor
    func canScan() -> Bool {
        if StoreKitService.shared.isPro { return true }
        resetIfNewMonth()
        return scansThisMonth < Self.freeMonthlyLimit
    }

    /// Call after scanState reaches .result (successful scan only).
    @MainActor
    func recordScan() {
        if StoreKitService.shared.isPro { return }
        scansThisMonth += 1
        defaults.set(scansThisMonth, forKey: countKey)
    }

    private func resetIfNewMonth() {
        let now = Date()
        let stored = defaults.object(forKey: resetDateKey) as? Date ?? .distantPast
        guard !Calendar.current.isDate(stored, equalTo: now, toGranularity: .month) else { return }
        scansThisMonth = 0
        defaults.set(0, forKey: countKey)
        defaults.set(now, forKey: resetDateKey)
    }
}
