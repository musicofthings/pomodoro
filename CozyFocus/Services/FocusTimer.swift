import Foundation
import UIKit
import UserNotifications

@MainActor
final class FocusTimer: ObservableObject {
    struct Completion: Equatable {
        let id: UUID
        let completedAt: Date
        let duration: TimeInterval
        let companion: Companion
    }

    static let durationOptions: [TimeInterval] = [1, 2, 5, 10, 15, 25, 30, 35, 40, 45, 50, 55, 60].map { TimeInterval($0 * 60) }

    @Published private(set) var remaining: TimeInterval
    @Published private(set) var isRunning: Bool
    @Published private(set) var isComplete: Bool
    @Published var hapticsEnabled = true
    @Published private(set) var durationIndex: Int

    private var endDate: Date?
    private var activeSessionID: UUID?
    private var activeSessionDuration: TimeInterval?
    private var activeSessionCompanion: Companion?
    private var lastPulseMinute = -1
    private var notificationTask: Task<Void, Never>?
    private let defaults: UserDefaults
    private let now: () -> Date
    private let schedulesNotifications: Bool

    private enum Keys {
        static let durationIndex = "timer.durationIndex"
        static let pausedRemaining = "timer.pausedRemaining"
        static let endDate = "timer.endDate"
        static let activeSessionID = "timer.activeSessionID"
        static let activeSessionDuration = "timer.activeSessionDuration"
        static let activeSessionCompanion = "timer.activeSessionCompanion"
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
        activeSessionID = defaults.string(forKey: Keys.activeSessionID).flatMap(UUID.init(uuidString:))
        activeSessionDuration = defaults.object(forKey: Keys.activeSessionDuration) as? TimeInterval
        activeSessionCompanion = defaults.string(forKey: Keys.activeSessionCompanion)
            .flatMap(Companion.init(rawValue:))
        remaining = Self.durationOptions[boundedIndex]
        isRunning = false
        isComplete = false

        if let storedEndDate = defaults.object(forKey: Keys.endDate) as? Date {
            ensureActiveSessionMetadata()
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

    func start(companion: Companion) {
        guard !isRunning else { return }
        guard !(isComplete && activeSessionID != nil) else { return }
        if isComplete { reset() }
        if activeSessionID == nil {
            activeSessionID = UUID()
            activeSessionDuration = sessionDuration
            activeSessionCompanion = companion
        }
        persistActiveSessionMetadata()
        let scheduledEndDate = now().addingTimeInterval(remaining)
        endDate = scheduledEndDate
        isRunning = true
        defaults.set(scheduledEndDate, forKey: Keys.endDate)
        defaults.removeObject(forKey: Keys.pausedRemaining)
        scheduleCompletionNotification(at: scheduledEndDate)
    }

    func pause(onComplete: ((Completion) -> Bool)? = nil) {
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
        guard !(isComplete && activeSessionID != nil) else { return }
        durationIndex = min(max(index, 0), Self.durationOptions.count - 1)
        remaining = sessionDuration
        isComplete = false
        lastPulseMinute = -1
        defaults.set(durationIndex, forKey: Keys.durationIndex)
        defaults.removeObject(forKey: Keys.endDate)
        defaults.removeObject(forKey: Keys.pausedRemaining)
        clearActiveSessionMetadata()
    }

    func reset() {
        guard !(isComplete && activeSessionID != nil) else { return }
        remaining = sessionDuration
        isRunning = false
        isComplete = false
        endDate = nil
        lastPulseMinute = -1
        defaults.set(durationIndex, forKey: Keys.durationIndex)
        defaults.removeObject(forKey: Keys.endDate)
        defaults.removeObject(forKey: Keys.pausedRemaining)
        clearActiveSessionMetadata()
        cancelCompletionNotification()
    }

    func tick(onComplete: ((Completion) -> Bool)? = nil) {
        if isComplete {
            completePendingSession(using: onComplete)
            return
        }
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
            defaults.removeObject(forKey: Keys.pausedRemaining)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            completePendingSession(completedAt: endDate, using: onComplete)
        }
    }

    private func completePendingSession(
        completedAt: Date? = nil,
        using onComplete: ((Completion) -> Bool)?
    ) {
        guard let onComplete,
              let id = activeSessionID,
              let duration = activeSessionDuration,
              let companion = activeSessionCompanion,
              let completedAt = completedAt ?? defaults.object(forKey: Keys.endDate) as? Date else {
            return
        }

        let completion = Completion(
            id: id,
            completedAt: completedAt,
            duration: duration,
            companion: companion
        )
        guard onComplete(completion) else { return }

        defaults.removeObject(forKey: Keys.endDate)
        defaults.removeObject(forKey: Keys.pausedRemaining)
        clearActiveSessionMetadata()
        cancelCompletionNotification()
    }

    private func ensureActiveSessionMetadata() {
        if activeSessionID == nil { activeSessionID = UUID() }
        if activeSessionDuration == nil { activeSessionDuration = sessionDuration }
        if activeSessionCompanion == nil { activeSessionCompanion = .redPanda }
        persistActiveSessionMetadata()
    }

    private func persistActiveSessionMetadata() {
        guard let activeSessionID, let activeSessionDuration, let activeSessionCompanion else { return }
        defaults.set(activeSessionID.uuidString, forKey: Keys.activeSessionID)
        defaults.set(activeSessionDuration, forKey: Keys.activeSessionDuration)
        defaults.set(activeSessionCompanion.rawValue, forKey: Keys.activeSessionCompanion)
    }

    private func clearActiveSessionMetadata() {
        activeSessionID = nil
        activeSessionDuration = nil
        activeSessionCompanion = nil
        defaults.removeObject(forKey: Keys.activeSessionID)
        defaults.removeObject(forKey: Keys.activeSessionDuration)
        defaults.removeObject(forKey: Keys.activeSessionCompanion)
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
