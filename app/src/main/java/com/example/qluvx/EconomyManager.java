package com.example.qluvx;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class EconomyManager {

    public static class GiftItem {
        public final String id;
        public final String category;
        public final String emoji;
        public final String name;
        public final int priceCoins;

        public GiftItem(String id, String category, String emoji, String name, int priceCoins) {
            this.id = id;
            this.category = category;
            this.emoji = emoji;
            this.name = name;
            this.priceCoins = priceCoins;
        }
    }

    public static class WalletTransaction {
        public final String idempotencyKey;
        public final String type; // "GIFT_SENT", "GIFT_RECEIVED", "WALLET_INIT"
        public final String description;
        public final int deltaCoins;
        public final int balanceAfter;
        public final String validationState; // "SERVER_VALIDATED" or "LOCAL_PREVIEW_LEDGER"
        public final String timestamp;

        public WalletTransaction(String idempotencyKey, String type, String description,
                                 int deltaCoins, int balanceAfter, String validationState) {
            this.idempotencyKey = idempotencyKey;
            this.type = type;
            this.description = description;
            this.deltaCoins = deltaCoins;
            this.balanceAfter = balanceAfter;
            this.validationState = validationState;
            this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        }
    }

    public interface GiftCallback {
        void onGiftCompleted(boolean success, String statusMessage, GiftItem gift, String recipientName, String validationMode);
    }

    public static final GiftItem[] GIFT_CATALOGUE = new GiftItem[]{
            new GiftItem("gift_rose", "Popular", "🌹", "Neon Rose", 10),
            new GiftItem("gift_fire_mic", "Popular", "🔥", "Fire Mic", 25),
            new GiftItem("gift_heart", "Popular", "💖", "Heart Burst", 50),
            new GiftItem("gift_guitar", "Stage", "🎸", "Electric Guitar", 150),
            new GiftItem("gift_crown", "Luxury", "👑", "Golden Crown", 200),
            new GiftItem("gift_rocket", "Luxury", "🚀", "Cyber Rocket", 500),
            new GiftItem("gift_supercar", "Luxury", "🏎️", "Neon Supercar", 1000),
            new GiftItem("gift_trophy", "Stage", "🏆", "Diamond Stage Trophy", 2500)
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int coinBalance = 1500;
    private final Set<String> processedIdempotencyKeys = new HashSet<>();
    private final List<WalletTransaction> transactions = new ArrayList<>();
    private long lastGiftActionTimestampMs = 0L;

    public EconomyManager() {
        String initKey = "init-" + UUID.randomUUID().toString().substring(0, 8);
        processedIdempotencyKeys.add(initKey);
        transactions.add(new WalletTransaction(
                initKey,
                "WALLET_INIT",
                "Initial Preview Test Balance",
                1500,
                1500,
                isBackendConfigured() ? "PENDING_SERVER_SYNC" : "LOCAL_PREVIEW_LEDGER"
        ));
    }

    public static boolean isBackendConfigured() {
        return !AgoraVoiceManager.getConfiguredBackendApiUrl().isEmpty();
    }

    public synchronized int getCoinBalance() {
        return coinBalance;
    }

    public synchronized List<WalletTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public static List<GiftItem> getGiftsByCategory(String category) {
        List<GiftItem> list = new ArrayList<>();
        for (GiftItem item : GIFT_CATALOGUE) {
            if (category == null || category.equalsIgnoreCase("All") || item.category.equalsIgnoreCase(category)) {
                list.add(item);
            }
        }
        return list;
    }

    public synchronized void sendGiftWithDuplicateProtection(
            String idempotencyKey,
            GiftItem gift,
            int senderUid,
            String senderName,
            int recipientUid,
            String recipientName,
            String channelName,
            GiftCallback callback
    ) {
        // 1. Duplicate-payment / rapid double-tap protection
        long now = System.currentTimeMillis();
        if (processedIdempotencyKeys.contains(idempotencyKey)) {
            callback.onGiftCompleted(false, "Duplicate transaction blocked (Idempotency Key " + idempotencyKey + " already processed).", gift, recipientName, "BLOCKED_DUPLICATE");
            return;
        }
        if (now - lastGiftActionTimestampMs < 600) {
            callback.onGiftCompleted(false, "Duplicate-payment protection: Please wait a moment before sending another gift.", gift, recipientName, "RATE_LIMITED");
            return;
        }
        if (coinBalance < gift.priceCoins) {
            callback.onGiftCompleted(false, "Insufficient wallet balance (" + coinBalance + " coins available, " + gift.priceCoins + " required).", gift, recipientName, "INSUFFICIENT_FUNDS");
            return;
        }

        lastGiftActionTimestampMs = now;
        processedIdempotencyKeys.add(idempotencyKey);

        String backendUrl = AgoraVoiceManager.getConfiguredBackendApiUrl();
        if (!backendUrl.isEmpty()) {
            // Validate transaction on configured backend server
            new Thread(() -> {
                boolean serverOk = false;
                String serverMsg = "";
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(backendUrl + (backendUrl.endsWith("/") ? "wallet/gift" : "/wallet/gift"));
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("X-Idempotency-Key", idempotencyKey);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("idempotencyKey", idempotencyKey);
                    body.put("giftId", gift.id);
                    body.put("priceCoins", gift.priceCoins);
                    body.put("senderUid", senderUid);
                    body.put("recipientUid", recipientUid);
                    body.put("channelName", channelName);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        serverOk = true;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        JSONObject resp = new JSONObject(sb.toString());
                        if (resp.has("balance")) {
                            synchronized (EconomyManager.this) {
                                coinBalance = resp.optInt("balance", coinBalance - gift.priceCoins);
                            }
                        } else {
                            synchronized (EconomyManager.this) {
                                coinBalance -= gift.priceCoins;
                            }
                        }
                    } else {
                        serverMsg = "Backend returned HTTP " + code;
                    }
                } catch (Throwable e) {
                    serverMsg = "Backend unreachable (" + e.getMessage() + ")";
                } finally {
                    if (conn != null) conn.disconnect();
                }

                final boolean finalOk = serverOk;
                final String finalMsg = serverMsg;
                mainHandler.post(() -> {
                    synchronized (EconomyManager.this) {
                        if (finalOk) {
                            transactions.add(0, new WalletTransaction(
                                    idempotencyKey,
                                    "GIFT_SENT",
                                    "Sent " + gift.emoji + " " + gift.name + " to " + recipientName,
                                    -gift.priceCoins,
                                    coinBalance,
                                    "SERVER_VALIDATED"
                            ));
                            callback.onGiftCompleted(true, "Server-validated gift sent!", gift, recipientName, "SERVER_VALIDATED");
                        } else {
                            processedIdempotencyKeys.remove(idempotencyKey);
                            callback.onGiftCompleted(false, "Backend validation failed: " + finalMsg, gift, recipientName, "SERVER_REJECTED");
                        }
                    }
                });
            }, "GiftValidator").start();
        } else {
            // Local Preview Ledger mode (explicitly labeled as Local Preview Ledger since QLUVX_BACKEND_API_URL is not configured)
            coinBalance -= gift.priceCoins;
            transactions.add(0, new WalletTransaction(
                    idempotencyKey,
                    "GIFT_SENT",
                    "Sent " + gift.emoji + " " + gift.name + " to " + recipientName,
                    -gift.priceCoins,
                    coinBalance,
                    "LOCAL_PREVIEW_LEDGER"
            ));
            callback.onGiftCompleted(
                    true,
                    "Recorded in Local Preview Ledger (Set QLUVX_BACKEND_API_URL in Secrets for server-validated billing).",
                    gift,
                    recipientName,
                    "LOCAL_PREVIEW_LEDGER"
            );
        }
    }

    public synchronized void recordIncomingRemoteGift(String idempotencyKey, String giftEmoji, String giftName, int coins, String senderName) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            if (processedIdempotencyKeys.contains(idempotencyKey)) return;
            processedIdempotencyKeys.add(idempotencyKey);
        }
        transactions.add(0, new WalletTransaction(
                idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString().substring(0, 8),
                "GIFT_EVENT",
                senderName + " sent " + giftEmoji + " " + giftName + " (" + coins + " coins)",
                0,
                coinBalance,
                "AGORA_DATASTREAM_EVENT"
        ));
    }
}
