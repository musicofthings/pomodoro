import AVFoundation

@MainActor
final class AudioSessionActivityCoordinator {
    static let shared = AudioSessionActivityCoordinator(
        activate: {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.ambient, mode: .default)
            try session.setActive(true)
        },
        deactivate: {
            try? AVAudioSession.sharedInstance().setActive(
                false,
                options: .notifyOthersOnDeactivation
            )
        }
    )

    private let activate: () throws -> Void
    private let deactivate: () -> Void
    private var owners: Set<ObjectIdentifier> = []

    init(
        activate: @escaping () throws -> Void,
        deactivate: @escaping () -> Void
    ) {
        self.activate = activate
        self.deactivate = deactivate
    }

    func begin(owner: AnyObject) throws {
        let identifier = ObjectIdentifier(owner)
        guard !owners.contains(identifier) else { return }
        if owners.isEmpty { try activate() }
        owners.insert(identifier)
    }

    func end(owner: AnyObject) {
        guard owners.remove(ObjectIdentifier(owner)) != nil else { return }
        if owners.isEmpty { deactivate() }
    }
}

enum AmbientSound: String, CaseIterable, Identifiable {
    case blackNoise = "black_noise"
    case waterfall
    case rainfall
    case oceanWaves = "ocean_waves"
    case binauralPeakFocus = "binaural_peak_focus"
    case binauralAnalytical = "binaural_analytical"
    case binauralFlowState = "binaural_flow_state"
    case binauralShortBreak = "binaural_short_break"
    case binauralLongBreak = "binaural_long_break"
    case isochronicAnalytical = "isochronic_analytical"
    case somaticPurr = "somatic_purr"
    case earBrushing = "ear_brushing"
    case stochasticCrinkle = "stochastic_crinkle"
    case vinylCrackle = "vinyl_crackle"

    var id: String { rawValue }
    var label: String {
        switch self {
        case .blackNoise: "Black Noise"
        case .waterfall: "Waterfall"
        case .rainfall: "Dynamic Rainfall"
        case .oceanWaves: "Ocean Waves"
        case .binauralPeakFocus: "Binaural: Peak Focus"
        case .binauralAnalytical: "Binaural: Analytical"
        case .binauralFlowState: "Binaural: Flow State"
        case .binauralShortBreak: "Binaural: Short Break"
        case .binauralLongBreak: "Binaural: Long Break"
        case .isochronicAnalytical: "Isochronic: Analytical"
        case .somaticPurr: "Somatic Purr"
        case .earBrushing: "Ear-to-Ear Brushing"
        case .stochasticCrinkle: "Stochastic Crinkle"
        case .vinylCrackle: "Lo-Fi Vinyl Crackle"
        }
    }
    var icon: String {
        switch self {
        case .blackNoise: "speaker.slash.fill"
        case .waterfall: "water.waves"
        case .rainfall: "cloud.rain.fill"
        case .oceanWaves: "water.waves.and.arrow.trianglehead.up"
        case .binauralPeakFocus, .binauralAnalytical, .binauralFlowState, .binauralShortBreak, .binauralLongBreak: "headphones"
        case .isochronicAnalytical: "waveform.path.ecg"
        case .somaticPurr: "pawprint.fill"
        case .earBrushing: "ear.fill"
        case .stochasticCrinkle: "sparkles"
        case .vinylCrackle: "record.circle"
        }
    }
}

@MainActor
final class AmbientAudioPlayer: ObservableObject {
    @Published var selectedSound: AmbientSound = .rainfall
    @Published private(set) var isPlaying = false

    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private var hasAttachedPlayer = false
    private var activeBuffer: AVAudioPCMBuffer?
    private let audioSession = AudioSessionActivityCoordinator.shared

    var isRenderingAudio: Bool {
        isPlaying && engine.isRunning && playerNode.isPlaying
    }

    func toggle() {
        isPlaying ? stop() : play()
    }

    func play() {
        guard !isPlaying else { return }
        guard let url = Bundle.main.url(forResource: selectedSound.rawValue, withExtension: "m4a", subdirectory: "Sounds") else { return }
        do {
            let file = try AVAudioFile(forReading: url)
            guard file.length > 0,
                  file.length <= AVAudioFramePosition(UInt32.max),
                  let buffer = AVAudioPCMBuffer(
                      pcmFormat: file.processingFormat,
                      frameCapacity: AVAudioFrameCount(file.length)
                  ) else { return }
            try file.read(into: buffer)

            if !hasAttachedPlayer {
                engine.attach(playerNode)
                hasAttachedPlayer = true
            } else {
                engine.disconnectNodeOutput(playerNode)
            }
            engine.connect(playerNode, to: engine.mainMixerNode, format: file.processingFormat)

            try audioSession.begin(owner: self)
            activeBuffer = buffer
            playerNode.volume = 1
            playerNode.scheduleBuffer(buffer, at: nil, options: .loops)
            engine.prepare()
            try engine.start()
            playerNode.play()
            isPlaying = playerNode.isPlaying
        } catch {
            playerNode.stop()
            engine.stop()
            activeBuffer = nil
            isPlaying = false
            audioSession.end(owner: self)
        }
    }

    func stop() {
        playerNode.stop()
        engine.stop()
        activeBuffer = nil
        isPlaying = false
        audioSession.end(owner: self)
    }
}

@MainActor
final class MeditationBellPlayer: ObservableObject {
    private var player: AVAudioPlayer?

    func play() {
        guard let url = Bundle.main.url(forResource: "meditation_bell", withExtension: "m4a", subdirectory: "Sounds") else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
            let audioPlayer = try AVAudioPlayer(contentsOf: url)
            audioPlayer.volume = 0.55
            audioPlayer.prepareToPlay()
            audioPlayer.play()
            player = audioPlayer
        } catch {
            player = nil
        }
    }
}

/// Short, warm procedural greetings played whenever a companion is chosen.
/// They use a one-shot player node, so no render callback touches SwiftUI state.
@MainActor
final class AnimalSoundPlayer: ObservableObject {
    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private var hasAttachedPlayer = false
    private var activeBuffer: AVAudioPCMBuffer?
    private var playbackGeneration = 0
    private let audioSession = AudioSessionActivityCoordinator.shared

    func playSelection(for companion: Companion) {
        let format = engine.mainMixerNode.outputFormat(forBus: 0)
        guard let buffer = makeGreeting(format: format, companion: companion) else { return }
        playbackGeneration += 1
        let generation = playbackGeneration
        activeBuffer = buffer
        if !hasAttachedPlayer {
            engine.attach(playerNode)
            engine.connect(playerNode, to: engine.mainMixerNode, format: format)
            hasAttachedPlayer = true
        }
        do {
            try audioSession.begin(owner: self)
            playerNode.stop()
            if !engine.isRunning { try engine.start() }
            playerNode.scheduleBuffer(buffer, at: nil, options: [], completionCallbackType: .dataPlayedBack) { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.finishGreeting(generation: generation)
                }
            }
            playerNode.play()
        } catch {
            finishGreeting(generation: generation)
        }
    }

    private func finishGreeting(generation: Int) {
        guard playbackGeneration == generation else { return }
        playerNode.stop()
        engine.stop()
        activeBuffer = nil
        audioSession.end(owner: self)
    }

    private func makeGreeting(format: AVAudioFormat, companion: Companion) -> AVAudioPCMBuffer? {
        let duration = companion == .horse ? 0.72 : 0.42
        let frames = AVAudioFrameCount(Int(format.sampleRate * duration))
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames) else { return nil }
        buffer.frameLength = frames
        let channels = UnsafeMutableAudioBufferListPointer(buffer.mutableAudioBufferList)
        let baseFrequency: Double = switch companion {
        case .redPanda: 780
        case .capybara: 230
        case .rabbit: 620
        case .puppy: 430
        case .cat: 480
        case .horse: 180
        }

        for frame in 0..<Int(frames) {
            let progress = Double(frame) / Double(frames)
            let envelope = sin(Double.pi * min(1, progress * 1.3)) * (1 - max(0, progress - 0.75) * 3.2)
            let glide: Double
            switch companion {
            case .cat: glide = 1.0 + sin(progress * .pi) * 0.65
            case .puppy, .redPanda: glide = 1.15 - progress * 0.35
            case .horse: glide = 0.70 + progress * 0.85
            default: glide = 1.0
            }
            let time = Double(frame) / format.sampleRate
            let fundamental = sin(2 * .pi * baseFrequency * glide * time)
            let overtone = sin(2 * .pi * baseFrequency * glide * 2.02 * time) * 0.24
            let signal = Float((fundamental + overtone) * envelope * 0.13)
            for channel in channels {
                channel.mData?.assumingMemoryBound(to: Float.self)[frame] = signal
            }
        }
        return buffer
    }
}
