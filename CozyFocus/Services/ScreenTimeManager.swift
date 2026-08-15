import Foundation
import FamilyControls
import ManagedSettings
import DeviceActivity
import Combine

@MainActor
final class ScreenTimeManager: ObservableObject {
    static let storeName = ManagedSettingsStore.Name("group.com.cozyfocus.app")
    static let activityName = DeviceActivityName("cozyFocusSession")

    private static let selectionKey = "screenTime.selection"

    @Published var selection = FamilyActivitySelection() {
        didSet { saveSelection() }
    }
    @Published private(set) var isShielding = false
    @Published private(set) var statusText = "Not enabled"
    private let managedSettings = ManagedSettingsStore(named: storeName)
    private let activityCenter = DeviceActivityCenter()
    private var authorizationCancellable: AnyCancellable?

    init() {
        if let data = UserDefaults.standard.data(forKey: Self.selectionKey),
           let decoded = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) {
            self.selection = decoded
        }

        let hasApplications = managedSettings.shield.applications?.isEmpty == false
        let hasCategories: Bool
        switch managedSettings.shield.applicationCategories {
        case .specific(let categories, except: _): hasCategories = !categories.isEmpty
        default: hasCategories = false
        }
        isShielding = hasApplications || hasCategories
        statusText = isShielding ? "Distractions are paused for this focus sprint" : "Not enabled"

        authorizationCancellable = AuthorizationCenter.shared.$authorizationStatus
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                guard status == .denied else { return }
                Task { @MainActor [weak self] in
                    self?.endShielding(statusText: "Screen Time access was removed")
                }
            }
    }

    private func saveSelection() {
        if let data = try? JSONEncoder().encode(selection) {
            UserDefaults.standard.set(data, forKey: Self.selectionKey)
        }
    }

    func requestAccess() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            statusText = AuthorizationCenter.shared.authorizationStatus == .approved
                ? "Ready to choose distractions"
                : "Screen Time permission is needed"
        } catch {
            statusText = "Screen Time permission is needed"
        }
    }

    func beginShielding(until endDate: Date) {
        guard AuthorizationCenter.shared.authorizationStatus == .approved else {
            statusText = "Allow Screen Time access before shielding distractions"
            return
        }
        guard !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty else {
            statusText = "Choose at least one app or category"
            return
        }
        let startDate = Date.now
        guard endDate > startDate else {
            statusText = "This focus sprint has already ended"
            return
        }

        let calendar = Calendar.current
        let components: Set<Calendar.Component> = [.year, .month, .day, .hour, .minute, .second]
        let monitoringEndDate = startDate.addingTimeInterval(
            max(15 * 60, endDate.timeIntervalSince(startDate))
        )
        let warningMinutes = Int(
            (monitoringEndDate.timeIntervalSince(endDate) / 60).rounded()
        )
        let schedule = DeviceActivitySchedule(
            intervalStart: calendar.dateComponents(components, from: startDate),
            intervalEnd: calendar.dateComponents(components, from: monitoringEndDate),
            repeats: false,
            warningTime: warningMinutes > 0 ? DateComponents(minute: warningMinutes) : nil
        )

        do {
            activityCenter.stopMonitoring([Self.activityName])
            try activityCenter.startMonitoring(Self.activityName, during: schedule)
        } catch {
            managedSettings.clearAllSettings()
            isShielding = false
            statusText = "Could not schedule distraction shielding"
            return
        }

        managedSettings.shield.applications = selection.applicationTokens
        managedSettings.shield.applicationCategories = .specific(selection.categoryTokens)
        isShielding = true
        statusText = "Distractions are paused for this focus sprint"
    }

    func endShielding() {
        endShielding(statusText: "Not enabled")
    }

    private func endShielding(statusText: String) {
        activityCenter.stopMonitoring([Self.activityName])
        managedSettings.clearAllSettings()
        isShielding = false
        self.statusText = statusText
    }


    func reconcile(activeUntil endDate: Date?) {
        guard let endDate, endDate > .now else {
            endShielding()
            return
        }

        let hasApplications = managedSettings.shield.applications?.isEmpty == false
        let hasCategories: Bool
        switch managedSettings.shield.applicationCategories {
        case .specific(let categories, except: _): hasCategories = !categories.isEmpty
        default: hasCategories = false
        }
        isShielding = hasApplications || hasCategories
        statusText = isShielding ? "Distractions are paused for this focus sprint" : "Not enabled"
    }
}
