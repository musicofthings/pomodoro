import SwiftUI
import SwiftData

@main
struct CozyFocusApp: App {
    private let container: ModelContainer = {
        do {
            return try ModelContainer(for: FocusSession.self, InventoryEntry.self, CoinLedgerEntry.self)
        } catch {
            fatalError("Could not create Cozy Focus' local library: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.light)
        }
        .modelContainer(container)
    }
}
