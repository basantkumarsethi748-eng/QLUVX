# QLUVX • Phase 4: Google Play Billing, Streamer Diamond Payouts, Web Admin Console & FCM Push Blueprint

This document covers the architecture, endpoints, and setup steps for the 4 core monetization & operation systems integrated into **QLUVX (`com.example.qluvx`)**.

---

## 1. Google Play In-App Billing (`BillingManager.java`)

### Architecture Flow
```text
[User taps Buy Coins] 
       │
       ▼
[Google Play Bottom Sheet opens (UPI / Card / Netbanking)]
       │
       ▼ (Payment Successful)
[BillingManager gets PurchaseToken]
       │
       ▼ (HTTPS POST /api/v1/wallet/verify-iap)
[Backend verifies token with Google Play Developer API]
       │
       ▼
[Backend atomically credits coins to user_wallets table in PostgreSQL]
```

### Supported SKU Packs
| SKU ID | Product Type | Price (INR) | Reward |
| :--- | :--- | :--- | :--- |
| `coins_bundle_100` | Consumable In-App (`INAPP`) | ₹89 | **100 Gold Coins** |
| `coins_bundle_500` | Consumable In-App (`INAPP`) | ₹399 | **500 Gold Coins** |
| `coins_bundle_2000` | Consumable In-App (`INAPP`) | ₹1,499 | **2,000 Gold Coins** |
| `vip_monthly_subscription` | Subscription (`SUBS`) | ₹499 / month | **Royal VIP Status + 1,000 Bonus Coins** |

### Key Implementation Files
- **Android Client**: `app/src/main/java/com/example/qluvx/BillingManager.java`
- **Retrofit Endpoint**: `POST /api/v1/wallet/verify-iap` in `ApiService.java`
- **Backend Verification**: `verifyGooglePlayIap` in `backend/src/controllers/payout.controller.ts`

---

## 2. Streamer Diamond Payout Engine (`payout.controller.ts`)

### Conversion & Withdrawal Rules
- **Conversion Rate**: `1 Diamond = ₹0.50 INR`
- **Minimum Withdrawal**: `5,000 Diamonds = ₹2,500.00 INR`
- **Supported Withdrawal Methods**:
  1. **UPI Instant Transfer** (`username@upi`)
  2. **Direct Bank IMPS / NEFT** (`Account Number + IFSC`)
  3. **PayPal International** (`email@domain.com`)

### Key Implementation Files
- **Backend Controller**: `backend/src/controllers/payout.controller.ts` (`requestDiamondPayout`, `approveDiamondPayout`)
- **Android UI**: `ProfileFragment.java` (`showDiamondPayoutDialog()` & `submitDiamondPayoutRequest()`)

---

## 3. Web Admin & Moderation Console (`admin_dashboard.html`)

### Features Included
1. **Live Metrics**:
   - Real-time Daily Active Users (**DAU**), Live Running Streams count, Daily Coin Sales Revenue (**₹ INR**), and Pending Diamond Payouts.
2. **Live Stream & PK Battle Moderation**:
   - Real-time list of active Agora video streams, 5-minute PK battles, and 8-seat voice rooms with a 1-click **"Terminate Stream"** kill switch.
3. **Google Play UGC User Reports Queue**:
   - Review reported user accounts with 1-click **"Ban Account"** or **"Dismiss"**.
4. **Streamer Payout Approvals**:
   - Verify and approve pending UPI, IMPS/NEFT, and PayPal withdrawal requests with 1-click **"Approve & Pay"**.

### Key Implementation Files
- **Standalone Web Console**: `/admin_dashboard.html`
- **In-App Embedded Asset**: `app/src/main/assets/admin_dashboard.html` (accessible directly from the **Me / VIP Profile** tab via **"🛡️ Open Web Admin & Moderation Console"**).

---

## 4. Firebase Cloud Messaging Push Alerts (`MyFirebaseMessagingService.java`)

### Supported High-Priority Push Notification Templates
1. **Secret Disappearing Photo**: `"Alex sent you a 5-second disappearing photo ⏱️"`
2. **VIP Live Broadcast**: `"🔥 Julian Vance just went live! Tap to watch"`
3. **Live PK Battle Challenge**: `"⚡ You have been challenged to a 5-minute PK battle!"`
4. **Luxury Virtual Gift**: `"🎉 You received a Super Sports Car from Leo!"`

### Key Implementation Files
- **Service Class**: `app/src/main/java/com/example/qluvx/MyFirebaseMessagingService.java`
- **Manifest Registration**: Declared in `app/src/main/AndroidManifest.xml` with `com.google.firebase.MESSAGING_EVENT`.
- **In-App Test Trigger**: Available in the **Me / VIP Profile** tab via **"🔔 Trigger FCM Push Notification Alerts"**.
