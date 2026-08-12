import Foundation
import UIKit

@MainActor
final class FocusTimer: ObservableObject {
    static let durationOptions: [TimeInterval] = [1, 2, 5, 10, 15, 25, 30, 35, 40, 45, 50, 55, 60].map { TimeInterval($0 * 60) }

    @Published private(set) var remaining: TimeInterval = 25 * 60
    @Published private(set) var isRunning = false
    @Published private(set) var isComplete = false
    @Published var hapticsEnabled = true
    @Published private(set) var durationIndex = 5

    private var endDate: Date?
    private var lastPulseMinute = -1

    var sessionDuration: TimeInterval { Self.durationOptions[durationIndex] }
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
        endDate = Date().addingTimeInterval(remaining)
        isRunning = true
    }

    func pause() {
        tick()
        isRunning = false
        endDate = nil
    }

    func chooseDuration(at index: Int) {
        guard !isRunning else { return }
        durationIndex = min(max(index, 0), Self.durationOptions.count - 1)
        remaining = sessionDuration
        isComplete = false
        lastPulseMinute = -1
    }

    func reset() {
        remaining = sessionDuration
        isRunning = false
        isComplete = false
        endDate = nil
        lastPulseMinute = -1
    }

    func tick(onComplete: (() -> Void)? = nil) {
        guard isRunning, let endDate else { return }
        remaining = max(0, endDate.timeIntervalSinceNow)
        let elapsedMinutes = Int((sessionDuration - remaining) / 60)
        if hapticsEnabled, elapsedMinutes > 0, elapsedMinutes.isMultiple(of: 5), elapsedMinutes != lastPulseMinute {
            UIImpactFeedbackGenerator(style: .soft).impactOccurred(intensity: 0.35)
            lastPulseMinute = elapsedMinutes
        }
        if remaining <= 0 {
            isRunning = false
            isComplete = true
            self.endDate = nil
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            onComplete?()
        }
    }
}
