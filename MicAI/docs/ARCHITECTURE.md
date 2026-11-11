# MIC AI — Architecture & honest limitations

## Request flow

```
Voice / text input
        │
        ▼
AssistantViewModel.route()
   ├─ 1. Device intents (notifications, storage) — live Android state
   ├─ 2. LocalBrain intents → Action → executed via PhoneAssistant
   │     (open app, note, reminder, alarm, contact, draft, weather)
   ├─ 3. AiClient.chat() — OpenAI-compatible /chat/completions
   │     (text + vision; only when configured & online)
   └─ 4. LocalBrain fallback text (offline explanation)
        │
        ▼
Reply → chat UI + history DB + TextToSpeech
```

## Modules

| Module | Responsibility |
|---|---|
| `ai/AiClient` | HTTP chat completions, vision messages, timeouts, null-on-failure |
| `ai/LocalBrain` | Pure rule-based intents + safe arithmetic parser (no Android deps) |
| `ai/WeatherClient` | Open-Meteo geocoding + forecast (no key needed) |
| `voice/SpeechEngine` | `SpeechRecognizer` STT + `TextToSpeech` with script-based locale |
| `voice/WakeWordService` | Foreground service looping the recognizer for "Hey MIC" |
| `phone/PhoneAssistant` | Contacts, apps, battery/storage, alarms, reminders, drafts |
| `phone/CallAnnouncer` | `PhoneStateListener` → contact lookup → TTS announcement |
| `phone/MicNotificationListener` | In-memory recent-notification store |
| `data/MicDatabase` | SQLite: notes, reminders, history (offline-first) |
| `data/SettingsStore` | Preferences; API key in `EncryptedSharedPreferences` (plain private prefs if the keystore refuses the master key) |

## Known limitations (by design, not bugs)

1. **Wake word** uses the system speech recognizer in a loop rather than a
   dedicated on-device hotword model. It works with any installed speech
   pack and needs no SDK key, but uses more battery than a Porcupine-style
   model. Upgrade path: replace the loop in `WakeWordService` with
   Porcupine's `WakeWordDetector` (free tier) and keep the same broadcast.
2. **Speech-to-text** depends on the device's recognizer service (usually
   Google). Offline recognition works only for language packs installed
   on the device.
3. **Caller announcements** run while the app is in the foreground.
   Android 12+ restricts background phone-state listeners; a full
   background implementation would need `PhoneCallService` /
   `TelecomManager` integration.
4. **Image/document understanding** needs the online AI (vision-capable
   model + API key). Text documents (.txt/.md) can be pasted into chat
   directly; PDF text extraction is not implemented (Android's
   `PdfRenderer` does not extract text).
5. **Exact alarms** on Android 12+ require the user to grant
   `SCHEDULE_EXACT_ALARM`; without it reminders fall back to inexact
   delivery (may be a few minutes late).
6. **No auto-sending.** MIC AI never sends messages, emails or calls on
   its own — it drafts and asks. This is a safety choice.

## Testing

What is verified automatically, on every push touching `MicAI/`:

- **Compilation** — `:app:assembleDebug` on JDK 17 with AGP 8.2.2 / Kotlin
  2.0.21 in `.github/workflows/mic-ai-android.yml`. Compiler errors are
  surfaced as workflow annotations.
- **Packaging** — `aapt2 dump badging` on the produced APK is published in
  the `mic-ai-latest` release notes, so the package name, `minSdkVersion`,
  declared permissions and launcher activity are checkable without
  installing anything.

What is **not** verified automatically:

- Runtime behaviour. No emulator or device run is part of the pipeline, so
  voice capture, TTS output, the wake word, caller announcements and
  reminder delivery have not been executed by CI.
- `LocalBrain` is pure Kotlin with no Android dependencies and is written
  to be unit-testable on the JVM, but no unit tests are wired up yet.

Manual test checklist: voice command, wake word, note, reminder (short
delay), alarm, open app, contact search, battery, weather (online),
AI chat (online), image question (online), caller announcement.
