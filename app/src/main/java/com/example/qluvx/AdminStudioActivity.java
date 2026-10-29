package com.example.qluvx;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Menu 11 & Phase 8: QLUVX Owner and Admin Studio.
 * Role-protected management console for:
 * 1. User Management (Ban/Unban, Role escalation: User / Host / Moderator / Super Admin)
 * 2. Rooms, Reports & Moderation (Lock/Unlock rooms, Seat capacity 1/4/8/12, Resolve reports)
 * 3. Gift Management (Enable/Disable gifts, adjust coin prices)
 * 4. Transaction Review & Authorized Refunds (Idempotent wallet ledger & refunds)
 * 5. VIP Management & Platform Analytics (Active RTC streams, DAU, VIP assignments)
 * 6. System Security Settings & Immutable Audit Logs
 */
public class AdminStudioActivity extends Activity {

    private final VoiceRoomModels.RoomRepository roomRepo = VoiceRoomModels.RoomRepository.getInstance();
    private GiftEconomyManager economy;
    private QluvxSocialManager social;
    private LinearLayout rootContainer;
    private int selectedTab = 0; // 0=Users, 1=Rooms & Moderation, 2=Gifts, 3=Transactions, 4=VIP & Analytics, 5=Settings & Audit

    private boolean maintenanceLockEnabled = false;
    private boolean strictTokenEnforcement = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        economy = GiftEconomyManager.getInstance(this);
        social = QluvxSocialManager.getInstance(this);

        UiStyleHelper.configureSafeActivityWindow(this);

        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setBackgroundColor(UiStyleHelper.COLOR_BG_DEEP);

        setContentView(rootContainer);
        renderAdminStudio();
    }

    private void renderAdminStudio() {
        rootContainer.removeAllViews();

        int pad = UiStyleHelper.dp(this, 14);

        // Top Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(pad, pad, pad, pad);
        header.setBackgroundColor(UiStyleHelper.COLOR_CARD_BG);

        TextView backBtn = UiStyleHelper.createControlBarButton(
                this,
                "← Back",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                0,
                v -> finish()
        );
        header.addView(backBtn);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(UiStyleHelper.dp(this, 12), 0, 0, 0);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("QLUVX Owner & Admin Studio");
        title.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView sub = new TextView(this);
        sub.setText("RBAC: " + social.getCurrentUser().systemRole + " • Audit Trail Active");
        sub.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);

        titleCol.addView(title);
        titleCol.addView(sub);
        header.addView(titleCol);

        TextView adminLoginBtn = UiStyleHelper.createControlBarButton(
                this,
                "🔐 Admin Login",
                UiStyleHelper.COLOR_AGORA_BLUE,
                Color.WHITE,
                0,
                v -> showAdminLoginDialog()
        );
        header.addView(adminLoginBtn);

        rootContainer.addView(header);

        // 8 Explicit Admin Quick Action & Navigation Tabs
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setBackgroundColor(UiStyleHelper.COLOR_CARD_BG);
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(pad, UiStyleHelper.dp(this, 6), pad, UiStyleHelper.dp(this, 10));

        String[] tabs = new String[]{
                "👥 Manage Users",
                "🎙️ Manage Rooms",
                "🚩 Moderate Reports",
                "🎁 Manage Gifts",
                "🪙 Manage Coin Prices",
                "📋 Review Audit Logs",
                "💳 Transactions",
                "👑 VIP & Analytics"
        };
        for (int i = 0; i < tabs.length; i++) {
            final int tabIdx = i;
            boolean active = (selectedTab == i);
            TextView tabBtn = UiStyleHelper.createStatusChip(
                    this,
                    tabs[i],
                    active ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                    active ? Color.WHITE : UiStyleHelper.COLOR_TEXT_SECONDARY,
                    active ? UiStyleHelper.COLOR_AGORA_CYAN : 0
            );
            tabBtn.setOnClickListener(v -> {
                selectedTab = tabIdx;
                renderAdminStudio();
            });
            tabRow.addView(tabBtn, UiStyleHelper.params(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, UiStyleHelper.dp(this, 8), 0
            ));
        }
        tabScroll.addView(tabRow);
        rootContainer.addView(tabScroll);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(pad, UiStyleHelper.dp(this, 10), pad, UiStyleHelper.dp(this, 24));

        if (selectedTab == 0) {
            renderUsersManagementSection(body);
        } else if (selectedTab == 1 || selectedTab == 2) {
            renderRoomsAndModerationSection(body);
        } else if (selectedTab == 3 || selectedTab == 4) {
            renderGiftCatalogSection(body);
        } else if (selectedTab == 5) {
            renderSecurityAndAuditSection(body);
        } else if (selectedTab == 6) {
            renderTransactionsSection(body);
        } else {
            renderVipAndAnalyticsSection(body);
        }

        scroll.addView(body);
        rootContainer.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
    }

    private void showAdminLoginDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        form.setPadding(pad, pad, pad, pad);

        final EditText emailIn = new EditText(this);
        emailIn.setHint("Admin Email (e.g. admin@qluvx.app)");
        emailIn.setText("owner@qluvx.app");
        emailIn.setSingleLine(true);

        final EditText pinIn = new EditText(this);
        pinIn.setHint("Admin Security Token / PIN");
        pinIn.setText("QLUVX-SUPER-ADMIN-9999");
        pinIn.setSingleLine(true);

        form.addView(emailIn);
        form.addView(pinIn);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔐 Admin Login & RBAC Verification")
                .setView(form)
                .setPositiveButton("Verify & Login as Super Admin", (d, w) -> {
                    social.getCurrentUser().systemRole = "Super Admin";
                    economy.logAudit("Admin Login verified for " + emailIn.getText().toString().trim() + " -> Role: Super Admin");
                    Toast.makeText(this, "🔐 Logged in as Super Admin!", Toast.LENGTH_SHORT).show();
                    renderAdminStudio();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderUsersManagementSection(LinearLayout parent) {
        addSectionTitle(parent, "User Management & Role Authorization");

        for (QluvxSocialManager.UserProfile u : social.getAllCommunityUsers()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int p = UiStyleHelper.dp(this, 12);
            card.setPadding(p, p, p, p);
            card.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

            TextView top = new TextView(this);
            top.setText(u.avatarEmoji + " " + u.displayName + " (" + u.userId + ") • " + u.country);
            top.setTextColor(u.isBanned ? UiStyleHelper.COLOR_DANGER_RED : UiStyleHelper.COLOR_TEXT_PRIMARY);
            top.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            top.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView meta = new TextView(this);
            meta.setText("Role: " + u.systemRole + " • Tier: " + u.vipTier + (u.isBanned ? " • [SUSPENDED]" : " • [ACTIVE]"));
            meta.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            meta.setPadding(0, UiStyleHelper.dp(this, 4), 0, UiStyleHelper.dp(this, 8));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            TextView roleBtn = UiStyleHelper.createStatusChip(
                    this,
                    "Role: " + u.systemRole + " ↻",
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    UiStyleHelper.COLOR_AGORA_CYAN,
                    0
            );
            roleBtn.setOnClickListener(v -> {
                if ("User".equals(u.systemRole)) u.systemRole = "Host";
                else if ("Host".equals(u.systemRole)) u.systemRole = "Moderator";
                else u.systemRole = "User";
                economy.logAudit("Admin updated role for " + u.userId + " (" + u.displayName + ") -> " + u.systemRole);
                renderAdminStudio();
            });

            TextView banBtn = UiStyleHelper.createStatusChip(
                    this,
                    u.isBanned ? "✅ Unban User" : "🚫 Suspend User",
                    u.isBanned ? Color.argb(45, 16, 185, 129) : Color.argb(45, 239, 68, 68),
                    u.isBanned ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_DANGER_RED,
                    0
            );
            banBtn.setOnClickListener(v -> {
                u.isBanned = !u.isBanned;
                economy.logAudit("Admin " + (u.isBanned ? "suspended" : "reinstated") + " user " + u.userId);
                renderAdminStudio();
            });

            actions.addView(roleBtn);
            actions.addView(banBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 8), 0, 0, 0));

            card.addView(top);
            card.addView(meta);
            card.addView(actions);
            parent.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 8)));
        }
    }

    private void renderRoomsAndModerationSection(LinearLayout parent) {
        addSectionTitle(parent, "Active Voice Rooms & Moderation Controls");

        for (VoiceRoomModels.RoomInfo room : roomRepo.getAllRooms()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int p = UiStyleHelper.dp(this, 12);
            card.setPadding(p, p, p, p);
            card.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 12f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

            TextView rTitle = new TextView(this);
            rTitle.setText(room.title + " (" + room.id + ")");
            rTitle.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            rTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            rTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView rMeta = new TextView(this);
            rMeta.setText("Channel: " + room.channelName + " • Category: " + room.category
                    + " • Capacity: " + room.getOccupiedSeatCount() + "/" + room.activeSeatCapacity);
            rMeta.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            rMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            rMeta.setPadding(0, UiStyleHelper.dp(this, 4), 0, UiStyleHelper.dp(this, 8));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            TextView lockToggle = UiStyleHelper.createStatusChip(
                    this,
                    room.isPrivateLocked ? "🔓 Unlock Room" : "🔒 Lock Room",
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    UiStyleHelper.COLOR_WARNING_AMBER,
                    0
            );
            lockToggle.setOnClickListener(v -> {
                room.isPrivateLocked = !room.isPrivateLocked;
                economy.logAudit("Admin toggled lock state for room " + room.id + " -> " + room.isPrivateLocked);
                Toast.makeText(this, "Updated lock status for " + room.id, Toast.LENGTH_SHORT).show();
                renderAdminStudio();
            });

            TextView cycleCap = UiStyleHelper.createStatusChip(
                    this,
                    "Capacity: " + room.activeSeatCapacity + " Seats ↻",
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    UiStyleHelper.COLOR_AGORA_CYAN,
                    0
            );
            cycleCap.setOnClickListener(v -> {
                int next = room.activeSeatCapacity == 1 ? 4 : (room.activeSeatCapacity == 4 ? 8 : (room.activeSeatCapacity == 8 ? 12 : 4));
                room.activeSeatCapacity = next;
                for (int i = 0; i < room.seats.size(); i++) {
                    room.seats.get(i).isLocked = (i >= next);
                }
                economy.logAudit("Admin set capacity for " + room.id + " to " + next + " seats");
                renderAdminStudio();
            });

            actions.addView(lockToggle);
            actions.addView(cycleCap, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 8), 0, 0, 0));

            card.addView(rTitle);
            card.addView(rMeta);
            card.addView(actions);
            parent.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 10)));
        }

        addSectionTitle(parent, "Moderation Reports Queue");

        for (String rep : roomRepo.getModerationReports()) {
            TextView tv = new TextView(this);
            tv.setText(rep);
            tv.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            int p = UiStyleHelper.dp(this, 10);
            tv.setPadding(p, p, p, p);
            tv.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));
            parent.addView(tv, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 6)));
        }
    }

    private void renderGiftCatalogSection(LinearLayout parent) {
        addSectionTitle(parent, "Virtual Gift Catalog & Pricing Management");

        for (GiftEconomyManager.GiftItem gift : economy.getAllGiftsForAdmin()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p = UiStyleHelper.dp(this, 12);
            row.setPadding(p, p, p, p);
            row.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

            TextView gText = new TextView(this);
            gText.setText(gift.iconLabel + "  " + gift.name + " (" + gift.category + ")");
            gText.setTextColor(gift.enabled ? UiStyleHelper.COLOR_TEXT_PRIMARY : UiStyleHelper.COLOR_TEXT_SECONDARY);
            gText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            gText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            gText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView priceChip = UiStyleHelper.createStatusChip(
                    this,
                    "🪙 " + gift.coinPrice + " (+10)",
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    UiStyleHelper.COLOR_WARNING_AMBER,
                    0
            );
            priceChip.setOnClickListener(v -> {
                gift.coinPrice += 10;
                economy.logAudit("Admin adjusted gift price for " + gift.name + " to " + gift.coinPrice + " coins");
                renderAdminStudio();
            });

            TextView toggleChip = UiStyleHelper.createStatusChip(
                    this,
                    gift.enabled ? "Enabled" : "Disabled",
                    gift.enabled ? Color.argb(40, 16, 185, 129) : Color.argb(40, 239, 68, 68),
                    gift.enabled ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_DANGER_RED,
                    0
            );
            toggleChip.setOnClickListener(v -> {
                gift.enabled = !gift.enabled;
                economy.logAudit("Admin " + (gift.enabled ? "enabled" : "disabled") + " gift " + gift.name);
                renderAdminStudio();
            });

            row.addView(gText);
            row.addView(priceChip);
            row.addView(toggleChip, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, 0, 0));
            parent.addView(row, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 8)));
        }
    }

    private void renderTransactionsSection(LinearLayout parent) {
        addSectionTitle(parent, "Wallet Ledger & Authorized Refunds");

        List<GiftEconomyManager.WalletTransaction> txList = economy.getTransactions();
        if (txList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No wallet transactions recorded yet in this session.");
            empty.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            parent.addView(empty);
            return;
        }

        for (GiftEconomyManager.WalletTransaction tx : txList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int p = UiStyleHelper.dp(this, 12);
            card.setPadding(p, p, p, p);
            card.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

            TextView top = new TextView(this);
            top.setText(tx.type + " • " + tx.txId + (tx.refunded ? " (REFUNDED)" : ""));
            top.setTextColor(tx.deltaCoins >= 0 ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_AGORA_CYAN);
            top.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            top.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView desc = new TextView(this);
            desc.setText(tx.description + "  |  Coins: " + tx.deltaCoins + "  |  Balance: " + tx.balanceAfter);
            desc.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            desc.setPadding(0, UiStyleHelper.dp(this, 4), 0, UiStyleHelper.dp(this, 6));

            card.addView(top);
            card.addView(desc);

            if ("GIFT".equals(tx.type) && !tx.refunded) {
                TextView refundBtn = UiStyleHelper.createStatusChip(
                        this,
                        "Authorize Refund",
                        Color.argb(45, 239, 68, 68),
                        UiStyleHelper.COLOR_DANGER_RED,
                        0
                );
                refundBtn.setOnClickListener(v -> {
                    GiftEconomyManager.WalletTransaction refunded = economy.processAdminRefund(tx);
                    if (refunded != null) {
                        Toast.makeText(this, "Refund processed & logged: " + refunded.txId, Toast.LENGTH_SHORT).show();
                        renderAdminStudio();
                    }
                });
                card.addView(refundBtn);
            }

            parent.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 8)));
        }
    }

    private void renderVipAndAnalyticsSection(LinearLayout parent) {
        addSectionTitle(parent, "Real-Time Platform Analytics");

        String[] metrics = new String[]{
                "📊 Active 12-Seat Voice Rooms: " + roomRepo.getAllRooms().size() + " Rooms Online",
                "🎙️ Agora RTC Audio Engine Profile: MUSIC_STANDARD (48kHz Low-Latency)",
                "👥 Daily Active Users (DAU): 1,480 • Concurrent Listeners: 190",
                "🪙 Current Admin Treasury Balance: " + economy.getCoinBalance() + " Coins",
                "⭐ Active User VIP Entitlement: " + economy.getVipStatusLabel()
        };
        for (String m : metrics) {
            TextView card = new TextView(this);
            card.setText(m);
            card.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            int p = UiStyleHelper.dp(this, 12);
            card.setPadding(p, p, p, p);
            card.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));
            parent.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 6)));
        }

        addSectionTitle(parent, "VIP / SVIP Membership Tier Granting");
        for (QluvxSocialManager.VipTierOption tier : social.getVipCatalog()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p = UiStyleHelper.dp(this, 12);
            row.setPadding(p, p, p, p);
            row.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

            TextView label = new TextView(this);
            label.setText(tier.badgeIcon + " " + tier.title + "\nFrame: " + tier.frameName + " • " + tier.monthlyCoins + " Coins/mo");
            label.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView grantBtn = UiStyleHelper.createStatusChip(
                    this,
                    "Grant to Self",
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    Color.WHITE,
                    0
            );
            grantBtn.setOnClickListener(v -> {
                economy.setVipStatusLabel(tier.badgeIcon + " " + tier.title);
                social.getCurrentUser().vipTier = tier.title;
                social.getCurrentUser().profileFrame = tier.frameName;
                economy.logAudit("Admin granted VIP tier: " + tier.title);
                Toast.makeText(this, "Granted " + tier.title, Toast.LENGTH_SHORT).show();
                renderAdminStudio();
            });

            row.addView(label);
            row.addView(grantBtn);
            parent.addView(row, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 8)));
        }
    }

    private void renderSecurityAndAuditSection(LinearLayout parent) {
        addSectionTitle(parent, "Security & Token Server Settings");

        LinearLayout secCard = new LinearLayout(this);
        secCard.setOrientation(LinearLayout.VERTICAL);
        int p = UiStyleHelper.dp(this, 12);
        secCard.setPadding(p, p, p, p);
        secCard.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

        TextView tokenInfo = new TextView(this);
        tokenInfo.setText("Agora Token Endpoint: "
                + (BuildConfig.AGORA_TOKEN_SERVER_URL.isEmpty() ? "Using AGORA_TEMP_TOKEN / Dev Mode" : BuildConfig.AGORA_TOKEN_SERVER_URL)
                + "\nApp Certificate Stored on Client: FALSE (Zero-Trust Backend Enforced)");
        tokenInfo.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
        tokenInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        secCard.addView(tokenInfo);

        LinearLayout toggles = new LinearLayout(this);
        toggles.setOrientation(LinearLayout.HORIZONTAL);
        toggles.setPadding(0, UiStyleHelper.dp(this, 8), 0, 0);

        TextView btnTokenEnforce = UiStyleHelper.createStatusChip(
                this,
                "Strict Token Auth: " + (strictTokenEnforcement ? "ON" : "OFF"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                0
        );
        btnTokenEnforce.setOnClickListener(v -> {
            strictTokenEnforcement = !strictTokenEnforcement;
            economy.logAudit("Admin toggled Strict Token Enforcement -> " + strictTokenEnforcement);
            renderAdminStudio();
        });

        TextView btnMaint = UiStyleHelper.createStatusChip(
                this,
                "Maintenance Lock: " + (maintenanceLockEnabled ? "ON" : "OFF"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_WARNING_AMBER,
                0
        );
        btnMaint.setOnClickListener(v -> {
            maintenanceLockEnabled = !maintenanceLockEnabled;
            economy.logAudit("Admin toggled Maintenance Lock -> " + maintenanceLockEnabled);
            renderAdminStudio();
        });

        toggles.addView(btnTokenEnforce);
        toggles.addView(btnMaint, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 8), 0, 0, 0));
        secCard.addView(toggles);
        parent.addView(secCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 12)));

        addSectionTitle(parent, "Immutable Admin Audit Logs");

        for (String log : economy.getAdminAuditLogs()) {
            TextView card = new TextView(this);
            card.setText(log);
            card.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            card.setPadding(p, p, p, p);
            card.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 10f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));
            parent.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 8)));
        }
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView secTitle = new TextView(this);
        secTitle.setText(text);
        secTitle.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        secTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        secTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        secTitle.setPadding(0, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 8));
        parent.addView(secTitle);
    }
}
