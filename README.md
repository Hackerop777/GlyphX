# GlyphX ⚡

[![Status](https://img.shields.io/badge/status-under%20development-yellow.svg)](https://github.com/Hackerop777/GlyphX)
[![Purpose](https://img.shields.io/badge/purpose-for%20fun-blue.svg)](https://github.com/Hackerop777/GlyphX)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](https://android.com)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

> **Tags:** `under development` • `for fun`

**GlyphX** is a modern, privacy-first Android productivity app combining **Notes + To-Do lists** with a signature **in-app Canvas Lock** (a drawn gesture/glyph password).

---

## ✨ Signature Features

### 1. In-App Canvas Lock
- **Drawn Gesture Password**: Draw your custom signature or glyph to unlock notes and tasks.
- **Hardware-Accelerated Glow**: Smooth, hardware-accelerated neon trailing strokes with a luminous tip orb and dynamic pulse animations.
- **Shape-Invariant Matching Engine**: Powered by bidirectional Chamfer distance ($P Point-Cloud Recognizer) and cyclic phase search. Whether you draw a star starting from the top vertex or bottom-left, clockwise or counter-clockwise, it recognizes your shape reliably while rejecting foreign gestures.
- **Hardware Keystore Protection**: Gestures are normalized and encrypted using **AES-256-GCM** hardware-backed Keystore keys. No raw drawings are ever stored or leaked.
- **Rate-Limiting & Lockout**: 5 consecutive failed attempts trigger an automated 30-second lockout with a live countdown timer.
- **Old-Password Verification**: Re-authentication is strictly required before changing the glyph or disabling security.
- **Biometric Backup**: Full integration with `BiometricPrompt` (fingerprint, face, device credential) as an optional recovery mechanism.

### 2. Notes
- Searchable, categorized note-taking with instant full-text filtering.
- Rich formatting bar: Bold, Italic, Underline, Bullet lists, Checklists, Code blocks, and Tag badges.
- Pinned notes for quick access to priority ideas.

### 3. To-Do Lists
- Organized with filter pills: `All`, `Today`, `Upcoming`, and `Completed`.
- Interactive checkboxes with animated strikethrough effects.
- Priority flag badges: High (Red), Medium (Amber), Low (Blue).
- Quick-add modal bottom sheet for seamless task creation.

### 4. Responsive & Foldable Friendly
- **Phone (< 680dp)**: Sleek bottom navigation bar.
- **Foldable / Tablet ($\ge$ 680dp)**: Dynamic dual-pane split view (Navigation rail & list on left, Editor/Detail on right).

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose & Material 3
- **Architecture**: MVVM + Clean Modular Architecture + Kotlin Coroutines & StateFlow
- **Database**: Room Database 2.6.1 (local, offline-first)
- **Cryptography**: Android Keystore AES-256-GCM
- **Biometrics**: AndroidX Biometric 1.1.0
- **Serialization**: Kotlinx Serialization JSON

---

## 🚀 Building & Running

### Prerequisites
- JDK 17
- Android SDK (API 35, Build-tools 35.0.0)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Unit Tests
```bash
./gradlew test
```

---

## 📄 License
This project is open-source under the MIT License.
