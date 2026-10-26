# VoiceSong Studio 🎙️🎶

A native Android music production app written in Kotlin with Jetpack Compose. **VoiceSong Studio** allows you to record your voice (singing, humming, whistling, beatboxing) and transforms it into authentic instrument sounds—including traditional Chinese instruments, Western classical & band instruments, drums, bass, and vocal synthesis—then combine them into multi-track songs!

---

## ✨ Features

### 1. 🎤 Voice-to-Instrument Transformation Engine
- **Traditional Chinese Instruments**:
  - **Erhu (二胡)**: Bowed 2-string instrument with characteristic python-skin formant resonance, expressive slide articulation, and natural vibrato.
  - **Guzheng (古筝)**: 21-string Chinese plucked zither with metallic ring and shimmering harmonics (Karplus-Strong physical modeling).
  - **Dizi (笛子)**: Chinese bamboo flute with buzzing membrane (*dimo*) harmonic distortion and breath noise.
  - **Pipa (琵琶)**: Traditional plucked lute with crisp percussive attack.
- **Western Instruments**:
  - **Grand Piano (钢琴)**: Concert grand piano with dynamic hammer strike velocity and unison string beating.
  - **Violin / Strings (小提琴)**: Bowed sawtooth with Hutchins violin wood cavity formant filtering.
  - **Acoustic Guitar (吉他)**: Karplus-Strong string pluck with warm acoustic body cavity resonance.
  - **Saxophone (萨克斯)**: Reed pressure non-linearity with resonant brass horn bell harmonics.
- **Drums & Percussion**:
  - **Beatbox Drum Kit**: Transforms vocal chest thuds / "boom" into **Kick Drums**, "ka" / "cha" into **Snare Drums**, and "tss" / "ti" into **Closed & Open Hi-Hats**.
  - **Percussion & Shaker**: Shakers, woodblock clicks, and tuned bongos.
- **Bass**:
  - **808 Sub Bass**: Saturated deep sub bass tracking vocal pitch down to 35–120 Hz.
  - **Synth Funk Bass**: Punchy resonant low-pass filtered saw bassline.
- **Vocal FX**:
  - **Pitch-Correct Vocal (Autotune)**: Snaps singing to perfect musical scale pitch degrees.
  - **Robot Vocoder**: Modulates a multi-harmonic carrier synth with your vocal formants and dynamics.

### 2. 🎼 Smart Pitch & Musical Scale Snapping
- **Chinese Pentatonic Scales (五声音阶)**:
  - **Gong (宫)**: `1, 2, 3, 5, 6` (C, D, E, G, A) — instantly gives hummed melodies an authentic traditional Chinese flavor!
  - **Yu (羽)**: `6, 1, 2, 3, 5` (A, C, D, E, G) — melancholic, ancient ballad style.
  - **Zhi (徵)**: `5, 6, 1, 2, 3` (G, A, C, D, E).
- **Western Scales**: Major, Natural Minor, Blues, Pentatonic Minor, Chromatic, and Free (continuous pitch tracking).
- **Octave Shifter**: Shift hummed pitch by -2, -1, 0, +1, or +2 octaves.

### 3. 🎛️ Multi-Track Song Arranger & DAW
- Multi-track timeline supporting unlimited concurrent tracks.
- Per-track **Volume fader** (0–120%), **Stereo Pan knob** (L 100% to R 100%), **Solo (S)**, and **Mute (M)**.
- Real-time audio waveform visualizer for each track.
- Master Transport bar: **Play / Pause**, **Stop**, **Seamless Looping**, and **BPM Tempo Stepper** (50 to 220 BPM).
- **Pre-loaded Demo Song ("Silk Road Beats")**: 6-track Asian fusion demo song ready to play upon launch (Beatbox Drums + 808 Bass + Erhu Melody + Guzheng Arpeggio + Piano Chords + Shaker).
- **Master Mix Export**: Renders the complete multi-track song to a high-fidelity 16-bit 44.1 kHz stereo `.WAV` file and provides a one-tap system Share sheet.

---

## 🚀 How to Open in Android Studio

1. Open **Android Studio** (Koala, Ladybug, Meerkat, Iguana, or later).
2. Click **Open** (or `File -> Open...`).
3. Browse to this directory:
   ```
   C:\Users\Ma\.gemini\antigravity\scratch\VoiceSongStudio
   ```
4. Click **OK**. Android Studio will recognize the Gradle project and automatically sync dependencies (`build.gradle.kts` with Gradle 8.9 and Jetpack Compose BOM).
5. Connect an Android device (via USB or WiFi) or start an Android Virtual Device (AVD).
6. Press the green **Run ▶** button (or `Shift + F10`) to build and launch the app!

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0 with Coroutines and Flow
- **UI Toolkit**: Jetpack Compose with Material 3 Dark Studio Theme
- **Architecture**: Clean Architecture / MVVM (`StudioViewModel` + StateFlow)
- **Audio Engine (Zero Native NDK headaches)**:
  - `AudioRecord` for low-latency microphone capture with RMS live amplitude feedback.
  - `AudioTrack` for low-latency multi-track stereo PCM stream mixing.
  - Custom Pure-Kotlin DSP Pipeline:
    - **YIN Pitch Detection**: Autocorrelation difference function with parabolic interpolation and voicing confidence gating.
    - **Transient / Spectral Flux Analysis**: Zero-crossing rate & energy band classification for beatbox drum triggers.
    - **Biquad IIR Formant Filters**: Bandpass and resonant shaping for acoustic instrument cavity modeling.
    - **Physical Modeling**: Karplus-Strong string synthesis, stick-slip bowed saw wave generation, and hammer strike dynamics.
    - **Soft Limiter / Saturation**: `tanh` soft-knee limiter ensuring distortion-free multi-track mixdowns.
