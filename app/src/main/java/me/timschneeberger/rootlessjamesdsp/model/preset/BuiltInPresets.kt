package me.timschneeberger.rootlessjamesdsp.model.preset

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.AssetManagerExtensions.installPrivateAssets

/**
 * Registry of all built-in presets — both per-effect and global.
 * Strictly adheres to 0 dB unity gain / neutral loudness so that processing focuses on
 * acoustic correction, impulse response convolution, DDC, LiveProg scripts, crossfeed, and tone.
 */
object BuiltInPresets {

    data class PresetEntry(
        val name: String,
        val description: String,
        val values: Map<String, String>
    )

    data class GlobalPresetEntry(
        val name: String,
        val description: String,
        /** Map of (SharedPreferences namespace) → (Map of key → value) */
        val effectValues: Map<String, Map<String, String>>
    )

    // ═══════════════════════════════════════════════════════════════════
    //  PER-EFFECT PRESETS
    // ═══════════════════════════════════════════════════════════════════

    /** Convolver (Impulse Responses / IRS / HRTF) */
    val convolverPresets: List<PresetEntry> = listOf(
        PresetEntry(
            "Binaural HRTF Crossfeed",
            "Head-Related Transfer Function crossfeed to reduce headphone fatigue",
            mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/CorredHRTF_Crossfeed.wav", "convolver_mode" to "0")
        ),
        PresetEntry(
            "3D Virtual Surround 1",
            "Psychoacoustic binaural spatial room expansion for headphones",
            mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/CorredHRTF_Surround1.wav", "convolver_mode" to "0")
        ),
        PresetEntry(
            "3D Virtual Surround 2 (Cinematic)",
            "Wide cinematic surround impulse response for movies and gaming",
            mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/CorredHRTF_Surround2.wav", "convolver_mode" to "0")
        ),
        PresetEntry(
            "Acoustic Hall / Church",
            "Natural acoustic concert hall reflections and air",
            mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/Church.wav", "convolver_mode" to "0")
        ),
        PresetEntry(
            "Swap Stereo Channels",
            "Inverts Left and Right channels via impulse convolution",
            mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/SwapChannels.wav", "convolver_mode" to "0")
        )
    )

    /** DDC (Digital Device Correction) */
    val ddcPresets: List<PresetEntry> = listOf(
        PresetEntry(
            "Beyerdynamic DT-770 Pro",
            "Linearizes frequency response and tames treble peaks on studio headphones",
            mapOf("ddc_enable" to "true", "ddc_file" to "DDC/Beyerdynamic DT770.vdc")
        ),
        PresetEntry(
            "Butterworth Linear Crossover",
            "Smooth phase-accurate Butterworth digital correction",
            mapOf("ddc_enable" to "true", "ddc_file" to "DDC/Butterworth.vdc")
        ),
        PresetEntry(
            "Front-Rear Contrast",
            "Enhances front-to-back spatial contrast and depth separation",
            mapOf("ddc_enable" to "true", "ddc_file" to "DDC/FrontRearContrast.vdc")
        ),
        PresetEntry(
            "Sony MH750 In-Ear Correction",
            "Target response correction for balanced IEM listening",
            mapOf("ddc_enable" to "true", "ddc_file" to "DDC/mh750.vdc")
        )
    )

    /** LiveProg (Real-time EEL DSP Scripts) */
    val liveprogPresets: List<PresetEntry> = listOf(
        PresetEntry(
            "3D Spatial Depth Surround",
            "Psychoacoustic head-shadow and pinna spatialization",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/depthsurround.eel")
        ),
        PresetEntry(
            "Dynamic Auto Wideness",
            "Intelligent stereo widener that automatically preserves center vocals",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/autoWideness.eel")
        ),
        PresetEntry(
            "Mid/Side Center Vocal Boost",
            "Enhances lead vocal and dialogue clarity without muddying stereo background",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/msCentreBoost.eel")
        ),
        PresetEntry(
            "STFT Real-time Spectral Denoiser",
            "Fourier-transform background noise suppression for clean audio",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/stftDenoise.eel")
        ),
        PresetEntry(
            "ViPER Dynamic Bass Synthesizer",
            "Harmonic excitation for deep psychoacoustic sub-bass punch",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/viper_dynamicbass_preset.eel")
        ),
        PresetEntry(
            "Joe0Bloggs DRX10K High-Res Compander",
            "Multi-band dynamic range expander and compressor",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/Joe0Bloggs DRX10K compander-HR.eel")
        ),
        PresetEntry(
            "Hadamard Spatial Reverb",
            "Hadamard matrix feedback delay network for smooth diffusion",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/hadamVerb.eel")
        ),
        PresetEntry(
            "Stereo Dimension Chorus",
            "Multi-tap spatial modulation for rich lush instruments",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/delayChorus.eel")
        ),
        PresetEntry(
            "Stereo Field Manipulator",
            "Geometric stereo width, balance, and spatial orientation matrix",
            mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/stereoFieldManipulator.eel")
        )
    )

    /** BS2B Binaural Crossfeed */
    val crossfeedPresets: List<PresetEntry> = listOf(
        PresetEntry("BS2B Weak (700Hz, 4.5dB)", "Subtle crossfeed for mild fatigue reduction", mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "0")),
        PresetEntry("BS2B Strong (700Hz, 6.0dB)", "Standard natural speaker crossfeed simulation", mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "1")),
        PresetEntry("Out of Head", "Simulates wide monitor speaker dispersion in a room", mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "2")),
        PresetEntry("Surround 1", "Immersive virtual surround crossfeed blend", mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "3")),
        PresetEntry("Surround 2", "Wide theater crossfeed angle", mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "4")),
        PresetEntry("Joe0Bloggs Realistic Surround", "Natural spatial acoustic blending for headphones", mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "5"))
    )

    /** Reverb */
    val reverbPresets: List<PresetEntry> = listOf(
        PresetEntry("Studio / Living Room", "Subtle, clean acoustic reflections without muddy decay", mapOf("reverb_enable" to "true", "reverb_preset" to "15")),
        PresetEntry("Small Room", "Tight room acoustics for close-mic simulation", mapOf("reverb_enable" to "true", "reverb_preset" to "5")),
        PresetEntry("Medium Hall", "Natural spacious hall reverberation", mapOf("reverb_enable" to "true", "reverb_preset" to "3")),
        PresetEntry("Large Concert Hall", "Vast acoustic stage with smooth trailing decay", mapOf("reverb_enable" to "true", "reverb_preset" to "4")),
        PresetEntry("Vintage Plate Reverb", "Bright metallic plate resonance for vocals", mapOf("reverb_enable" to "true", "reverb_preset" to "9"))
    )

    /** Multiband / 15-band EQ presets */
    val equalizerPresets: List<PresetEntry> = listOf(
        PresetEntry(
            "Flat",
            "Bit-perfect neutral response",
            mapOf(
                "eq_enable" to "true", "eq_filter_type" to "0", "eq_interpolation" to "0",
                "eq_bands" to "25.0;40.0;63.0;100.0;160.0;250.0;400.0;630.0;1000.0;1600.0;2500.0;4000.0;6300.0;10000.0;16000.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0"
            )
        ),
        PresetEntry(
            "Audiophile Natural",
            "Subtle sub-bass warming with smooth upper-treble extension",
            mapOf(
                "eq_enable" to "true", "eq_filter_type" to "0", "eq_interpolation" to "0",
                "eq_bands" to "25.0;40.0;63.0;100.0;160.0;250.0;400.0;630.0;1000.0;1600.0;2500.0;4000.0;6300.0;10000.0;16000.0;2.0;2.0;1.5;1.0;0.5;0.0;0.0;0.0;0.0;0.5;1.0;1.5;2.0;1.5;1.0"
            )
        ),
        PresetEntry(
            "V-Shape (Engaging)",
            "Dynamic low-end and sparkling highs with balanced mids",
            mapOf(
                "eq_enable" to "true", "eq_filter_type" to "0", "eq_interpolation" to "0",
                "eq_bands" to "25.0;40.0;63.0;100.0;160.0;250.0;400.0;630.0;1000.0;1600.0;2500.0;4000.0;6300.0;10000.0;16000.0;3.5;3.0;2.5;1.5;0.5;-0.5;-1.0;-1.5;-1.0;0.0;1.0;2.0;2.5;3.0;3.5"
            )
        ),
        PresetEntry(
            "Vocal Presence",
            "Brings vocals forward with clarity and reduces boxiness",
            mapOf(
                "eq_enable" to "true", "eq_filter_type" to "0", "eq_interpolation" to "0",
                "eq_bands" to "25.0;40.0;63.0;100.0;160.0;250.0;400.0;630.0;1000.0;1600.0;2500.0;4000.0;6300.0;10000.0;16000.0;-1.0;-0.5;0.0;0.0;0.0;0.5;1.0;1.5;2.0;2.5;2.0;1.5;0.5;0.0;-0.5"
            )
        ),
        PresetEntry(
            "Acoustic & Classical",
            "Balanced timbre with open air for strings and acoustic instruments",
            mapOf(
                "eq_enable" to "true", "eq_filter_type" to "0", "eq_interpolation" to "0",
                "eq_bands" to "25.0;40.0;63.0;100.0;160.0;250.0;400.0;630.0;1000.0;1600.0;2500.0;4000.0;6300.0;10000.0;16000.0;1.0;1.0;0.5;0.0;0.0;0.0;0.0;0.5;1.0;1.5;2.0;2.0;2.0;1.5;1.0"
            )
        )
    )

    /** Parametric EQ presets (Format: PEQ: <freq> <gain> <q> <code>; where 0=PK, 1=LS, 2=HS) */
    val parametricEqPresets: List<PresetEntry> = listOf(
        PresetEntry("Reference Flat", "Bit-perfect zero correction", mapOf("peq_enable" to "true", "peq_bands" to "PEQ: ")),
        PresetEntry(
            "Harman Target (2019 Research)",
            "Industry-standard headphone target curve for natural balance",
            mapOf(
                "peq_enable" to "true",
                "peq_bands" to "PEQ: 105.0 4.5 0.71 1; 200.0 -1.0 1.41 0; 1200.0 -0.5 1.00 0; 3000.0 -1.5 1.41 0; 6000.0 -1.5 2.00 0; 10000.0 -1.0 0.71 2; "
            )
        ),
        PresetEntry(
            "Diffuse Field Target",
            "Diffusely reflected room acoustic curve for open soundstages",
            mapOf(
                "peq_enable" to "true",
                "peq_bands" to "PEQ: 2000.0 -1.5 1.00 0; 4000.0 -2.5 1.41 0; 6000.0 -2.0 2.00 0; 10000.0 -2.5 2.00 0; "
            )
        ),
        PresetEntry(
            "Vocal Clarity & Air",
            "2.8kHz presence boost + 12kHz high shelf for breath and clarity",
            mapOf(
                "peq_enable" to "true",
                "peq_bands" to "PEQ: 250.0 -1.5 1.41 0; 1000.0 1.0 0.71 0; 2800.0 2.5 1.20 0; 6500.0 -1.5 2.50 0; 12000.0 1.5 0.71 2; "
            )
        ),
        PresetEntry(
            "De-Esser (Harshness Tamer)",
            "Precision surgical cuts at sibilance frequencies (5.5kHz - 8.5kHz)",
            mapOf(
                "peq_enable" to "true",
                "peq_bands" to "PEQ: 5800.0 -3.5 3.00 0; 7200.0 -3.0 2.50 0; 8500.0 -2.5 3.00 0; "
            )
        )
    )

    /** Graphic EQ presets */
    val graphicEqPresets: List<PresetEntry> = listOf(
        PresetEntry("Flat", "Linear zero EQ", mapOf("geq_enable" to "true", "geq_nodes" to "GraphicEQ: 20.0 0.0; 50.0 0.0; 100.0 0.0; 200.0 0.0; 500.0 0.0; 1000.0 0.0; 2000.0 0.0; 5000.0 0.0; 10000.0 0.0; 20000.0 0.0;")),
        PresetEntry(
            "Gentle Warmth",
            "Smooth natural shelf with no harsh highs",
            mapOf("geq_enable" to "true", "geq_nodes" to "GraphicEQ: 20.0 2.0; 50.0 2.0; 100.0 1.5; 200.0 0.5; 500.0 0.0; 1000.0 0.0; 2000.0 0.0; 5000.0 -0.5; 10000.0 -1.0; 20000.0 -1.5;")
        ),
        PresetEntry(
            "Airy Detail",
            "Opens up high-frequency space and micro-detail",
            mapOf("geq_enable" to "true", "geq_nodes" to "GraphicEQ: 20.0 0.0; 50.0 0.0; 100.0 0.0; 200.0 0.0; 500.0 0.0; 1000.0 0.5; 2000.0 1.0; 5000.0 2.0; 10000.0 2.5; 20000.0 2.0;")
        )
    )

    /** Bass Boost presets */
    val bassPresets: List<PresetEntry> = listOf(
        PresetEntry("Subtle Touch (4dB)", "Gentle sub-bass foundation without bloat", mapOf("bass_enable" to "true", "bass_max_gain" to "4")),
        PresetEntry("Moderate (6dB)", "Balanced punch for modern pop and rock", mapOf("bass_enable" to "true", "bass_max_gain" to "6")),
        PresetEntry("Deep Bass (9dB)", "Full low-end response for electronic and hip-hop", mapOf("bass_enable" to "true", "bass_max_gain" to "9")),
        PresetEntry("Heavy (12dB)", "Maximum low-end power", mapOf("bass_enable" to "true", "bass_max_gain" to "12"))
    )

    /** Compander presets */
    val companderPresets: List<PresetEntry> = listOf(
        PresetEntry(
            "Transparent Dynamic Smoothing",
            "Gentle multiband leveling without altering perceived loudness",
            mapOf(
                "compander_enable" to "true", "compander_timeconstant" to "0.22", "compander_granularity" to "1", "compander_tftransforms" to "0",
                "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-1;-1;-1;-1;-1;-1;-1"
            )
        ),
        PresetEntry(
            "Punchy Transients",
            "Enhances dynamic impact of drums and plucked strings",
            mapOf(
                "compander_enable" to "true", "compander_timeconstant" to "0.08", "compander_granularity" to "2", "compander_tftransforms" to "0",
                "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;2;2;1;0;0;0;1"
            )
        ),
        PresetEntry(
            "Dialogue & Speech Leveler",
            "Maintains consistent vocal volume for podcasts and audiobooks",
            mapOf(
                "compander_enable" to "true", "compander_timeconstant" to "0.15", "compander_granularity" to "2", "compander_tftransforms" to "0",
                "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-3;-2;-1;-1;-1;-2;-2"
            )
        ),
        PresetEntry(
            "Night Mode (Reduced Dynamic Range)",
            "Prevents sudden loud explosions while keeping dialogue audible",
            mapOf(
                "compander_enable" to "true", "compander_timeconstant" to "0.15", "compander_granularity" to "2", "compander_tftransforms" to "0",
                "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-5;-4;-3;-3;-3;-4;-4"
            )
        )
    )

    /** Tube Simulator presets */
    val tubePresets: List<PresetEntry> = listOf(
        PresetEntry("Subtle Warmth (1.5dB)", "Gentle 2nd-order analog harmonics", mapOf("tube_enable" to "true", "tube_drive" to "1.5")),
        PresetEntry("Vintage Console (3.5dB)", "Rich analog tape and transformer character", mapOf("tube_enable" to "true", "tube_drive" to "3.5")),
        PresetEntry("Tube Saturation (6.0dB)", "Noticeable analog warmth and bloom", mapOf("tube_enable" to "true", "tube_drive" to "6.0"))
    )

    /** Stereo Widen presets */
    val stereoWidePresets: List<PresetEntry> = listOf(
        PresetEntry("Natural Width (55)", "Slight soundstage opening for headphones", mapOf("stereowide_enable" to "true", "stereowide_mode" to "55")),
        PresetEntry("Expansive Stage (60)", "Wide holographic image", mapOf("stereowide_enable" to "true", "stereowide_mode" to "60")),
        PresetEntry("Cinema Width (65)", "Ultra-wide spatial presentation", mapOf("stereowide_enable" to "true", "stereowide_mode" to "65")),
        PresetEntry("Focused Center (45)", "Narrowed image for mono or vintage recordings", mapOf("stereowide_enable" to "true", "stereowide_mode" to "45"))
    )

    /** Output Control presets (Strictly 0dB postgain / transparent protection) */
    val outputControlPresets: List<PresetEntry> = listOf(
        PresetEntry("Transparent Safety (-0.1dB)", "True-peak inter-sample clipping protection", mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0")),
        PresetEntry("Mastering Peak Guard (-0.5dB)", "Ensures zero DAC distortion across all headphones", mapOf("limiter_threshold" to "-0.5", "limiter_release" to "50", "output_postgain" to "0"))
    )

    // ═══════════════════════════════════════════════════════════════════
    //  GLOBAL PRESETS (Simple, Intuitive Audiophile Combinations)
    //  ALL calibrated to 0dB output postgain for pure sound processing
    // ═══════════════════════════════════════════════════════════════════

    val globalPresets: List<GlobalPresetEntry> = listOf(
        GlobalPresetEntry(
            "🎧 3D Spatial Audio",
            "Immersive surround sound that feels like speakers around you instead of inside your ears.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/CorredHRTF_Crossfeed.wav", "convolver_mode" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/depthsurround.eel"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "0"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "true", "stereowide_mode" to "55"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_COMPANDER to mapOf("compander_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false"),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 105.0 3.0 0.71 1; 3000.0 -1.5 1.41 0; "
                )
            )
        ),
        GlobalPresetEntry(
            "💎 Clean & Balanced (Harman)",
            "Accurate, natural sound tuned to the industry-standard Harman curve with crystal clarity.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "true", "ddc_file" to "DDC/Butterworth.vdc"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/autoWideness.eel"),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 105.0 4.0 0.71 1; 200.0 -1.0 1.41 0; 3000.0 -1.5 1.41 0; 6000.0 -1.5 2.00 0; "
                ),
                Constants.PREF_TUBE to mapOf("tube_enable" to "true", "tube_drive" to "1.5"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "false"),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_COMPANDER to mapOf("compander_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🔊 Deep Bass & Punch",
            "Deep, powerful sub-bass for hip-hop, EDM, and pop without muddying vocals.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "50", "output_postgain" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/viper_dynamicbass_preset.eel"),
                Constants.PREF_BASS to mapOf("bass_enable" to "true", "bass_max_gain" to "6"),
                Constants.PREF_COMPANDER to mapOf(
                    "compander_enable" to "true", "compander_timeconstant" to "0.08", "compander_granularity" to "2", "compander_tftransforms" to "0",
                    "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;2;2;1;0;0;0;1"
                ),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 80.0 3.5 0.71 1; 250.0 -1.0 1.41 0; "
                ),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "true", "stereowide_mode" to "55"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🎤 Vocal & Speech Booster",
            "Lifts voices and lyrics with background hiss suppression for podcasts and songs.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/msCentreBoost.eel"),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 250.0 -1.5 1.41 0; 1000.0 1.0 0.71 0; 2800.0 2.5 1.20 0; 6500.0 -2.0 2.50 0; "
                ),
                Constants.PREF_COMPANDER to mapOf(
                    "compander_enable" to "true", "compander_timeconstant" to "0.15", "compander_granularity" to "2", "compander_tftransforms" to "0",
                    "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-2;-1;0;0;0;-1;-1"
                ),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🎮 Gaming & Movie 3D Surround",
            "Wide cinematic soundstage with crisp footsteps and clear explosions.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "50", "output_postgain" to "0"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/CorredHRTF_Surround2.wav", "convolver_mode" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/depthsurround.eel"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "true", "stereowide_mode" to "65"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "true", "reverb_preset" to "15"),
                Constants.PREF_BASS to mapOf("bass_enable" to "true", "bass_max_gain" to "4"),
                Constants.PREF_COMPANDER to mapOf(
                    "compander_enable" to "true", "compander_timeconstant" to "0.10", "compander_granularity" to "2", "compander_tftransforms" to "0",
                    "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;0;0;1;1;2;1;0"
                ),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false"),
                Constants.PREF_PEQ to mapOf("peq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "📻 Warm Analog & Vinyl",
            "Smooth, warm vintage sound with tube saturation and gentle highs.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "true", "tube_drive" to "3.5"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "true", "ddc_file" to "DDC/FrontRearContrast.vdc"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "true", "stereowide_mode" to "55"),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 80.0 2.5 0.71 1; 250.0 1.0 1.41 0; 3000.0 -1.0 1.00 0; 10000.0 -1.5 0.71 2; "
                ),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_COMPANDER to mapOf("compander_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🏛️ Live Concert Hall",
            "Recreates the wide acoustics and natural reverb of a live amphitheater.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/Church.wav", "convolver_mode" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/hadamVerb.eel"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "true", "tube_drive" to "2.0"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "true", "stereowide_mode" to "60"),
                Constants.PREF_BASS to mapOf("bass_enable" to "true", "bass_max_gain" to "4"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_COMPANDER to mapOf("compander_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false"),
                Constants.PREF_PEQ to mapOf("peq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🧘 Relaxed Listening (No Fatigue)",
            "Softens harsh treble and eliminates ear fatigue for long sessions.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "1"),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 5800.0 -3.0 3.00 0; 7200.0 -2.5 2.50 0; 10000.0 -2.0 0.71 2; "
                ),
                Constants.PREF_COMPANDER to mapOf(
                    "compander_enable" to "true", "compander_timeconstant" to "0.22", "compander_granularity" to "1", "compander_tftransforms" to "0",
                    "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-1;-1;-1;-1;-1;-1;-1"
                ),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🎵 Pure Studio Flat",
            "Bit-perfect, untouched sound as originally recorded in the studio.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "true", "ddc_file" to "DDC/Butterworth.vdc"),
                Constants.PREF_PEQ to mapOf("peq_enable" to "true", "peq_bands" to "PEQ: "),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_COMPANDER to mapOf("compander_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🌌 Super Wide Stereo",
            "Expands the stereo width for an open, airy, 3D soundstage.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/autoWideness.eel"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "true", "stereowide_mode" to "60"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "true", "convolver_file" to "Convolver/CorredHRTF_Crossfeed.wav", "convolver_mode" to "0"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "true", "tube_drive" to "1.5"),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_COMPANDER to mapOf("compander_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false"),
                Constants.PREF_PEQ to mapOf("peq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "📱 Phone Speaker Optimizer",
            "Optimizes sound for built-in phone or tablet speakers without distortion.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "50", "output_postgain" to "0"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "true", "ddc_file" to "DDC/mh750.vdc"),
                Constants.PREF_PEQ to mapOf(
                    "peq_enable" to "true",
                    "peq_bands" to "PEQ: 60.0 0.0 0.71 1; 800.0 1.5 1.00 0; 2500.0 2.0 1.20 0; "
                ),
                Constants.PREF_COMPANDER to mapOf(
                    "compander_enable" to "true", "compander_timeconstant" to "0.10", "compander_granularity" to "2", "compander_tftransforms" to "0",
                    "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-3;-2;-1;-1;-1;-2;-2"
                ),
                Constants.PREF_BASS to mapOf("bass_enable" to "true", "bass_max_gain" to "5"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "false"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false")
            )
        ),
        GlobalPresetEntry(
            "🌙 Night Mode (Calm Dynamics)",
            "Balances out loud sounds and lifts quiet dialogue for low-volume listening.",
            mapOf(
                Constants.PREF_OUTPUT to mapOf("limiter_threshold" to "-0.1", "limiter_release" to "60", "output_postgain" to "0"),
                Constants.PREF_LIVEPROG to mapOf("liveprog_enable" to "true", "liveprog_file" to "Liveprog/Joe0Bloggs DRX10K compander-HR.eel"),
                Constants.PREF_CROSSFEED to mapOf("bs2b_crossfeed_enable" to "true", "bs2b_crossfeed_mode" to "0"),
                Constants.PREF_COMPANDER to mapOf(
                    "compander_enable" to "true", "compander_timeconstant" to "0.15", "compander_granularity" to "2", "compander_tftransforms" to "0",
                    "compander_response" to "95.0;200.0;400.0;800.0;1600.0;3400.0;7500.0;-4;-3;-2;-2;-2;-3;-3"
                ),
                Constants.PREF_BASS to mapOf("bass_enable" to "false"),
                Constants.PREF_TUBE to mapOf("tube_enable" to "false"),
                Constants.PREF_STEREOWIDE to mapOf("stereowide_enable" to "false"),
                Constants.PREF_CONVOLVER to mapOf("convolver_enable" to "false"),
                Constants.PREF_DDC to mapOf("ddc_enable" to "false"),
                Constants.PREF_REVERB to mapOf("reverb_enable" to "false"),
                Constants.PREF_EQ to mapOf("eq_enable" to "false"),
                Constants.PREF_GEQ to mapOf("geq_enable" to "false"),
                Constants.PREF_PEQ to mapOf("peq_enable" to "false")
            )
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    //  LOOKUP HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /** Returns the list of built-in presets for the given SharedPreferences namespace */
    fun getPresetsForEffect(namespace: String): List<PresetEntry> {
        return when (namespace) {
            Constants.PREF_CONVOLVER -> convolverPresets
            Constants.PREF_DDC -> ddcPresets
            Constants.PREF_LIVEPROG -> liveprogPresets
            Constants.PREF_CROSSFEED -> crossfeedPresets
            Constants.PREF_REVERB -> reverbPresets
            Constants.PREF_EQ -> equalizerPresets
            Constants.PREF_PEQ -> parametricEqPresets
            Constants.PREF_GEQ -> graphicEqPresets
            Constants.PREF_BASS -> bassPresets
            Constants.PREF_COMPANDER -> companderPresets
            Constants.PREF_TUBE -> tubePresets
            Constants.PREF_STEREOWIDE -> stereoWidePresets
            Constants.PREF_OUTPUT -> outputControlPresets
            else -> emptyList()
        }
    }

    /** Apply a per-effect preset to the given SharedPreferences namespace */
    fun applyEffectPreset(context: Context, namespace: String, preset: PresetEntry) {
        context.assets.installPrivateAssets(context, force = false)
        val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        prefs.edit().apply {
            for ((key, value) in preset.values) {
                when {
                    isBooleanKey(key, value) -> putBoolean(key, value.toBoolean())
                    isFloatKey(key) -> putFloat(key, value.toFloatOrNull() ?: 0f)
                    else -> putString(key, value)
                }
            }
            commit()
        }
    }

    /** Apply a global preset across all relevant SharedPreferences namespaces */
    fun applyGlobalPreset(context: Context, preset: GlobalPresetEntry) {
        context.assets.installPrivateAssets(context, force = false)
        for ((namespace, values) in preset.effectValues) {
            val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
            prefs.edit().apply {
                for ((key, value) in values) {
                    when {
                        isBooleanKey(key, value) -> putBoolean(key, value.toBoolean())
                        isFloatKey(key) -> putFloat(key, value.toFloatOrNull() ?: 0f)
                        else -> putString(key, value)
                    }
                }
                commit()
            }
        }
    }

    private fun isBooleanKey(key: String, value: String): Boolean {
        return key.endsWith("_enable") || value == "true" || value == "false"
    }

    private fun isFloatKey(key: String): Boolean {
        return key == "bass_max_gain" || key == "tube_drive" || key == "stereowide_mode" ||
                key == "compander_timeconstant" || key == "compander_granularity" ||
                key == "limiter_threshold" || key == "limiter_release" || key == "output_postgain"
    }
}
