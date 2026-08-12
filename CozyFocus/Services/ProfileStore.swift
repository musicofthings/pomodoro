import Foundation
import SwiftData

@MainActor
final class ProfileStore: ObservableObject {
    @Published private(set) var coins: Int
    @Published var selectedCompanion: Companion {
        didSet { defaults.set(selectedCompanion.rawValue, forKey: Keys.companion) }
    }
    @Published var equippedCosmetic: Cosmetic? {
        didSet { defaults.set(equippedCosmetic?.rawValue, forKey: Keys.equippedCosmetic) }
    }

    private let defaults: UserDefaults

    private enum Keys {
        static let coins = "profile.coins"
        static let companion = "profile.companion"
        static let equippedCosmetic = "profile.equippedCosmetic"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        coins = defaults.object(forKey: Keys.coins) as? Int ?? 0
        selectedCompanion = Companion(rawValue: defaults.string(forKey: Keys.companion) ?? "") ?? .redPanda
        equippedCosmetic = defaults.string(forKey: Keys.equippedCosmetic).flatMap(Cosmetic.init(rawValue:))
    }

    func earn(_ amount: Int) {
        coins += amount
        defaults.set(coins, forKey: Keys.coins)
    }

    func owns(_ cosmetic: Cosmetic, inventory: [InventoryEntry]) -> Bool {
        inventory.contains { $0.cosmeticRaw == cosmetic.rawValue }
    }

    @discardableResult
    func purchase(_ cosmetic: Cosmetic, context: ModelContext) -> Bool {
        let cosmeticRaw = cosmetic.rawValue
        let descriptor = FetchDescriptor<InventoryEntry>(
            predicate: #Predicate { $0.cosmeticRaw == cosmeticRaw }
        )

        guard coins >= cosmetic.price,
              let matchingEntries = try? context.fetch(descriptor),
              matchingEntries.isEmpty else {
            return false
        }

        let entry = InventoryEntry(cosmetic: cosmetic)
        context.insert(entry)

        do {
            try context.save()
        } catch {
            context.delete(entry)
            return false
        }

        coins -= cosmetic.price
        defaults.set(coins, forKey: Keys.coins)
        equippedCosmetic = cosmetic
        return true
    }
}
