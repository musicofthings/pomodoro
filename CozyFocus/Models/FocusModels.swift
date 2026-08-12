import Foundation
import SwiftUI
import SwiftData

enum Companion: String, CaseIterable, Identifiable, Codable {
    case redPanda, capybara, rabbit, puppy, cat, horse

    var id: String { rawValue }
    var name: String {
        switch self {
        case .redPanda: "Red Panda"
        case .capybara: "Capybara"
        case .rabbit: "Rabbit"
        case .puppy: "Puppy"
        case .cat: "Cat"
        case .horse: "Horse"
        }
    }
    var symbol: String {
        switch self {
        case .redPanda: "🦊"
        case .capybara: "🦫"
        case .rabbit: "🐇"
        case .puppy: "🐶"
        case .cat: "🐈"
        case .horse: "🐴"
        }
    }
    var accent: Color {
        switch self {
        case .redPanda: .orange
        case .capybara: .brown
        case .rabbit: .purple
        case .puppy: .yellow
        case .cat: .pink
        case .horse: .teal
        }
    }
}

enum Cosmetic: String, CaseIterable, Identifiable, Codable {
    case flowerCrown, sunHat, bandana, roundGlasses, bowTie

    var id: String { rawValue }
    var name: String {
        switch self {
        case .flowerCrown: "Flower crown"
        case .sunHat: "Sun hat"
        case .bandana: "Bandana"
        case .roundGlasses: "Round glasses"
        case .bowTie: "Bow tie"
        }
    }
    var mark: String {
        switch self {
        case .flowerCrown: "🌼"
        case .sunHat: "👒"
        case .bandana: "🧣"
        case .roundGlasses: "👓"
        case .bowTie: "🎀"
        }
    }
    var price: Int {
        switch self {
        case .flowerCrown: 15
        case .sunHat: 20
        case .bandana: 10
        case .roundGlasses: 25
        case .bowTie: 15
        }
    }
}

@Model
final class FocusSession {
    @Attribute(.unique) var id: UUID
    var completedAt: Date
    var duration: TimeInterval
    var companionRaw: String
    var coinsEarned: Int

    init(completedAt: Date = .now, duration: TimeInterval, companion: Companion, coinsEarned: Int = 5) {
        self.id = UUID()
        self.completedAt = completedAt
        self.duration = duration
        self.companionRaw = companion.rawValue
        self.coinsEarned = coinsEarned
    }
}

@Model
final class InventoryEntry {
    @Attribute(.unique) var cosmeticRaw: String
    var acquiredAt: Date

    init(cosmetic: Cosmetic, acquiredAt: Date = .now) {
        self.cosmeticRaw = cosmetic.rawValue
        self.acquiredAt = acquiredAt
    }
}
