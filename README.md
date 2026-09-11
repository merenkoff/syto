<p align="center">
  <img src="assets/logo/syto-ink-lockup-transparent.png" width="640" alt="Сито — Свої проходять. Чужі — ні." />
</p>
<p align="center">
  <b>Screen · Reject · Auto-reply · Talk back</b>
</p>
<p align="center">
  A local-only call and SMS sieve for Android. Unknown numbers get filtered, texted, or handled by an on-device voice bot — nothing leaves your phone.
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Android-10%2B%20(API%2029)-3DDC84?style=flat-square&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack-Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Cloud-none-0e1a3c?style=flat-square" />
  <img src="https://img.shields.io/badge/Data-stays%20on%20device-3fb950?style=flat-square" />
  <img src="https://img.shields.io/badge/License-OwnNet%201.1-8b5cf6?style=flat-square" />
</p>
<p align="center">
  <a href="#-why-syto">Why</a> •
  <a href="#-features">Features</a> •
  <a href="#-how-it-works">How it works</a> •
  <a href="#-permissions">Permissions</a> •
  <a href="#-privacy">Privacy</a> •
  <a href="#-known-limitations">Limitations</a> •
  <a href="#-roadmap">Roadmap</a> •
  <a href="#-build">Build</a>
</p>

---

## 🧭 Why Syto?

**Сито** (*syto*, Ukrainian for "sieve") does one thing: it stands between your phone and the people who aren't in your contacts.

Most call-blocking apps solve this by uploading your call log, your contacts, or your audio to someone else's server and matching them against a crowd-sourced spam database. Syto doesn't. It uses the same Android APIs the system dialer uses — `CallScreeningService`, `SmsManager`, `TextToSpeech`, on-device `SpeechRecognizer` — and keeps every decision, every transcript, and every contact lookup on the device.

If a number is in your contacts, the call rings through untouched. If it isn't, Syto does what you told it to: silently rejects, rejects and texts back, or picks up and lets a local voice bot find out what the caller wants.

---

## ✨ Features

Legend: ✅ done · 🚧 in progress · 🔭 planned

**Filtering** — MVP

* 🚧 Contact-based screening via `CallScreeningService` (no default-dialer takeover required)
* 🚧 Per-mode handling for unknown numbers: allow / silence / reject / reject + SMS
* 🚧 Short-code and service-number filter (no auto-replies to `900`-style senders)
* 🚧 Rate limit on outbound SMS replies
* 🔭 Allow-list / block-list overrides
* 🔭 Time-based rules (quiet hours, work hours)

**SMS auto-reply** — MVP

* 🚧 Reply to unknown senders with a template ("I'm busy — write me on Telegram")
* 🚧 Reply once per number per configurable window
* 🔭 Multiple templates, picked by rule

**Voice bot ("Talk back")** — stage 2

* 🔭 Auto-answer via `InCallService` (requires `ROLE_DIALER`)
* 🔭 On-device TTS greeting in Ukrainian / Russian / English
* 🔭 On-device speech recognition (Vosk, fully offline)
* 🔭 Keyword-driven dialog state machine (delivery → "leave it at the door"; sales → "not interested" → hang up)
* 🔭 Full transcript saved locally

**App**

* 🚧 Local log of every screened call and sent reply (Room)
* 🔭 Export log as JSON / CSV
* 🔭 Compose UI for rules and history

---

## 🏗 How it works

| Component | Role |
|---|---|
| `SytoScreeningService` | `CallScreeningService` — decides allow / silence / reject for every incoming call |
| `SmsReplyReceiver` | `BroadcastReceiver` on `SMS_RECEIVED` — auto-replies to unknown senders |
| `ContactResolver` | Cached lookup against `ContactsContract`; the only component that touches your address book |
| `ReplyThrottle` | Per-number rate limiting and short-code exclusion for outbound SMS |
| `VoiceBot` *(planned)* | `InCallService` + `TextToSpeech` + Vosk recognizer + dialog state machine |
| `SytoDatabase` | Room — call log, SMS log, transcripts, rules |

Architecture is MVVM for the UI layer and a small clean-architecture core so that the screening logic is testable without an Android device.

---

## 🔐 Permissions

| Permission / role | Why | Required for |
|---|---|---|
| `ROLE_CALL_SCREENING` | Let Android hand incoming calls to Syto before they ring | Filtering |
| `READ_CONTACTS` | Check whether the caller is someone you know | Filtering, SMS reply |
| `RECEIVE_SMS` | See incoming texts from unknown senders | SMS reply |
| `SEND_SMS` | Send the auto-reply | SMS reply |
| `ROLE_DIALER` + `InCallService` | Programmatically answer a call | Voice bot |
| `RECORD_AUDIO` | Let the recognizer hear the caller | Voice bot |

Syto requests **no** network permission. There is no `INTERNET` in the manifest.

---

## 🛡 Privacy

* Contacts are read locally and compared locally. They are never copied out of `ContactsContract` except into an in-memory cache.
* Call metadata, SMS replies, and voice-bot transcripts are stored in a local Room database you can export or wipe at any time.
* No analytics, no crash reporting, no cloud spam database, no telemetry of any kind.
* The voice bot's greeting announces that the call is being handled by an assistant and transcribed. Check your local law on call recording before enabling it.

This is the point of the project. It's also what the [OwnNet Source License](LICENSE) requires of anyone who forks it.

---

## ⚠️ Known limitations

Be honest about the platform, so nobody wastes a week finding these out:

* **Android does not give third-party apps access to the cellular call audio stream** in either direction. The voice bot therefore works by turning on the speakerphone and using the microphone — TTS is played through the loudspeaker, the caller is heard through the mic. It works with the phone on a table. It does not work in a pocket, and some OEM builds (Samsung, Xiaomi) block the microphone in `MODE_IN_CALL` entirely.
* `CallScreeningService` **cannot answer a call**. Auto-answer needs `ROLE_DIALER` + `InCallService`, which means Syto becomes your default phone app while the voice bot is enabled. The MVP filtering mode does not need this.
* **Google Play** restricts the SMS permission group to default SMS apps and a short list of approved use cases. Syto is designed for sideloading (GitHub Releases / Obtainium); a Play listing is not a goal.
* Offline recognition quality for Ukrainian depends on the model. The bundled Vosk model is ~50 MB and is downloaded once, from GitHub, on first enable — the only network call the app will ever make, and it's opt-in.

---

## 🗺 Roadmap

| Stage | Scope | Status |
|---|---|---|
| 0 | Repo, license, icon, plan | ✅ |
| 1 | `CallScreeningService` + contact lookup + reject unknown | 🚧 |
| 2 | SMS auto-reply with throttle and short-code filter | 🔭 |
| 3 | Room log + Compose UI (rules, history, export) | 🔭 |
| 4 | `ROLE_DIALER` + `InCallService` auto-answer | 🔭 |
| 5 | TTS + Vosk + dialog state machine | 🔭 |
| 6 | Transcript view, per-keyword actions | 🔭 |

---

## 🔧 Build

```bash
git clone https://github.com/merenkoff/syto
cd syto
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then open Syto, grant the call-screening role when prompted, and pick a mode for unknown numbers.

Requirements: Android Studio Ladybug or newer, JDK 17, a device running Android 10+.

---

## 📄 License

**OwnNet Source License 1.1** (Source Available) — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

This is a source-available license, **not** an OSI Open Source license:

- Free to use, study, modify, and share for any **non-commercial** purpose, and
  in a business whose annual gross revenue is under **USD $100,000**.
- **Commercial Use** — selling the app or offering its functionality as a paid
  service — requires a separate written agreement. Contact **mer.sergei@gmail.com**.
- **No Closed Systems**: no one may build remote kill switches, undocumented
  data lock-in, anti-repair measures, or undisclosed user tracking on top of
  this code. For a call-screening app this clause is not decoration.
- Derivative works stay under this license (ShareAlike) and must credit:
  *"Based on Syto by Serhii Merenkov / Technologies LLC (own-net.com)"*.

Contributions are accepted under the inbound terms in Section 6 of the LICENSE
(you keep your copyright; the Steward gets the right to relicense, including
commercially).

---

<p align="center">
  <b>Свої проходять. Чужі — ні.</b><br/>
  Made in Ukraine 🇺🇦
</p>
