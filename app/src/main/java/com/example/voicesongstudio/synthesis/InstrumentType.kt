package com.example.voicesongstudio.synthesis

enum class InstrumentCategory(val title: String) {
    CHINESE("Traditional Chinese"),
    WESTERN("Western Classical & Band"),
    DRUMS_PERCUSSION("Drums & Percussion"),
    BASS("Bass"),
    VOCAL_FX("Vocal & Vocoder")
}

enum class InstrumentType(
    val displayName: String,
    val iconEmoji: String,
    val category: InstrumentCategory,
    val defaultOctaveShift: Int = 0,
    val description: String
) {
    // Chinese Instruments
    ERHU(
        displayName = "Erhu (二胡)",
        iconEmoji = "🎻",
        category = InstrumentCategory.CHINESE,
        defaultOctaveShift = 0,
        description = "Chinese 2-string bowed fiddle with nasal warmth, expressive vibrato and slides"
    ),
    GUZHENG(
        displayName = "Guzheng (古筝)",
        iconEmoji = "🪕",
        category = InstrumentCategory.CHINESE,
        defaultOctaveShift = 0,
        description = "Traditional 21-string Chinese plucked zither with bright shimmering harmonics"
    ),
    DIZI(
        displayName = "Dizi (笛子)",
        iconEmoji = "🪈",
        category = InstrumentCategory.CHINESE,
        defaultOctaveShift = 0,
        description = "Chinese bamboo flute with vibrant buzzing membrane (dimo)"
    ),
    PIPA(
        displayName = "Pipa (琵琶)",
        iconEmoji = "🎸",
        category = InstrumentCategory.CHINESE,
        defaultOctaveShift = 0,
        description = "Traditional Chinese plucked lute with crisp percussive attack"
    ),

    // Western Instruments
    PIANO(
        displayName = "Grand Piano",
        iconEmoji = "🎹",
        category = InstrumentCategory.WESTERN,
        defaultOctaveShift = 0,
        description = "Acoustic concert grand piano with dynamic hammer response"
    ),
    VIOLIN(
        displayName = "Violin / Strings",
        iconEmoji = "🎻",
        category = InstrumentCategory.WESTERN,
        defaultOctaveShift = 0,
        description = "Bowed orchestra violin with warm resonant body and rich overtones"
    ),
    GUITAR_ACOUSTIC(
        displayName = "Acoustic Guitar",
        iconEmoji = "🎸",
        category = InstrumentCategory.WESTERN,
        defaultOctaveShift = 0,
        description = "Karplus-Strong physical modeling acoustic steel/nylon string pluck"
    ),
    SAXOPHONE(
        displayName = "Saxophone",
        iconEmoji = "🎷",
        category = InstrumentCategory.WESTERN,
        defaultOctaveShift = 0,
        description = "Warm reed wind instrument with brass bell harmonics"
    ),

    // Drums & Percussion
    DRUM_KIT(
        displayName = "Beatbox Drum Kit",
        iconEmoji = "🥁",
        category = InstrumentCategory.DRUMS_PERCUSSION,
        defaultOctaveShift = 0,
        description = "Transforms vocal beatbox thuds into Kicks, 'ka' into Snares, 'tss' into Hi-Hats"
    ),
    PERCUSSION(
        displayName = "Percussion & Shaker",
        iconEmoji = "🪇",
        category = InstrumentCategory.DRUMS_PERCUSSION,
        defaultOctaveShift = 0,
        description = "Tuned percussion, shakers, woodblocks and bongos mapped to vocal rhythm"
    ),

    // Bass
    BASS_808(
        displayName = "808 Sub Bass",
        iconEmoji = "🔊",
        category = InstrumentCategory.BASS,
        defaultOctaveShift = -1,
        description = "Deep saturated 808 sub bass following vocal pitch down into the sub frequencies"
    ),
    BASS_SYNTH(
        displayName = "Synth Funk Bass",
        iconEmoji = "⚡",
        category = InstrumentCategory.BASS,
        defaultOctaveShift = -1,
        description = "Punchy resonant low-pass filtered sawtooth bassline"
    ),

    // Vocal FX
    VOCAL_AUTOTUNE(
        displayName = "Pitch-Correct Vocal",
        iconEmoji = "🎤",
        category = InstrumentCategory.VOCAL_FX,
        defaultOctaveShift = 0,
        description = "Quantizes singing into perfect pitch snaps in the selected musical scale"
    ),
    VOCAL_VOCODER(
        displayName = "Robot Vocoder",
        iconEmoji = "🤖",
        category = InstrumentCategory.VOCAL_FX,
        defaultOctaveShift = 0,
        description = "Synthesizer carrier modulated by your voice envelope and formants"
    )
}
