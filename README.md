<h1 align="center">
  <br>
  RootlessBrennanDSP
  <br>
</h1>
<h4 align="center">High-performance, ultra-low-latency system-wide DSP audio engine for non-rooted Android devices</h4>

<p align="center">
  <a href="https://github.com/bshea-1/RootlessBrennanDSP/releases"><img src="https://img.shields.io/badge/release-v1.0-blue?style=flat-square" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-green?style=flat-square" alt="License"></a>
</p>

> **Fork Notice**: **RootlessBrennanDSP** is a modernized, high-performance fork of [RootlessJamesDSP](https://github.com/ThePBone/RootlessJamesDSP) by [Tim Schneeberger (@ThePBone)](https://github.com/ThePBone), powered by [libjamesdsp](https://github.com/james34602/JamesDSPManager) by [James Fung (@james34602)](https://github.com/james34602).

---

## ⚡ What's Changed from the Original

### 1. Ultra-Low-Latency Audio Engine (FastMixer Integration)
- **FastMixer Kernel Pipeline**: Configured `AudioTrack` with `PERFORMANCE_MODE_LOW_LATENCY` and `AudioAttributes.FLAG_LOW_LATENCY`, routing audio directly to Android's low-latency FastMixer hardware thread.
- **Latency Cut**: Reduced default audio buffer from **8192 frames → 2048 frames** (~42 ms @ 48 kHz), cutting Bluetooth/AirPods audio lag by 4×.
- **Real-Time Priority**: Core recording and playback thread runs at `THREAD_PRIORITY_URGENT_AUDIO`.
- **Bypassed System Spatializer**: Added `SPATIALIZATION_BEHAVIOR_NEVER` for Android 12+ to skip redundant processing stages.

### 2. Zero-Copy Direct ByteBuffer Processing
- **Native Direct Memory I/O**: Implemented `processDirectBuffer` in C++ using `GetDirectBufferAddress`. Audio samples pass directly from hardware capture into C++ DSP without JVM array allocation or GC pauses.

### 3. Anti-Clipping Output Protection
- **Hard output clamp** on all native DSP processing paths (float and int16). Prevents digital crackling/static when effects like bass boost, EQ, or tube drive push samples beyond the valid PCM range — regardless of whether the user has configured the built-in limiter.
- **4× hardware buffer sizing** to absorb momentary processing stalls during effect reconfiguration.

### 4. Thread-Safe JNI & Crash-Free Memory Lifecycle
- **Global `JavaVM*` Management**: Replaced thread-local `JNIEnv*` caching with dynamic thread attachment, resolving multi-thread race conditions.
- **Deterministic Teardown**: Replaced the unsafe 100ms `Timer` deallocation with synchronized atomic state management.

### 5. Automatic Shizuku AudioPolicy Bypass
- **Automatic Capture Policy Unblocking**: Integrated privileged `IAudioPolicyService.setAllowedCapturePolicy(uid, ALLOW_CAPTURE_BY_ALL)` through Shizuku. Apps that typically restrict audio capture (Spotify, YouTube Music, SoundCloud, Chrome) now work **without requiring ReVanced or modified APKs**.

### 6. Bluetooth LE Audio & Modern Device Routing
- Detection and routing support for **Bluetooth LE Audio (LC3)**, **Auracast Broadcasts**, and **Hearing Aids**.
- **Bluetooth Sample Rate (48 kHz max)**: Bluetooth audio processing operates at a maximum sample rate of 48 kHz (44.1 kHz / 48 kHz). Higher sample rates (such as 96 kHz LDAC / LHDC) are not supported by the real-time capture pipeline and will lead to audio stuttering or dropouts. If using high-res codecs, ensure Bluetooth sample rate is configured to 48 kHz in Developer Options.

### 7. Modern Toolchain & Build
- Removed unconditional `-DDEBUG` in release builds; enabled `-Ofast -ftree-vectorize` with ARM NEON SIMD.
- Modernized to `targetSdk = 35` (Android 15/16/17 compatible), Shizuku API `13.1.5`.
- Migrated from `kapt` to KSP, updated Kotlin, Material Components 1.12.0.
- Cleaned up repo: removed Fastlane, Crowdin, unused CI workflows.

---

## 🎧 Features

- **Graphic & Parametric Equalizer** — Multi-band with smooth filter response
- **AutoEQ Integration** — Thousands of headphone profiles (AirPods, Sony, Sennheiser, Bose, etc.)
- **Convolution Reverb** — FIR impulse response processing (`.irs` / `.wav`)
- **Dynamic Bass Boost & ViPER Bass** — Frequency-tailored analog-modeled bass enhancement
- **Stereo Widening & Vacuum Tube Modeling** — Harmonic saturation and soundstage expansion
- **LiveProg (EEL VM)** — Programmable DSP scripting in real-time
- **Dynamic Range Compander** — Multi-band dynamics processing
- **DDC/ViPER DDC** — Device-specific frequency response correction

---

## 🚀 Installation

### Download
Get the latest APK from [**GitHub Releases**](https://github.com/bshea-1/RootlessBrennanDSP/releases).

### Setup
1. Install the APK on your device.
2. Start [**Shizuku**](https://shizuku.rikka.app/) and grant the requested permissions, or run via ADB:
   ```bash
   adb shell pm grant me.timschneeberger.rootlessjamesdsp android.permission.DUMP
   adb shell appops set me.timschneeberger.rootlessjamesdsp PROJECT_MEDIA allow
   adb shell appops set me.timschneeberger.rootlessjamesdsp SYSTEM_ALERT_WINDOW allow
   ```
3. Launch **RootlessBrennanDSP** and enable your desired sound profiles.

### Updating
Install the new APK over the old version — your settings and profiles are preserved automatically.

---

## 🔧 How It Works

Regular rootless audio effect apps on the Play Store are restricted to Android's [built-in audio effects](https://developer.android.com/reference/android/media/audiofx/AudioEffect), which limits them to basic EQ and bass boost.

RootlessBrennanDSP uses Android's internal audio capture to gain full access to the audio stream of other apps, allowing it to apply custom DSP effects directly. This is done without root access, using only standard Android APIs + Shizuku for privileged operations.

**DRM Note:** Some apps (Spotify, YouTube Music) block internal audio capture. With Shizuku's AudioPolicy bypass enabled, RootlessBrennanDSP automatically unblocks these apps. If that doesn't work for a specific app, the [ReVanced](https://github.com/revanced/revanced-manager/releases) `Remove screen capture restriction` patch can be applied to the problematic app's APK.

---

<details>
<summary><strong>🔑 Root Usage (Magisk)</strong></summary>

This app focuses on rootless operation, but can work with the JamesDSP Magisk module:

1. Install the JamesDSP Magisk module.
2. Uninstall the original JamesDSP app.
3. Install this APK.
4. Restart any active music apps (or reboot).

**Differences from original root app:**
- Different preset format (`.tar`) — old presets can't be imported directly.
- Uses `/sdcard/Android/data/james.dsp/` instead of `/sdcard/JamesDSP` (scoped storage). Files are deleted on uninstall — use the auto-backup feature.

**Updating:** Install over the old version. Root builds include a self-updater.

</details>

---

## 📝 About

**RootlessBrennanDSP v1.0**

A modernized fork focused on eliminating audio latency (especially for Bluetooth/AirPods), preventing audio artifacts, and providing automatic DRM bypass via Shizuku — all without requiring root or modified app APKs.

Built and maintained by [@bshea-1](https://github.com/bshea-1).

---

## 📜 Credits

- **JamesDSP** — [James Fung (@james34602)](https://github.com/james34602)
- **RootlessJamesDSP** — [Tim Schneeberger (@ThePBone)](https://github.com/ThePBone)
- Theming system & backup system based on [Tachiyomi](https://github.com/tachiyomiorg/tachiyomi)

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

This is a fork of GPL v3 licensed software. All modifications are distributed under the same license.
### Translators

<!-- CROWDIN-CONTRIBUTORS-START -->
<table>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/ThePBone"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15683553/medium/d13428d1e0922bc2069500aef57d1459.png" />
        <br />
        <sub><b>Tim Schneeberger (ThePBone)</b></sub></a>
      <br />
      <sub><b>~22396 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/netrunner-exe"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15209210/medium/dabb33b18a6eb0e59cee34e448d81e40.jpg" />
        <br />
        <sub><b>Oleksandr Tkachenko (netrunner-exe)</b></sub></a>
      <br />
      <sub><b>~13732 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/hanifz99"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15997687/medium/77003f34134a90b1b9089af86bbef755.png" />
        <br />
        <sub><b>Hanifz99 (hanifz99)</b></sub></a>
      <br />
      <sub><b>~4192 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/rex07"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/13820943/medium/5b5499d4f13f168e0eab0499857a831e.jpeg" />
        <br />
        <sub><b>Rex_sa (rex07)</b></sub></a>
      <br />
      <sub><b>~3543 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/FrameXX"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/14591682/medium/071f9d859dc36f9281f6f84b9c18c852.png" />
        <br />
        <sub><b>FrameXX</b></sub></a>
      <br />
      <sub><b>~3518 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/eevan78"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/12664235/medium/ee2d64bed2ea9a0a1a5ee31e59fa9d7c.jpg" />
        <br />
        <sub><b>Ivan Pesic (eevan78)</b></sub></a>
      <br />
      <sub><b>~3471 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Add000"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15913337/medium/5bb6874d577c3c856b729fdcd2f9137a.jpg" />
        <br />
        <sub><b>Add000</b></sub></a>
      <br />
      <sub><b>~3469 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/FlavioPonte"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15994613/medium/6ad9919ecb9cf61c034282b68e8bac17_default.png" />
        <br />
        <sub><b>FlavioPonte</b></sub></a>
      <br />
      <sub><b>~3455 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/Gokwu"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15975377/medium/7be6218dc0f81f4f2dc8418ea983bd9e.png" />
        <br />
        <sub><b>Choi Jun Hyeong (Gokwu)</b></sub></a>
      <br />
      <sub><b>~3438 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/narpatosian"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15572339/medium/887ab0b501163ccf586003a7bca29ee1.jpg" />
        <br />
        <sub><b>narpatosian</b></sub></a>
      <br />
      <sub><b>~3431 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/AeroShark333"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16224190/medium/e0b34056ea348d30906f48054f716f3c_default.png" />
        <br />
        <sub><b>Abiram Kanagaratnam (AeroShark333)</b></sub></a>
      <br />
      <sub><b>~3373 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/fankesyooni"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15676501/medium/6ee6d7e4c63bfb0f90dc5088a5ff0efd.jpg" />
        <br />
        <sub><b>fankesyooni</b></sub></a>
      <br />
      <sub><b>~3316 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/vjburic1"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16358724/medium/2c068e312e7171e555b24f08c4ac9ae2.jpeg" />
        <br />
        <sub><b>Vjekoslav Buric (vjburic1)</b></sub></a>
      <br />
      <sub><b>~3237 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/beruanglaut"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15727477/medium/928d69a437d753d783f03c22bf2d2c10.png" />
        <br />
        <sub><b>Beruanglaut (beruanglaut)</b></sub></a>
      <br />
      <sub><b>~3168 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/fred199542"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15215886/medium/9a13bdf396f1b87097813de7767f36a4_default.png" />
        <br />
        <sub><b>Federico D. (fred199542)</b></sub></a>
      <br />
      <sub><b>~2903 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/ismaeloi1"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15576171/medium/6909c87c219971037460a9110677b64a.png" />
        <br />
        <sub><b>Ismaël GUERET (ismaeloi1)</b></sub></a>
      <br />
      <sub><b>~2844 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/MajorCanel"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15507252/medium/4a02e1c8d12aae3330baa229e5f8fb5e.jpeg" />
        <br />
        <sub><b>HasanDgn37 (MajorCanel)</b></sub></a>
      <br />
      <sub><b>~2679 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/marcin.petrusiewicz"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/13535169/medium/29d3f1c6a1a270a85b8fda88e8d1c848.jpeg" />
        <br />
        <sub><b>Marcin Petrusiewicz (marcin.petrusiewicz)</b></sub></a>
      <br />
      <sub><b>~2360 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/liziq"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15757161/medium/f3903c160404f095de68760f81609430.jpeg" />
        <br />
        <sub><b>zhiq liu (liziq)</b></sub></a>
      <br />
      <sub><b>~1950 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/timli103117"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16184616/medium/5bb20ab441ea015a44b727baf585c20d.png" />
        <br />
        <sub><b>Tim Li (timli103117)</b></sub></a>
      <br />
      <sub><b>~1886 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/TecitoDeMenta"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15859109/medium/09cc6632c3686add5d52d4e7a3dec25a.jpg" />
        <br />
        <sub><b>Alondra Márquez (TecitoDeMenta)</b></sub></a>
      <br />
      <sub><b>~1847 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/phannhanh"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/14543576/medium/229892a475f27a927eb4ac8874c1a648.jpg" />
        <br />
        <sub><b>Phan Nhanh (phannhanh)</b></sub></a>
      <br />
      <sub><b>~1842 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/MES-INARI"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15690555/medium/d0cc094c5ae8ad9419d7e229d4ed76c0.jpg" />
        <br />
        <sub><b>MES-mitutti (MES-INARI)</b></sub></a>
      <br />
      <sub><b>~1750 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/jontix"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15464490/medium/bd7f97dff61f637d007652f9947d8f17.jpeg" />
        <br />
        <sub><b>jontix</b></sub></a>
      <br />
      <sub><b>~1731 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/LePom_"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16923449/medium/6a5f5297df7dc5fd9d6c91f40a9becc0.png" />
        <br />
        <sub><b>Miko Nurmi (LePom_)</b></sub></a>
      <br />
      <sub><b>~1464 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/SkyAfterRain_tw"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16631123/medium/3665e526285f3ed15a2b2f7d68b13cbc.jpeg" />
        <br />
        <sub><b>SkyAfterRain_tw</b></sub></a>
      <br />
      <sub><b>~1419 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/dang15082006"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16282184/medium/bb9dbdbd49c8a5bf049bedc83a0d0cfc.jpeg" />
        <br />
        <sub><b>Đăng Nguyễn (dang15082006)</b></sub></a>
      <br />
      <sub><b>~1307 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/AXEN.dev"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15755831/medium/4f31c78564ed55fef4b2bf8d96213a55.jpeg" />
        <br />
        <sub><b>Alessandro Belfiore (AXEN.dev)</b></sub></a>
      <br />
      <sub><b>~1228 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/TheGary"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15713727/medium/4f9ede8b07ace57124001fb6678aeff7_default.png" />
        <br />
        <sub><b>Gary Bonilla (TheGary)</b></sub></a>
      <br />
      <sub><b>~1030 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/kyunairi"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15925091/medium/7b1dd408c51242ab8602eb68408987cb_default.png" />
        <br />
        <sub><b>kyunairi</b></sub></a>
      <br />
      <sub><b>~888 words</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/LuckyMehra776"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16617215/medium/d2326af0f75dbef63ba72a5057bc32ec.jpeg" />
        <br />
        <sub><b>Lucky Mehra (LuckyMehra776)</b></sub></a>
      <br />
      <sub><b>~819 words</b></sub>
    </td>
    <td align="center" valign="top">
      <a href="https://crowdin.com/profile/roccovantechno"><img alt="logo" style="width: 64px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15818971/medium/75663306f941c87c2d9088c923aa89ad.jpeg" />
        <br />
        <sub><b>Gyuri Gergely (roccovantechno)</b></sub></a>
      <br />
      <sub><b>~714 words</b></sub>
    </td>
  </tr>
</table><a href="https://crowdin.com/project/rootlessjamesdsp" target="_blank">Translate in Crowdin 🚀</a>
<!-- CROWDIN-CONTRIBUTORS-END -->
