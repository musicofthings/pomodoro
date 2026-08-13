import Foundation
import UIKit
import UserNotifications

@MainActor
final class FocusTimer: ObservableObject {
    static let durationOptions: [TimeInterval] = [1, 2, 5, 10, 15, 25, 30, 35, 40, 45, 50, 55, 60].map { TimeInterval($0 * 60) }

    @Published private(set) var remaining: TimeInterval
    @Published private(set) var isRunning: Bool
    @Published private(set) var isComplete: Bool
    @Published var hapticsEnabled = true
    @Published private(set) var durationIndex: Int

    private var endDate: Date?
    private var lastPulseMinute = -1
    private var notificationTask: Task<Void, Never>?
    private let defaults: UserDefaults
    private let now: () -> Date
    private let schedulesNotifications: Bool

    private enum Keys {
        static let durationIndex = "timer.durationIndex"
        static let pausedRemaining = "timer.pausedRemaining"
        static let endDate = "timer.endDate"
    }

    private static let completionNotificationID = "cozy-focus.session-complete"

    init(
        defaults: UserDefaults = .standard,
        now: @escaping () -> Date = Date.init,
        schedulesNotifications: Bool = true
    ) {
        self.defaults = defaults
        self.now = now
        self.schedulesNotifications = schedulesNotifications

        let storedIndex = defaults.object(forKey: Keys.durationIndex) as? Int ?? 5
        let boundedIndex = min(max(storedIndex, 0), Self.durationOptions.count - 1)
        durationIndex = boundedIndex

        if let storedEndDate = defaults.object(forKey: Keys.endDate) as? Date {
            let secondsRemaining = storedEndDate.timeIntervalSince(now())
            if secondsRemaining > 0 {
                endDate = storedEndDate
                remaining = secondsRemaining
                isRunning = true
                isComplete = false
            } else {
                endDate = nil
                remaining = 0
                isRunning = false
                isComplete = true
            }
        } else {
            remaining = defaults.object(forKey: Keys.pausedRemaining) as? TimeInterval
                ?? Self.durationOptions[boundedIndex]
            isRunning = false
            isComplete = false
        }
    }

    var sessionDuration: TimeInterval { Self.durationOptions[durationIndex] }
    var scheduledEndDate: Date? { isRunning ? endDate : nil }
    var durationText: String {
        let minutes = Int(sessionDuration / 60)
        return "\(minutes) minute\(minutes == 1 ? "" : "s")"
    }
    var durationAdjective: String { "\(Int(sessionDuration / 60))-minute" }

    var timeText: String {
        let value = max(0, Int(remaining.rounded(.up)))
        return String(format: "%02d:%02d", value / 60, value % 60)
    }

    func start() {
        guard !isRunning else { return }
        if isComplete { reset() }
        let scheduledEndDate = now().addingTimeInterval(remaining)
        endDate = scheduledEndDate
        isRunning = true
        defaults.set(scheduledEndDate, forKey: Keys.endDate)
        defaults.removeObject(forKey: Keys.pausedRemaining)
        scheduleCompletionNotification(at: scheduledEndDate)
    }

    func pause(onComplete: (() -> Void)? = nil) {
        tick(onComplete: onComplete)
        guard isRunning else { return }
        isRunning = false
        endDate = nil
        defaults.removeObject(forKey: Keys.endDate)
        defaults.set(remaining, forKey: Keys.pausedRemaining)
        cancelCompletionNotification()
    }

    func chooseDuration(at index: Int) {
        guard !isRunning else { return }
        durationIndex = min(max(index, 0), Self.durationOptions.count - 1)
        remaining = sessionDuration
        isComplete = false
        lastPulseMinute = -1
        defaults.set(durationIndex, forKey: Keys.durationIndex)
        defaults.removeObject(forKey: Keys.pausedRemaining)
    }

    func reset() {
        remaining = sessionDuration
        isRunning = false
        isComplete = false
        endDate = nil
        lastPulseMinute = -1
        defaults.set(durationIndex, forKey: Keys.durationIndex)
        defaults.removeObject(forKey: Keys.endDate)
        defaults.removeObject(forKey: Keys.pausedRemaining)
        cancelCompletionNotification()
    }

    func tick(onComplete: (() -> Void)? = nil) {
        guard isRunning, let endDate else { return }
        remaining = max(0, endDate.timeIntervalSince(now()))
        let elapsedMinutes = Int((sessionDuration - remaining) / 60)
        if hapticsEnabled, elapsedMinutes > 0, elapsedMinutes.isMultiple(of: 5), elapsedMinutes != lastPulseMinute {
            UIImpactFeedbackGenerator(style: .soft).impactOccurred(intensity: 0.35)
            lastPulseMinute = elapsedMinutes
        }
        if remaining <= 0 {
            isRunning = false
            isComplete = true
            self.endDate = nil
            defaults.removeObject(forKey: Keys.endDate)
            defaults.removeObject(forKey: Keys.pausedRemaining)
            cancelCompletionNotification()
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            onComplete?()
        }
    }

    private func scheduleCompletionNotification(at date: Date) {
        guard schedulesNotifications else { return }
        notificationTask?.cancel()
        notificationTask = Task {
            let center = UNUserNotificationCenter.current()
            let settings = await center.notificationSettings()
            var isAuthorized = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional

            if settings.authorizationStatus == .notDetermined {
                isAuthorized = (try? await center.requestAuthorization(options: [.alert, .sound])) == true
            }

            guard !Task.isCancelled, isAuthorized, isRunning, endDate == date else { return }
            let content = UNMutableNotificationContent()
            content.title = "Your cozy focus is complete"
            content.body = "Take a gentle breath and come back when you're ready."
            content.sound = .default
            let trigger = UNTimeIntervalNotificationTrigger(
                timeInterval: max(1, date.timeIntervalSince(now())),
                repeats: false
            )
            let request = UNNotificationRequest(
                identifier: Self.completionNotificationID,
                content: content,
                trigger: trigger
            )
            try? await center.add(request)
        }
    }

    private func cancelCompletionNotification() {
        guard schedulesNotifications else { return }
        notificationTask?.cancel()
        notificationTask = nil
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: [Self.completionNotificationID]
        )
    }
}
