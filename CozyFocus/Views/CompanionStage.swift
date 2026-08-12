import SwiftUI

/// A swipeable gallery of face-only, front-facing companion portraits.
struct CompanionStage: View {
    @Binding var companion: Companion
    let cosmetic: Cosmetic?
    let isFocusing: Bool
    let height: CGFloat
    let onSelection: (Companion) -> Void

    var body: some View {
        VStack(spacing: 4) {
            TabView(selection: $companion) {
                ForEach(Companion.allCases) { animal in
                    PortraitCard(companion: animal, cosmetic: animal == companion ? cosmetic : nil, isFocusing: isFocusing, scale: height / 294)
                        .tag(animal)
                        .padding(.horizontal, 8)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .frame(height: height - 26)
            .onChange(of: companion) { _, selected in
                onSelection(selected)
            }

            Text("Swipe for another companion")
                .font(.footnote.weight(.medium))
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .contain)
    }
}

private struct PortraitCard: View {
    let companion: Companion
    let cosmetic: Cosmetic?
    let isFocusing: Bool
    let scale: CGFloat

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30)) { timeline in
            let seconds = timeline.date.timeIntervalSinceReferenceDate
            let breathing = 1 + sin(seconds * 1.5) * 0.016
            let drift = sin(seconds * 0.7) * 2.5
            ZStack {
                Circle()
                    .fill(companion.accent.opacity(0.11))
                    .frame(width: 254, height: 254)
                Circle()
                    .stroke(companion.accent.opacity(0.24), lineWidth: 1)
                    .frame(width: 224, height: 224)
                AnimalFace(companion: companion, cosmetic: cosmetic)
                    .frame(width: 198, height: 198)
                    .scaleEffect(breathing)
                    .offset(y: drift)
            }
            .scaleEffect(scale)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(companion.name) looking right at you")
        }
        .overlay(alignment: .bottom) {
            Text(isFocusing ? "\(companion.name) is breathing alongside you" : "\(companion.name) is ready when you are")
                .font(.footnote.weight(.medium))
                .foregroundStyle(.secondary)
                .padding(.bottom, -20)
        }
        .padding(.bottom, 14)
    }
}

/// All faces are deliberately symmetrical and front-facing: no side-profile animal glyphs.
private struct AnimalFace: View {
    let companion: Companion
    let cosmetic: Cosmetic?

    private var fur: Color {
        switch companion {
        case .redPanda: .orange
        case .capybara: Color(red: 0.55, green: 0.35, blue: 0.20)
        case .rabbit: Color(red: 0.83, green: 0.76, blue: 0.70)
        case .puppy: Color(red: 0.76, green: 0.53, blue: 0.30)
        case .cat: Color(red: 0.58, green: 0.58, blue: 0.62)
        case .horse: Color(red: 0.45, green: 0.25, blue: 0.14)
        }
    }

    private var innerEar: Color { companion == .rabbit ? .pink.opacity(0.55) : fur.opacity(0.65) }

    var body: some View {
        GeometryReader { proxy in
            let side = min(proxy.size.width, proxy.size.height)
            ZStack {
                ears(side: side)
                face(side: side)
                faceMarkings(side: side)
                eyes(side: side)
                muzzle(side: side)
                if let cosmetic {
                    Text(cosmetic.mark)
                        .font(.system(size: side * 0.28))
                        .offset(y: -side * 0.47)
                        .zIndex(2)
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
    }

    @ViewBuilder private func ears(side: CGFloat) -> some View {
        switch companion {
        case .rabbit:
            HStack(spacing: side * 0.14) {
                Capsule().fill(fur).overlay(Capsule().fill(innerEar).padding(side * 0.07)).frame(width: side * 0.26, height: side * 0.72).rotationEffect(.degrees(-8))
                Capsule().fill(fur).overlay(Capsule().fill(innerEar).padding(side * 0.07)).frame(width: side * 0.26, height: side * 0.72).rotationEffect(.degrees(8))
            }
            .offset(y: -side * 0.23)
        case .puppy:
            HStack(spacing: side * 0.50) {
                Capsule().fill(fur.opacity(0.88)).frame(width: side * 0.25, height: side * 0.56).rotationEffect(.degrees(-25))
                Capsule().fill(fur.opacity(0.88)).frame(width: side * 0.25, height: side * 0.56).rotationEffect(.degrees(25))
            }
            .offset(y: -side * 0.14)
        case .capybara:
            HStack(spacing: side * 0.52) {
                Circle().fill(fur).frame(width: side * 0.24, height: side * 0.24)
                Circle().fill(fur).frame(width: side * 0.24, height: side * 0.24)
            }
            .offset(y: -side * 0.28)
        default:
            HStack(spacing: side * 0.35) {
                Triangle().fill(fur).frame(width: side * 0.52, height: side * 0.55).rotationEffect(.degrees(-8))
                Triangle().fill(fur).frame(width: side * 0.52, height: side * 0.55).rotationEffect(.degrees(8))
            }
            .offset(y: -side * 0.29)
        }
    }

    @ViewBuilder private func face(side: CGFloat) -> some View {
        switch companion {
        case .horse:
            RoundedRectangle(cornerRadius: side * 0.40, style: .continuous).fill(fur).frame(width: side * 0.68, height: side * 1.02).offset(y: side * 0.05)
        case .capybara:
            RoundedRectangle(cornerRadius: side * 0.34, style: .continuous).fill(fur).frame(width: side * 0.90, height: side * 0.75).offset(y: side * 0.07)
        default:
            Circle().fill(fur).frame(width: side * 0.94, height: side * 0.90).offset(y: side * 0.05)
        }
    }

    @ViewBuilder private func faceMarkings(side: CGFloat) -> some View {
        switch companion {
        case .redPanda:
            HStack(spacing: side * 0.14) {
                Capsule().fill(.white.opacity(0.86)).frame(width: side * 0.26, height: side * 0.38).rotationEffect(.degrees(27))
                Capsule().fill(.white.opacity(0.86)).frame(width: side * 0.26, height: side * 0.38).rotationEffect(.degrees(-27))
            }.offset(y: side * 0.08)
        case .cat:
            VStack(spacing: side * 0.05) {
                Capsule().fill(.black.opacity(0.35)).frame(width: side * 0.045, height: side * 0.22)
                Capsule().fill(.black.opacity(0.28)).frame(width: side * 0.035, height: side * 0.13)
            }.offset(y: -side * 0.15)
        case .horse:
            Capsule().fill(.white.opacity(0.7)).frame(width: side * 0.18, height: side * 0.48).offset(y: -side * 0.12)
        default:
            EmptyView()
        }
    }

    private func eyes(side: CGFloat) -> some View {
        HStack(spacing: companion == .horse ? side * 0.22 : side * 0.27) {
            eye(side: side)
            eye(side: side)
        }
        .offset(y: companion == .horse ? -side * 0.03 : -side * 0.02)
    }

    private func eye(side: CGFloat) -> some View {
        ZStack(alignment: .topLeading) {
            Circle().fill(.black).frame(width: side * 0.15, height: side * 0.18)
            Circle().fill(.white.opacity(0.92)).frame(width: side * 0.04, height: side * 0.04).offset(x: side * 0.035, y: side * 0.035)
        }
    }

    private func muzzle(side: CGFloat) -> some View {
        VStack(spacing: -side * 0.03) {
            RoundedRectangle(cornerRadius: side * 0.20, style: .continuous)
                .fill(companion == .capybara ? fur.opacity(0.65) : .white.opacity(0.78))
                .frame(width: companion == .horse ? side * 0.42 : side * 0.52, height: side * 0.28)
            Circle().fill(.black.opacity(0.83)).frame(width: side * 0.12, height: side * 0.08)
        }
        .offset(y: companion == .horse ? side * 0.24 : side * 0.25)
    }
}

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.midX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
            path.closeSubpath()
        }
    }
}
