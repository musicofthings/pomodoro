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
        timer.start(companion: .redPanda)

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
        restored.tick { _ in completionCount += 1; return true }

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
        timer.start(companion: .redPanda)
        currentDate.addTimeInterval(61)

        var completionCount = 0
        timer.pause { _ in completionCount += 1; return true }

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

        XCTAssertTrue(profile.purchase(.sunHat, context: context))
        XCTAssertFalse(profile.purchase(.flowerCrown, context: context))
        let inventory = try context.fetch(FetchDescriptor<InventoryEntry>())
        let ledger = try context.fetch(FetchDescriptor<CoinLedgerEntry>())

        XCTAssertEqual(inventory.map(\.cosmeticRaw), [Cosmetic.sunHat.rawValue])
        XCTAssertEqual(ledger.reduce(0) { $0 + $1.amount }, 0)
    }

    @MainActor
    func testExpiredTimerRelaunchState() throws {
        let suiteName = "FocusTimerExpiredTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        var currentDate = Date(timeIntervalSinceReferenceDate: 3_000_000)
        let timer = FocusTimer(
            defaults: defaults,
            now: { currentDate },
            schedulesNotifications: false
        )
        timer.chooseDuration(at: 0)
        timer.start(companion: .capybara)

        // Fast forward 120 seconds (past 60s duration)
        currentDate.addTimeInterval(120)

        let restored = FocusTimer(
            defaults: defaults,
            now: { currentDate },
            schedulesNotifications: false
        )

        // Verify that expired timer initializes directly in complete, non-running state
        XCTAssertFalse(restored.isRunning)
        XCTAssertTrue(restored.isComplete)
        XCTAssertEqual(restored.remaining, 0)

        var completions: [FocusTimer.Completion] = []
        restored.tick { completion in
            completions.append(completion)
            return true
        }
        restored.tick { completion in
            completions.append(completion)
            return true
        }

        XCTAssertEqual(completions.count, 1)
        XCTAssertEqual(completions.first?.duration, 60)
        XCTAssertEqual(completions.first?.companion, .capybara)
        XCTAssertNil(defaults.object(forKey: "timer.activeSessionID"))
    }

    @MainActor
    func testFailedCompletionCanRetryAfterRelaunch() throws {
        let suiteName = "FocusTimerRetryTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        var currentDate = Date(timeIntervalSinceReferenceDate: 4_000_000)
        let timer = FocusTimer(
            defaults: defaults,
            now: { currentDate },
            schedulesNotifications: false
        )
        timer.chooseDuration(at: 0)
        timer.start(companion: .rabbit)
        currentDate.addTimeInterval(61)

        var attempts = 0
        timer.tick { _ in attempts += 1; return false }
        timer.start(companion: .horse)
        XCTAssertTrue(timer.isComplete)
        XCTAssertNotNil(defaults.object(forKey: "timer.endDate"))
        timer.tick { _ in attempts += 1; return true }
        timer.tick { _ in attempts += 1; return true }

        XCTAssertEqual(attempts, 2)
        XCTAssertNil(defaults.object(forKey: "timer.endDate"))
    }
}
