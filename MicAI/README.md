# MIC AI — Personal AI Assistant for Android

**MIC AI** is a JARVIS-inspired personal AI assistant for Android, built by
**JIGNESH GAMING**. It combines an offline device brain (voice commands,
notes, reminders, caller announcements) with an online AI brain (general
knowledge, coding, Minecraft, Discord bots, VPS help, image understanding)
behind a futuristic glowing-orb interface.

- **Platform:** Android 9 (API 28) and above
- **Language:** Kotlin, Jetpack Compose, Material 3
- **Architecture:** Online + Offline hybrid
- **Package:** `com.jigneshgaming.micai`

## Features

### Voice assistant
- Wake word **"Hey MIC"** (foreground service with a speech-recognizer loop)
- Speech-to-text + text-to-speech in any installed device language
  (English, Hindi, Gujarati and more — auto-detected from the device /
  reply script)
- Continuous conversation, listening / thinking / speaking states

### Phone assistant
- Announces incoming callers by saved name, or "unknown number"
- Reads and summarises notifications (with Notification access)
- Searches contacts, opens installed apps by name
- Battery and storage reports
- Alarms (via the system clock) and reminders (via AlarmManager)
- Drafts SMS / email (always reviewed by you before sending)

### AI brain
- **Offline (no internet):** time/date, battery, notes, reminders, alarms,
  open apps, contacts, calculator, jokes, weather prompt
- **Online (needs API key):** general questions, internet-style research,
  coding & programming, websites/apps, Minecraft Java & Bedrock, Discord
  bots, VPS/server guidance, image & screenshot understanding, document
  summaries, multi-step planning

### Interface
- Animated glowing blue orb, voice waveform, dark JARVIS theme
- Home, Chat, Notes, Reminders, History, Settings, Permissions screens
- Battery + clock status, online/offline indicator

## Build & install

### Option A — download the built APK (no setup at all)

Every push under `MicAI/` is built by GitHub Actions and the debug APK is
published to the **[`mic-ai-latest` release](../releases/tag/mic-ai-latest)**:

1. Open that release on your phone and download `MIC-AI-debug.apk`.
2. Allow "install from unknown sources" when Android asks, then open the file.

The same APK is also attached to the workflow run as the
`mic-ai-debug-apk` artifact (Actions → MIC AI Android → latest run).

### Option B — GitHub Actions on your own fork

1. Push this repo to GitHub.
2. Open **Actions → MIC AI Android → Run workflow** (or push any change
   under `MicAI/`). Note: GitHub disables Actions on forks until you turn
   them on in **Settings → Actions → General**.
3. Download `MIC-AI-debug.apk` from the run artifacts, or from the
   `mic-ai-latest` release the workflow publishes.

### Option C — Local build

Requirements: JDK 17, Android SDK (platform 34, build-tools 34).

```bash
cd MicAI
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## First-time setup

1. Open MIC AI → grant **Microphone** (and optionally Phone/Contacts).
2. (Optional) **Settings → Online AI brain**: add your API base URL, model
   and key — or deploy the secure proxy in [`server/`](server/README.md)
   so no key ever touches the APK.
3. (Optional) Enable **"Hey MIC"** in Settings, then grant notification
   access in **Permissions** to let MIC read notifications.
4. (Optional) Grant **Alarms & reminders** in Permissions for reminders to
   fire at the exact minute. Without it, Android 12+ still fires them, a
   few minutes late.
5. Say **"Hey MIC"** or tap the mic and try: *"Open YouTube"*, *"Note buy
   milk"*, *"Remind me in 10 minutes to call mom"*, *"What is the weather
   in Rajkot?"*.

## Permissions

| Permission | Used for | Optional? |
|---|---|---|
| Microphone | Voice commands, wake word | Yes (voice needs it) |
| Phone state | Detect incoming calls | Yes |
| Contacts | Caller names, contact search | Yes |
| Notifications | Reminder notifications | Yes |
| Notification access | Read/summarise notifications | Yes |
| Exact alarms | On-time reminders (Android 12+) | Yes (falls back to inexact) |

## Security

- **No API keys in the APK.** Keys are entered by the user, stored with
  `EncryptedSharedPreferences` (AES-256), and sent only to the configured
  endpoint. See [`server/`](server/README.md) for a key-hiding proxy.
- Sensitive actions (sending messages, dialling) are never automatic —
  MIC AI drafts and asks.
- Notes, reminders and history stay on-device in a local SQLite database.

## Project layout

```
MicAI/
  app/src/main/java/com/jigneshgaming/micai/
    MainActivity.kt            single-activity shell + bottom nav
    AssistantViewModel.kt      routing: local intents → online AI → offline brain
    ai/                        AiClient (OpenAI-compatible), LocalBrain, WeatherClient
    voice/                     SpeechEngine (STT/TTS), WakeWordService
    phone/                     contacts, apps, alarms, call announcer, notifications
    service/                   reminder + boot receivers
    data/                      SQLite database, encrypted settings
    ui/                        JARVIS theme, orb, waveform, screens
  server/                      Cloudflare Worker proxy (keeps keys off-device)
  docs/                        architecture notes
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for design details and
known limitations.
