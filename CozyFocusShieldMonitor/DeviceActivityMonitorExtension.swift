import DeviceActivity
import ManagedSettings

final class DeviceActivityMonitorExtension: DeviceActivityMonitor {
    private let store = ManagedSettingsStore(named: .init("group.com.cozyfocus.app"))

    override func intervalDidEnd(for activity: DeviceActivityName) {
        guard activity.rawValue == "cozyFocusSession" else { return }
        store.clearAllSettings()
    }

    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        guard activity.rawValue == "cozyFocusSession" else { return }
        store.clearAllSettings()
    }
}
