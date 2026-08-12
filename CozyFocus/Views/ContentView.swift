import SwiftUI
import SwiftData
import FamilyControls

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.scenePhase) private var scenePhase
    @Query(sort: \FocusSession.completedAt, order: .reverse) private var sessions: [FocusSession]
    @Query private var inventory: [InventoryEntry]
    @StateObject private var profile = ProfileStore()
    @StateObject private var timer = FocusTimer()
    @StateObject private var ambient = AmbientAudioPlayer()
    @StateObject private var completionBell = MeditationBellPlayer()
    @StateObject private var animalSounds = AnimalSoundPlayer()
    @StateObject private var screenTime = ScreenTimeManager()
    @State private var showCompanions = false
    @State private var showShieldPicker = false
    @State private var shareImage: UIImage?
    @State private var showShareSheet = false
    @State private var completionMessage = false

    private var completedToday: [FocusSession] {
        sessions.filter { Calendar.current.isDateInToday($0.completedAt) }
    }
    private var totalMinutes: Int {
        Int(sessions.reduce(0) { $0 + $1.duration } / 60)
    }

    var body: some View {
        TabView {
            focusTab
                .tabItem { Label("Focus", systemImage: "timer") }
            journeyTab
                .tabItem { Label("Journey", systemImage: "chart.bar.fill") }
            denTab
                .tabItem { Label("Den", systemImage: "pawprint.fill") }
        }
        .tint(.orange)
        .background(Color(.systemGroupedBackground))
        .sheet(isPresented: $showCompanions) { companionPicker }
        .sheet(isPresented: $showShieldPicker) { shieldPicker }
        .sheet(isPresented: $showShareSheet) {
            if let shareImage { ActivityView(items: [shareImage]) }
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            timer.tick(onComplete: finishSession)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { timer.tick(onComplete: finishSession) }
        }
    }

    private var focusTab: some View {
        NavigationStack {
            GeometryReader { proxy in
                let isCompact = proxy.size.height < 720
                VStack(spacing: isCompact ? 7 : 10) {
                    HStack {
                        Text(greeting).font(.title2.bold())
                        Spacer()
                        Label("\(profile.coins)", systemImage: "circle.inset.filled")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.orange)
                            .padding(.horizontal, 10).padding(.vertical, 7)
                            .background(.orange.opacity(0.1), in: Capsule())
                    }

                    CompanionStage(
                        companion: $profile.selectedCompanion,
                        cosmetic: profile.equippedCosmetic,
                        isFocusing: timer.isRunning,
                        height: isCompact ? 194 : 224
                    ) { selected in
                        animalSounds.playSelection(for: selected)
                    }

                    VStack(spacing: 0) {
                        Text(timer.isComplete ? "Lovely work" : timer.isRunning ? "Focus gently" : "A \(timer.durationAdjective) moment")
                            .font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                        Text(timer.timeText)
                            .font(.system(size: isCompact ? 56 : 64, weight: .bold, design: .rounded).monospacedDigit())
                            .contentTransition(.numericText())
                            .animation(.snappy, value: timer.timeText)
                    }

                    HStack(spacing: 10) {
                        Button(action: toggleFocus) {
                            Label(primaryButtonLabel, systemImage: timer.isRunning ? "pause.fill" : "play.fill")
                                .frame(maxWidth: .infinity).padding(.vertical, 14)
                        }
                        .buttonStyle(.borderedProminent).tint(.orange)
                        Button(role: .destructive, action: stopFocus) {
                            Image(systemName: "stop.fill").frame(width: 24, height: 24).padding(11)
                        }
                        .buttonStyle(.bordered)
                        .disabled(!canStop)
                        .accessibilityLabel("Stop and reset")
                    }

                    durationPicker
                    focusControls
                }
                .padding(.horizontal, 18)
                .padding(.top, 8)
                .padding(.bottom, 8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .navigationBarHidden(true)
            .overlay(alignment: .top) {
                if completionMessage {
                    Label("+5 cozy coins — you did it", systemImage: "sparkles")
                        .font(.subheadline.weight(.bold))
                        .padding(12).background(.thinMaterial, in: Capsule())
                        .padding(.top, 8)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
        }
    }

    private var journeyTab: some View {
        NavigationStack {
            List {
                Section("Your quiet progress") {
                    HStack { Label("Focus sessions", systemImage: "checkmark.circle.fill"); Spacer(); Text("\(sessions.count)").fontWeight(.bold) }
                    HStack { Label("Time protected", systemImage: "clock.fill"); Spacer(); Text("\(totalMinutes) min").fontWeight(.bold) }
                    HStack { Label("Today", systemImage: "sun.max.fill"); Spacer(); Text("\(completedToday.count) sessions").fontWeight(.bold) }
                }
                Section("Recent moments") {
                    if sessions.isEmpty { Text("Your completed focus moments will rest here.").foregroundStyle(.secondary) }
                    ForEach(sessions.prefix(12)) { session in
                        HStack {
                            Text(Companion(rawValue: session.companionRaw)?.symbol ?? "✨").font(.title2)
                            VStack(alignment: .leading) {
                                Text("A gentle focus session").fontWeight(.medium)
                                Text(session.completedAt, format: .dateTime.weekday().month().day().hour().minute())
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text("+\(session.coinsEarned)").foregroundStyle(.orange).fontWeight(.bold)
                        }
                    }
                }
            }
            .navigationTitle("Journey")
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button { createShareCard() } label: { Label("Share", systemImage: "square.and.arrow.up") } } }
        }
    }

    private var denTab: some View {
        NavigationStack {
            List {
                Section { companionPickerBody } header: { Text("Your companion") }
                Section("Tiny treasures") {
                    ForEach(Cosmetic.allCases) { cosmetic in
                        let owned = profile.owns(cosmetic, inventory: inventory)
                        HStack(spacing: 14) {
                            Text(cosmetic.mark).font(.largeTitle)
                            VStack(alignment: .leading) { Text(cosmetic.name).fontWeight(.semibold); Text(owned ? "Yours to wear" : "\(cosmetic.price) cozy coins").font(.caption).foregroundStyle(.secondary) }
                            Spacer()
                            if owned { Button(profile.equippedCosmetic == cosmetic ? "Wearing" : "Wear") { profile.equippedCosmetic = cosmetic }.buttonStyle(.bordered) }
                            else { Button("Unlock") { profile.purchase(cosmetic, context: modelContext) }.buttonStyle(.borderedProminent).tint(.orange).disabled(profile.coins < cosmetic.price) }
                        }
                    }
                }
            }
            .navigationTitle("The Den")
        }
    }

    private var companionPicker: some View {
        NavigationStack { List { companionPickerBody }.navigationTitle("Choose your companion").toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { showCompanions = false } } } }
            .presentationDetents([.medium, .large])
    }

    private var companionPickerBody: some View {
        ForEach(Companion.allCases) { companion in
            Button { profile.selectedCompanion = companion; animalSounds.playSelection(for: companion) } label: {
                HStack { Text(companion.symbol).font(.largeTitle); Text(companion.name).font(.headline); Spacer(); if profile.selectedCompanion == companion { Image(systemName: "checkmark.circle.fill").foregroundStyle(.orange) } }
            }
            .foregroundStyle(.primary)
        }
    }

    private var shieldPicker: some View {
        NavigationStack {
            Form {
                Section("Optional Screen Time focus") {
                    Text("Choose apps or categories you want hidden while this focus session is active. This stays on your device.")
                    FamilyActivityPicker(selection: $screenTime.selection)
                        .frame(minHeight: 300)
                    Text(screenTime.statusText).font(.footnote).foregroundStyle(.secondary)
                }
                Section {
                    Button("Allow Screen Time access") { Task { await screenTime.requestAccess() } }
                    if screenTime.isShielding { Button("Stop shielding", role: .destructive) { screenTime.endShielding() } }
                }
            }
            .navigationTitle("Pause distractions")
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { showShieldPicker = false } } }
        }
    }

    private var greeting: String {
        switch Calendar.current.component(.hour, from: .now) {
        case 5..<12: "Good morning"
        case 12..<18: "Good afternoon"
        default: "Good evening"
        }
    }

    private var primaryButtonLabel: String {
        if timer.isRunning { return "Pause gently" }
        if timer.isComplete { return "Begin another \(timer.durationAdjective) session" }
        if timer.remaining < timer.sessionDuration { return "Resume focus" }
        return "Begin \(timer.durationAdjective) focus"
    }

    private var canStop: Bool {
        timer.isRunning || timer.remaining < timer.sessionDuration || timer.isComplete
    }

    private func toggleFocus() {
        if timer.isRunning {
            timer.pause()
            ambient.stop()
            screenTime.endShielding()
        } else {
            timer.start()
            ambient.play()
            if !screenTime.selection.applicationTokens.isEmpty || !screenTime.selection.categoryTokens.isEmpty {
                screenTime.beginShielding()
            }
        }
    }

    private var durationPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Choose your time", systemImage: "slider.horizontal.3")
                Spacer()
                Text(timer.durationText).fontWeight(.bold).foregroundStyle(.orange)
            }
            Slider(
                value: Binding(
                    get: { Double(timer.durationIndex) },
                    set: { timer.chooseDuration(at: Int($0.rounded())) }
                ),
                in: 0...Double(FocusTimer.durationOptions.count - 1),
                step: 1
            )
            .tint(.orange)
            HStack { Text("1 min"); Spacer(); Text("60 min") }
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.background, in: RoundedRectangle(cornerRadius: 16))
    }

    private var focusControls: some View {
        HStack(spacing: 8) {
            Menu {
                ForEach(AmbientSound.allCases) { sound in
                    Button { ambient.stop(); ambient.selectedSound = sound; if timer.isRunning { ambient.play() } } label: {
                        Label(sound.label, systemImage: sound.icon)
                    }
                }
            } label: {
                Label(ambient.selectedSound.label, systemImage: ambient.selectedSound.icon)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            Toggle(isOn: $timer.hapticsEnabled) { Image(systemName: "waveform.path") }
                .labelsHidden()
                .toggleStyle(.button)
                .accessibilityLabel("Gentle haptics")

            Button { showShieldPicker = true } label: {
                Image(systemName: screenTime.isShielding ? "lock.shield.fill" : "lock.shield")
                    .frame(width: 24, height: 24)
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Pause distractions")
        }
        .controlSize(.small)
    }

    private func stopFocus() {
        timer.reset()
        ambient.stop()
        screenTime.endShielding()
    }

    private func finishSession() {
        ambient.stop()
        screenTime.endShielding()
        completionBell.play()
        let session = FocusSession(duration: timer.sessionDuration, companion: profile.selectedCompanion)
        modelContext.insert(session)

        do {
            try modelContext.save()
        } catch {
            modelContext.delete(session)
            return
        }

        profile.earn(session.coinsEarned)
        withAnimation { completionMessage = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { withAnimation { completionMessage = false } }
    }

    private func createShareCard() {
        let renderer = ImageRenderer(content: ShareCard(companion: profile.selectedCompanion, cosmetic: profile.equippedCosmetic, completedSessions: sessions.count, totalMinutes: totalMinutes))
        renderer.scale = 1
        shareImage = renderer.uiImage
        showShareSheet = shareImage != nil
    }
}

struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController { UIActivityViewController(activityItems: items, applicationActivities: nil) }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) { }
}
