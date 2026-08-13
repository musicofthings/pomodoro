import XCTest
import SwiftData
@testable import CozyFocus

final class FocusPersistenceTests: XCTestCase {
    @MainActor
    func testRunningTimerRestoresDeadlineAndCompletes() throws {
        let suiteName = "FocusTimerTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        var currentDate = Date(timeIntervalSinceReferenceDate: 1_000_000)
        let timer = FocusTimer(
            defaults: defaults,
            now: { currentDate },
            schedulesNotifications: false
        )
        timer.chooseDuration(at: 0)
        timer.start()

        currentDate.addTimeInterval(30)
        let restored = FocusTimer(
            defaults: defaults,
            now: { currentDate },
            schedulesNotifications: false
        )
        XCTAssertTrue(restored.isRunning)
        XCTAssertEqual(restored.remaining, 30, accuracy: 0.001)

        currentDate.addTimeInterval(31)
        var completionCount = 0
        restored.tick { completionCount += 1 }

        XCTAssertEqual(completionCount, 1)
        XCTAssertTrue(restored.isComplete)
        XCTAssertFalse(restored.isRunning)
        XCTAssertNil(defaults.object(forKey: "timer.endDate"))
    }

    @MainActor
    func testPausingAtDeadlineStillCompletesSession() throws {
        let suiteName = "FocusTimerPauseTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        var currentDate = Date(timeIntervalSinceReferenceDate: 2_000_000)
        let timer = FocusTimer(
            defaults: defaults,
            now: { currentDate },
            schedulesNotifications: false
        )
        timer.chooseDuration(at: 0)
        timer.start()
        currentDate.addTimeInterval(61)

        var completionCount = 0
        timer.pause { completionCount += 1 }

        XCTAssertEqual(completionCount, 1)
        XCTAssertTrue(timer.isComplete)
        XCTAssertNil(defaults.object(forKey: "timer.pausedRemaining"))
    }

    @MainActor
    func testPurchaseWritesInventoryAndDebitTogether() throws {
        let configuration = ModelConfiguration(isStoredInMemoryOnly: true)
        let container = try ModelContainer(
            for: InventoryEntry.self,
            CoinLedgerEntry.self,
            configurations: configuration
        )
        let context = container.mainContext
        let suiteName = "ProfileStoreTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let profile = ProfileStore(defaults: defaults)

        context.insert(CoinLedgerEntry(amount: 20, reason: "Test credit"))
        try context.save()

        XCTAssertTrue(profile.purchase(.sunHat, balance: 20, context: context))
        let inventory = try context.fetch(FetchDescriptor<InventoryEntry>())
        let ledger = try context.fetch(FetchDescriptor<CoinLedgerEntry>())

        XCTAssertEqual(inventory.map(\.cosmeticRaw), [Cosmetic.sunHat.rawValue])
        XCTAssertEqual(ledger.reduce(0) { $0 + $1.amount }, 0)
    }
}
