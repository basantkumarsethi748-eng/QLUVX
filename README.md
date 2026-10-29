# QLUVX — Real-Time Agora Voice, Live Video & Social Android Application (`com.example.qluvx`)

QLUVX is a native **Java + XML** Android application (`minSdk 26`, `compileSdk 35`, `targetSdk 35`) built on the official **Agora Android RTC SDK (`io.agora.rtc:voice-sdk:4.5.0`)** with a secure **Node.js/Express token and wallet backend (`/backend/server.js`)** and **Firestore RBAC rules (`/backend/firestore.rules`)**.

---

## 1. Architecture & 8-Phase Implementation Summary

| Phase | Scope | Key Files |
| :--- | :--- | :--- |
| **Phase 1** | Complete Java/XML project structure & 12-Menu Navigation Hub | `app/build.gradle`, `AndroidManifest.xml`, `res/layout/activity_main.xml`, `MainActivity.java` |
| **Phase 2** | Authentication & User Profile (User ID, avatar, bio, followers, auth session) | `QluvxSocialManager.java`, `MainActivity.java` (Menu 6) |
| **Phase 3** | Real Agora Two-Client Voice-Room Test (`RtcEngine`, `joinChannel`, `onAudioVolumeIndication`, token renewal) | `VoiceRoomActivity.java`, `TokenServiceClient.java`, `AgoraVoiceManager.java` |
| **Phase 4** | 12 Microphone Seats (`SeatView` `@id/seat_1`..`@id/seat_12`), Host Crown, Seat Lock/Unlock, Capacity (`1/4/8/12`), Invitations & Permissions | `SeatView.java`, `res/layout/activity_voice_room.xml`, `VoiceRoomActivity.java`, `VoiceRoomModels.java` |
| **Phase 5** | Chat & Messaging (Private 1-on-1 DM, Room Chat, Emojis, Stickers, Image Picker, Voice Notes) | `QluvxSocialManager.java`, `MainActivity.java` (Menu 5), `VoiceRoomActivity.java` |
| **Phase 6** | Gifts (`Popular`, `Combo`, `Luxury` catalog, `x1/x5/x10` combos), Idempotent Coin Wallet & VIP/SVIP Tiers | `GiftEconomyManager.java`, `QluvxSocialManager.java`, `MainActivity.java` (Menus 7, 8, 9) |
| **Phase 7** | Live Video Stage, PK Battles & Playable Game Center (**Ludo 2/4P**, **Carrom**, **8 Ball Pool**) | `LiveVideoActivity.java`, `res/layout/activity_live_video.xml`, `GameCenterActivity.java`, `res/layout/activity_game_center.xml` |
| **Phase 8** | Owner/Admin Studio (Users, Moderation, Gifts, Ledger Refunds, VIP/Analytics, Security & Audit Logs) + Backend | `AdminStudioActivity.java`, `backend/server.js`, `backend/firestore.rules` |

---

## 2. All 12 Menus Included in QLUVX

1. **Home** — Recommended, Live Now, Popular Rooms, Voice Rooms, Live Video, New Users, Following, Trending, and Game Center.
2. **Discover** — Search users and rooms, nearby users (`<2.5 km`), countries filter (`All`, `India`, `USA`, `UAE`, `UK`, `Indonesia`, `Brazil`), categories, and filters.
3. **Live (12-Seat)** — Agora voice rooms with 12 custom `SeatView` microphone seats, Seat 1 Host Crown, listeners, speaker requests/invitations, mic mute/unmute, speaker/earpiece routing, seat permissions, and room chat.
4. **Live Video** — Dual video stage (Camera preview + Agora stream), live scrolling comments, viewer count, floating reactions, and real-time PK battle scoreboard.
5. **Chat** — Private messages, room chat sync, emojis, animated stickers, image attachments, and playable voice notes.
6. **Profile** — User ID, profile photo avatar, bio, followers/following, account authentication (`Sign In / Sign Out`), and profile editing.
7. **Wallet** — Coin balance, verified recharge packs (`+500`, `+1,500`, `+5,000`, `+12,000`), idempotent transaction history, and gift spending log.
8. **Gifts** — Full gift catalog (`Rose`, `Heart`, `Coffee`, `Teddy Bear`, `Flowers`, `Crown`, `Diamond`, `Luxury Car`, `Royal Throne`, `Love Castle`) with `x1 / x5 / x10` combo multipliers.
9. **VIP/SVIP** — `VIP 1 Silver`, `VIP 2 Gold`, `VIP 3 Diamond`, and `SVIP Royal Emperor` memberships, badges, profile frames, and benefits.
10. **Game Center** — Playable **Ludo (2 / 4 Players)**, **Carrom** (slingshot striker + pocket physics), and **8 Ball Pool** (cue aiming + 6-pocket physics).
11. **Owner/Admin Studio** — Role-protected console for User management, Room & Report moderation, Gift pricing, Transaction refunds, VIP granting, Analytics, and Immutable Audit Logs.
12. **Settings** — Privacy toggles, push/gift notifications, language selector, runtime permissions verifier (`RECORD_AUDIO`, `CAMERA`), support tickets, and account deletion.

---

## 3. How to Configure Agora & Environment Variables

> **Security Rule**: Never place your `AGORA_APP_CERTIFICATE` inside the Android client source code. The App Certificate belongs **only** on the backend token server (`/backend/server.js`).

### Step 1: Get Your Agora Credentials
1. Sign in to [Agora Console](https://console.agora.io/).
2. Create a project and copy your **App ID** (`AGORA_APP_ID`).
3. For quick two-phone testing before deploying the backend server, you can generate a temporary RTC token (`AGORA_TEMP_TOKEN`) in Agora Console for channel `qluvx_live_1`.
4. For production token generation, copy your **Primary App Certificate** (`AGORA_APP_CERTIFICATE`) for use in `/backend/server.js`.

### Step 2: Configure Android Secrets (`.env` or AI Studio Secrets Panel)
Set the following keys in the **AI Studio Secrets panel** (or `.env` in the project root; see `.env.example`):

```env
AGORA_APP_ID=your_32_char_agora_app_id
AGORA_TEMP_TOKEN=optional_temp_token_for_dev_testing
AGORA_DEFAULT_CHANNEL=qluvx_live_1
AGORA_TOKEN_SERVER_URL=https://your-token-backend.example.com
```

`app/build.gradle` automatically injects these values into `BuildConfig.AGORA_APP_ID`, `BuildConfig.AGORA_TEMP_TOKEN`, `BuildConfig.AGORA_DEFAULT_CHANNEL`, and `BuildConfig.AGORA_TOKEN_SERVER_URL`.

---

## 4. Deploying the Secure Agora Token & Wallet Backend (`/backend`)

The `/backend` folder contains a ready-to-deploy Node.js + Express backend (`backend/server.js`) using `agora-access-token` (`RtcTokenBuilder`).

```bash
cd backend
npm install
export AGORA_APP_ID="your_agora_app_id"
export AGORA_APP_CERTIFICATE="your_agora_app_certificate"
export PORT=8080
npm start
```

### Backend Endpoints
- `GET /health` — Health & configuration check.
- `POST /api/agora/token` — Validates user/channel/role (`publisher` or `subscriber`) and issues a short-lived `RtcTokenBuilder` token.
- `POST /api/wallet/gift` — Server-side idempotent gift deduction and ledger entry.
- `POST /api/admin/refund` — Role-protected admin refund processor with audit logging.

---

## 5. Building the APK & Testing on Physical Android Phones

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The generated APK is output to:
`app/build/outputs/apk/debug/app-debug.apk`

### Two-Phone Real Agora Voice Test (Phase 3 & 4 Verification)
1. Install `app-debug.apk` on **Phone A** and **Phone B** (or run one on the streaming emulator and one on a physical Android phone).
2. Ensure `AGORA_APP_ID` is set and both devices join the same channel (`qluvx_live_1`).
3. Tap **3. Live (12-Seat)** -> **Join 12-Seat Test Channel** (or tap any room card on **1. Home**).
4. Grant `RECORD_AUDIO` permission when prompted.
5. Verify:
   - Top status bar transitions to `CONNECTED` and shows real-time RTT (`ms`).
   - Speaking into Phone A's microphone triggers `onAudioVolumeIndication` and animates the active speaker ring around Seat 1 (`@id/seat_1`) while transmitting live audio to Phone B.
   - Tapping any of seats `@id/seat_1`..`@id/seat_12` lets the Host/Participant take a seat, mute/unmute, lock/unlock seats, or switch audio route (`Speaker` / `Earpiece`).
