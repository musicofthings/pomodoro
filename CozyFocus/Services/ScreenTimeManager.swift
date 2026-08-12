import Foundation
import FamilyControls
import ManagedSettings

@MainActor
final class ScreenTimeManager: ObservableObject {
    @Published var selection = FamilyActivitySelection()
    @Published private(set) var isShielding = false
    @Published private(set) var statusText = "Not enabled"
    private let managedSettings = ManagedSettingsStore()

    init() {
        // A focus timer only exists while this app process is alive. Clear settings
        // from an interrupted prior run so a force-quit cannot leave apps shielded.
        managedSettings.clearAllSettings()
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

    func beginShielding() {
        guard AuthorizationCenter.shared.authorizationStatus == .approved else {
            statusText = "Allow Screen Time access before shielding distractions"
            return
        }
        guard !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty else {
            statusText = "Choose at least one app or category"
            return
        }
        managedSettings.shield.applications = selection.applicationTokens
        managedSettings.shield.applicationCategories = .specific(selection.categoryTokens)
        isShielding = true
        statusText = "Distractions are paused for this focus sprint"
    }

    func endShielding() {
        managedSettings.clearAllSettings()
        isShielding = false
        statusText = "Not enabled"
    }
}
