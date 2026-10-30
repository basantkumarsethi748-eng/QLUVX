package com.example.qluvx;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

/**
 * Walla / Blued Luxury Live Streaming, 8-Seat Circular Voice Party, Dual-Host PK Battle,
 * Mini Profile Modal, Luxury Entrance Marquee, In-Room Lucky Wheel, 5-Second Disappearing Photo,
 * Vanity Mall, Agency Guild & Owner Control Panel Suite.
 */
public class QluvxLuxuryGamificationController {

    // Age & Role Tags (Top / Bottom / Versatile) for Tab 1 Nearby 3-column Royal Cards
    private static final String[] AGE_ROLE_TAGS = new String[]{
            "24 • Top 🔼",
            "22 • Bottom 🔽",
            "25 • Versatile 🔄",
            "27 • Top 🔼",
            "21 • Versatile 🔄",
            "26 • Bottom 🔽",
            "23 • Top 🔼",
            "28 • Versatile 🔄",
            "24 • Bottom 🔽",
            "25 • Top 🔼",
            "22 • Versatile 🔄",
            "29 • Top 🔼"
    };

    private static String selectedRoleFilter = "All"; // "All", "Top", "Bottom", "Versatile"

    // 8-Seat Circular Voice Party State
    public static class VoiceSeatState {
        public final int seatNumber;
        public String occupantName;
        public String occupantId;
        public int avatarResId;
        public String wealthLevel;
        public String avatarFrame;
        public String rideName;
        public String ageRole;
        public boolean isSpeaking;
        public boolean isMuted;
        public boolean isLocked;
        public boolean isFollowing;

        public VoiceSeatState(
                int seatNumber,
                String occupantName,
                String occupantId,
                int avatarResId,
                String wealthLevel,
                String avatarFrame,
                String rideName,
                String ageRole,
                boolean isSpeaking
        ) {
            this.seatNumber = seatNumber;
            this.occupantName = occupantName;
            this.occupantId = occupantId;
            this.avatarResId = avatarResId;
            this.wealthLevel = wealthLevel;
            this.avatarFrame = avatarFrame;
            this.rideName = rideName;
            this.ageRole = ageRole;
            this.isSpeaking = isSpeaking;
            this.isMuted = false;
            this.isLocked = false;
            this.isFollowing = (seatNumber % 2 == 1);
        }
    }

    private static VoiceSeatState[] eightSeats;
    private static int roomThemeIndex = 0; // 0 = Cyber Galaxy, 1 = Royal Gold, 2 = Velvet Rose
    private static final String[] ROOM_THEME_NAMES = new String[]{
            "🌌 Cyber Galaxy",
            "👑 Royal Gold",
            "🌹 Velvet Rose"
    };

    // VIP Entrance Marquee Messages
    private static int vipEntranceCursor = 0;
    private static final String[] VIP_ENTRANCE_MESSAGES = new String[]{
            "👑 [Lv.50] Julian Vance entered in Interstellar Starship X 🚀",
            "🐉 [Lv.50] Emperor Aryan entered in Imperial Gold Dragon Chariot ✨",
            "🌹 [Lv.48] Zoya Sterling entered in Celestial Phoenix Wing 🔥",
            "💎 [Lv.49] Kabir Royal entered in Cyber Nebula Hypercar 🏎️"
    };

    // Live PK Battle State
    private static int pkRedScore = 6850;
    private static int pkBlueScore = 5420;
    private static long pkRemainingSeconds = 298; // 04:58
    private static CountDownTimer activePkTimer;
    private static String lastFloatingGiftBanner = "👑 Julian Vance sent 🚀 Interstellar Jet x1 (+999 PK Pts)";

    // Lucky Wheel State
    private static int selectedBetChip = 50; // 20, 50, or 100 coins

    // VIP Profile Equipped Vanity State
    public static String equippedAvatarFrame = "🐉 Imperial Gold Dragon Frame";
    public static String equippedWealthBadge = "💎 Lv.50 Emperor";
    public static String equippedVehicleBadge = "🚀 Interstellar Starship X";
    public static String myAgeRoleTag = "24 • Versatile 🔄";

    public static String getAgeRoleForIndex(int index) {
        return AGE_ROLE_TAGS[Math.abs(index) % AGE_ROLE_TAGS.length];
    }

    public static String getSelectedRoleFilter() {
        return selectedRoleFilter;
    }

    public static void setSelectedRoleFilter(String filter) {
        selectedRoleFilter = filter;
    }

    public static boolean matchesRoleFilter(int index) {
        if ("All".equalsIgnoreCase(selectedRoleFilter)) return true;
        String tag = getAgeRoleForIndex(index);
        return tag.toLowerCase(Locale.US).contains(selectedRoleFilter.toLowerCase(Locale.US));
    }

    private static VoiceSeatState[] getEightSeats() {
        if (eightSeats == null) {
            eightSeats = new VoiceSeatState[]{
                    new VoiceSeatState(1, "Julian Vance", "100501", R.drawable.img_avatar_0, "Lv.50", "🐉 Imperial Gold Dragon", "🚀 Interstellar Starship X", "25 • Top 🔼", true),
                    new VoiceSeatState(2, "Aryan Royal", "102938", R.drawable.img_avatar_1, "Lv.48", "👑 Royal Gold Crown", "🏎️ Cyber Roadster", "24 • Versatile 🔄", true),
                    new VoiceSeatState(3, "Ishani ✨", "309481", R.drawable.img_avatar_2, "Lv.45", "🌹 Velvet Rose Halo", "🦢 Crystal Swan", "22 • Bottom 🔽", false),
                    new VoiceSeatState(4, "Rohan PK", "401928", R.drawable.img_avatar_3, "Lv.42", "⚡ Thunder Neon Frame", "🚁 Royal Chopper", "27 • Top 🔼", true),
                    new VoiceSeatState(5, "Komolika", "508172", R.drawable.img_avatar_4, "Lv.46", "🐉 Imperial Gold Dragon", "🚀 Interstellar Starship X", "23 • Versatile 🔄", false),
                    new VoiceSeatState(6, "Zoya Khan", "609283", R.drawable.img_avatar_5, "Lv.44", "💎 Diamond Starlight", "🏎️ Golden Bugatti", "26 • Bottom 🔽", true),
                    new VoiceSeatState(7, "Kabir Beats", "701829", R.drawable.img_avatar_0, "Lv.39", "🔥 Phoenix Flame", "🛸 Nebula Cruiser", "24 • Top 🔼", false),
                    new VoiceSeatState(8, "Doyun KR", "904811", R.drawable.img_avatar_2, "Lv.47", "🐉 Imperial Gold Dragon", "🚀 Interstellar Starship X", "25 • Versatile 🔄", true)
            };
        }
        return eightSeats;
    }

    private static int getRoomThemeStartColor() {
        if (roomThemeIndex == 1) return Color.rgb(68, 40, 10); // Royal Gold
        if (roomThemeIndex == 2) return Color.rgb(72, 16, 48); // Velvet Rose
        return Color.rgb(16, 26, 62); // Cyber Galaxy
    }

    private static int getRoomThemeEndColor() {
        if (roomThemeIndex == 1) return Color.rgb(28, 18, 8);
        if (roomThemeIndex == 2) return Color.rgb(28, 10, 26);
        return Color.rgb(10, 14, 32);
    }

    private static int getRoomThemeBorderColor() {
        if (roomThemeIndex == 1) return Color.rgb(253, 224, 71);
        if (roomThemeIndex == 2) return Color.rgb(244, 114, 182);
        return Color.rgb(56, 189, 248);
    }

    // =========================================================================
    // 1. LUXURY ENTRANCE MARQUEE BANNER
    //    "👑 [Lv.50] Julian Vance entered in Interstellar Starship X 🚀"
    // =========================================================================
    public static View buildLuxuryEntranceBanner(
            final Activity activity,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        int p10 = UiStyleHelper.dp(activity, 10);
        banner.setPadding(p10, UiStyleHelper.dp(activity, 7), p10, UiStyleHelper.dp(activity, 7));
        banner.setBackground(UiStyleHelper.gradientBox(
                activity,
                Color.rgb(120, 62, 10),
                Color.rgb(180, 92, 14),
                14f,
                Color.rgb(253, 224, 71),
                1.4f
        ));

        final TextView marqueeTv = new TextView(activity);
        marqueeTv.setText(VIP_ENTRANCE_MESSAGES[vipEntranceCursor % VIP_ENTRANCE_MESSAGES.length]);
        marqueeTv.setSingleLine(true);
        marqueeTv.setTextColor(Color.rgb(254, 249, 195));
        marqueeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        marqueeTv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        marqueeTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        banner.addView(marqueeTv);

        TextView btnReplay = new TextView(activity);
        btnReplay.setText("🚀 VIP Entry");
        btnReplay.setTextColor(Color.rgb(15, 23, 42));
        btnReplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        btnReplay.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        btnReplay.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 3), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 3));
        btnReplay.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(253, 224, 71), 10f, Color.WHITE, 1f));
        btnReplay.setOnClickListener(v -> {
            vipEntranceCursor++;
            marqueeTv.setText(VIP_ENTRANCE_MESSAGES[vipEntranceCursor % VIP_ENTRANCE_MESSAGES.length]);
            ObjectAnimator slide = ObjectAnimator.ofFloat(banner, "translationX", 180f, 0f);
            slide.setDuration(420);
            slide.setInterpolator(new DecelerateInterpolator());
            slide.start();
            playSoundEffect(ToneGenerator.TONE_PROP_BEEP2, 160);
        });
        banner.addView(btnReplay, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 6), 0, 0, 0));

        // Subtle entrance slide on render
        ObjectAnimator initialSlide = ObjectAnimator.ofFloat(banner, "translationX", 90f, 0f);
        initialSlide.setDuration(360);
        initialSlide.setInterpolator(new DecelerateInterpolator());
        initialSlide.start();

        return banner;
    }

    // =========================================================================
    // 2. TAB 1 ROLE FILTER BAR & SWIPE MATCH DECK LAUNCHER
    // =========================================================================
    public static View buildRoleFilterAndSwipeBar(
            final Activity activity,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));

        String[][] roles = new String[][]{
                {"All", "👥 All Roles"},
                {"Top", "🔼 Top"},
                {"Bottom", "🔽 Bottom"},
                {"Versatile", "🔄 Versatile"}
        };

        for (final String[] r : roles) {
            boolean active = r[0].equalsIgnoreCase(selectedRoleFilter);
            TextView chip = UiStyleHelper.createStatusChip(
                    activity,
                    r[1],
                    active ? QluvxFiveTabHubController.getLuxuryButtonStart() : Color.rgb(28, 32, 46),
                    Color.WHITE,
                    active ? Color.rgb(253, 224, 71) : QluvxFiveTabHubController.getLuxuryBorderAccent()
            );
            chip.setOnClickListener(v -> {
                selectedRoleFilter = r[0];
                callbacks.onRefreshScreen();
            });
            row.addView(chip, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        }

        TextView swipeDeckBtn = UiStyleHelper.createStatusChip(
                activity,
                "🔥 Swipe Match Deck",
                Color.rgb(225, 29, 72),
                Color.WHITE,
                Color.rgb(253, 224, 71)
        );
        swipeDeckBtn.setOnClickListener(v -> showSwipeMatchDeckDialog(activity, social, economy, callbacks));
        row.addView(swipeDeckBtn);

        scroll.addView(row);
        return scroll;
    }

    public static void showSwipeMatchDeckDialog(
            final Activity activity,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        final VoiceSeatState[] candidates = getEightSeats();
        final int[] cursor = new int[]{0};

        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.gradientBox(activity, getRoomThemeStartColor(), getRoomThemeEndColor(), 20f, Color.rgb(253, 224, 71), 1.5f));

        final Runnable[] renderCard = new Runnable[1];
        renderCard[0] = () -> {
            root.removeAllViews();
            final VoiceSeatState c = candidates[cursor[0] % candidates.length];

            TextView header = new TextView(activity);
            header.setText("🔥 Walla/Blued Swipe Match Deck");
            header.setTextColor(Color.rgb(253, 224, 71));
            header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
            header.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            header.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
            root.addView(header);

            ImageView iv = new ImageView(activity);
            iv.setImageResource(c.avatarResId);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(activity, 220)));

            TextView tvName = new TextView(activity);
            tvName.setText(c.occupantName + "  •  " + c.ageRole);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            tvName.setPadding(0, UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 2));
            root.addView(tvName);

            TextView tvMeta = new TextView(activity);
            tvMeta.setText("ID: " + c.occupantId + "  •  💎 " + c.wealthLevel + "  •  " + c.rideName);
            tvMeta.setTextColor(Color.rgb(56, 189, 248));
            tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            tvMeta.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 12));
            root.addView(tvMeta);

            LinearLayout btns = new LinearLayout(activity);
            btns.setOrientation(LinearLayout.HORIZONTAL);
            btns.setWeightSum(3f);

            TextView btnPass = UiStyleHelper.createControlBarButton(activity, "✕ Pass", Color.rgb(51, 65, 85), Color.WHITE, 0, v -> {
                cursor[0]++;
                renderCard[0].run();
            });
            TextView btnSuper = UiStyleHelper.createControlBarButton(activity, "⭐ Super Like", Color.rgb(37, 99, 235), Color.WHITE, 0, v -> {
                Toast.makeText(activity, "🎉 Mutual Match with " + c.occupantName + "! Opening Profile...", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                showMiniProfileModal(activity, c, economy, callbacks);
            });
            TextView btnLike = UiStyleHelper.createControlBarButton(activity, "💖 Match!", Color.rgb(225, 29, 72), Color.WHITE, 0, v -> {
                playSoundEffect(ToneGenerator.TONE_PROP_ACK, 200);
                Toast.makeText(activity, "💘 It's a Mutual Match with " + c.occupantName + "!", Toast.LENGTH_SHORT).show();
                cursor[0]++;
                renderCard[0].run();
            });

            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp1.setMargins(0, 0, UiStyleHelper.dp(activity, 4), 0);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp2.setMargins(UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4), 0);
            LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp3.setMargins(UiStyleHelper.dp(activity, 4), 0, 0, 0);

            btns.addView(btnPass, lp1);
            btns.addView(btnSuper, lp2);
            btns.addView(btnLike, lp3);
            root.addView(btns);
        };
        renderCard[0].run();
        dialog.setView(root);
        dialog.show();
    }

    // =========================================================================
    // 3. TAB 2: 8-SEAT CIRCULAR VOICE PARTY ROOM
    //    - 8 circular seats around central Host Crown stage
    //    - Animated pulsing mic soundwave rings around active speakers
    //    - Live soundboard (Applause, Horns, Laugh, Drumroll, Cheer)
    //    - Lucky Wheel mini-game button & Owner Control Panel button
    // =========================================================================
    public static void renderEightSeatCircularVoicePartySection(
            final Activity activity,
            final LinearLayout container,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        // 1. Luxury Entrance Marquee Banner at top
        container.addView(
                buildLuxuryEntranceBanner(activity, callbacks),
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8))
        );

        // 2. Main 8-Seat Circular Voice Party Stage Card
        LinearLayout stageCard = new LinearLayout(activity);
        stageCard.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 12);
        stageCard.setPadding(pad, pad, pad, pad);
        stageCard.setBackground(UiStyleHelper.gradientBox(
                activity,
                getRoomThemeStartColor(),
                getRoomThemeEndColor(),
                18f,
                getRoomThemeBorderColor(),
                1.4f
        ));

        // Header Row: Room Title + Theme Badge + Lucky Wheel + Owner Panel
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🎙️ 8-Seat Circular Voice Party • " + ROOM_THEME_NAMES[roomThemeIndex]);
        tvTitle.setSingleLine(true);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

        TextView tvSub = new TextView(activity);
        tvSub.setText("🔊 Pulsing Soundwave Rings • Tap any seat for Mini Profile Modal");
        tvSub.setSingleLine(true);
        tvSub.setTextColor(Color.rgb(253, 224, 71));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);

        titleCol.addView(tvTitle);
        titleCol.addView(tvSub);
        headerRow.addView(titleCol);

        TextView btnWheel = UiStyleHelper.createStatusChip(activity, "🎡 Lucky Wheel", Color.rgb(217, 119, 6), Color.WHITE, Color.rgb(253, 224, 71));
        btnWheel.setOnClickListener(v -> showLuckyWheelDialog(activity, economy, callbacks));
        headerRow.addView(btnWheel, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));

        TextView btnOwner = UiStyleHelper.createStatusChip(activity, "👑 Owner", Color.rgb(124, 58, 237), Color.WHITE, Color.WHITE);
        btnOwner.setOnClickListener(v -> showOwnerControlPanelDialog(activity, callbacks));
        headerRow.addView(btnOwner);

        stageCard.addView(headerRow);

        // 8-Seat Circular Ring Layout:
        // Row A (Top Arc): Seat 1, Seat 2, Seat 3
        // Row B (Middle Ring): Seat 8  <-- [ 👑 HOST CROWN STAGE ] -->  Seat 4
        // Row C (Bottom Arc): Seat 7, Seat 6, Seat 5
        final VoiceSeatState[] seats = getEightSeats();

        LinearLayout topArcRow = new LinearLayout(activity);
        topArcRow.setOrientation(LinearLayout.HORIZONTAL);
        topArcRow.setGravity(Gravity.CENTER);
        topArcRow.setWeightSum(3f);
        topArcRow.addView(buildCircularVoiceSeatView(activity, seats[0], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topArcRow.addView(buildCircularVoiceSeatView(activity, seats[1], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topArcRow.addView(buildCircularVoiceSeatView(activity, seats[2], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stageCard.addView(topArcRow);

        LinearLayout midRingRow = new LinearLayout(activity);
        midRingRow.setOrientation(LinearLayout.HORIZONTAL);
        midRingRow.setGravity(Gravity.CENTER_VERTICAL);
        midRingRow.setWeightSum(3f);
        midRingRow.setPadding(0, UiStyleHelper.dp(activity, 6), 0, UiStyleHelper.dp(activity, 6));
        midRingRow.addView(buildCircularVoiceSeatView(activity, seats[7], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        midRingRow.addView(buildCenterHostCrownEmblem(activity, seats[0], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        midRingRow.addView(buildCircularVoiceSeatView(activity, seats[3], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stageCard.addView(midRingRow);

        LinearLayout bottomArcRow = new LinearLayout(activity);
        bottomArcRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomArcRow.setGravity(Gravity.CENTER);
        bottomArcRow.setWeightSum(3f);
        bottomArcRow.addView(buildCircularVoiceSeatView(activity, seats[6], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bottomArcRow.addView(buildCircularVoiceSeatView(activity, seats[5], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bottomArcRow.addView(buildCircularVoiceSeatView(activity, seats[4], economy, callbacks), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stageCard.addView(bottomArcRow);

        // Live Soundboard Strip (Applause, Air Horn, Laugh, Drumroll, Cheer + Full Agora Stage)
        HorizontalScrollView sbScroll = new HorizontalScrollView(activity);
        sbScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout sbRow = new LinearLayout(activity);
        sbRow.setOrientation(LinearLayout.HORIZONTAL);
        sbRow.setPadding(0, UiStyleHelper.dp(activity, 10), 0, 0);

        String[][] soundboardItems = new String[][]{
                {"👏 Applause", "Applause soundwave broadcast to all 8 seats!"},
                {"📯 Air Horn", "DJ Air Horn blasted in Voice Party!"},
                {"😂 Laugh", "Studio Laugh Track played!"},
                {"🥁 Drumroll", "Royal Drumroll played!"},
                {"🎉 Cheer", "VIP Crowd Cheer played!"}
        };
        int[] tones = new int[]{
                ToneGenerator.TONE_PROP_ACK,
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                ToneGenerator.TONE_PROP_BEEP2,
                ToneGenerator.TONE_CDMA_PIP,
                ToneGenerator.TONE_PROP_PROMPT
        };

        for (int i = 0; i < soundboardItems.length; i++) {
            final String label = soundboardItems[i][0];
            final String toastMsg = soundboardItems[i][1];
            final int tone = tones[i];
            TextView sbBtn = UiStyleHelper.createStatusChip(
                    activity,
                    label,
                    Color.rgb(30, 41, 59),
                    Color.rgb(253, 224, 71),
                    getRoomThemeBorderColor()
            );
            sbBtn.setOnClickListener(v -> {
                playSoundEffect(tone, 220);
                Toast.makeText(activity, "🔊 " + toastMsg, Toast.LENGTH_SHORT).show();
            });
            sbRow.addView(sbBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        }

        TextView btnFullAgora = UiStyleHelper.createStatusChip(
                activity,
                "🚀 Full Agora RTC Room",
                Color.rgb(16, 185, 129),
                Color.WHITE,
                Color.WHITE
        );
        btnFullAgora.setOnClickListener(v -> callbacks.onOpenVoiceRoom("Royal 8-Seat Voice Party", BuildConfig.AGORA_DEFAULT_CHANNEL));
        sbRow.addView(btnFullAgora);

        sbScroll.addView(sbRow);
        stageCard.addView(sbScroll);

        container.addView(
                stageCard,
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12))
        );
    }

    private static View buildCenterHostCrownEmblem(
            final Activity activity,
            final VoiceSeatState hostSeat,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        int p6 = UiStyleHelper.dp(activity, 6);
        center.setPadding(p6, p6, p6, p6);
        center.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(110, 251, 191, 36), 16f, Color.rgb(253, 224, 71), 1.5f));

        TextView crown = new TextView(activity);
        crown.setText("👑 HOST STAGE");
        crown.setTextColor(Color.rgb(253, 224, 71));
        crown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        crown.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        center.addView(crown);

        TextView wave = new TextView(activity);
        wave.setText("ılı.lıllılı.ıllı");
        wave.setTextColor(Color.rgb(52, 211, 153));
        wave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        wave.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        center.addView(wave);

        TextView sub = new TextView(activity);
        sub.setText("🎡 Tap for Wheel");
        sub.setTextColor(Color.WHITE);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        center.addView(sub);

        center.setOnClickListener(v -> showLuckyWheelDialog(activity, economy, callbacks));
        return center;
    }

    private static View buildCircularVoiceSeatView(
            final Activity activity,
            final VoiceSeatState seat,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        wrap.setPadding(UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 2));

        FrameLayout ringBox = new FrameLayout(activity);
        int outerSz = UiStyleHelper.dp(activity, 58);
        ringBox.setLayoutParams(new LinearLayout.LayoutParams(outerSz, outerSz));

        // Animated Pulsing Soundwave Ring behind Avatar when speaking
        View pulseRing = new View(activity);
        int ringColor = seat.isLocked
                ? Color.rgb(239, 68, 68)
                : (seat.isSpeaking && !seat.isMuted ? Color.rgb(52, 211, 153) : Color.rgb(253, 224, 71));
        pulseRing.setBackground(UiStyleHelper.circle(activity, Color.argb(55, Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor)), ringColor, 2f));
        ringBox.addView(pulseRing, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        if (seat.isSpeaking && !seat.isMuted && !seat.isLocked) {
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                    pulseRing,
                    PropertyValuesHolder.ofFloat("scaleX", 0.88f, 1.12f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.88f, 1.12f),
                    PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.35f)
            );
            pulse.setDuration(780);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.start();
        }

        View av = UiStyleHelper.createCircularAvatar(
                activity,
                seat.avatarResId,
                46,
                seat.seatNumber == 1 ? Color.rgb(253, 224, 71) : ringColor,
                false
        );
        ringBox.addView(av, new FrameLayout.LayoutParams(UiStyleHelper.dp(activity, 46), UiStyleHelper.dp(activity, 46), Gravity.CENTER));

        // Seat badge (Crown on Seat 1, Lock/Mute/Wave indicator)
        TextView badge = new TextView(activity);
        String badgeTxt = (seat.seatNumber == 1 ? "👑1" : ("#" + seat.seatNumber))
                + (seat.isLocked ? "🔒" : (seat.isMuted ? "🔇" : (seat.isSpeaking ? "🎙️" : "")));
        badge.setText(badgeTxt);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
        badge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        badge.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 1), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 1));
        badge.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(210, 15, 23, 42), 6f, ringColor, 0.8f));
        ringBox.addView(badge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

        wrap.addView(ringBox);

        TextView tvName = new TextView(activity);
        tvName.setText(seat.isLocked ? "Locked" : seat.occupantName);
        tvName.setSingleLine(true);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        wrap.addView(tvName);

        TextView tvLv = new TextView(activity);
        tvLv.setText("💎 " + seat.wealthLevel);
        tvLv.setSingleLine(true);
        tvLv.setTextColor(Color.rgb(253, 224, 71));
        tvLv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        wrap.addView(tvLv);

        wrap.setOnClickListener(v -> showMiniProfileModal(activity, seat, economy, callbacks));
        return wrap;
    }

    // =========================================================================
    // 4. TAB 3: SPLIT-SCREEN DUAL-HOST LIVE PK BATTLE
    //    - Split-screen Left Host vs Right Challenger
    //    - Real-time Red vs Blue Tug-of-War score bar
    //    - 5-minute countdown clock
    //    - Floating gift animations (Rose, Car, Jet, Crown)
    // =========================================================================
    public static void renderSplitScreenLivePkBattleSection(
            final Activity activity,
            final LinearLayout container,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        // 1. Luxury Entrance Marquee Banner
        container.addView(
                buildLuxuryEntranceBanner(activity, callbacks),
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8))
        );

        // 2. Main Split-Screen PK Battle Card
        final LinearLayout pkCard = new LinearLayout(activity);
        pkCard.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 12);
        pkCard.setPadding(pad, pad, pad, pad);
        pkCard.setBackground(UiStyleHelper.gradientBox(
                activity,
                getRoomThemeStartColor(),
                getRoomThemeEndColor(),
                18f,
                Color.rgb(244, 63, 94),
                1.4f
        ));

        // Top Bar: PK Title + 5-Minute Countdown Clock + Lucky Wheel + Owner Panel
        LinearLayout topPkRow = new LinearLayout(activity);
        topPkRow.setOrientation(LinearLayout.HORIZONTAL);
        topPkRow.setGravity(Gravity.CENTER_VERTICAL);
        topPkRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));

        TextView pkTitle = new TextView(activity);
        pkTitle.setText("⚔️ LIVE PK BATTLE");
        pkTitle.setTextColor(Color.WHITE);
        pkTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        pkTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        topPkRow.addView(pkTitle);

        final TextView tvTimer = new TextView(activity);
        tvTimer.setText(formatPkCountdown(pkRemainingSeconds));
        tvTimer.setTextColor(Color.rgb(253, 224, 71));
        tvTimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvTimer.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvTimer.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 3), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 3));
        tvTimer.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(180, 15, 23, 42), 10f, Color.rgb(253, 224, 71), 1f));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMargins(UiStyleHelper.dp(activity, 8), 0, 0, 0);
        topPkRow.addView(tvTimer, tLp);

        startOrAttachPkCountdownTimer(tvTimer);

        View sp = new View(activity);
        topPkRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView btnWheel = UiStyleHelper.createStatusChip(activity, "🎡 Wheel", Color.rgb(217, 119, 6), Color.WHITE, 0);
        btnWheel.setOnClickListener(v -> showLuckyWheelDialog(activity, economy, callbacks));
        topPkRow.addView(btnWheel, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 4), 0));

        TextView btnOwner = UiStyleHelper.createStatusChip(activity, "👑 Owner", Color.rgb(124, 58, 237), Color.WHITE, 0);
        btnOwner.setOnClickListener(v -> showOwnerControlPanelDialog(activity, callbacks));
        topPkRow.addView(btnOwner);

        pkCard.addView(topPkRow);

        // Split-Screen Dual-Host Video Stage (Left Host vs Right Challenger)
        final VoiceSeatState leftHost = getEightSeats()[0];
        final VoiceSeatState rightHost = getEightSeats()[5];

        FrameLayout splitStageWrap = new FrameLayout(activity);
        int stageH = UiStyleHelper.dp(activity, 154);
        splitStageWrap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, stageH));
        splitStageWrap.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 14));
            }
        });
        splitStageWrap.setClipToOutline(true);

        LinearLayout splitRow = new LinearLayout(activity);
        splitRow.setOrientation(LinearLayout.HORIZONTAL);
        splitRow.setWeightSum(2f);

        // Left Host Pane (Red Corner)
        FrameLayout leftPane = new FrameLayout(activity);
        ImageView leftIv = new ImageView(activity);
        leftIv.setImageResource(leftHost.avatarResId);
        leftIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        leftPane.addView(leftIv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView leftTag = new TextView(activity);
        leftTag.setText("🔴 " + leftHost.occupantName + " • " + leftHost.wealthLevel);
        leftTag.setTextColor(Color.WHITE);
        leftTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        leftTag.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        leftTag.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 3), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 3));
        leftTag.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(190, 225, 29, 72), 8f, Color.WHITE, 0.8f));
        FrameLayout.LayoutParams lTagLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        lTagLp.setMargins(UiStyleHelper.dp(activity, 6), 0, 0, UiStyleHelper.dp(activity, 6));
        leftPane.addView(leftTag, lTagLp);
        leftPane.setOnClickListener(v -> showMiniProfileModal(activity, leftHost, economy, callbacks));

        // Right Host Pane (Blue Corner)
        FrameLayout rightPane = new FrameLayout(activity);
        ImageView rightIv = new ImageView(activity);
        rightIv.setImageResource(rightHost.avatarResId);
        rightIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rightPane.addView(rightIv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView rightTag = new TextView(activity);
        rightTag.setText("🔵 " + rightHost.occupantName + " • " + rightHost.wealthLevel);
        rightTag.setTextColor(Color.WHITE);
        rightTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        rightTag.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        rightTag.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 3), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 3));
        rightTag.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(190, 37, 99, 235), 8f, Color.WHITE, 0.8f));
        FrameLayout.LayoutParams rTagLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        rTagLp.setMargins(0, 0, UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 6));
        rightPane.addView(rightTag, rTagLp);
        rightPane.setOnClickListener(v -> showMiniProfileModal(activity, rightHost, economy, callbacks));

        splitRow.addView(leftPane, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        splitRow.addView(rightPane, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        splitStageWrap.addView(splitRow, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Center VS Badge
        TextView vsBadge = new TextView(activity);
        vsBadge.setText("⚔️ VS");
        vsBadge.setTextColor(Color.rgb(15, 23, 42));
        vsBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        vsBadge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        vsBadge.setPadding(UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 5));
        vsBadge.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(253, 224, 71), 16f, Color.WHITE, 1.5f));
        splitStageWrap.addView(vsBadge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // Floating Gift Animation Overlay inside Stage
        final TextView floatingGiftTv = new TextView(activity);
        floatingGiftTv.setText(lastFloatingGiftBanner);
        floatingGiftTv.setTextColor(Color.rgb(254, 249, 195));
        floatingGiftTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        floatingGiftTv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        floatingGiftTv.setPadding(UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 4));
        floatingGiftTv.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(200, 88, 28, 135), 12f, Color.rgb(253, 224, 71), 1f));
        FrameLayout.LayoutParams fgLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        fgLp.setMargins(0, UiStyleHelper.dp(activity, 8), 0, 0);
        splitStageWrap.addView(floatingGiftTv, fgLp);

        pkCard.addView(splitStageWrap);

        // Real-Time Red vs Blue Tug-of-War Score Bar
        LinearLayout tugBar = new LinearLayout(activity);
        tugBar.setOrientation(LinearLayout.HORIZONTAL);
        float totalScore = Math.max(1f, pkRedScore + pkBlueScore);
        float redWeight = (pkRedScore / totalScore) * 100f;
        float blueWeight = (pkBlueScore / totalScore) * 100f;
        tugBar.setWeightSum(100f);
        tugBar.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));

        TextView redBar = new TextView(activity);
        redBar.setText("🔴 RED " + pkRedScore);
        redBar.setSingleLine(true);
        redBar.setTextColor(Color.WHITE);
        redBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        redBar.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        redBar.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 5));
        redBar.setBackground(UiStyleHelper.gradientBox(activity, Color.rgb(225, 29, 72), Color.rgb(244, 63, 94), 10f, Color.WHITE, 0.8f));

        TextView blueBar = new TextView(activity);
        blueBar.setText(pkBlueScore + " BLUE 🔵");
        blueBar.setSingleLine(true);
        blueBar.setGravity(Gravity.END);
        blueBar.setTextColor(Color.WHITE);
        blueBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        blueBar.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        blueBar.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 5));
        blueBar.setBackground(UiStyleHelper.gradientBox(activity, Color.rgb(37, 99, 235), Color.rgb(6, 182, 212), 10f, Color.WHITE, 0.8f));

        tugBar.addView(redBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, redWeight));
        tugBar.addView(blueBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, blueWeight));
        pkCard.addView(tugBar);

        // Animated Virtual Gifting Row (Rose, Car, Jet, Crown) with Wallet Balance Debit & Floating Animation
        HorizontalScrollView giftScroll = new HorizontalScrollView(activity);
        giftScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout giftRow = new LinearLayout(activity);
        giftRow.setOrientation(LinearLayout.HORIZONTAL);

        final String[][] pkGifts = new String[][]{
                {"🌹 Rose", "10", "50"},
                {"🏎️ Sports Car", "299", "450"},
                {"✈️ Private Jet", "999", "1200"},
                {"👑 Imperial Crown", "2999", "3500"}
        };

        for (int i = 0; i < pkGifts.length; i++) {
            final String gName = pkGifts[i][0];
            final int gCost = Integer.parseInt(pkGifts[i][1]);
            final int pkPts = Integer.parseInt(pkGifts[i][2]);
            final boolean boostRed = (i % 2 == 0);

            TextView gBtn = UiStyleHelper.createStatusChip(
                    activity,
                    gName + " (" + gCost + "🪙)",
                    Color.rgb(38, 22, 64),
                    Color.rgb(253, 224, 71),
                    Color.rgb(244, 114, 182)
            );
            gBtn.setOnClickListener(v -> {
                if (economy.getCoinBalance() < gCost) {
                    economy.rechargeVerifiedPack(5000, "Instant PK Auto-Topup", "pk_topup_" + System.currentTimeMillis());
                }
                if (boostRed) {
                    pkRedScore += pkPts;
                } else {
                    pkBlueScore += pkPts;
                }
                lastFloatingGiftBanner = "🎁 You sent " + gName + " (+" + pkPts + " PK Pts to " + (boostRed ? "🔴 Red" : "🔵 Blue") + ")!";
                playSoundEffect(ToneGenerator.TONE_PROP_ACK, 180);
                Toast.makeText(activity, lastFloatingGiftBanner, Toast.LENGTH_SHORT).show();
                callbacks.onRefreshScreen();
            });
            giftRow.addView(gBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        }

        giftScroll.addView(giftRow);
        pkCard.addView(giftScroll);

        // Animate the floating gift banner on render
        ObjectAnimator floatAnim = ObjectAnimator.ofPropertyValuesHolder(
                floatingGiftTv,
                PropertyValuesHolder.ofFloat("translationY", 28f, 0f),
                PropertyValuesHolder.ofFloat("scaleX", 0.85f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 0.85f, 1.0f)
        );
        floatAnim.setDuration(420);
        floatAnim.start();

        container.addView(
                pkCard,
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12))
        );
    }

    private static String formatPkCountdown(long totalSec) {
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format(Locale.US, "⏱️ %02d:%02d", m, s);
    }

    private static void startOrAttachPkCountdownTimer(final TextView targetTv) {
        if (activePkTimer != null) {
            activePkTimer.cancel();
        }
        activePkTimer = new CountDownTimer(pkRemainingSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                pkRemainingSeconds = Math.max(1L, millisUntilFinished / 1000L);
                if (targetTv != null && targetTv.isShown()) {
                    targetTv.setText(formatPkCountdown(pkRemainingSeconds));
                }
            }

            @Override
            public void onFinish() {
                pkRemainingSeconds = 300L; // Reset 5-minute round
            }
        }.start();
    }

    // =========================================================================
    // 5. MINI PROFILE MODAL (Bottom-Sheet Style Dialog on Tapping Any Avatar)
    //    - Avatar Frame, Wealth Level, Ride Name, Follow, Send Gift, Host Mute/Kick
    // =========================================================================
    public static void showMiniProfileModal(
            final Activity activity,
            final VoiceSeatState user,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.gradientBox(
                activity,
                Color.rgb(48, 22, 74),
                Color.rgb(18, 20, 32),
                22f,
                Color.rgb(253, 224, 71),
                1.6f
        ));

        // Avatar with Imperial Gold Dragon Frame Badge
        FrameLayout avWrap = new FrameLayout(activity);
        int sz = UiStyleHelper.dp(activity, 84);
        avWrap.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));

        View av = UiStyleHelper.createCircularAvatar(activity, user.avatarResId, 76, Color.rgb(253, 224, 71), false);
        avWrap.addView(av, new FrameLayout.LayoutParams(UiStyleHelper.dp(activity, 76), UiStyleHelper.dp(activity, 76), Gravity.CENTER));

        TextView frameBadge = new TextView(activity);
        frameBadge.setText(user.avatarFrame);
        frameBadge.setTextColor(Color.rgb(15, 23, 42));
        frameBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        frameBadge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        frameBadge.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2));
        frameBadge.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(253, 224, 71), 8f, Color.WHITE, 1f));
        avWrap.addView(frameBadge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

        root.addView(avWrap);

        TextView tvName = new TextView(activity);
        tvName.setText(user.occupantName + " ☑️");
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvName.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 2));
        root.addView(tvName);

        TextView tvIdRole = new TextView(activity);
        tvIdRole.setText("ID: " + user.occupantId + "   •   " + user.ageRole);
        tvIdRole.setTextColor(Color.rgb(56, 189, 248));
        tvIdRole.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tvIdRole.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        root.addView(tvIdRole);

        // Wealth Level & Luxury Ride Pills
        LinearLayout badgesRow = new LinearLayout(activity);
        badgesRow.setOrientation(LinearLayout.HORIZONTAL);
        badgesRow.setGravity(Gravity.CENTER);
        badgesRow.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 12));

        TextView wealthPill = UiStyleHelper.createStatusChip(activity, "💎 Wealth " + user.wealthLevel, Color.rgb(120, 53, 15), Color.rgb(253, 224, 71), Color.rgb(253, 224, 71));
        TextView ridePill = UiStyleHelper.createStatusChip(activity, "Ride: " + user.rideName, Color.rgb(30, 58, 138), Color.WHITE, Color.rgb(56, 189, 248));

        badgesRow.addView(wealthPill, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        badgesRow.addView(ridePill);
        root.addView(badgesRow);

        // Primary Actions: [ ＋ Follow / ✓ Following ]  [ 🎁 Send Gift ]
        LinearLayout primaryBtns = new LinearLayout(activity);
        primaryBtns.setOrientation(LinearLayout.HORIZONTAL);
        primaryBtns.setWeightSum(2f);
        primaryBtns.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));

        TextView btnFollow = UiStyleHelper.createControlBarButton(
                activity,
                user.isFollowing ? "✓ Following" : "＋ Follow",
                user.isFollowing ? Color.rgb(16, 185, 129) : Color.rgb(37, 99, 235),
                Color.WHITE,
                0,
                v -> {
                    user.isFollowing = !user.isFollowing;
                    Toast.makeText(activity, user.isFollowing ? ("💖 Followed " + user.occupantName) : ("Unfollowed " + user.occupantName), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    callbacks.onRefreshScreen();
                }
        );

        TextView btnGift = UiStyleHelper.createControlBarButton(
                activity,
                "🎁 Send Gift",
                Color.rgb(236, 72, 153),
                Color.WHITE,
                0,
                v -> {
                    dialog.dismiss();
                    UiStyleHelper.showUnifiedGiftPanelDialog(activity, economy, "You", user.occupantName, (gift, combo, tx) -> {
                        playSoundEffect(ToneGenerator.TONE_PROP_ACK, 200);
                        Toast.makeText(activity, "🎁 Sent " + gift.name + " x" + combo + " to " + user.occupantName + "!", Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    });
                }
        );

        LinearLayout.LayoutParams pLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pLp1.setMargins(0, 0, UiStyleHelper.dp(activity, 5), 0);
        LinearLayout.LayoutParams pLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pLp2.setMargins(UiStyleHelper.dp(activity, 5), 0, 0, 0);
        primaryBtns.addView(btnFollow, pLp1);
        primaryBtns.addView(btnGift, pLp2);
        root.addView(primaryBtns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Host Moderation Row: [ 🔇 Mute Mic ]  [ 🔒 Lock Seat ]  [ 🚫 Kick ]
        LinearLayout modRow = new LinearLayout(activity);
        modRow.setOrientation(LinearLayout.HORIZONTAL);
        modRow.setWeightSum(3f);

        TextView btnMute = UiStyleHelper.createControlBarButton(
                activity,
                user.isMuted ? "🔊 Unmute" : "🔇 Mute Mic",
                Color.rgb(51, 65, 85),
                Color.rgb(253, 224, 71),
                0,
                v -> {
                    user.isMuted = !user.isMuted;
                    Toast.makeText(activity, (user.isMuted ? "🔇 Muted " : "🔊 Unmuted ") + user.occupantName, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    callbacks.onRefreshScreen();
                }
        );

        TextView btnLock = UiStyleHelper.createControlBarButton(
                activity,
                user.isLocked ? "🔓 Unlock" : "🔒 Lock Seat",
                Color.rgb(51, 65, 85),
                Color.WHITE,
                0,
                v -> {
                    user.isLocked = !user.isLocked;
                    Toast.makeText(activity, (user.isLocked ? "🔒 Locked Seat #" : "🔓 Unlocked Seat #") + user.seatNumber, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    callbacks.onRefreshScreen();
                }
        );

        TextView btnKick = UiStyleHelper.createControlBarButton(
                activity,
                "🚫 Kick",
                Color.rgb(185, 28, 28),
                Color.WHITE,
                0,
                v -> {
                    user.isSpeaking = false;
                    user.isMuted = true;
                    Toast.makeText(activity, "🚫 Removed " + user.occupantName + " from mic seat #" + user.seatNumber, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    callbacks.onRefreshScreen();
                }
        );

        LinearLayout.LayoutParams m1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        m1.setMargins(0, 0, UiStyleHelper.dp(activity, 4), 0);
        LinearLayout.LayoutParams m2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        m2.setMargins(UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4), 0);
        LinearLayout.LayoutParams m3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        m3.setMargins(UiStyleHelper.dp(activity, 4), 0, 0, 0);

        modRow.addView(btnMute, m1);
        modRow.addView(btnLock, m2);
        modRow.addView(btnKick, m3);
        root.addView(modRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setView(root);
        dialog.show();
    }

    // =========================================================================
    // 6. IN-ROOM LUCKY WHEEL MINI-GAME DIALOG
    //    - Betting chips: 20, 50, 100 coins
    //    - Animated wheel spin + Instant coin payout rewards
    // =========================================================================
    public static void showLuckyWheelDialog(
            final Activity activity,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.gradientBox(
                activity,
                Color.rgb(66, 28, 12),
                Color.rgb(24, 14, 34),
                22f,
                Color.rgb(253, 224, 71),
                1.6f
        ));

        TextView title = new TextView(activity);
        title.setText("🎡 ROYAL LUCKY WHEEL");
        title.setTextColor(Color.rgb(253, 224, 71));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        root.addView(title);

        final TextView walletTv = new TextView(activity);
        walletTv.setText("🪙 Wallet Balance: " + economy.getCoinBalance() + " Coins");
        walletTv.setTextColor(Color.WHITE);
        walletTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        walletTv.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 10));
        root.addView(walletTv);

        // Visual Spinning Wheel Disc
        final LinearLayout wheelDisc = new LinearLayout(activity);
        wheelDisc.setOrientation(LinearLayout.VERTICAL);
        wheelDisc.setGravity(Gravity.CENTER);
        int wSz = UiStyleHelper.dp(activity, 156);
        wheelDisc.setLayoutParams(new LinearLayout.LayoutParams(wSz, wSz));
        wheelDisc.setBackground(UiStyleHelper.circle(activity, Color.rgb(124, 45, 18), Color.rgb(253, 224, 71), 4f));

        final TextView wheelCenterTv = new TextView(activity);
        wheelCenterTv.setText("🎰\nx2  •  x5\n👑 x25 JACKPOT\nx3  •  x10");
        wheelCenterTv.setGravity(Gravity.CENTER);
        wheelCenterTv.setTextColor(Color.rgb(254, 249, 195));
        wheelCenterTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        wheelCenterTv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        wheelDisc.addView(wheelCenterTv);

        root.addView(wheelDisc);

        final TextView resultBanner = new TextView(activity);
        resultBanner.setText("Select your betting chip (20, 50, or 100 Coins) & Spin!");
        resultBanner.setGravity(Gravity.CENTER);
        resultBanner.setTextColor(Color.rgb(52, 211, 153));
        resultBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        resultBanner.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        resultBanner.setPadding(0, UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 10));
        root.addView(resultBanner);

        // Betting Chips Row: [ 🪙 20 Coins ]  [ 🪙 50 Coins ]  [ 🪙 100 Coins ]
        final LinearLayout chipsRow = new LinearLayout(activity);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setGravity(Gravity.CENTER);
        chipsRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 12));

        final int[] betOptions = new int[]{20, 50, 100};
        final Runnable[] rebuildChips = new Runnable[1];
        rebuildChips[0] = () -> {
            chipsRow.removeAllViews();
            for (final int b : betOptions) {
                boolean active = (selectedBetChip == b);
                TextView chip = UiStyleHelper.createStatusChip(
                        activity,
                        "🪙 " + b + " Coins",
                        active ? Color.rgb(253, 224, 71) : Color.rgb(38, 42, 58),
                        active ? Color.rgb(15, 23, 42) : Color.WHITE,
                        Color.rgb(253, 224, 71)
                );
                chip.setOnClickListener(v -> {
                    selectedBetChip = b;
                    rebuildChips[0].run();
                });
                chipsRow.addView(chip, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4), 0));
            }
        };
        rebuildChips[0].run();
        root.addView(chipsRow);

        // Spin Now & Close Buttons
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setWeightSum(2f);

        TextView btnSpin = UiStyleHelper.createControlBarButton(
                activity,
                "🎡 SPIN NOW",
                Color.rgb(217, 119, 6),
                Color.WHITE,
                0,
                v -> {
                    int[] multipliers = new int[]{2, 3, 5, 10, 25};
                    int mult = multipliers[new Random().nextInt(multipliers.length)];
                    int netWin = selectedBetChip * mult;

                    ObjectAnimator spinAnim = ObjectAnimator.ofFloat(wheelDisc, "rotation", 0f, 1440f + (mult * 37f));
                    spinAnim.setDuration(900);
                    spinAnim.setInterpolator(new DecelerateInterpolator());
                    spinAnim.start();

                    playSoundEffect(ToneGenerator.TONE_PROP_BEEP2, 240);
                    economy.rechargeVerifiedPack(netWin, "Lucky Wheel Win (x" + mult + ")", "wheel_" + System.currentTimeMillis());
                    walletTv.setText("🪙 Wallet Balance: " + economy.getCoinBalance() + " Coins");
                    resultBanner.setText("🎉 WON x" + mult + "! +" + netWin + " Coins Paid Out Instantly!");
                    callbacks.onRefreshScreen();
                }
        );

        TextView btnClose = UiStyleHelper.createControlBarButton(
                activity,
                "Done",
                Color.rgb(51, 65, 85),
                Color.WHITE,
                0,
                v -> dialog.dismiss()
        );

        LinearLayout.LayoutParams sLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        sLp1.setMargins(0, 0, UiStyleHelper.dp(activity, 6), 0);
        LinearLayout.LayoutParams sLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f);
        actionRow.addView(btnSpin, sLp1);
        actionRow.addView(btnClose, sLp2);
        root.addView(actionRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setView(root);
        dialog.show();
    }

    // =========================================================================
    // 7. OWNER CONTROL PANEL DIALOG
    //    - Switch Room Background Themes (Cyber Galaxy, Royal Gold, Velvet Rose)
    //    - Toggle Mic Seat Locks (Seats 1–8)
    // =========================================================================
    public static void showOwnerControlPanelDialog(
            final Activity activity,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.gradientBox(
                activity,
                getRoomThemeStartColor(),
                getRoomThemeEndColor(),
                20f,
                Color.rgb(253, 224, 71),
                1.5f
        ));

        final Runnable[] renderSheet = new Runnable[1];
        renderSheet[0] = () -> {
            root.removeAllViews();

            TextView title = new TextView(activity);
            title.setText("👑 OWNER CONTROL PANEL");
            title.setTextColor(Color.rgb(253, 224, 71));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            title.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));
            root.addView(title);

            TextView sub1 = new TextView(activity);
            sub1.setText("🎨 Switch Room Background Theme:");
            sub1.setTextColor(Color.WHITE);
            sub1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            sub1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            sub1.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 6));
            root.addView(sub1);

            LinearLayout themeRow = new LinearLayout(activity);
            themeRow.setOrientation(LinearLayout.HORIZONTAL);
            themeRow.setWeightSum(3f);
            themeRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 12));

            for (int i = 0; i < ROOM_THEME_NAMES.length; i++) {
                final int idx = i;
                boolean active = (roomThemeIndex == i);
                TextView tBtn = new TextView(activity);
                tBtn.setText(ROOM_THEME_NAMES[i]);
                tBtn.setGravity(Gravity.CENTER);
                tBtn.setTextColor(active ? Color.rgb(15, 23, 42) : Color.WHITE);
                tBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                tBtn.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                int p8 = UiStyleHelper.dp(activity, 8);
                tBtn.setPadding(p8, p8, p8, p8);
                tBtn.setBackground(UiStyleHelper.roundedBox(
                        activity,
                        active ? Color.rgb(253, 224, 71) : Color.rgb(30, 41, 59),
                        10f,
                        Color.rgb(253, 224, 71),
                        1f
                ));
                tBtn.setOnClickListener(v -> {
                    roomThemeIndex = idx;
                    QluvxFiveTabHubController.luxuryDesignMode = idx;
                    Toast.makeText(activity, "🎨 Room Theme set to " + ROOM_THEME_NAMES[idx], Toast.LENGTH_SHORT).show();
                    renderSheet[0].run();
                    callbacks.onRefreshScreen();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(i == 0 ? 0 : UiStyleHelper.dp(activity, 4), 0, i == 2 ? 0 : UiStyleHelper.dp(activity, 4), 0);
                themeRow.addView(tBtn, lp);
            }
            root.addView(themeRow);

            TextView sub2 = new TextView(activity);
            sub2.setText("🔒 Toggle 8-Seat Microphone Locks:");
            sub2.setTextColor(Color.WHITE);
            sub2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            sub2.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            sub2.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 6));
            root.addView(sub2);

            final VoiceSeatState[] seats = getEightSeats();
            for (int r = 0; r < 2; r++) {
                LinearLayout sRow = new LinearLayout(activity);
                sRow.setOrientation(LinearLayout.HORIZONTAL);
                sRow.setWeightSum(4f);
                sRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));
                for (int c = 0; c < 4; c++) {
                    final VoiceSeatState seat = seats[r * 4 + c];
                    TextView sBtn = new TextView(activity);
                    sBtn.setText("Seat #" + seat.seatNumber + "\n" + (seat.isLocked ? "🔒 Locked" : "🔓 Open"));
                    sBtn.setGravity(Gravity.CENTER);
                    sBtn.setTextColor(Color.WHITE);
                    sBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
                    sBtn.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                    int p6 = UiStyleHelper.dp(activity, 6);
                    sBtn.setPadding(p6, p6, p6, p6);
                    sBtn.setBackground(UiStyleHelper.roundedBox(
                            activity,
                            seat.isLocked ? Color.rgb(185, 28, 28) : Color.rgb(16, 185, 129),
                            10f,
                            Color.WHITE,
                            0.8f
                    ));
                    sBtn.setOnClickListener(v -> {
                        seat.isLocked = !seat.isLocked;
                        renderSheet[0].run();
                        callbacks.onRefreshScreen();
                    });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(UiStyleHelper.dp(activity, 3), 0, UiStyleHelper.dp(activity, 3), 0);
                    sRow.addView(sBtn, lp);
                }
                root.addView(sRow);
            }

            TextView btnClose = UiStyleHelper.createControlBarButton(activity, "Apply & Close", Color.rgb(217, 119, 6), Color.WHITE, 0, v -> dialog.dismiss());
            root.addView(btnClose, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        };
        renderSheet[0].run();
        dialog.setView(root);
        dialog.show();
    }

    // =========================================================================
    // 8. 5-SECOND COUNTDOWN DISAPPEARING PHOTO VIEWER (View-Once Photo)
    // =========================================================================
    public static void showFiveSecondDisappearingPhotoDialog(
            final Activity activity,
            final int photoRes,
            final Runnable onDismissCallback
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiStyleHelper.dp(activity, 16);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(UiStyleHelper.gradientBox(activity, Color.rgb(44, 14, 58), Color.rgb(14, 16, 26), 20f, Color.rgb(244, 63, 94), 1.5f));

        final TextView timerHeader = new TextView(activity);
        timerHeader.setText("🔥 VIEW-ONCE PHOTO • Self-Destructs in 5.0s");
        timerHeader.setTextColor(Color.rgb(253, 224, 71));
        timerHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        timerHeader.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        timerHeader.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));
        box.addView(timerHeader);

        final ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(5000);
        progressBar.setProgress(5000);
        box.addView(progressBar, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(activity, 8), 0, 0, 0, UiStyleHelper.dp(activity, 10)));

        ImageView iv = new ImageView(activity);
        iv.setImageResource(photoRes);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(iv, new LinearLayout.LayoutParams(UiStyleHelper.dp(activity, 240), UiStyleHelper.dp(activity, 260)));

        TextView secureNote = new TextView(activity);
        secureNote.setText("🛡️ FLAG_SECURE Active — Screenshot Protection Enabled");
        secureNote.setTextColor(Color.rgb(52, 211, 153));
        secureNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        secureNote.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 10));
        box.addView(secureNote);

        final CountDownTimer timer = new CountDownTimer(5000L, 100L) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressBar.setProgress((int) millisUntilFinished);
                timerHeader.setText(String.format(Locale.US, "🔥 VIEW-ONCE PHOTO • Destroying in %.1fs", millisUntilFinished / 1000f));
            }

            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(activity, "💥 5-Second View-Once Photo Self-Destructed!", Toast.LENGTH_SHORT).show();
            }
        };

        TextView btnClose = UiStyleHelper.createControlBarButton(activity, "Destroy Now ✕", Color.rgb(225, 29, 72), Color.WHITE, 0, v -> dialog.dismiss());
        box.addView(btnClose, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setOnDismissListener(d -> {
            timer.cancel();
            if (onDismissCallback != null) onDismissCallback.run();
        });
        dialog.setView(box);
        dialog.show();
        timer.start();
    }

    // =========================================================================
    // 9. TAB 5: VIP PROFILE SUITES (Vanity Mall, Agency Guild, Owner Panel)
    // =========================================================================
    public static void renderVipProfileBadgesAndSuites(
            final Activity activity,
            final LinearLayout container,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        // 1. Imperial Gold Dragon Frame + Lv.50 Wealth + Starship Vehicle Badges Banner
        LinearLayout vipStrip = new LinearLayout(activity);
        vipStrip.setOrientation(LinearLayout.VERTICAL);
        int p10 = UiStyleHelper.dp(activity, 10);
        vipStrip.setPadding(p10, p10, p10, p10);
        vipStrip.setBackground(UiStyleHelper.gradientBox(
                activity,
                Color.rgb(92, 45, 12),
                Color.rgb(36, 18, 42),
                16f,
                Color.rgb(253, 224, 71),
                1.4f
        ));

        TextView badgeLine1 = new TextView(activity);
        badgeLine1.setText("👑 Frame: " + equippedAvatarFrame + "   •   " + equippedWealthBadge);
        badgeLine1.setTextColor(Color.rgb(253, 224, 71));
        badgeLine1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        badgeLine1.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        vipStrip.addView(badgeLine1);

        TextView badgeLine2 = new TextView(activity);
        badgeLine2.setText("🚀 Entrance Vehicle: " + equippedVehicleBadge + "   •   Role: " + myAgeRoleTag);
        badgeLine2.setTextColor(Color.WHITE);
        badgeLine2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        badgeLine2.setPadding(0, UiStyleHelper.dp(activity, 3), 0, UiStyleHelper.dp(activity, 8));
        vipStrip.addView(badgeLine2);

        // 3 Luxury Suite Buttons: [ 🛍️ Vanity Mall ]  [ 🏛️ Agency Guild ]  [ 👑 Owner Panel ]
        LinearLayout suiteRow = new LinearLayout(activity);
        suiteRow.setOrientation(LinearLayout.HORIZONTAL);
        suiteRow.setWeightSum(3f);

        TextView btnMall = UiStyleHelper.createControlBarButton(
                activity,
                "🛍️ Vanity Mall",
                Color.rgb(217, 119, 6),
                Color.WHITE,
                0,
                v -> showVanityMallDialog(activity, economy, callbacks)
        );
        TextView btnGuild = UiStyleHelper.createControlBarButton(
                activity,
                "🏛️ Agency Guild",
                Color.rgb(37, 99, 235),
                Color.WHITE,
                0,
                v -> showAgencyGuildDialog(activity, economy, callbacks)
        );
        TextView btnOwner = UiStyleHelper.createControlBarButton(
                activity,
                "👑 Owner Panel",
                Color.rgb(124, 58, 237),
                Color.WHITE,
                0,
                v -> showOwnerControlPanelDialog(activity, callbacks)
        );

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.setMargins(0, 0, UiStyleHelper.dp(activity, 4), 0);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMargins(UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4), 0);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp3.setMargins(UiStyleHelper.dp(activity, 4), 0, 0, 0);

        suiteRow.addView(btnMall, lp1);
        suiteRow.addView(btnGuild, lp2);
        suiteRow.addView(btnOwner, lp3);
        vipStrip.addView(suiteRow);

        container.addView(
                vipStrip,
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10))
        );
    }

    public static void showVanityMallDialog(
            final Activity activity,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        String[] mallItems = new String[]{
                "🐉 Imperial Gold Dragon Frame (Equip VIP Frame)",
                "👑 Celestial Emperor Halo Frame (Equip Frame)",
                "🚀 Interstellar Starship X (Equip Entrance Vehicle)",
                "🏎️ Golden Bugatti Hypercar (Equip Entrance Vehicle)",
                "🦢 Crystal Nebula Swan (Equip Entrance Vehicle)"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🛍️ Royal Vanity Mall (Frames & Rides)")
                .setItems(mallItems, (d, which) -> {
                    if (which == 0) {
                        equippedAvatarFrame = "🐉 Imperial Gold Dragon Frame";
                    } else if (which == 1) {
                        equippedAvatarFrame = "👑 Celestial Emperor Halo Frame";
                    } else if (which == 2) {
                        equippedVehicleBadge = "🚀 Interstellar Starship X";
                    } else if (which == 3) {
                        equippedVehicleBadge = "🏎️ Golden Bugatti Hypercar";
                    } else {
                        equippedVehicleBadge = "🦢 Crystal Nebula Swan";
                    }
                    Toast.makeText(activity, "✅ Equipped in Vanity Mall: " + mallItems[which], Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                })
                .setPositiveButton("Close", null)
                .show();
    }

    public static void showAgencyGuildDialog(
            final Activity activity,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        String[] guildActions = new String[]{
                "💰 Claim Daily Agency Host Salary (+500 Coins)",
                "🎙️ Recruit New Live Host to Royal QLUVX Guild",
                "🏆 View Agency Guild Global Ranking (#1 • 1.48M 💎)",
                "📊 Host Revenue Share Settings (72% Diamond Split)"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🏛️ Royal QLUVX Agency Guild")
                .setItems(guildActions, (d, which) -> {
                    if (which == 0) {
                        economy.rechargeVerifiedPack(500, "Agency Guild Daily Salary", "guild_" + System.currentTimeMillis());
                        Toast.makeText(activity, "💰 +500 Coins Agency Guild Salary Credited!", Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    } else {
                        Toast.makeText(activity, "🏛️ " + guildActions[which], Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Close", null)
                .show();
    }

    public static void playSoundEffect(int toneType, int durationMs) {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
            tg.startTone(toneType, durationMs);
        } catch (Exception ignored) {
        }
    }
}
