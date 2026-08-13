import Foundation
import SwiftData

@MainActor
final class ProfileStore: ObservableObject {
    @Published var selectedCompanion: Companion {
        didSet { defaults.set(selectedCompanion.rawValue, forKey: Keys.companion) }
    }
    @Published var equippedCosmetic: Cosmetic? {
        didSet { defaults.set(equippedCosmetic?.rawValue, forKey: Keys.equippedCosmetic) }
    }

    private let defaults: UserDefaults

    private enum Keys {
        static let legacyCoins = "profile.coins"
        static let migratedCoins = "profile.coinsMigratedToLedger"
        static let companion = "profile.companion"
        static let equippedCosmetic = "profile.equippedCosmetic"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        selectedCompanion = Companion(rawValue: defaults.string(forKey: Keys.companion) ?? "") ?? .redPanda
        equippedCosmetic = defaults.string(forKey: Keys.equippedCosmetic).flatMap(Cosmetic.init(rawValue:))
    }

    func owns(_ cosmetic: Cosmetic, inventory: [InventoryEntry]) -> Bool {
        inventory.contains { $0.cosmeticRaw == cosmetic.rawValue }
    }

    @discardableResult
    func purchase(_ cosmetic: Cosmetic, balance: Int, context: ModelContext) -> Bool {
        let cosmeticRaw = cosmetic.rawValue
        let descriptor = FetchDescriptor<InventoryEntry>(
            predicate: #Predicate { $0.cosmeticRaw == cosmeticRaw }
        )

        guard balance >= cosmetic.price,
              let matchingEntries = try? context.fetch(descriptor),
              matchingEntries.isEmpty else {
            return false
        }

        let entry = InventoryEntry(cosmetic: cosmetic)
        let debit = CoinLedgerEntry(amount: -cosmetic.price, reason: "Purchased \(cosmetic.rawValue)")
        context.insert(entry)
        context.insert(debit)

        do {
            try context.save()
        } catch {
            context.delete(entry)
            context.delete(debit)
            return false
        }

        equippedCosmetic = cosmetic
        return true
    }

    func migrateLegacyCoinsIfNeeded(hasLedgerEntries: Bool, context: ModelContext) {
        guard !defaults.bool(forKey: Keys.migratedCoins) else { return }

        let legacyCoins = defaults.integer(forKey: Keys.legacyCoins)
        guard !hasLedgerEntries, legacyCoins > 0 else {
            defaults.set(true, forKey: Keys.migratedCoins)
            return
        }

        let credit = CoinLedgerEntry(amount: legacyCoins, reason: "Migrated balance")
        context.insert(credit)
        do {
            try context.save()
            defaults.removeObject(forKey: Keys.legacyCoins)
            defaults.set(true, forKey: Keys.migratedCoins)
        } catch {
            context.delete(credit)
        }
    }
}
