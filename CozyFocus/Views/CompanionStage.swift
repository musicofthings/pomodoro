import SwiftUI

/// A continuously wrapping gallery of front-facing companion portraits.
struct CompanionStage: View {
    @Binding var companion: Companion
    let cosmetic: Cosmetic?
    let isFocusing: Bool
    let height: CGFloat
    let onSelection: (Companion) -> Void

    @State private var page = 1
    private let companions = Companion.allCases

    private var circularPages: [Companion] {
        guard let first = companions.first, let last = companions.last else { return [] }
        return [last] + companions + [first]
    }

    var body: some View {
        VStack(spacing: 5) {
            TabView(selection: $page) {
                ForEach(Array(circularPages.enumerated()), id: \.offset) { index, animal in
                    PortraitCard(
                        companion: animal,
                        cosmetic: animal == companion ? cosmetic : nil,
                        isFocusing: isFocusing,
                        scale: height / 294
                    )
                    .tag(index)
                    .padding(.horizontal, 8)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: height - 30)
            .onAppear { synchronizePage(with: companion) }
            .onChange(of: page) { _, newPage in selectPage(newPage) }
            .onChange(of: companion) { _, selected in
                onSelection(selected)
                guard circularPages.indices.contains(page), circularPages[page] != selected else { return }
                synchronizePage(with: selected)
            }

            HStack(spacing: 6) {
                ForEach(companions) { animal in
                    Capsule()
                        .fill(animal == companion ? companion.accent : Color.secondary.opacity(0.22))
                        .frame(width: animal == companion ? 18 : 6, height: 6)
                        .animation(.snappy, value: companion)
                }
            }
            .accessibilityHidden(true)
        }
        .accessibilityElement(children: .contain)
        .accessibilityHint("Swipe in either direction to keep circling through companions")
    }

    private func selectPage(_ newPage: Int) {
        guard circularPages.indices.contains(newPage) else { return }
        let selected = circularPages[newPage]
        if companion != selected { companion = selected }

        if newPage == 0 {
            teleport(to: companions.count)
        } else if newPage == companions.count + 1 {
            teleport(to: 1)
        }
    }

    private func synchronizePage(with selected: Companion) {
        guard let index = companions.firstIndex(of: selected) else { return }
        let target = index + 1
        guard page != target else { return }
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction) { page = target }
    }

    private func teleport(to target: Int) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { page = target }
        }
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
            let breathing = 1 + sin(seconds * 1.5) * 0.014
            let drift = sin(seconds * 0.7) * 2.2

            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [companion.accent.opacity(0.18), companion.accent.opacity(0.02)],
                            center: .center,
                            startRadius: 20,
                            endRadius: 128
                        )
                    )
                    .frame(width: 254, height: 254)

                AnimalFace(companion: companion, cosmetic: cosmetic)
                    .frame(width: 198, height: 198)
                    .scaleEffect(breathing)
                    .offset(y: drift)
            }
            .scaleEffect(scale)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(companion.name) looking at you")
        }
        .overlay(alignment: .bottom) {
            Text(isFocusing ? "\(companion.name) is breathing alongside you" : "\(companion.name) is ready when you are")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .glassSurface(cornerRadius: 14, tint: companion.accent, shadowRadius: 6)
                .padding(.bottom, -17)
        }
        .padding(.bottom, 16)
    }
}

/// Dimensional portraits built from organic Bezier silhouettes. The artwork
/// stays vector-based so it remains crisp over future photo and video themes.
private struct AnimalFace: View {
    let companion: Companion
    let cosmetic: Cosmetic?

    private var palette: FurPalette { FurPalette(companion: companion) }

    var body: some View {
        GeometryReader { proxy in
            let side = min(proxy.size.width, proxy.size.height)

            ZStack {
                ears(side: side)

                FaceSilhouette(companion: companion)
                    .fill(
                        LinearGradient(
                            colors: [palette.highlight, palette.base, palette.shadow],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: side * 0.94, height: side * 0.91)
                    .offset(y: side * 0.06)
                    .shadow(color: palette.shadow.opacity(0.30), radius: side * 0.055, y: side * 0.04)
                    .overlay {
                        Ellipse()
                            .fill(.white.opacity(0.14))
                            .frame(width: side * 0.45, height: side * 0.24)
                            .blur(radius: side * 0.025)
                            .offset(x: -side * 0.13, y: -side * 0.22)
                    }

                markings(side: side)
                brows(side: side)
                eyes(side: side)
                muzzle(side: side)
                facialDetails(side: side)

                if let cosmetic {
                    Text(cosmetic.mark)
                        .font(.system(size: side * 0.27))
                        .shadow(color: .black.opacity(0.18), radius: 3, y: 2)
                        .offset(y: -side * 0.47)
                        .zIndex(4)
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
    }

    @ViewBuilder
    private func ears(side: CGFloat) -> some View {
        switch companion {
        case .rabbit:
            HStack(spacing: side * 0.12) {
                organicEar(RabbitEarShape(), side: side, width: 0.25, height: 0.70, angle: -7)
                organicEar(RabbitEarShape(), side: side, width: 0.25, height: 0.70, angle: 7, mirrored: true)
            }
            .offset(y: -side * 0.25)

        case .puppy:
            HStack(spacing: side * 0.46) {
                organicEar(FloppyEarShape(), side: side, width: 0.30, height: 0.58, angle: -12)
                organicEar(FloppyEarShape(), side: side, width: 0.30, height: 0.58, angle: 12, mirrored: true)
            }
            .offset(y: -side * 0.10)

        case .capybara:
            HStack(spacing: side * 0.50) {
                roundEar(side: side)
                roundEar(side: side)
            }
            .offset(y: -side * 0.29)

        case .horse:
            HStack(spacing: side * 0.27) {
                organicEar(LeafEarShape(), side: side, width: 0.23, height: 0.49, angle: -11)
                organicEar(LeafEarShape(), side: side, width: 0.23, height: 0.49, angle: 11, mirrored: true)
            }
            .offset(y: -side * 0.37)

        case .redPanda, .cat:
            HStack(spacing: side * 0.28) {
                organicEar(RoundedPointEarShape(), side: side, width: 0.39, height: 0.46, angle: -8)
                organicEar(RoundedPointEarShape(), side: side, width: 0.39, height: 0.46, angle: 8, mirrored: true)
            }
            .offset(y: -side * 0.31)
        }
    }

    private func organicEar<S: InsettableShape>(
        _ shape: S,
        side: CGFloat,
        width: CGFloat,
        height: CGFloat,
        angle: Double,
        mirrored: Bool = false
    ) -> some View {
        shape
            .fill(
                LinearGradient(
                    colors: [palette.highlight, palette.base, palette.shadow],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .overlay { shape.inset(by: side * 0.035).fill(palette.innerEar.opacity(0.82)) }
            .overlay { shape.stroke(.white.opacity(0.18), lineWidth: 1) }
            .frame(width: side * width, height: side * height)
            .scaleEffect(x: mirrored ? -1 : 1)
            .rotationEffect(.degrees(angle))
            .shadow(color: palette.shadow.opacity(0.22), radius: side * 0.025, y: side * 0.02)
    }

    private func roundEar(side: CGFloat) -> some View {
        Circle()
            .fill(RadialGradient(colors: [palette.highlight, palette.base], center: .topLeading, startRadius: 1, endRadius: side * 0.13))
            .overlay { Circle().fill(palette.innerEar.opacity(0.72)).padding(side * 0.045) }
            .frame(width: side * 0.25, height: side * 0.25)
            .shadow(color: palette.shadow.opacity(0.24), radius: 4, y: 3)
    }

    @ViewBuilder
    private func markings(side: CGFloat) -> some View {
        switch companion {
        case .redPanda:
            HStack(spacing: side * 0.10) {
                SoftFacePatch().fill(palette.muzzle.opacity(0.92)).frame(width: side * 0.31, height: side * 0.40).rotationEffect(.degrees(22))
                SoftFacePatch().fill(palette.muzzle.opacity(0.92)).frame(width: side * 0.31, height: side * 0.40).scaleEffect(x: -1).rotationEffect(.degrees(-22))
            }
            .offset(y: side * 0.065)
        case .cat:
            VStack(spacing: side * 0.025) {
                Capsule().fill(palette.shadow.opacity(0.46)).frame(width: side * 0.045, height: side * 0.18)
                HStack(spacing: side * 0.07) {
                    Capsule().fill(palette.shadow.opacity(0.34)).frame(width: side * 0.035, height: side * 0.13).rotationEffect(.degrees(-13))
                    Capsule().fill(palette.shadow.opacity(0.34)).frame(width: side * 0.035, height: side * 0.13).rotationEffect(.degrees(13))
                }
            }
            .offset(y: -side * 0.21)
        case .horse:
            SoftFacePatch().fill(palette.muzzle.opacity(0.75)).frame(width: side * 0.18, height: side * 0.52).offset(y: -side * 0.10)
        default:
            EmptyView()
        }
    }

    private func brows(side: CGFloat) -> some View {
        HStack(spacing: companion == .horse ? side * 0.21 : side * 0.25) {
            Capsule().fill(palette.shadow.opacity(0.30)).frame(width: side * 0.15, height: side * 0.025).rotationEffect(.degrees(-7))
            Capsule().fill(palette.shadow.opacity(0.30)).frame(width: side * 0.15, height: side * 0.025).rotationEffect(.degrees(7))
        }
        .offset(y: companion == .horse ? -side * 0.16 : -side * 0.15)
    }

    private func eyes(side: CGFloat) -> some View {
        HStack(spacing: companion == .horse ? side * 0.18 : side * 0.23) {
            eye(side: side)
            eye(side: side)
        }
        .offset(y: companion == .horse ? -side * 0.06 : -side * 0.035)
    }

    private func eye(side: CGFloat) -> some View {
        ZStack {
            Ellipse()
                .fill(Color(red: 0.11, green: 0.075, blue: 0.055))
                .frame(width: side * 0.17, height: side * 0.205)
                .shadow(color: .black.opacity(0.22), radius: side * 0.012, y: side * 0.01)
            Ellipse()
                .fill(RadialGradient(colors: [Color(red: 0.50, green: 0.28, blue: 0.12), Color(red: 0.08, green: 0.045, blue: 0.03)], center: .bottomTrailing, startRadius: 1, endRadius: side * 0.09))
                .frame(width: side * 0.125, height: side * 0.16)
            Ellipse().fill(.black.opacity(0.84)).frame(width: side * 0.072, height: side * 0.105)
            Circle().fill(.white.opacity(0.96)).frame(width: side * 0.045).offset(x: -side * 0.035, y: -side * 0.045)
            Circle().fill(.white.opacity(0.55)).frame(width: side * 0.018).offset(x: side * 0.03, y: side * 0.045)
        }
    }

    private func muzzle(side: CGFloat) -> some View {
        ZStack {
            MuzzlePadShape()
                .fill(LinearGradient(colors: [palette.muzzle.opacity(0.98), palette.muzzle.opacity(0.78)], startPoint: .top, endPoint: .bottom))
                .frame(width: companion == .horse ? side * 0.47 : side * 0.55, height: companion == .horse ? side * 0.34 : side * 0.31)
                .shadow(color: palette.shadow.opacity(0.16), radius: 3, y: 2)
            SoftNoseShape()
                .fill(LinearGradient(colors: [Color(red: 0.20, green: 0.14, blue: 0.12), .black.opacity(0.88)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: side * 0.13, height: side * 0.09)
                .offset(y: -side * 0.035)
                .overlay(alignment: .topLeading) {
                    Ellipse().fill(.white.opacity(0.38)).frame(width: side * 0.038, height: side * 0.018).offset(x: side * 0.018, y: -side * 0.045)
                }
            MouthShape()
                .stroke(.black.opacity(0.48), style: StrokeStyle(lineWidth: side * 0.012, lineCap: .round))
                .frame(width: side * 0.20, height: side * 0.10)
                .offset(y: side * 0.075)
        }
        .offset(y: companion == .horse ? side * 0.28 : side * 0.245)
    }

    @ViewBuilder
    private func facialDetails(side: CGFloat) -> some View {
        if companion == .cat || companion == .rabbit {
            HStack(spacing: side * 0.46) {
                Whiskers().stroke(palette.shadow.opacity(0.38), style: StrokeStyle(lineWidth: 1.1, lineCap: .round)).frame(width: side * 0.22, height: side * 0.15)
                Whiskers().stroke(palette.shadow.opacity(0.38), style: StrokeStyle(lineWidth: 1.1, lineCap: .round)).frame(width: side * 0.22, height: side * 0.15).scaleEffect(x: -1)
            }
            .offset(y: side * 0.26)
        }
    }
}

private struct FurPalette {
    let base: Color
    let highlight: Color
    let shadow: Color
    let muzzle: Color
    let innerEar: Color

    init(companion: Companion) {
        switch companion {
        case .redPanda:
            base = Color(red: 0.88, green: 0.31, blue: 0.10); highlight = Color(red: 1.00, green: 0.52, blue: 0.20); shadow = Color(red: 0.45, green: 0.12, blue: 0.055); muzzle = Color(red: 1.00, green: 0.88, blue: 0.75); innerEar = Color(red: 0.38, green: 0.12, blue: 0.09)
        case .capybara:
            base = Color(red: 0.54, green: 0.34, blue: 0.20); highlight = Color(red: 0.72, green: 0.51, blue: 0.32); shadow = Color(red: 0.27, green: 0.15, blue: 0.09); muzzle = Color(red: 0.65, green: 0.45, blue: 0.28); innerEar = Color(red: 0.36, green: 0.19, blue: 0.15)
        case .rabbit:
            base = Color(red: 0.82, green: 0.76, blue: 0.72); highlight = Color(red: 0.98, green: 0.95, blue: 0.92); shadow = Color(red: 0.53, green: 0.46, blue: 0.45); muzzle = Color(red: 0.98, green: 0.91, blue: 0.88); innerEar = Color(red: 0.90, green: 0.57, blue: 0.62)
        case .puppy:
            base = Color(red: 0.70, green: 0.45, blue: 0.24); highlight = Color(red: 0.91, green: 0.68, blue: 0.40); shadow = Color(red: 0.38, green: 0.20, blue: 0.10); muzzle = Color(red: 0.91, green: 0.79, blue: 0.63); innerEar = Color(red: 0.49, green: 0.24, blue: 0.18)
        case .cat:
            base = Color(red: 0.56, green: 0.58, blue: 0.64); highlight = Color(red: 0.80, green: 0.82, blue: 0.88); shadow = Color(red: 0.30, green: 0.31, blue: 0.38); muzzle = Color(red: 0.91, green: 0.89, blue: 0.91); innerEar = Color(red: 0.80, green: 0.50, blue: 0.58)
        case .horse:
            base = Color(red: 0.42, green: 0.23, blue: 0.13); highlight = Color(red: 0.66, green: 0.39, blue: 0.21); shadow = Color(red: 0.20, green: 0.095, blue: 0.05); muzzle = Color(red: 0.79, green: 0.67, blue: 0.56); innerEar = Color(red: 0.33, green: 0.14, blue: 0.12)
        }
    }
}

private struct FaceSilhouette: Shape {
    let companion: Companion
    func path(in rect: CGRect) -> Path {
        let w = rect.width, h = rect.height
        return Path { path in
            switch companion {
            case .horse:
                path.move(to: CGPoint(x: w * 0.50, y: h * 0.01))
                path.addCurve(to: CGPoint(x: w * 0.84, y: h * 0.32), control1: CGPoint(x: w * 0.72, y: 0), control2: CGPoint(x: w * 0.83, y: h * 0.13))
                path.addCurve(to: CGPoint(x: w * 0.73, y: h * 0.92), control1: CGPoint(x: w * 0.88, y: h * 0.58), control2: CGPoint(x: w * 0.84, y: h * 0.82))
                path.addCurve(to: CGPoint(x: w * 0.27, y: h * 0.92), control1: CGPoint(x: w * 0.63, y: h), control2: CGPoint(x: w * 0.37, y: h))
                path.addCurve(to: CGPoint(x: w * 0.16, y: h * 0.32), control1: CGPoint(x: w * 0.16, y: h * 0.82), control2: CGPoint(x: w * 0.12, y: h * 0.58))
                path.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.01), control1: CGPoint(x: w * 0.17, y: h * 0.13), control2: CGPoint(x: w * 0.28, y: 0))
            case .capybara:
                path.move(to: CGPoint(x: w * 0.50, y: h * 0.07))
                path.addCurve(to: CGPoint(x: w * 0.96, y: h * 0.48), control1: CGPoint(x: w * 0.82, y: h * 0.03), control2: CGPoint(x: w, y: h * 0.22))
                path.addCurve(to: CGPoint(x: w * 0.73, y: h * 0.94), control1: CGPoint(x: w * 0.97, y: h * 0.74), control2: CGPoint(x: w * 0.89, y: h * 0.91))
                path.addCurve(to: CGPoint(x: w * 0.27, y: h * 0.94), control1: CGPoint(x: w * 0.61, y: h), control2: CGPoint(x: w * 0.39, y: h))
                path.addCurve(to: CGPoint(x: w * 0.04, y: h * 0.48), control1: CGPoint(x: w * 0.11, y: h * 0.91), control2: CGPoint(x: w * 0.03, y: h * 0.74))
                path.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.07), control1: CGPoint(x: 0, y: h * 0.22), control2: CGPoint(x: w * 0.18, y: h * 0.03))
            default:
                path.move(to: CGPoint(x: w * 0.50, y: h * 0.02))
                path.addCurve(to: CGPoint(x: w * 0.95, y: h * 0.43), control1: CGPoint(x: w * 0.79, y: h * 0.01), control2: CGPoint(x: w * 0.97, y: h * 0.19))
                path.addCurve(to: CGPoint(x: w * 0.73, y: h * 0.93), control1: CGPoint(x: w, y: h * 0.70), control2: CGPoint(x: w * 0.88, y: h * 0.89))
                path.addCurve(to: CGPoint(x: w * 0.27, y: h * 0.93), control1: CGPoint(x: w * 0.61, y: h), control2: CGPoint(x: w * 0.39, y: h))
                path.addCurve(to: CGPoint(x: w * 0.05, y: h * 0.43), control1: CGPoint(x: w * 0.12, y: h * 0.89), control2: CGPoint(x: 0, y: h * 0.70))
                path.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.02), control1: CGPoint(x: w * 0.03, y: h * 0.19), control2: CGPoint(x: w * 0.21, y: h * 0.01))
            }
            path.closeSubpath()
        }
    }
}

private protocol OrganicInsetShape: InsettableShape where InsetShape == Self {
    var insetAmount: CGFloat { get set }
}

private extension OrganicInsetShape {
    func inset(by amount: CGFloat) -> Self {
        var copy = self
        copy.insetAmount += amount
        return copy
    }
}

private struct RoundedPointEarShape: OrganicInsetShape {
    var insetAmount: CGFloat = 0
    func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: insetAmount, dy: insetAmount)
        return Path { path in
            path.move(to: CGPoint(x: r.maxX * 0.92, y: r.maxY * 0.92))
            path.addCurve(to: CGPoint(x: r.midX * 0.88, y: r.minY + r.height * 0.08), control1: CGPoint(x: r.maxX * 0.83, y: r.maxY * 0.46), control2: CGPoint(x: r.midX * 1.08, y: r.minY + r.height * 0.02))
            path.addCurve(to: CGPoint(x: r.minX + r.width * 0.08, y: r.maxY * 0.80), control1: CGPoint(x: r.midX * 0.45, y: r.minY + r.height * 0.12), control2: CGPoint(x: r.minX, y: r.maxY * 0.58))
            path.addCurve(to: CGPoint(x: r.maxX * 0.92, y: r.maxY * 0.92), control1: CGPoint(x: r.minX + r.width * 0.32, y: r.maxY), control2: CGPoint(x: r.maxX * 0.66, y: r.maxY))
            path.closeSubpath()
        }
    }
}

private struct RabbitEarShape: OrganicInsetShape {
    var insetAmount: CGFloat = 0
    func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: insetAmount, dy: insetAmount)
        return Path { path in
            path.move(to: CGPoint(x: r.midX, y: r.minY))
            path.addCurve(to: CGPoint(x: r.maxX * 0.92, y: r.height * 0.48), control1: CGPoint(x: r.maxX * 0.86, y: r.height * 0.04), control2: CGPoint(x: r.maxX, y: r.height * 0.28))
            path.addCurve(to: CGPoint(x: r.midX, y: r.maxY), control1: CGPoint(x: r.maxX * 0.90, y: r.height * 0.79), control2: CGPoint(x: r.maxX * 0.73, y: r.maxY))
            path.addCurve(to: CGPoint(x: r.minX + r.width * 0.08, y: r.height * 0.48), control1: CGPoint(x: r.minX + r.width * 0.27, y: r.maxY), control2: CGPoint(x: r.minX, y: r.height * 0.79))
            path.addCurve(to: CGPoint(x: r.midX, y: r.minY), control1: CGPoint(x: r.minX, y: r.height * 0.28), control2: CGPoint(x: r.minX + r.width * 0.14, y: r.height * 0.04))
            path.closeSubpath()
        }
    }
}

private struct FloppyEarShape: OrganicInsetShape {
    var insetAmount: CGFloat = 0
    func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: insetAmount, dy: insetAmount)
        return Path { path in
            path.move(to: CGPoint(x: r.maxX * 0.82, y: r.minY + r.height * 0.05))
            path.addCurve(to: CGPoint(x: r.maxX * 0.94, y: r.height * 0.58), control1: CGPoint(x: r.maxX, y: r.height * 0.18), control2: CGPoint(x: r.maxX, y: r.height * 0.38))
            path.addCurve(to: CGPoint(x: r.midX * 0.72, y: r.maxY * 0.96), control1: CGPoint(x: r.maxX * 0.86, y: r.height * 0.87), control2: CGPoint(x: r.midX * 1.08, y: r.maxY))
            path.addCurve(to: CGPoint(x: r.minX + r.width * 0.08, y: r.height * 0.39), control1: CGPoint(x: r.minX + r.width * 0.08, y: r.maxY * 0.86), control2: CGPoint(x: r.minX, y: r.height * 0.61))
            path.addCurve(to: CGPoint(x: r.maxX * 0.82, y: r.minY + r.height * 0.05), control1: CGPoint(x: r.minX + r.width * 0.17, y: r.height * 0.11), control2: CGPoint(x: r.midX, y: r.minY))
            path.closeSubpath()
        }
    }
}

private struct LeafEarShape: OrganicInsetShape {
    var insetAmount: CGFloat = 0
    func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: insetAmount, dy: insetAmount)
        return Path { path in
            path.move(to: CGPoint(x: r.midX, y: r.minY))
            path.addCurve(to: CGPoint(x: r.maxX * 0.90, y: r.height * 0.56), control1: CGPoint(x: r.maxX * 0.82, y: r.height * 0.12), control2: CGPoint(x: r.maxX, y: r.height * 0.34))
            path.addCurve(to: CGPoint(x: r.midX, y: r.maxY), control1: CGPoint(x: r.maxX * 0.82, y: r.height * 0.84), control2: CGPoint(x: r.maxX * 0.66, y: r.maxY))
            path.addCurve(to: CGPoint(x: r.minX + r.width * 0.10, y: r.height * 0.56), control1: CGPoint(x: r.minX + r.width * 0.34, y: r.maxY), control2: CGPoint(x: r.minX, y: r.height * 0.84))
            path.addCurve(to: CGPoint(x: r.midX, y: r.minY), control1: CGPoint(x: r.minX, y: r.height * 0.34), control2: CGPoint(x: r.minX + r.width * 0.18, y: r.height * 0.12))
            path.closeSubpath()
        }
    }
}

private struct SoftFacePatch: Shape {
    func path(in rect: CGRect) -> Path { organicBlob(in: rect) }
}

private struct MuzzlePadShape: Shape {
    func path(in rect: CGRect) -> Path { organicBlob(in: rect) }
}

private func organicBlob(in rect: CGRect) -> Path {
    Path { path in
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addCurve(to: CGPoint(x: rect.maxX, y: rect.midY), control1: CGPoint(x: rect.maxX * 0.86, y: rect.minY), control2: CGPoint(x: rect.maxX, y: rect.height * 0.25))
        path.addCurve(to: CGPoint(x: rect.midX, y: rect.maxY), control1: CGPoint(x: rect.maxX * 0.90, y: rect.maxY * 0.86), control2: CGPoint(x: rect.maxX * 0.72, y: rect.maxY))
        path.addCurve(to: CGPoint(x: rect.minX, y: rect.midY), control1: CGPoint(x: rect.maxX * 0.20, y: rect.maxY), control2: CGPoint(x: rect.minX, y: rect.maxY * 0.78))
        path.addCurve(to: CGPoint(x: rect.midX, y: rect.minY), control1: CGPoint(x: rect.minX, y: rect.height * 0.21), control2: CGPoint(x: rect.maxX * 0.18, y: rect.minY))
        path.closeSubpath()
    }
}

private struct SoftNoseShape: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.midX, y: rect.maxY))
            path.addCurve(to: CGPoint(x: rect.minX, y: rect.height * 0.35), control1: CGPoint(x: rect.width * 0.30, y: rect.maxY), control2: CGPoint(x: rect.minX, y: rect.height * 0.72))
            path.addCurve(to: CGPoint(x: rect.maxX, y: rect.height * 0.35), control1: CGPoint(x: rect.width * 0.16, y: rect.minY), control2: CGPoint(x: rect.width * 0.84, y: rect.minY))
            path.addCurve(to: CGPoint(x: rect.midX, y: rect.maxY), control1: CGPoint(x: rect.maxX, y: rect.height * 0.72), control2: CGPoint(x: rect.width * 0.70, y: rect.maxY))
            path.closeSubpath()
        }
    }
}

private struct MouthShape: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.midX, y: rect.minY))
            path.addCurve(to: CGPoint(x: rect.minX, y: rect.midY), control1: CGPoint(x: rect.midX, y: rect.height * 0.34), control2: CGPoint(x: rect.width * 0.27, y: rect.midY))
            path.move(to: CGPoint(x: rect.midX, y: rect.minY))
            path.addCurve(to: CGPoint(x: rect.maxX, y: rect.midY), control1: CGPoint(x: rect.midX, y: rect.height * 0.34), control2: CGPoint(x: rect.width * 0.73, y: rect.midY))
        }
    }
}

private struct Whiskers: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.minX, y: rect.height * 0.24))
            path.addCurve(to: CGPoint(x: rect.maxX, y: rect.height * 0.40), control1: CGPoint(x: rect.width * 0.32, y: rect.height * 0.18), control2: CGPoint(x: rect.width * 0.68, y: rect.height * 0.32))
            path.move(to: CGPoint(x: rect.minX, y: rect.height * 0.58))
            path.addCurve(to: CGPoint(x: rect.maxX, y: rect.height * 0.56), control1: CGPoint(x: rect.width * 0.34, y: rect.height * 0.52), control2: CGPoint(x: rect.width * 0.69, y: rect.height * 0.52))
            path.move(to: CGPoint(x: rect.minX, y: rect.height * 0.87))
            path.addCurve(to: CGPoint(x: rect.maxX, y: rect.height * 0.72), control1: CGPoint(x: rect.width * 0.34, y: rect.height * 0.90), control2: CGPoint(x: rect.width * 0.68, y: rect.height * 0.78))
        }
    }
}
