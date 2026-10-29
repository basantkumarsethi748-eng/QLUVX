package com.example.qluvx;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class GiftEconomyManager {

    public static class GiftItem {
        public final String id;
        public String name;
        public final String iconLabel;
        public final String category; // Popular, Luxury, Combo
        public int coinPrice;
        public boolean enabled;

        public GiftItem(String id, String name, String iconLabel, String category, int coinPrice, boolean enabled) {
            this.id = id;
            this.name = name;
            this.iconLabel = iconLabel;
            this.category = category;
            this.coinPrice = coinPrice;
            this.enabled = enabled;
        }
    }

    public static class WalletTransaction {
        public final String txId;
        public final String type; // GIFT, RECHARGE, REFUND
        public final String description;
        public final int deltaCoins;
        public final int balanceAfter;
        public final String timestamp;
        public boolean refunded;

        public WalletTransaction(String txId, String type, String description, int deltaCoins, int balanceAfter, String timestamp) {
            this.txId = txId;
            this.type = type;
            this.description = description;
            this.deltaCoins = deltaCoins;
            this.balanceAfter = balanceAfter;
            this.timestamp = timestamp;
            this.refunded = false;
        }
    }

    private static final String PREF_NAME = "qluvx_wallet_store";
    private static final String KEY_COINS = "wallet_coins";
    private static final String KEY_VIP_TIER = "vip_tier";
    private static final int DEFAULT_INITIAL_COINS = 1500;

    private static GiftEconomyManager instance;

    private final SharedPreferences prefs;
    private final List<GiftItem> catalog = new ArrayList<>();
    private final List<WalletTransaction> transactions = new ArrayList<>();
    private final List<String> adminAuditLogs = new ArrayList<>();
    private final Set<String> idempotencyKeys = new HashSet<>();

    public static synchronized GiftEconomyManager getInstance(Context context) {
        if (instance == null) {
            instance = new GiftEconomyManager(context.getApplicationContext());
        }
        return instance;
    }

    private GiftEconomyManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_COINS)) {
            prefs.edit().putInt(KEY_COINS, DEFAULT_INITIAL_COINS).apply();
        }
        seedGiftCatalog();
        logAudit("Economy ledger initialized with idempotent transaction verification.");
    }

    private void seedGiftCatalog() {
        catalog.clear();
        catalog.add(new GiftItem("rose", "Rose", "🌹", "Popular", 10, true));
        catalog.add(new GiftItem("heart", "Heart", "❤️", "Popular", 20, true));
        catalog.add(new GiftItem("coffee", "Coffee", "☕", "Popular", 35, true));
        catalog.add(new GiftItem("teddy", "Teddy Bear", "🧸", "Popular", 80, true));
        catalog.add(new GiftItem("flowers", "Flowers", "💐", "Combo", 120, true));
        catalog.add(new GiftItem("crown", "Crown", "👑", "Combo", 250, true));
        catalog.add(new GiftItem("diamond", "Diamond", "💎", "Luxury", 500, true));
        catalog.add(new GiftItem("luxury_car", "Luxury Car", "🏎️", "Luxury", 1200, true));
        catalog.add(new GiftItem("royal_throne", "Royal Throne", "🪑", "Luxury", 2500, true));
        catalog.add(new GiftItem("love_castle", "Love Castle", "🏰", "Luxury", 5000, true));
    }

    public synchronized List<GiftItem> getGiftsByCategory(String categoryFilter) {
        List<GiftItem> result = new ArrayList<>();
        for (GiftItem item : catalog) {
            if (!item.enabled) continue;
            if (categoryFilter == null || categoryFilter.equalsIgnoreCase("All")
                    || item.category.equalsIgnoreCase(categoryFilter)) {
                result.add(item);
            }
        }
        return result;
    }

    public synchronized List<GiftItem> getAllGiftsForAdmin() {
        return new ArrayList<>(catalog);
    }

    public synchronized int getCoinBalance() {
        return prefs.getInt(KEY_COINS, DEFAULT_INITIAL_COINS);
    }

    public synchronized String getVipStatusLabel() {
        return prefs.getString(KEY_VIP_TIER, "VIP 1 • Verified Member");
    }

    public synchronized void setVipStatusLabel(String label) {
        prefs.edit().putString(KEY_VIP_TIER, label).apply();
        logAudit("Updated user entitlement status to: " + label);
    }

    public synchronized WalletTransaction sendGiftCombo(
            GiftItem gift,
            int comboMultiplier,
            String senderName,
            String recipientName,
            String idempotencyKey
    ) {
        if (gift == null || !gift.enabled) return null;
        int multiplier = (comboMultiplier == 5 || comboMultiplier == 10) ? comboMultiplier : 1;
        int totalCost = gift.coinPrice * multiplier;

        String reqKey = (idempotencyKey == null || idempotencyKey.isEmpty())
                ? UUID.randomUUID().toString()
                : idempotencyKey;
        if (idempotencyKeys.contains(reqKey)) {
            return null; // Replay / duplicate request blocked
        }

        int currentBalance = getCoinBalance();
        if (currentBalance < totalCost) {
            return null;
        }

        idempotencyKeys.add(reqKey);
        int updatedBalance = currentBalance - totalCost;
        prefs.edit().putInt(KEY_COINS, updatedBalance).apply();

        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String txId = "TX-" + reqKey.substring(0, Math.min(6, reqKey.length())).toUpperCase(Locale.US);
        String desc = senderName + " sent " + gift.iconLabel + " " + gift.name
                + " x" + multiplier + " to " + recipientName;

        WalletTransaction tx = new WalletTransaction(txId, "GIFT", desc, -totalCost, updatedBalance, time);
        transactions.add(0, tx);
        logAudit(txId + ": " + desc + " (" + (-totalCost) + " coins)");
        return tx;
    }

    public synchronized WalletTransaction rechargeVerifiedPack(int coins, String packName, String receiptId) {
        if (coins <= 0) return null;
        String key = (receiptId == null || receiptId.isEmpty()) ? UUID.randomUUID().toString() : receiptId;
        if (idempotencyKeys.contains(key)) {
            return null;
        }
        idempotencyKeys.add(key);

        int updated = getCoinBalance() + coins;
        prefs.edit().putInt(KEY_COINS, updated).apply();

        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String txId = "RC-" + key.substring(0, Math.min(6, key.length())).toUpperCase(Locale.US);
        String desc = "Verified Recharge: " + packName + " (+" + coins + " Coins)";
        WalletTransaction tx = new WalletTransaction(txId, "RECHARGE", desc, coins, updated, time);
        transactions.add(0, tx);
        logAudit(txId + ": " + desc);
        return tx;
    }

    public synchronized WalletTransaction processAdminRefund(WalletTransaction originalTx) {
        if (originalTx == null || originalTx.refunded || originalTx.deltaCoins >= 0) {
            return null;
        }
        originalTx.refunded = true;
        int refundAmount = Math.abs(originalTx.deltaCoins);
        int updated = getCoinBalance() + refundAmount;
        prefs.edit().putInt(KEY_COINS, updated).apply();

        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String txId = "RF-" + originalTx.txId.replace("TX-", "");
        String desc = "Admin Refund for " + originalTx.txId + " (+" + refundAmount + " Coins)";
        WalletTransaction refundTx = new WalletTransaction(txId, "REFUND", desc, refundAmount, updated, time);
        transactions.add(0, refundTx);
        logAudit("Admin processed refund " + txId + " for +" + refundAmount + " coins.");
        return refundTx;
    }

    public synchronized List<WalletTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public synchronized void logAudit(String entry) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        adminAuditLogs.add(0, "[" + time + "] " + entry);
    }

    public synchronized List<String> getAdminAuditLogs() {
        return new ArrayList<>(adminAuditLogs);
    }
}
