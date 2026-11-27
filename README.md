# Vizyon Music (`com.vizyon.music`)

Modern Kotlin & Jetpack Compose Material 3 music discovery, legal YouTube video playback, and social listening community app.

## Core Features
- **Music Discovery & Search:** Powered by iTunes Search API, YouTube Data API v3, and LRCLIB time-synced lyrics, with cancellable search jobs and deduplicated artist/track catalogs.
- **Shared Video Mini-Player & Now Playing:** Seamless single-WebView YouTube IFrame playback across fullscreen `NowPlayingModal` and responsive full-frame 16:9 `MiniPlayerBar`.
- **3-Panel Song Share Studio:** Share any track directly to the **Akış** (social feed), **Hikaye** (24-hour customizable Story card + PNG export), or **Mesaj** (direct message or message request).
- **Social Feed, Toggle Likes & Stories:** Rich activity posts with like/unlike support backed by Firestore transactions, long-press **Beğenenler** modal, threaded comments, reposts, and 24h story cards.
- **Direct Messaging & Voice Clips:** Real-time 1-on-1 chat threads with optimistic instant send, directional spring bubble animations, track attachments, and previewed AAC (`.m4a`, max 30s) voice messages.
- **Instant Notifications (Firestore + FCM):** Zero-latency Cloud Firestore snapshot listeners when foregrounded, plus Firebase Cloud Messaging (`VizyonFirebaseMessagingService`) push alerts with per-sender `NotificationCompat.MessagingStyle` windows and >=1150 ms audio cooldown when backgrounded or closed.
- **Privacy & Security Vault:** Google Sign-In via Jetpack `CredentialManager`, zero-permission Android Photo Picker, salted SHA-256 Security Vault lock, user blocking, safety reporting, and one-tap **Hesabı Kalıcı Sil**.

## Tech Stack
- **UI:** Kotlin, Jetpack Compose, Material Design 3, Navigation Compose (type-safe `@Serializable` routes), Coil.
- **Local Data:** Room Database, Jetpack DataStore Preferences.
- **Cloud & Auth:** Firebase Authentication (Google Sign-In), Cloud Firestore (Enterprise rules validated via `@firebase/rules-unit-testing`), Firebase Cloud Messaging (`firebase-messaging`).
- **Networking:** Retrofit, OkHttp, Kotlinx Serialization.

## GitHub & Local Build Setup
1. **API Keys:** Copy `.env.example` to `.env` (never commit `.env` or keystores) and provide your `YOUTUBE_API_KEY`.
2. **Firebase Config:** Ensure `app/google-services.json` matches your Firebase project and register both your debug and Play App Signing SHA-1/SHA-256 certificates in Firebase Console.
3. **Firebase Cloud Messaging (FCM):** Enable the **Firebase Cloud Messaging API (V1)** in your Firebase/Google Cloud project and deploy the included Cloud Function trigger (`/functions/index.js`) so closed devices receive high-priority data-only pushes:
   ```bash
   cd functions && npm install && firebase deploy --only functions
   ```
4. **Firestore Rules & Tests:**
   ```bash
   firebase emulators:start --only firestore
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```
5. **Android Build & Unit Tests:**
   ```bash
   gradle :app:testDebugUnitTest assembleDebug
   ```

## Google Play & Privacy Documentation
- [Privacy Policy](PRIVACY_POLICY.md)
- [Google Play Data Safety Declaration](DATA_SAFETY.md)
- [Play Console Release Guide](PLAY_CONSOLE_GUIDE.md)
