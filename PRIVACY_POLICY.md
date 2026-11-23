# Privacy Policy — Vizyon Music

**Effective Date:** November 23, 2026  
**Application ID:** `com.vizyon.music`

Vizyon Music ("we", "our", "the App") respects your privacy and is designed with data minimization, on-device storage options, and transparent user control.

---

## 1. Data We Collect & How It Is Used

### 1.1 Optional Profile & Account Data
- **Device-Only Mode:** You can browse music, manage local playlists/favorites, and customize themes completely without creating an account. Preferences and local data remain on your device via Room and Jetpack DataStore.
- **Google Cloud Sync (Optional):** If you choose to sign in with Google (via Android Jetpack `CredentialManager` and Firebase Authentication), we store your Firebase UID, email address, display name, `@handle`, bio, profile photo, favorites, playlists, theme preferences, and recent tracks in Google Cloud Firestore to provide cross-device backup and social features.

### 1.2 Social, Messaging & Voice Clips
- When signed in and using social features, we store your shared activity posts, 24-hour music stories, follow connections, follow/message requests, direct text messages, and optional **voice messages** (AAC `.m4a`, maximum 30 seconds).
- **Microphone Access (`RECORD_AUDIO`):** Requested **only** when you explicitly press the record button inside a direct message thread. Recordings are previewed locally so you can listen, discard, or send.
- **Photo Selection:** Profile photos are selected through Android's zero-permission **Photo Picker** (`PickVisualMedia`). Vizyon Music never requests broad access to your device photo/video library.

### 1.3 User Safety, Blocking & Reporting
- You can block any user to immediately hide their profile, posts, stories, and direct messages, and you can unblock them anytime from **Hakkında & Gizlilik -> Engellenen Kullanıcılar**.
- You can submit user/content safety reports (`USER`, `POST`, `STORY`). Report records contain the reporter UID, reported target, category, and optional note for moderation review.

### 1.4 Real-Time Notifications
- With `POST_NOTIFICATIONS` permission and your notification preference enabled, Vizyon Music listens for incoming follow requests, message requests, likes, comments, reposts, stories, and direct/voice messages so alerts appear reliably even when the app is in the background. You can disable device notifications or background live notification listening at any time in **Hakkında & Gizlilik**.

---

## 2. Third-Party Services & APIs
Vizyon Music uses the following services strictly for core app functionality:
- **Firebase Authentication & Cloud Firestore:** Account sign-in, cloud backup, real-time social feed, requests, direct messages, and notifications.
- **YouTube Data API v3 & Official YouTube IFrame Player:** Legal music video playback inside the embedded player.
- **iTunes Search API & LRCLIB:** Music metadata catalog and time-synced lyrics.

Vizyon Music **does not** display third-party ads, **does not** include ad-tracking SDKs, and **never** sells personal data.

---

## 3. Data Security
- All network communications use **HTTPS / TLS** encryption in transit.
- Optional **Studio Security Vault** passcodes are salted and hashed with **SHA-256** and verified before any passcode change or unlock.

---

## 4. Data Retention & Account Deletion
- You can delete individual chat messages, entire chat threads, your posts, and your stories inside the app.
- To permanently delete your entire account and associated data:
  1. Open **Profil -> Profil Ayarları -> Hesabı Kalıcı Sil**.
  2. Confirm re-authentication if required and complete the security confirmation.
  3. All local databases and Firestore user records, posts, stories, requests, notifications, and authentication credentials are permanently deleted.

---

## 5. Children's Privacy
Vizyon Music is intended for users aged **13 and older** (`13+` onboarding age verification). We do not knowingly collect personal information from children under 13.

---

## 6. Contact
For privacy questions or repository inquiries, please open an issue in the project's GitHub repository.
