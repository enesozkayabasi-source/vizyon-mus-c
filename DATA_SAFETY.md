# Google Play Data Safety Form — Vizyon Music (`com.vizyon.music`)

This guide maps **Vizyon Music**'s exact runtime behavior, permissions, and Firebase/device storage architecture to the Google Play Console **Data safety** questionnaire so it can be submitted directly and reviewed alongside the GitHub repository.

---

## 1. Overview (Data Collection & Security)

| Play Console Question | Answer | Notes |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (only when user creates/uses social/cloud features) | Local-only playback works without login; Google Sign-In & Firestore sync are optional. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | All network calls (Firebase Auth, Cloud Firestore, YouTube Data API v3, iTunes Search API, LRCLIB) use **HTTPS / TLS**. |
| Do you provide a way for users to request that their data is deleted? | **Yes** | In-app **Hesabı Kalıcı Sil** flow (`Profile Settings -> AccountDeletionSecurityWall`) wipes Firebase Auth user + Firestore profile, posts, stories, DMs, requests, notifications, and local Room/DataStore state. |
| Independent security review? | **No** (Optional) | No third-party ad/analytics tracking SDKs are bundled. |

---

## 2. Data Types Collected (Play Console Categories)

### A. Personal Info
- **Name** (Görünen Ad & `@handle`) — **Optional** (user-entered during onboarding or Profile Edit).
  - *Purpose:* App functionality, Account management, Social profile display.
  - *Shared with third parties:* Only displayed to other Vizyon Music users when `discoverable` is enabled in Firestore `publicProfiles`.
- **Email Address** — **Optional** (collected only if user signs in with Google Sign-In via Jetpack `CredentialManager`).
  - *Purpose:* Account management & cross-device cloud backup authentication.
- **User IDs** (Firebase Auth UID / sanitized handle ID) — **Optional** (only for signed-in cloud features).
  - *Purpose:* App functionality, account management, social follow/DM/request routing, moderation reports, and blocking.

### B. Photos and Videos
- **Photos** (Profile Avatar & optional 24-hour Music Story card share PNG) — **Optional**.
  - Selected exclusively via zero-permission **Android Photo Picker** (`ActivityResultContracts.PickVisualMedia`). No `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE` permission is requested.
  - *Purpose:* App functionality (displaying user avatar alongside posts, stories, and direct chats).

### C. Audio Files
- **Voice or sound recordings** (Direct Message Voice Clips, max 30 seconds `.m4a` AAC) — **Optional**.
  - Recorded strictly on-demand when the user taps the microphone button in a Direct Message thread (`Manifest.permission.RECORD_AUDIO`).
  - Includes a local preview/play/discard step before sending.
  - *Purpose:* App functionality (Direct messaging & message requests between users).

### D. Messages
- **Other in-app messages** (Direct chat text messages, message requests, story notes, post comments, fan lounge messages) — **Optional**.
  - *Purpose:* App functionality & social interaction.
  - Users can delete individual messages, entire chat threads, or their own posts/stories at any time.

### E. App Activity & User-Generated Moderation Data
- **In-app search history / Recent tracks / Favorites / Playlists** — Stored locally in Room/DataStore and optionally synced to Firestore when signed in.
- **Blocked users list & User safety reports** — Used strictly for safety, abuse prevention, and user-controlled moderation.

---

## 3. Permissions & Justification

| Permission | Protection Level | When Requested | Purpose |
|---|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Normal | Install-time | Music catalog, lyrics, video playback, optional cloud sync |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `WAKE_LOCK` | Normal | Only when user enables background live notification service | Keeps realtime Firestore social/DM notification listener reliable when app is backgrounded |
| `RECEIVE_BOOT_COMPLETED` | Normal | System boot (signed-in users) | Restores realtime notification listener after device reboot |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | On user toggle or notification setup | Delivers follow, like, comment, DM, and request alerts |
| `RECORD_AUDIO` | Runtime | Only when tapping mic in DM thread | Records <=30s AAC voice messages with preview before send |

**Removed / Never Requested Permissions:**
- `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `ACCESS_FINE_LOCATION`, `CAMERA`.

---

## 4. Security Practices Implemented
- **Vault Lock Protection:** Passcodes are hashed locally and in cloud backup using **SHA-256 + per-user cryptographic salt** (`PasscodeSecurityHelper`), never stored in plaintext. Updating an existing passcode requires verifying the current passcode, and locked sessions block background UI taps and Back navigation.
- **No Ad SDKs / No Tracking / No Data Selling:** Zero advertising networks, zero cross-app tracking identifiers (`AD_ID`), and zero third-party data monetization.
- **Age Gate (13+):** Enforced during onboarding before social & cloud features activate.
