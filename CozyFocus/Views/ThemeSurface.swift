import SwiftUI

/// Owns the current full-screen backdrop. Future photo and video themes can
/// replace this renderer without changing any foreground controls.
struct FocusThemeBackdrop: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 1.00, green: 0.97, blue: 0.93),
                    Color(red: 0.95, green: 0.97, blue: 1.00),
                    Color(red: 1.00, green: 0.94, blue: 0.91)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Circle()
                .fill(.orange.opacity(0.16))
                .frame(width: 330, height: 330)
                .blur(radius: 50)
                .offset(x: 150, y: -310)

            Circle()
                .fill(.purple.opacity(0.12))
                .frame(width: 300, height: 300)
                .blur(radius: 58)
                .offset(x: -160, y: 270)
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

private struct GlassSurfaceModifier: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    let cornerRadius: CGFloat
    let tint: Color
    let shadowRadius: CGFloat

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        content
            .background {
                shape
                    .fill(
                        reduceTransparency
                            ? AnyShapeStyle(Color.white.opacity(0.94))
                            : AnyShapeStyle(.ultraThinMaterial)
                    )
                    .overlay {
                        shape.fill(
                            LinearGradient(
                                colors: [tint.opacity(0.18), .white.opacity(0.03)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    }
                    .overlay {
                        shape.stroke(
                            LinearGradient(
                                colors: [.white.opacity(0.82), .white.opacity(0.22)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                    }
                    .shadow(color: .black.opacity(0.10), radius: shadowRadius, y: 7)
            }
    }
}

extension View {
    func glassSurface(
        cornerRadius: CGFloat = 22,
        tint: Color = .white,
        shadowRadius: CGFloat = 16
    ) -> some View {
        modifier(
            GlassSurfaceModifier(
                cornerRadius: cornerRadius,
                tint: tint,
                shadowRadius: shadowRadius
            )
        )
    }
}

struct GlassActionButtonStyle: ButtonStyle {
    let tint: Color
    let isProminent: Bool

    init(tint: Color = .orange, isProminent: Bool = false) {
        self.tint = tint
        self.isProminent = isProminent
    }

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(isProminent ? Color.white : tint)
            .background {
                Capsule(style: .continuous)
                    .fill(
                        isProminent
                            ? AnyShapeStyle(tint.opacity(configuration.isPressed ? 0.72 : 0.88))
                            : AnyShapeStyle(.ultraThinMaterial)
                    )
                    .overlay {
                        Capsule(style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [.white.opacity(0.24), .clear],
                                    startPoint: .top,
                                    endPoint: .center
                                )
                            )
                    }
                    .overlay {
                        Capsule(style: .continuous)
                            .stroke(.white.opacity(isProminent ? 0.42 : 0.75), lineWidth: 1)
                    }
                    .shadow(color: tint.opacity(0.18), radius: 10, y: 5)
            }
            .scaleEffect(configuration.isPressed ? 0.975 : 1)
            .animation(.snappy(duration: 0.18), value: configuration.isPressed)
    }
}
