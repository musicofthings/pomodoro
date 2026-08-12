import SwiftUI

struct ShareCard: View {
    let companion: Companion
    let cosmetic: Cosmetic?
    let completedSessions: Int
    let totalMinutes: Int

    var body: some View {
        ZStack {
            LinearGradient(colors: [.orange.opacity(0.18), .yellow.opacity(0.12), .white], startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(alignment: .leading, spacing: 22) {
                Text("COZY FOCUS")
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .tracking(3)
                    .foregroundStyle(.orange)
                Spacer()
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("I showed up for myself today.")
                            .font(.system(size: 44, weight: .bold, design: .rounded))
                            .fixedSize(horizontal: false, vertical: true)
                        HStack(spacing: 22) {
                            Label("\(completedSessions) sessions", systemImage: "checkmark.circle.fill")
                            Label("\(totalMinutes) minutes", systemImage: "clock.fill")
                        }
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 30)
                    VStack(spacing: -16) {
                        if let cosmetic { Text(cosmetic.mark).font(.system(size: 50)).zIndex(1) }
                        Text(companion.symbol).font(.system(size: 160))
                    }
                }
            }
            .padding(54)
        }
        .frame(width: 1920, height: 1080)
        .background(.white)
    }
}
