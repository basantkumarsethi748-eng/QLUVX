package com.example.qluvx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * QLUVX Master Screens Controller (Black, Royal Purple & Neon-Pink Design).
 * Implements all interactive buttons and layouts for:
 * - HOME SCREEN (LIVE tab, VOICE tab, Search, Notifications, VIP, My Story, Add Story, Friends,
 *   Live Gifting Ranking, Voice Gifting Ranking, Recommended, Global, Following,
 *   All, PK, Hot, New, Nearby, Follow Creator, Open Live Room, Open Voice Room)
 * - PROFILE SCREEN (Edit Profile, Follow, Unfollow, Message, Open Album, Open Gifts, Open Moments,
 *   My Level, VIP Level, My Badges, Wallet, Settings, Report, Block, Unblock)
 * - WALLET SCREEN (Coin Balance, Recharge, Transaction History, Purchase History)
 */
public class QluvxMasterScreensController {

    public interface HostNavigationCallbacks {
        void onSelectMenu(int menuIndex);
        void onOpenVoiceRoom(String roomTitle, String channelName);
        void onOpenLiveVideoRoom(String roomTitle, String channelName, String hostName, String hostId, int hostAvatarRes);
        void onOpenDirectChatWithPeer(QluvxSocialManager.UserProfile user);
        void onShowCreateVoiceRoomDialog();
        void onShowEditProfileDialog();
        void onRefreshScreen();
    }

    public static class LiveCreatorItem {
        public final String id;
        public final String name;
        public final String levelLabel;
        public final String country;
        public final String viewersLabel;
        public final String tag1;
        public final String tag2;
        public final String filterCategory; // PK, Hot, New, Nearby
        public final boolean isGlobal;
        public final int avatarResId;
        public final String channelName;
        public final String topBadge; // "", "PK", "MULTI PK", "TOP.1", "TOP.2"
        public boolean isFollowing;

        public LiveCreatorItem(
                String id,
                String name,
                String levelLabel,
                String country,
                String viewersLabel,
                String tag1,
                String tag2,
                String filterCategory,
                boolean isGlobal,
                int avatarResId,
                String channelName,
                String topBadge,
                boolean isFollowing
        ) {
            this.id = id;
            this.name = name;
            this.levelLabel = levelLabel;
            this.country = country;
            this.viewersLabel = viewersLabel;
            this.tag1 = tag1;
            this.tag2 = tag2;
            this.filterCategory = filterCategory;
            this.isGlobal = isGlobal;
            this.avatarResId = avatarResId;
            this.channelName = channelName;
            this.topBadge = topBadge;
            this.isFollowing = isFollowing;
        }
    }

    private static final String[][] GLOBAL_COUNTRIES_LIST = new String[][]{
            {"🌐", "Global"}, {"🇮🇩", "Indonesia"},
            {"🇰🇷", "South Korea"}, {"🇵🇭", "Philippines"},
            {"🇻🇳", "Vietnam"}, {"🇮🇳", "India"},
            {"🇯🇵", "Japan"}, {"🇲🇾", "Malaysia"},
            {"🇹🇭", "Thailand"}, {"🇹🇼", "Taiwan"},
            {"🇺🇸", "United States"}, {"🇵🇰", "Pakistan"},
            {"🇸🇬", "Singapore"}, {"🇧🇩", "Bangladesh"},
            {"🇦🇪", "United Arab Emirates"}, {"🇸🇦", "Saudi Arabia"},
            {"🇹🇷", "Turkey"}, {"🇭🇰", "Hong Kong"},
            {"🌍", "Others"}, {"🇩🇿", "Algeria"},
            {"🇧🇭", "Bahrain"}, {"🇩🇯", "Djibouti"},
            {"🇪🇬", "Egypt"}, {"🇮🇶", "Iraq"},
            {"🇮🇱", "Israel"}, {"🇯🇴", "Jordan"},
            {"🇱🇾", "Libya"}, {"🇱🇰", "Sri Lanka"},
            {"🇲🇦", "Morocco"}, {"🇲🇷", "Mauritania"},
            {"🇲🇻", "Maldives"}, {"🇳🇵", "Nepal"},
            {"🇴🇲", "Oman"}, {"🇵🇸", "Palestine"},
            {"🇶🇦", "Qatar"}, {"🇸🇩", "Sudan"},
            {"🇸🇾", "Syria"}, {"🇹🇳", "Tunisia"},
            {"🇾🇪", "Yemen"}
    };

    private static boolean isHomeVoiceTab = false; // false = LIVE tab, true = VOICE tab
    private static String activePrimaryTab = "Recommended"; // Recommended, Global, Following
    private static String activeSubFilter = "All";          // All, PK, Hotpick, Game, Hot, New, Nearby
    private static String activeVoiceFollowingSub = "Owners"; // Owners, Rooms
    private static String selectedGlobalCountry = "Global";
    private static int activeRankingBannerMode = 0; // 0 = Gifting Ranking, 1 = Charm Ranking, 2 = Room Ranking
    private static int activePromoBannerIndex = 0;
    private static int wonderfulLiveBatchOffset = 0;
    private static final Set<String> dismissedWonderfulIds = new HashSet<>();
    private static final Set<String> historyBellSubscribed = new HashSet<>();
    private static String myCustomStoryStatus = "Feeling awesome today! Join my Live ✨";
    private static String activeProfileSubTab = "Profile";  // Profile, Album, Gifts, Moment
    private static String activeWalletSubTab = "Transactions"; // Transactions, Purchases
    private static final Set<String> blockedUserIds = new HashSet<>();
    private static List<LiveCreatorItem> liveCreators;

    public static List<LiveCreatorItem> getLiveCreators() {
        if (liveCreators == null) {
            liveCreators = new ArrayList<>();
            liveCreators.add(new LiveCreatorItem(
                    "12345678", "_Sawadde_ka's ...", "LV 21", "India", "21", "#Friends", "#Music",
                    "Hot", false, R.drawable.img_avatar_0, "qluvx_live_sawadde", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "23456789", "ça va?", "LV 25", "India", "21", "#Chat", "#Chill",
                    "Hot", false, R.drawable.img_avatar_1, "qluvx_live_cava", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "34567890", "😌 Aryan Fitness", "LV 40", "India", "7", "#Gym", "#Friends",
                    "Nearby", false, R.drawable.img_avatar_2, "qluvx_live_aryan", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "45678901", "🎤RaHaT_...", "LV 28", "India", "31", "#PKBattle", "#Music",
                    "PK", false, R.drawable.img_avatar_3, "qluvx_live_rahat", "PK", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "56789012", "9016327676 a...", "LV 30", "India", "44", "#Vibes", "#Hindi",
                    "New", false, R.drawable.img_avatar_4, "qluvx_live_kabir", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "67890123", "🤌Unsaid_❤️‍🔥's ...", "LV 29", "India", "51", "#PKBattle", "#Live",
                    "PK", false, R.drawable.img_avatar_5, "qluvx_live_unsaid", "PK", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "78901234", "g • Seoul Live", "LV 37", "Republic of Korea", "28", "#KPop", "#Global",
                    "Hot", true, R.drawable.img_avatar_0, "qluvx_live_seoul", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "89012345", "morning ✨", "LV 41", "India", "46", "#Top2", "#PK",
                    "PK", true, R.drawable.img_avatar_2, "qluvx_live_morning", "TOP.2", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "90123456", "차에서 잠깐", "LV 28", "Republic of Korea", "19", "#Drive", "#Talk",
                    "New", true, R.drawable.img_avatar_3, "qluvx_live_korea2", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "91234567", "Jakarta Vibes", "LV 20", "Indonesia", "36", "#Indo", "#Music",
                    "Nearby", true, R.drawable.img_avatar_4, "qluvx_live_indo", "", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "92345678", "👑 King Live Stage", "LV 30", "Republic of Korea", "84", "#Top1", "#Crown",
                    "PK", true, R.drawable.img_avatar_1, "qluvx_live_top1", "TOP.1", false
            ));
            liveCreators.add(new LiveCreatorItem(
                    "93456789", "🖤SABA_صبا's L...", "LV 22", "India", "26", "#MultiPK", "#Friends",
                    "PK", true, R.drawable.img_avatar_5, "qluvx_live_saba", "MULTI PK", false
            ));
        }
        return liveCreators;
    }

    // =========================================================================
    // 1. HOME SCREEN (Exact Video Layout Branded as QLUVX + All 21 Home Buttons)
    // =========================================================================
    public static void renderHomeScreen(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final VoiceRoomModels.RoomRepository roomRepo,
            final HostNavigationCallbacks callbacks
    ) {
        // 1A. Top Header Row: [ LIVE ] [ VOICE ] on left + [ VIP ] [ 🔍 ] [ 🔔 ] [ 📹+ / 🎙️+ ] on right
        LinearLayout topModeBar = new LinearLayout(activity);
        topModeBar.setOrientation(LinearLayout.HORIZONTAL);
        topModeBar.setGravity(Gravity.CENTER_VERTICAL);
        topModeBar.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 8));

        // LIVE Tab
        TextView tvLiveTab = new TextView(activity);
        tvLiveTab.setText("LIVE");
        tvLiveTab.setTextColor(!isHomeVoiceTab ? Color.WHITE : Color.rgb(148, 163, 184));
        tvLiveTab.setTextSize(TypedValue.COMPLEX_UNIT_SP, !isHomeVoiceTab ? 21f : 16.5f);
        tvLiveTab.setTypeface(Typeface.create("sans-serif-black", !isHomeVoiceTab ? Typeface.BOLD : Typeface.NORMAL));
        tvLiveTab.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 14), UiStyleHelper.dp(activity, 4));
        tvLiveTab.setOnClickListener(v -> {
            isHomeVoiceTab = false;
            callbacks.onRefreshScreen();
        });

        // VOICE Tab
        TextView tvVoiceTab = new TextView(activity);
        tvVoiceTab.setText("VOICE");
        tvVoiceTab.setTextColor(isHomeVoiceTab ? Color.WHITE : Color.rgb(148, 163, 184));
        tvVoiceTab.setTextSize(TypedValue.COMPLEX_UNIT_SP, isHomeVoiceTab ? 21f : 16.5f);
        tvVoiceTab.setTypeface(Typeface.create("sans-serif-black", isHomeVoiceTab ? Typeface.BOLD : Typeface.NORMAL));
        tvVoiceTab.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 12), UiStyleHelper.dp(activity, 4));
        tvVoiceTab.setOnClickListener(v -> {
            isHomeVoiceTab = true;
            callbacks.onRefreshScreen();
        });

        topModeBar.addView(tvLiveTab);
        topModeBar.addView(tvVoiceTab);

        View spacer = new View(activity);
        topModeBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        // VIP Chip
        TextView btnVip = UiStyleHelper.createStatusChip(
                activity,
                "👑 VIP",
                Color.argb(65, 245, 158, 11),
                UiStyleHelper.COLOR_WARNING_AMBER,
                0
        );
        btnVip.setOnClickListener(v -> callbacks.onSelectMenu(8));

        // Search Chip
        TextView btnSearch = UiStyleHelper.createStatusChip(
                activity,
                "🔍",
                Color.argb(55, 255, 255, 255),
                Color.WHITE,
                0
        );
        btnSearch.setOnClickListener(v -> callbacks.onSelectMenu(1));

        // Notifications Chip
        TextView btnNotif = UiStyleHelper.createStatusChip(
                activity,
                "🔔",
                Color.argb(55, 255, 255, 255),
                Color.WHITE,
                0
        );
        btnNotif.setOnClickListener(v -> showNotificationsDialog(activity, social, callbacks));

        // Top-Right Camera+ (on LIVE) or Mic+ (on VOICE) button matching the video!
        TextView btnTopRightCreate = new TextView(activity);
        btnTopRightCreate.setText(isHomeVoiceTab ? "🎙️+" : "📹+");
        btnTopRightCreate.setTextColor(Color.WHITE);
        btnTopRightCreate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        btnTopRightCreate.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        btnTopRightCreate.setGravity(Gravity.CENTER);
        int crPadH = UiStyleHelper.dp(activity, 10);
        int crPadV = UiStyleHelper.dp(activity, 5);
        btnTopRightCreate.setPadding(crPadH, crPadV, crPadH, crPadV);
        btnTopRightCreate.setBackground(UiStyleHelper.roundedBox(
                activity,
                Color.argb(55, 255, 255, 255),
                16f,
                Color.WHITE,
                1.2f
        ));
        btnTopRightCreate.setOnClickListener(v -> {
            if (isHomeVoiceTab) {
                callbacks.onShowCreateVoiceRoomDialog();
            } else {
                // Opens the Pre-LIVE Camera Setup Screen (matching 01:21 - 01:33 of the video)
                callbacks.onOpenLiveVideoRoom("Your LIVE Title", "qluvx_live_video_pk", "Pre-LIVE Setup", "10001", R.drawable.img_avatar_0);
            }
        });

        topModeBar.addView(btnVip, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 5), 0));
        topModeBar.addView(btnSearch, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 5), 0));
        topModeBar.addView(btnNotif, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        topModeBar.addView(btnTopRightCreate);
        container.addView(topModeBar);

        // 1B. Top Circles Strip: Friends / Gaming / Following / My Story / Add Story (matching 00:10 & 01:03 in video)
        container.addView(
                buildStoriesAndFriendsStrip(activity, social, callbacks),
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10))
        );

        // 1C. Dynamic Ranking Banner (VOICE Charm / Room / Gifting Ranking & LIVE Gifting / Charm Ranking with 2nd / 1st / 3rd Podium)
        container.addView(
                buildGiftingRankingBanner(activity),
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10))
        );

        // 1D. Primary Feed Tabs: Recommended | Global ▾ | Following
        LinearLayout primaryTabRow = new LinearLayout(activity);
        primaryTabRow.setOrientation(LinearLayout.HORIZONTAL);
        primaryTabRow.setGravity(Gravity.CENTER_VERTICAL);
        primaryTabRow.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 4));

        String[] primaryTabs = new String[]{"Recommended", "Global", "Following"};
        for (final String pTab : primaryTabs) {
            boolean active = pTab.equalsIgnoreCase(activePrimaryTab);
            TextView tv = new TextView(activity);
            if ("Global".equals(pTab)) {
                String cLabel = "Global".equalsIgnoreCase(selectedGlobalCountry) ? "Global ▾" : (selectedGlobalCountry + " ▾");
                tv.setText(cLabel);
            } else {
                tv.setText(pTab);
            }
            tv.setTextColor(active ? Color.WHITE : Color.rgb(148, 163, 184));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, active ? 18f : 15.5f);
            tv.setTypeface(Typeface.create("sans-serif-black", active ? Typeface.BOLD : Typeface.NORMAL));
            tv.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 18), UiStyleHelper.dp(activity, 4));

            tv.setOnClickListener(v -> {
                if ("Global".equals(pTab)) {
                    activePrimaryTab = "Global";
                    showGlobalCountryBottomSheetDialog(activity, callbacks);
                } else {
                    activePrimaryTab = pTab;
                    callbacks.onRefreshScreen();
                }
            });
            primaryTabRow.addView(tv);
        }
        container.addView(primaryTabRow);

        // 1E. Stylized Multi-Color "QLUVX" Brand Watermark Header (replacing "Walla" in the video!)
        container.addView(
                buildStylizedQluvxBrandHeader(activity, callbacks),
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 6))
        );

        // 1F. Sub-Filter Pills Row (adapts to LIVE vs VOICE and Recommended/Global vs Following)
        HorizontalScrollView filterScroll = new HorizontalScrollView(activity);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = new LinearLayout(activity);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        filterRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));

        if (isHomeVoiceTab && "Following".equalsIgnoreCase(activePrimaryTab)) {
            String[] fModes = new String[]{"Owners", "Rooms"};
            for (final String fm : fModes) {
                boolean active = fm.equalsIgnoreCase(activeVoiceFollowingSub);
                TextView pill = new TextView(activity);
                pill.setText(fm);
                pill.setTextColor(active ? Color.WHITE : Color.rgb(148, 163, 184));
                pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                pill.setPadding(UiStyleHelper.dp(activity, 16), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 16), UiStyleHelper.dp(activity, 6));
                pill.setBackground(UiStyleHelper.roundedBox(
                        activity,
                        active ? Color.rgb(45, 50, 62) : Color.rgb(24, 26, 34),
                        16f,
                        active ? Color.argb(90, 255, 255, 255) : 0,
                        1f
                ));
                pill.setOnClickListener(v -> {
                    activeVoiceFollowingSub = fm;
                    callbacks.onRefreshScreen();
                });
                filterRow.addView(pill, UiStyleHelper.params(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0, 0, UiStyleHelper.dp(activity, 8), 0
                ));
            }
        } else {
            String[] subFilters = isHomeVoiceTab
                    ? new String[]{"All", "PK", "Hotpick", "Game", "Hot", "New", "Nearby"}
                    : new String[]{"All", "PK", "Hot", "New", "Nearby"};
            for (final String fName : subFilters) {
                boolean active = fName.equalsIgnoreCase(activeSubFilter);
                TextView pill = new TextView(activity);
                pill.setText(fName);
                pill.setTextColor(active ? Color.WHITE : Color.rgb(148, 163, 184));
                pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                pill.setPadding(UiStyleHelper.dp(activity, 16), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 16), UiStyleHelper.dp(activity, 6));
                pill.setBackground(UiStyleHelper.roundedBox(
                        activity,
                        active ? Color.rgb(48, 52, 66) : Color.rgb(24, 26, 34),
                        16f,
                        active ? Color.argb(110, 255, 255, 255) : 0,
                        1f
                ));
                pill.setOnClickListener(v -> {
                    activeSubFilter = fName;
                    callbacks.onRefreshScreen();
                });
                filterRow.addView(pill, UiStyleHelper.params(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0, 0, UiStyleHelper.dp(activity, 8), 0
                ));
            }
        }

        // Compact Quick Open Live Room & Open Voice Room chips in the filter bar so all 21 buttons stay 1-tap accessible
        TextView btnOpenLiveQuick = UiStyleHelper.createStatusChip(
                activity,
                "📹 Open Live Room",
                Color.argb(75, 236, 72, 153),
                Color.WHITE,
                UiStyleHelper.COLOR_NEON_PINK
        );
        btnOpenLiveQuick.setOnClickListener(v ->
                callbacks.onOpenLiveVideoRoom("Your LIVE Title", "qluvx_live_video_pk", "Pre-LIVE Setup", "10001", R.drawable.img_avatar_0)
        );
        filterRow.addView(btnOpenLiveQuick, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));

        TextView btnOpenVoiceQuick = UiStyleHelper.createStatusChip(
                activity,
                "🎙️ Open Voice Room",
                Color.argb(75, 124, 58, 237),
                Color.WHITE,
                UiStyleHelper.COLOR_AGORA_CYAN
        );
        btnOpenVoiceQuick.setOnClickListener(v ->
                callbacks.onOpenVoiceRoom("⚡ CHINNU ✨", BuildConfig.AGORA_DEFAULT_CHANNEL)
        );
        filterRow.addView(btnOpenVoiceQuick);

        filterScroll.addView(filterRow);
        container.addView(filterScroll);

        // 1G. Main Feed Content:
        if (!isHomeVoiceTab) {
            if ("Following".equalsIgnoreCase(activePrimaryTab)) {
                container.addView(
                        buildLiveFollowingView(activity, callbacks),
                        UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 14))
                );
            } else {
                container.addView(
                        buildLiveCreatorsTwoColumnGrid(activity, callbacks),
                        UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 14))
                );
            }
        } else {
            container.addView(
                    buildVoiceRoomCardsList(activity, roomRepo, callbacks),
                    UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 14))
            );
        }
    }

    private static View buildStylizedQluvxBrandHeader(final Activity activity, final HostNavigationCallbacks callbacks) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 2));

        String[] letters = new String[]{"Q", "L", "U", "V", "X"};
        int[] colors = new int[]{
                Color.rgb(59, 130, 246),  // Royal Blue Q
                Color.rgb(236, 72, 153),  // Neon Pink L
                Color.rgb(168, 85, 247),  // Purple U
                Color.rgb(56, 189, 248),  // Cyan V
                Color.rgb(245, 158, 11)   // Amber Gold X
        };
        for (int i = 0; i < letters.length; i++) {
            TextView tv = new TextView(activity);
            tv.setText(letters[i]);
            tv.setTextColor(colors[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            tv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC));
            tv.setPadding(UiStyleHelper.dp(activity, 1), 0, UiStyleHelper.dp(activity, 1), 0);
            row.addView(tv);
        }
        row.setOnClickListener(v -> {
            Toast.makeText(activity, "✨ Refreshed QLUVX " + (isHomeVoiceTab ? "VOICE" : "LIVE") + " Feed!", Toast.LENGTH_SHORT).show();
            callbacks.onRefreshScreen();
        });
        return row;
    }

    private static void showGlobalCountryBottomSheetDialog(
            final Activity activity,
            final HostNavigationCallbacks callbacks
    ) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(26, 28, 35));
        int pad = UiStyleHelper.dp(activity, 14);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(activity);
        title.setText("🌐 Select Global Region / Country");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        title.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        root.addView(title);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.VERTICAL);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        for (int i = 0; i < GLOBAL_COUNTRIES_LIST.length; i += 2) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(2f);

            for (int c = 0; c < 2; c++) {
                int idx = i + c;
                if (idx >= GLOBAL_COUNTRIES_LIST.length) {
                    View empty = new View(activity);
                    row.addView(empty, new LinearLayout.LayoutParams(0, 1, 1f));
                    continue;
                }
                final String flag = GLOBAL_COUNTRIES_LIST[idx][0];
                final String countryName = GLOBAL_COUNTRIES_LIST[idx][1];
                boolean isSelected = countryName.equalsIgnoreCase(selectedGlobalCountry);

                TextView pill = new TextView(activity);
                pill.setText(flag + "  " + countryName);
                pill.setSingleLine(true);
                pill.setGravity(Gravity.CENTER);
                pill.setTextColor(isSelected ? Color.WHITE : Color.rgb(203, 213, 225));
                pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                pill.setMinHeight(UiStyleHelper.dp(activity, 42));
                pill.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 8));
                pill.setBackground(UiStyleHelper.roundedBox(
                        activity,
                        isSelected ? Color.rgb(124, 58, 237) : Color.rgb(42, 45, 56),
                        22f,
                        isSelected ? Color.rgb(192, 132, 252) : 0,
                        1f
                ));
                pill.setOnClickListener(v -> {
                    selectedGlobalCountry = countryName;
                    activePrimaryTab = "Global";
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    callbacks.onRefreshScreen();
                });

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(
                        c == 0 ? 0 : UiStyleHelper.dp(activity, 5),
                        0,
                        c == 0 ? UiStyleHelper.dp(activity, 5) : 0,
                        UiStyleHelper.dp(activity, 8)
                );
                row.addView(pill, lp);
            }
            grid.addView(row);
        }

        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(activity, 360)));

        dialogRef[0] = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(root)
                .setNegativeButton("Close", null)
                .create();
        dialogRef[0].show();
    }

    private static View buildStoriesAndFriendsStrip(
            final Activity activity,
            final QluvxSocialManager social,
            final HostNavigationCallbacks callbacks
    ) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int avSize = UiStyleHelper.dp(activity, 64);
        int innerPx = UiStyleHelper.dp(activity, 56);

        // Friends & Category Circles (matching 00:10 & 01:03 in the video: Friends, Friends, Gaming, Gaming, Following)
        String[] circleTags = isHomeVoiceTab
                ? new String[]{"Friends", "Friends", "Gaming", "Gaming", "Friends"}
                : new String[]{"Friends", "Following", "Friends", "PK Live", "Friends"};
        int[] tagColors = isHomeVoiceTab
                ? new int[]{Color.rgb(16, 185, 129), Color.rgb(16, 185, 129), Color.rgb(16, 185, 129), Color.rgb(16, 185, 129), Color.rgb(16, 185, 129)}
                : new int[]{Color.rgb(236, 72, 153), Color.rgb(168, 85, 247), Color.rgb(16, 185, 129), Color.rgb(245, 158, 11), Color.rgb(59, 130, 246)};

        List<LiveCreatorItem> creators = getLiveCreators();
        for (int i = 0; i < 5 && i < creators.size(); i++) {
            final LiveCreatorItem friend = creators.get(i);
            final String badgeLabel = circleTags[i];
            final int ringColor = tagColors[i];

            FrameLayout itemFrame = new FrameLayout(activity);
            itemFrame.setPadding(0, 0, UiStyleHelper.dp(activity, 12), 0);

            FrameLayout fWrap = new FrameLayout(activity);
            fWrap.setLayoutParams(new FrameLayout.LayoutParams(avSize, avSize, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
            fWrap.setBackground(UiStyleHelper.circle(activity, Color.rgb(18, 20, 28), ringColor, 2.4f));

            ImageView fIv = new ImageView(activity);
            fIv.setImageResource(friend.avatarResId);
            fIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            clipViewOval(fIv);
            fWrap.addView(fIv, new FrameLayout.LayoutParams(innerPx, innerPx, Gravity.CENTER));

            TextView bottomPill = new TextView(activity);
            bottomPill.setText(badgeLabel);
            bottomPill.setTextColor(Color.WHITE);
            bottomPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            bottomPill.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            bottomPill.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 2));
            bottomPill.setBackground(UiStyleHelper.roundedBox(activity, ringColor, 10f, Color.rgb(14, 15, 20), 1f));

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.addView(fWrap);
            LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pillLp.setMargins(0, -UiStyleHelper.dp(activity, 10), 0, 0);
            col.addView(bottomPill, pillLp);

            col.setOnClickListener(v -> {
                if (isHomeVoiceTab) {
                    callbacks.onOpenVoiceRoom("⚡ CHINNU ✨", friend.channelName);
                } else {
                    callbacks.onOpenLiveVideoRoom(friend.name, friend.channelName, friend.name, friend.id, friend.avatarResId);
                }
            });
            itemFrame.addView(col);
            row.addView(itemFrame);
        }

        // My Story circle
        LinearLayout myStoryCol = new LinearLayout(activity);
        myStoryCol.setOrientation(LinearLayout.VERTICAL);
        myStoryCol.setGravity(Gravity.CENTER_HORIZONTAL);
        myStoryCol.setPadding(0, 0, UiStyleHelper.dp(activity, 12), 0);

        FrameLayout myAvWrap = new FrameLayout(activity);
        myAvWrap.setLayoutParams(new LinearLayout.LayoutParams(avSize, avSize));
        myAvWrap.setBackground(UiStyleHelper.circle(activity, Color.rgb(18, 20, 28), UiStyleHelper.COLOR_NEON_PINK, 2.2f));

        ImageView myIv = new ImageView(activity);
        myIv.setImageResource(R.drawable.img_avatar_0);
        myIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        clipViewOval(myIv);
        myAvWrap.addView(myIv, new FrameLayout.LayoutParams(innerPx, innerPx, Gravity.CENTER));

        TextView tvMyStory = new TextView(activity);
        tvMyStory.setText("My Story");
        tvMyStory.setTextColor(Color.WHITE);
        tvMyStory.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvMyStory.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvMyStory.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 2));
        tvMyStory.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_NEON_PINK, 10f, 0, 0));
        LinearLayout.LayoutParams msLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msLp.setMargins(0, -UiStyleHelper.dp(activity, 10), 0, 0);

        myStoryCol.addView(myAvWrap);
        myStoryCol.addView(tvMyStory, msLp);
        myStoryCol.setOnClickListener(v -> showMyStoryAndAddStoryDialog(activity, callbacks));
        row.addView(myStoryCol);

        // Add Story circle
        LinearLayout addStoryCol = new LinearLayout(activity);
        addStoryCol.setOrientation(LinearLayout.VERTICAL);
        addStoryCol.setGravity(Gravity.CENTER_HORIZONTAL);
        addStoryCol.setPadding(0, 0, UiStyleHelper.dp(activity, 12), 0);

        FrameLayout addCircle = new FrameLayout(activity);
        addCircle.setLayoutParams(new LinearLayout.LayoutParams(avSize, avSize));
        addCircle.setBackground(UiStyleHelper.circle(activity, Color.rgb(28, 30, 40), UiStyleHelper.COLOR_AGORA_CYAN, 2f));

        TextView plusCenter = new TextView(activity);
        plusCenter.setText("＋");
        plusCenter.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        plusCenter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        plusCenter.setGravity(Gravity.CENTER);
        addCircle.addView(plusCenter, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvAddStory = new TextView(activity);
        tvAddStory.setText("Add Story");
        tvAddStory.setTextColor(Color.WHITE);
        tvAddStory.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvAddStory.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvAddStory.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 2));
        tvAddStory.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(59, 130, 246), 10f, 0, 0));
        LinearLayout.LayoutParams asLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        asLp.setMargins(0, -UiStyleHelper.dp(activity, 10), 0, 0);

        addStoryCol.addView(addCircle);
        addStoryCol.addView(tvAddStory, asLp);
        addStoryCol.setOnClickListener(v -> showMyStoryAndAddStoryDialog(activity, callbacks));
        row.addView(addStoryCol);

        scroll.addView(row);
        return scroll;
    }

    private static void showMyStoryAndAddStoryDialog(final Activity activity, final HostNavigationCallbacks callbacks) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        box.setPadding(pad, pad, pad, pad);

        TextView currentTv = new TextView(activity);
        currentTv.setText("📸 Current Story: \"" + myCustomStoryStatus + "\"");
        currentTv.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        currentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        currentTv.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        box.addView(currentTv);

        final EditText etStory = new EditText(activity);
        etStory.setHint("Write your new 24h Story or Live status...");
        etStory.setText(myCustomStoryStatus);
        etStory.setSingleLine(true);
        box.addView(etStory);

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("📸 My Story & + Add Story")
                .setView(box)
                .setPositiveButton("+ Add Story", (d, w) -> {
                    String s = etStory.getText().toString().trim();
                    if (!s.isEmpty()) {
                        myCustomStoryStatus = s;
                    }
                    Toast.makeText(activity, "✨ Story published to Friends feed!", Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                })
                .setNeutralButton("Open My Live Room", (d, w) ->
                        callbacks.onOpenLiveVideoRoom("Your LIVE Title", "qluvx_live_video_pk", "Pre-LIVE Setup", "10001", R.drawable.img_avatar_0)
                )
                .setNegativeButton("Close", null)
                .show();
    }

    private static View buildGiftingRankingBanner(final Activity activity) {
        LinearLayout outer = new LinearLayout(activity);
        outer.setOrientation(LinearLayout.VERTICAL);

        final int modeCount = isHomeVoiceTab ? 3 : 2;
        final int mode = activeRankingBannerMode % modeCount;

        int startColor;
        int endColor;
        int strokeColor;
        String iconEmoji;
        String bannerLabel;

        if (!isHomeVoiceTab) {
            if (mode == 0) {
                startColor = Color.rgb(66, 38, 12);
                endColor = Color.rgb(168, 102, 18);
                strokeColor = Color.rgb(251, 191, 36);
                iconEmoji = "🪙";
                bannerLabel = "LIVE Gifting\nRanking";
            } else {
                startColor = Color.rgb(88, 22, 65);
                endColor = Color.rgb(176, 38, 118);
                strokeColor = Color.rgb(244, 114, 182);
                iconEmoji = "💖";
                bannerLabel = "LIVE Charm\nRanking";
            }
        } else {
            if (mode == 0) {
                startColor = Color.rgb(15, 62, 66);
                endColor = Color.rgb(20, 118, 110);
                strokeColor = Color.rgb(45, 212, 191);
                iconEmoji = "💖";
                bannerLabel = "VOICE Charm\nRanking";
            } else if (mode == 1) {
                startColor = Color.rgb(18, 52, 86);
                endColor = Color.rgb(29, 104, 158);
                strokeColor = Color.rgb(56, 189, 248);
                iconEmoji = "⭐";
                bannerLabel = "Room\nRanking";
            } else {
                startColor = Color.rgb(66, 38, 12);
                endColor = Color.rgb(168, 102, 18);
                strokeColor = Color.rgb(251, 191, 36);
                iconEmoji = "🪙";
                bannerLabel = "VOICE Gifting\nRanking";
            }
        }

        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        int padH = UiStyleHelper.dp(activity, 12);
        int padV = UiStyleHelper.dp(activity, 8);
        banner.setPadding(padH, padV, padH, padV);
        banner.setBackground(UiStyleHelper.gradientBox(activity, startColor, endColor, 14f, strokeColor, 1f));

        TextView trophyIcon = new TextView(activity);
        trophyIcon.setText(iconEmoji);
        trophyIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
        banner.addView(trophyIcon);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 6), 0);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView bannerTitle = new TextView(activity);
        bannerTitle.setText(bannerLabel);
        bannerTitle.setTextColor(Color.WHITE);
        bannerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        bannerTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        titleCol.addView(bannerTitle);

        TextView subHint = new TextView(activity);
        subHint.setText("Tap to switch • View Leaderboard");
        subHint.setTextColor(Color.argb(200, 255, 255, 255));
        subHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        titleCol.addView(subHint);

        banner.addView(titleCol);

        // 2nd, 1st, 3rd Podium Avatars exactly like the video
        int[] podiumAvatars = new int[]{
                VoiceRoomModels.getAvatarResId(mode + 1),
                VoiceRoomModels.getAvatarResId(mode),
                VoiceRoomModels.getAvatarResId(mode + 2)
        };
        String[] podiumLabels = new String[]{"2nd", "1st", "3rd"};
        int[] podiumColors = new int[]{
                Color.rgb(203, 213, 225),
                Color.rgb(251, 191, 36),
                Color.rgb(249, 115, 22)
        };

        for (int p = 0; p < 3; p++) {
            LinearLayout podCol = new LinearLayout(activity);
            podCol.setOrientation(LinearLayout.VERTICAL);
            podCol.setGravity(Gravity.CENTER_HORIZONTAL);
            podCol.setPadding(UiStyleHelper.dp(activity, 3), 0, UiStyleHelper.dp(activity, 3), 0);

            int sz = (p == 1) ? 42 : 34;
            View av = UiStyleHelper.createCircularAvatar(activity, podiumAvatars[p], sz, podiumColors[p], false);

            TextView badge = new TextView(activity);
            badge.setText(podiumLabels[p]);
            badge.setTextColor(Color.BLACK);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
            badge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            badge.setPadding(UiStyleHelper.dp(activity, 5), 0, UiStyleHelper.dp(activity, 5), 0);
            badge.setBackground(UiStyleHelper.roundedBox(activity, podiumColors[p], 6f, Color.WHITE, 0.8f));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(0, -UiStyleHelper.dp(activity, 6), 0, 0);

            podCol.addView(av);
            podCol.addView(badge, bLp);
            banner.addView(podCol);
        }

        banner.setOnClickListener(v -> {
            activeRankingBannerMode++;
            showGiftingRankingLeaderboardDialog(activity, isHomeVoiceTab);
        });
        outer.addView(banner);

        // Compact quick buttons for Live Gifting Ranking & Voice Gifting Ranking
        LinearLayout rankBtnsRow = new LinearLayout(activity);
        rankBtnsRow.setOrientation(LinearLayout.HORIZONTAL);
        rankBtnsRow.setPadding(0, UiStyleHelper.dp(activity, 4), 0, 0);

        TextView btnLiveRank = new TextView(activity);
        btnLiveRank.setText("🪙 Live Gifting Ranking");
        btnLiveRank.setTextColor(Color.rgb(253, 224, 71));
        btnLiveRank.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        btnLiveRank.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        btnLiveRank.setGravity(Gravity.CENTER);
        btnLiveRank.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 4));
        btnLiveRank.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(32, 26, 20), 8f, Color.rgb(120, 82, 24), 1f));
        btnLiveRank.setOnClickListener(v -> showGiftingRankingLeaderboardDialog(activity, false));
        btnLiveRank.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnVoiceRank = new TextView(activity);
        btnVoiceRank.setText("💖 Voice Gifting Ranking");
        btnVoiceRank.setTextColor(Color.rgb(94, 234, 212));
        btnVoiceRank.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        btnVoiceRank.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        btnVoiceRank.setGravity(Gravity.CENTER);
        btnVoiceRank.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 4));
        btnVoiceRank.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(16, 36, 40), 8f, Color.rgb(28, 100, 96), 1f));
        btnVoiceRank.setOnClickListener(v -> showGiftingRankingLeaderboardDialog(activity, true));
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        vLp.setMargins(UiStyleHelper.dp(activity, 6), 0, 0, 0);
        btnVoiceRank.setLayoutParams(vLp);

        rankBtnsRow.addView(btnLiveRank);
        rankBtnsRow.addView(btnVoiceRank);
        outer.addView(rankBtnsRow);

        return outer;
    }

    private static void showGiftingRankingLeaderboardDialog(final Activity activity, boolean isVoiceRanking) {
        String title = isVoiceRanking ? "🏆 VOICE Gifting & Charm Ranking" : "🏆 LIVE Gifting & Charm Ranking";
        String[] ranks = isVoiceRanking
                ? new String[]{
                "🥇 1st • ⚡ CHINNU ✨ (ID: 880124) — 🪙 148,500 Coins • SVIP Crown",
                "🥈 2nd • fukrey boys (ID: 785772) — 🪙 112,200 Coins • Royal Yacht",
                "🥉 3rd • 🍁Nawabzade's🍁 (ID: 567890) — 🪙 94,800 Coins • Love Castle",
                "4th • House_STARK🐺 (ID: 456789) — 🪙 68,400 Coins",
                "5th • Eshwar_Sai_❤️ (ID: 345678) — 🪙 52,100 Coins"
        }
                : new String[]{
                "🥇 1st • sushmita 👑 TOP.1 — 🔥 245,000 PK Gift Points",
                "🥈 2nd • 🌺 Ishani 🌺 👑 TOP.2 — 🔥 189,400 PK Gift Points",
                "🥉 3rd • 🦋 Komolika 🦋 — 🔥 134,900 PK Gift Points",
                "4th • 🇰🇷 도윤 • Doyun — 🔥 98,200 PK Gift Points",
                "5th • 🇮🇩 Putri Cantik — 🔥 76,500 PK Gift Points"
        };

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setItems(ranks, (d, which) ->
                        Toast.makeText(activity, "Viewing Top Creator #" + (which + 1), Toast.LENGTH_SHORT).show()
                )
                .setPositiveButton("Close", null)
                .show();
    }

    private static View buildLiveCreatorsTwoColumnGrid(
            final Activity activity,
            final HostNavigationCallbacks callbacks
    ) {
        LinearLayout gridWrap = new LinearLayout(activity);
        gridWrap.setOrientation(LinearLayout.VERTICAL);

        List<LiveCreatorItem> filtered = new ArrayList<>();
        for (LiveCreatorItem item : getLiveCreators()) {
            if ("Global".equalsIgnoreCase(activePrimaryTab)) {
                if (!"Global".equalsIgnoreCase(selectedGlobalCountry)
                        && !item.country.toLowerCase().contains(selectedGlobalCountry.toLowerCase())) {
                    continue;
                }
            }
            if (!"All".equalsIgnoreCase(activeSubFilter) && !activeSubFilter.equalsIgnoreCase(item.filterCategory)) {
                continue;
            }
            filtered.add(item);
        }
        if (filtered.isEmpty()) {
            filtered.addAll(getLiveCreators());
        }

        for (int i = 0; i < filtered.size(); i += 2) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(2f);
            if (i > 0) {
                row.setPadding(0, UiStyleHelper.dp(activity, 8), 0, 0);
            }

            View cardLeft = buildSingleLiveCreatorCard(activity, filtered.get(i), callbacks);
            LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lpLeft.setMargins(0, 0, UiStyleHelper.dp(activity, 4), 0);
            row.addView(cardLeft, lpLeft);

            if (i + 1 < filtered.size()) {
                View cardRight = buildSingleLiveCreatorCard(activity, filtered.get(i + 1), callbacks);
                LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lpRight.setMargins(UiStyleHelper.dp(activity, 4), 0, 0, 0);
                row.addView(cardRight, lpRight);
            } else {
                View empty = new View(activity);
                row.addView(empty, new LinearLayout.LayoutParams(0, 1, 1f));
            }

            gridWrap.addView(row);
        }

        return gridWrap;
    }

    /**
     * Builds the exact LIVE creator card from the video:
     * - Square/portrait rounded card (12dp)
     * - Top-left badge: PK / MULTI PK / 👑 TOP.1 / 👑 TOP.2
     * - Bottom-left stack: [LV 21] blue/orange pill + 📍 Country + Creator Name
     * - Bottom-right: 🏆 21 count + Follow (+) button
     */
    private static View buildSingleLiveCreatorCard(
            final Activity activity,
            final LiveCreatorItem creator,
            final HostNavigationCallbacks callbacks
    ) {
        FrameLayout card = new FrameLayout(activity);
        int cardH = UiStyleHelper.dp(activity, 186);
        card.setMinimumHeight(cardH);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 12));
            }
        });
        card.setClipToOutline(true);
        card.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(24, 24, 30), 12f, Color.rgb(42, 42, 54), 1f));

        // Full Portrait Image
        ImageView ivCover = new ImageView(activity);
        ivCover.setImageResource(creator.avatarResId);
        ivCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(ivCover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cardH));

        // Top Overlay: PK / MULTI PK / 👑 TOP.1 badge on left + Follow (+) button on right
        LinearLayout topOverlay = new LinearLayout(activity);
        topOverlay.setOrientation(LinearLayout.HORIZONTAL);
        topOverlay.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiStyleHelper.dp(activity, 8);
        topOverlay.setPadding(pad, pad, pad, pad);

        if (creator.topBadge != null && !creator.topBadge.isEmpty()) {
            TextView topBadge = new TextView(activity);
            topBadge.setText(creator.topBadge);
            topBadge.setTextColor(Color.WHITE);
            topBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            topBadge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            topBadge.setPadding(UiStyleHelper.dp(activity, 7), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 7), UiStyleHelper.dp(activity, 2));
            int badgeBg = creator.topBadge.contains("TOP")
                    ? Color.rgb(217, 119, 6)
                    : (creator.topBadge.contains("MULTI") ? Color.rgb(37, 99, 235) : Color.rgb(236, 72, 153));
            topBadge.setBackground(UiStyleHelper.roundedBox(activity, badgeBg, 8f, 0, 0));
            topOverlay.addView(topBadge);
        }

        View topSpacer = new View(activity);
        topOverlay.addView(topSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        // Follow Creator (+) button
        TextView btnFollowCreator = new TextView(activity);
        btnFollowCreator.setText(creator.isFollowing ? "✓" : "＋");
        btnFollowCreator.setTextColor(Color.WHITE);
        btnFollowCreator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        btnFollowCreator.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        btnFollowCreator.setGravity(Gravity.CENTER);
        int fBtnSize = UiStyleHelper.dp(activity, 24);
        btnFollowCreator.setBackground(UiStyleHelper.circle(
                activity,
                creator.isFollowing ? UiStyleHelper.COLOR_LIVE_GREEN : Color.argb(200, 236, 72, 153),
                Color.WHITE,
                1f
        ));
        btnFollowCreator.setOnClickListener(v -> {
            creator.isFollowing = !creator.isFollowing;
            Toast.makeText(
                    activity,
                    creator.isFollowing ? ("💖 Followed " + creator.name) : ("Unfollowed " + creator.name),
                    Toast.LENGTH_SHORT
            ).show();
            callbacks.onRefreshScreen();
        });
        topOverlay.addView(btnFollowCreator, new LinearLayout.LayoutParams(fBtnSize, fBtnSize));

        card.addView(topOverlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // Bottom Scrim Overlay matching video: [LV 21] + 📍 Country, and below it Creator Name + 🏆 21
        LinearLayout bottomOverlay = new LinearLayout(activity);
        bottomOverlay.setOrientation(LinearLayout.VERTICAL);
        bottomOverlay.setPadding(pad, UiStyleHelper.dp(activity, 22), pad, pad);
        bottomOverlay.setBackground(UiStyleHelper.bottomScrimGradient(activity, 12f));

        LinearLayout metaRow = new LinearLayout(activity);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvLv = new TextView(activity);
        tvLv.setText("💎 " + creator.levelLabel);
        tvLv.setTextColor(Color.WHITE);
        tvLv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        tvLv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvLv.setPadding(UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 1), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 1));
        int lvColor = creator.levelLabel.contains("4") ? Color.rgb(234, 88, 12) : Color.rgb(16, 185, 129);
        tvLv.setBackground(UiStyleHelper.roundedBox(activity, lvColor, 6f, 0, 0));
        metaRow.addView(tvLv);

        TextView tvCountry = new TextView(activity);
        tvCountry.setText("  📍 " + creator.country);
        tvCountry.setSingleLine(true);
        tvCountry.setTextColor(Color.rgb(226, 232, 240));
        tvCountry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        metaRow.addView(tvCountry);

        bottomOverlay.addView(metaRow);

        LinearLayout nameAndViewerRow = new LinearLayout(activity);
        nameAndViewerRow.setOrientation(LinearLayout.HORIZONTAL);
        nameAndViewerRow.setGravity(Gravity.CENTER_VERTICAL);
        nameAndViewerRow.setPadding(0, UiStyleHelper.dp(activity, 3), 0, 0);

        TextView tvName = new TextView(activity);
        tvName.setText(creator.name);
        tvName.setSingleLine(true);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvViewers = new TextView(activity);
        tvViewers.setText("🏆 " + creator.viewersLabel);
        tvViewers.setTextColor(Color.rgb(253, 224, 71));
        tvViewers.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvViewers.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        nameAndViewerRow.addView(tvName);
        nameAndViewerRow.addView(tvViewers);
        bottomOverlay.addView(nameAndViewerRow);

        card.addView(bottomOverlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        card.setOnClickListener(v ->
                callbacks.onOpenLiveVideoRoom(creator.name + " Live", creator.channelName, creator.name, creator.id, creator.avatarResId)
        );

        return card;
    }

    /**
     * Exact replica of the video's LIVE -> Following tab (00:05 - 00:09):
     * 1. "The host you follow has not started LIVE yet" box
     * 2. "Wonderful LIVE at present" + "Change ↻" button + 3x2 dismissible grid (6 cards with ✕ button)
     * 3. "History LIVE" vertical list with Peak Viewers, Last LIVE: Xday(s) ago, and 🔔 reminder toggle
     */
    private static View buildLiveFollowingView(
            final Activity activity,
            final HostNavigationCallbacks callbacks
    ) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);

        // 1. Empty Live status box
        TextView emptyNotice = new TextView(activity);
        emptyNotice.setText("The host you follow has not started LIVE yet");
        emptyNotice.setTextColor(Color.rgb(148, 163, 184));
        emptyNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        emptyNotice.setGravity(Gravity.CENTER);
        emptyNotice.setPadding(0, UiStyleHelper.dp(activity, 26), 0, UiStyleHelper.dp(activity, 26));
        container.addView(emptyNotice);

        // 2. "Wonderful LIVE at present" + "Change ↻"
        LinearLayout wonderfulHeader = new LinearLayout(activity);
        wonderfulHeader.setOrientation(LinearLayout.HORIZONTAL);
        wonderfulHeader.setGravity(Gravity.CENTER_VERTICAL);
        wonderfulHeader.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 8));

        TextView tvWonderful = new TextView(activity);
        tvWonderful.setText("Wonderful LIVE at present");
        tvWonderful.setTextColor(Color.WHITE);
        tvWonderful.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvWonderful.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvWonderful.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnChange = new TextView(activity);
        btnChange.setText("Change ↻");
        btnChange.setTextColor(Color.rgb(45, 212, 191));
        btnChange.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        btnChange.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        btnChange.setOnClickListener(v -> {
            wonderfulLiveBatchOffset = (wonderfulLiveBatchOffset + 2) % getLiveCreators().size();
            dismissedWonderfulIds.clear();
            callbacks.onRefreshScreen();
        });

        wonderfulHeader.addView(tvWonderful);
        wonderfulHeader.addView(btnChange);
        container.addView(wonderfulHeader);

        // 3x2 Grid of 6 Wonderful LIVE cards
        List<LiveCreatorItem> pool = new ArrayList<>();
        List<LiveCreatorItem> all = getLiveCreators();
        for (int i = 0; i < all.size(); i++) {
            LiveCreatorItem c = all.get((i + wonderfulLiveBatchOffset) % all.size());
            if (!dismissedWonderfulIds.contains(c.id)) {
                pool.add(c);
            }
        }

        for (int rowIdx = 0; rowIdx < 2; rowIdx++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(3f);
            if (rowIdx > 0) {
                row.setPadding(0, UiStyleHelper.dp(activity, 6), 0, 0);
            }
            for (int colIdx = 0; colIdx < 3; colIdx++) {
                int itemIdx = rowIdx * 3 + colIdx;
                if (itemIdx < pool.size()) {
                    final LiveCreatorItem item = pool.get(itemIdx);
                    FrameLayout wCard = new FrameLayout(activity);
                    int wH = UiStyleHelper.dp(activity, 116);
                    wCard.setMinimumHeight(wH);
                    wCard.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 10));
                        }
                    });
                    wCard.setClipToOutline(true);

                    ImageView iv = new ImageView(activity);
                    iv.setImageResource(item.avatarResId);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    wCard.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, wH));

                    // Top-right ✕ dismiss button
                    TextView btnClose = new TextView(activity);
                    btnClose.setText("✕");
                    btnClose.setTextColor(Color.WHITE);
                    btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
                    btnClose.setGravity(Gravity.CENTER);
                    int closeSz = UiStyleHelper.dp(activity, 20);
                    btnClose.setBackground(UiStyleHelper.circle(activity, Color.argb(140, 0, 0, 0), 0, 0));
                    FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(closeSz, closeSz, Gravity.TOP | Gravity.END);
                    int m4 = UiStyleHelper.dp(activity, 4);
                    closeLp.setMargins(m4, m4, m4, m4);
                    btnClose.setOnClickListener(v -> {
                        dismissedWonderfulIds.add(item.id);
                        callbacks.onRefreshScreen();
                    });
                    wCard.addView(btnClose, closeLp);

                    // Bottom overlay: LV pill + Name + 🏆 viewers
                    LinearLayout bCol = new LinearLayout(activity);
                    bCol.setOrientation(LinearLayout.VERTICAL);
                    bCol.setPadding(UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 14), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 5));
                    bCol.setBackground(UiStyleHelper.bottomScrimGradient(activity, 10f));

                    TextView lv = new TextView(activity);
                    lv.setText(item.levelLabel);
                    lv.setTextColor(Color.WHITE);
                    lv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7.5f);
                    lv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                    lv.setPadding(UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4), 0);
                    lv.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(16, 185, 129), 4f, 0, 0));
                    bCol.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    LinearLayout bRow = new LinearLayout(activity);
                    bRow.setOrientation(LinearLayout.HORIZONTAL);
                    bRow.setGravity(Gravity.CENTER_VERTICAL);

                    TextView nm = new TextView(activity);
                    nm.setText(item.name);
                    nm.setSingleLine(true);
                    nm.setTextColor(Color.WHITE);
                    nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
                    nm.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    TextView vw = new TextView(activity);
                    vw.setText("🏆" + item.viewersLabel);
                    vw.setTextColor(Color.rgb(253, 224, 71));
                    vw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);

                    bRow.addView(nm);
                    bRow.addView(vw);
                    bCol.addView(bRow);

                    wCard.addView(bCol, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
                    wCard.setOnClickListener(v ->
                            callbacks.onOpenLiveVideoRoom(item.name + " Live", item.channelName, item.name, item.id, item.avatarResId)
                    );

                    LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    cellLp.setMargins(colIdx == 0 ? 0 : UiStyleHelper.dp(activity, 3), 0, colIdx == 2 ? 0 : UiStyleHelper.dp(activity, 3), 0);
                    row.addView(wCard, cellLp);
                }
            }
            container.addView(row);
        }

        // 3. "History LIVE" section
        TextView tvHistoryHeader = new TextView(activity);
        tvHistoryHeader.setText("History LIVE");
        tvHistoryHeader.setTextColor(Color.WHITE);
        tvHistoryHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tvHistoryHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvHistoryHeader.setPadding(0, UiStyleHelper.dp(activity, 16), 0, UiStyleHelper.dp(activity, 8));
        container.addView(tvHistoryHeader);

        String[][] historyItems = new String[][]{
                {"ribhu___", "Peak Viewers 169", "Last LIVE: 9day(s) ago"},
                {"⚜️E_L⚜️", "Peak Viewers 312", "Last LIVE: 24day(s) ago"},
                {"ladla_86u", "Peak Viewers 148", "Last LIVE: 2month(s) ago"},
                {"❤️ORÉSMÃO❤️", "Peak Viewers 275", "Last LIVE: 4month(s) ago"},
                {"Suspu💔", "Peak Viewers 190", "Last LIVE: 8month(s) ago"},
                {"jina_sirf_apne_liye", "Peak Viewers 410", "Last LIVE: 10month(s) ago"}
        };

        for (int h = 0; h < historyItems.length; h++) {
            final String[] hi = historyItems[h];
            final int hAv = VoiceRoomModels.getAvatarResId(h);

            LinearLayout hRow = new LinearLayout(activity);
            hRow.setOrientation(LinearLayout.HORIZONTAL);
            hRow.setGravity(Gravity.CENTER_VERTICAL);
            int padV = UiStyleHelper.dp(activity, 10);
            hRow.setPadding(0, padV, 0, padV);

            View av = UiStyleHelper.createCircularAvatar(activity, hAv, 48, Color.rgb(51, 65, 85), false);
            hRow.addView(av);

            LinearLayout infoCol = new LinearLayout(activity);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setPadding(UiStyleHelper.dp(activity, 12), 0, UiStyleHelper.dp(activity, 8), 0);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvHName = new TextView(activity);
            tvHName.setText(hi[0] + "  peak: " + hi[1]);
            tvHName.setTextColor(Color.WHITE);
            tvHName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            tvHName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView tvHLast = new TextView(activity);
            tvHLast.setText(hi[2]);
            tvHLast.setTextColor(Color.rgb(148, 163, 184));
            tvHLast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            tvHLast.setPadding(0, UiStyleHelper.dp(activity, 2), 0, 0);

            infoCol.addView(tvHName);
            infoCol.addView(tvHLast);
            hRow.addView(infoCol);

            final boolean isSub = historyBellSubscribed.contains(hi[0]);
            TextView bellBtn = new TextView(activity);
            bellBtn.setText(isSub ? "🔔" : "🔕");
            bellBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            bellBtn.setGravity(Gravity.CENTER);
            int bSz = UiStyleHelper.dp(activity, 36);
            bellBtn.setBackground(UiStyleHelper.circle(activity, Color.rgb(30, 41, 59), Color.rgb(71, 85, 105), 1f));
            bellBtn.setOnClickListener(v -> {
                if (isSub) {
                    historyBellSubscribed.remove(hi[0]);
                    Toast.makeText(activity, "LIVE reminder off for " + hi[0], Toast.LENGTH_SHORT).show();
                } else {
                    historyBellSubscribed.add(hi[0]);
                    Toast.makeText(activity, "🔔 LIVE notification enabled for " + hi[0], Toast.LENGTH_SHORT).show();
                }
                callbacks.onRefreshScreen();
            });
            hRow.addView(bellBtn, new LinearLayout.LayoutParams(bSz, bSz));

            container.addView(hRow);
        }

        return container;
    }

    /**
     * Exact replica of the video's VOICE Room cards (00:13 - 01:17):
     * - Horizontal gradient cards with left 76dp rounded square thumbnail + bottom-left green soundbar "ılı 5"
     * - Right side: Title + Country flag + [Lv.5] + [#Music] + [English] + overlapping circular connected participant avatars
     * - Promotional Banner Carousel ("Elite Club", "Point Mall", "Carnival Night") inserted after the 3rd card
     * - When on Following tab: shows Owners / Rooms sub-tabs ("🍁Nawabzade's🍁 ID: 785772 • 209 Followers", "fukrey boys")
     */
    private static View buildVoiceRoomCardsList(
            final Activity activity,
            final VoiceRoomModels.RoomRepository roomRepo,
            final HostNavigationCallbacks callbacks
    ) {
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);

        // Special layout when VOICE -> Following is selected (00:50 - 00:56 of video)
        if ("Following".equalsIgnoreCase(activePrimaryTab)) {
            if ("Owners".equalsIgnoreCase(activeVoiceFollowingSub)) {
                TextView emptyOwners = new TextView(activity);
                emptyOwners.setText("🎙️\n\nYou have not followed any room owner yet");
                emptyOwners.setTextColor(Color.rgb(148, 163, 184));
                emptyOwners.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                emptyOwners.setGravity(Gravity.CENTER);
                emptyOwners.setPadding(0, UiStyleHelper.dp(activity, 56), 0, UiStyleHelper.dp(activity, 56));
                list.addView(emptyOwners);
                return list;
            }

            TextView sectionHeader = new TextView(activity);
            sectionHeader.setText("Following in Progress");
            sectionHeader.setTextColor(Color.WHITE);
            sectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            sectionHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            sectionHeader.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 8));
            list.addView(sectionHeader);

            String[][] followedRooms = new String[][]{
                    {"🍁Nawabzade's🍁", "ID: 785772", "209 Followers", "qluvx_voice_nawabzade"},
                    {"fukrey boys", "ID: 641209", "318 Followers", "qluvx_voice_fukrey"}
            };
            for (int f = 0; f < followedRooms.length; f++) {
                final String[] fr = followedRooms[f];
                LinearLayout fCard = new LinearLayout(activity);
                fCard.setOrientation(LinearLayout.HORIZONTAL);
                fCard.setGravity(Gravity.CENTER_VERTICAL);
                int pad = UiStyleHelper.dp(activity, 12);
                fCard.setPadding(pad, pad, pad, pad);
                fCard.setBackground(UiStyleHelper.gradientBox(
                        activity,
                        Color.rgb(48, 36, 28),
                        Color.rgb(28, 25, 34),
                        14f,
                        Color.rgb(82, 65, 52),
                        1f
                ));

                View av = UiStyleHelper.createCircularAvatar(activity, VoiceRoomModels.getAvatarResId(f + 2), 56, Color.rgb(251, 191, 36), false);
                fCard.addView(av);

                LinearLayout info = new LinearLayout(activity);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(UiStyleHelper.dp(activity, 12), 0, UiStyleHelper.dp(activity, 8), 0);
                info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView t = new TextView(activity);
                t.setText(fr[0]);
                t.setTextColor(Color.WHITE);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                t.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

                TextView sub = new TextView(activity);
                sub.setText("🇮🇳  " + fr[1] + "   •   " + fr[2]);
                sub.setTextColor(Color.rgb(203, 213, 225));
                sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                sub.setPadding(0, UiStyleHelper.dp(activity, 3), 0, 0);

                info.addView(t);
                info.addView(sub);
                fCard.addView(info);

                TextView waveBadge = new TextView(activity);
                waveBadge.setText("ılı");
                waveBadge.setTextColor(Color.rgb(52, 211, 153));
                waveBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
                waveBadge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                fCard.addView(waveBadge);

                fCard.setOnClickListener(v -> callbacks.onOpenVoiceRoom(fr[0], fr[3]));
                list.addView(fCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10)));
            }
            return list;
        }

        // Standard VOICE Room cards matching the video
        String[][] featuredVoiceRooms = new String[][]{
                {"⚡ CHINNU ✨", "Lv.5", "#Music", "Telugu", "10", "qluvx_voice_chinnu", "🇮🇳", "All"},
                {"fukrey boys", "Lv.6", "#Chat", "Hindi", "5", "qluvx_voice_fukrey", "🇮🇳", "All"},
                {"♍ নাগ রানী নাগরাজ্য 🐍", "Lv.4", "#Party", "Bengali", "6", "qluvx_voice_nag", "🇮🇳", "Hotpick"},
                {"House_STARK🐺", "Lv.5", "#Friends", "English", "6", "qluvx_voice_stark", "🇮🇳", "Hot"},
                {"Eshwar_Sai_❤️", "Lv.5", "#Music", "Telugu", "4", "qluvx_voice_eshwar", "🇮🇳", "New"},
                {"🍁Nawabzade's🍁", "Lv.6", "#PK", "Hindi", "4", "qluvx_voice_nawabzade", "🇮🇳", "PK"},
                {"TRICK OR TREAT w/ P...", "Lv.5", "#Game", "Tagalog", "8", "qluvx_voice_trick", "🇵🇭", "Game"},
                {"✨WARZONE✨", "Lv.6", "#Game", "English", "16", "qluvx_voice_warzone", "🇵🇭", "Game"},
                {"KP GAME TIME", "Lv.4", "#Nearby", "Korean", "12", "qluvx_voice_kpgame", "🇰🇷", "Nearby"}
        };

        int[][] cardGradients = new int[][]{
                {Color.rgb(22, 48, 72), Color.rgb(16, 30, 48), Color.rgb(56, 189, 248)},
                {Color.rgb(58, 38, 28), Color.rgb(34, 24, 20), Color.rgb(251, 146, 60)},
                {Color.rgb(56, 24, 68), Color.rgb(32, 16, 44), Color.rgb(192, 132, 252)},
                {Color.rgb(20, 56, 54), Color.rgb(14, 34, 34), Color.rgb(45, 212, 191)},
                {Color.rgb(64, 24, 48), Color.rgb(36, 16, 28), Color.rgb(244, 114, 182)}
        };

        int renderedCount = 0;
        for (int i = 0; i < featuredVoiceRooms.length; i++) {
            final String[] r = featuredVoiceRooms[i];
            if (!"All".equalsIgnoreCase(activeSubFilter) && !activeSubFilter.equalsIgnoreCase(r[7])) {
                continue;
            }
            final int hostAvatar = VoiceRoomModels.getAvatarResId(i);
            final int[] grad = cardGradients[i % cardGradients.length];

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            int pad = UiStyleHelper.dp(activity, 10);
            card.setPadding(pad, pad, pad, pad);
            card.setBackground(UiStyleHelper.gradientBox(activity, grad[0], grad[1], 14f, grad[2], 0.8f));

            // Left Square Rounded Cover (76dp) with bottom-left green soundbar "ılı 10"
            FrameLayout hostThumbWrap = new FrameLayout(activity);
            int thumbSize = UiStyleHelper.dp(activity, 76);
            hostThumbWrap.setLayoutParams(new LinearLayout.LayoutParams(thumbSize, thumbSize));
            hostThumbWrap.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 12));
                }
            });
            hostThumbWrap.setClipToOutline(true);

            ImageView ivHost = new ImageView(activity);
            ivHost.setImageResource(hostAvatar);
            ivHost.setScaleType(ImageView.ScaleType.CENTER_CROP);
            hostThumbWrap.addView(ivHost, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView waveCount = new TextView(activity);
            waveCount.setText("ılı " + r[4]);
            waveCount.setTextColor(Color.rgb(52, 211, 153));
            waveCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            waveCount.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            waveCount.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2));
            waveCount.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(190, 10, 12, 20), 8f, 0, 0));
            hostThumbWrap.addView(waveCount, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START));

            card.addView(hostThumbWrap);

            // Right Content: Room Title + Tags Row + Overlapping Connected Agora Channel Participant Avatars
            LinearLayout midCol = new LinearLayout(activity);
            midCol.setOrientation(LinearLayout.VERTICAL);
            midCol.setPadding(UiStyleHelper.dp(activity, 12), 0, UiStyleHelper.dp(activity, 4), 0);
            midCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTitle = new TextView(activity);
            tvTitle.setText(r[0]);
            tvTitle.setSingleLine(true);
            tvTitle.setTextColor(Color.WHITE);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            midCol.addView(tvTitle);

            LinearLayout pillsRow = new LinearLayout(activity);
            pillsRow.setOrientation(LinearLayout.HORIZONTAL);
            pillsRow.setGravity(Gravity.CENTER_VERTICAL);
            pillsRow.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 6));

            TextView flagTv = new TextView(activity);
            flagTv.setText(r[6] + " ");
            flagTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            pillsRow.addView(flagTv);

            TextView lvPill = new TextView(activity);
            lvPill.setText("🏠 " + r[1]);
            lvPill.setTextColor(Color.WHITE);
            lvPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            lvPill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            lvPill.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 1), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 1));
            lvPill.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(16, 185, 129), 6f, 0, 0));
            pillsRow.addView(lvPill);

            TextView topicPill = new TextView(activity);
            topicPill.setText(r[2]);
            topicPill.setTextColor(Color.rgb(253, 186, 116));
            topicPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            topicPill.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 1), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 1));
            topicPill.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(120, 124, 45, 18), 6f, 0, 0));
            pillsRow.addView(topicPill, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 5), 0, 0, 0));

            TextView langPill = new TextView(activity);
            langPill.setText(r[3]);
            langPill.setTextColor(Color.rgb(203, 213, 225));
            langPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            langPill.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 1), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 1));
            langPill.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(120, 30, 41, 59), 6f, 0, 0));
            pillsRow.addView(langPill, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 5), 0, 0, 0));

            midCol.addView(pillsRow);

            // Connected Agora participants row + active seat count
            LinearLayout speakerAvRow = new LinearLayout(activity);
            speakerAvRow.setOrientation(LinearLayout.HORIZONTAL);
            speakerAvRow.setGravity(Gravity.CENTER_VERTICAL);
            for (int s = 0; s < 4; s++) {
                View spAv = UiStyleHelper.createCircularAvatar(
                        activity,
                        VoiceRoomModels.getAvatarResId(i + s + 1),
                        24,
                        Color.rgb(52, 211, 153),
                        false
                );
                LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(UiStyleHelper.dp(activity, 24), UiStyleHelper.dp(activity, 24));
                if (s > 0) {
                    avLp.setMargins(-UiStyleHelper.dp(activity, 5), 0, 0, 0);
                }
                speakerAvRow.addView(spAv, avLp);
            }

            TextView connectedHint = new TextView(activity);
            connectedHint.setText("  • " + r[4] + " in Agora channel");
            connectedHint.setTextColor(Color.rgb(148, 163, 184));
            connectedHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            speakerAvRow.addView(connectedHint);

            midCol.addView(speakerAvRow);
            card.addView(midCol);

            card.setOnClickListener(v -> callbacks.onOpenVoiceRoom(r[0], r[5]));
            list.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

            renderedCount++;
            // Insert Promotional Event Banner Carousel right after the 3rd card (00:13 - 00:46 of video)
            if (renderedCount == 3) {
                list.addView(buildVoicePromoCarouselBanner(activity, callbacks), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));
            }
        }

        // Also render any custom-created Voice Rooms from RoomRepository
        for (final VoiceRoomModels.RoomInfo custom : roomRepo.getAllRooms()) {
            LinearLayout cCard = new LinearLayout(activity);
            cCard.setOrientation(LinearLayout.HORIZONTAL);
            cCard.setGravity(Gravity.CENTER_VERTICAL);
            int pad = UiStyleHelper.dp(activity, 10);
            cCard.setPadding(pad, pad, pad, pad);
            cCard.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_CARD_BG, 14f, UiStyleHelper.COLOR_BORDER_SUBTLE, 1f));

            View av = UiStyleHelper.createCircularAvatar(activity, custom.coverResId != 0 ? custom.coverResId : R.drawable.img_avatar_0, 50, UiStyleHelper.COLOR_NEON_PINK, true);
            cCard.addView(av);

            LinearLayout info = new LinearLayout(activity);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 8), 0);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t = new TextView(activity);
            t.setText(custom.title);
            t.setTextColor(Color.WHITE);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView s = new TextView(activity);
            s.setText("🎙️ " + custom.getOccupiedSeatCount() + "/12 Seats • 👥 " + custom.listenerCount + " Connected");
            s.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

            info.addView(t);
            info.addView(s);
            cCard.addView(info);

            TextView jBtn = UiStyleHelper.createStatusChip(activity, "Join", UiStyleHelper.COLOR_NEON_PINK, Color.WHITE, 0);
            View.OnClickListener joinClick = v -> callbacks.onOpenVoiceRoom(custom.title, custom.channelName);
            jBtn.setOnClickListener(joinClick);
            cCard.setOnClickListener(joinClick);
            cCard.addView(jBtn);

            list.addView(cCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));
        }

        return list;
    }

    /**
     * Builds the promotional banner strip shown in the video between Voice Room cards:
     * cycles through "Elite Club", "Point Mall — Wait for You to Redeem", and "QLUVX Carnival Night".
     */
    private static View buildVoicePromoCarouselBanner(
            final Activity activity,
            final HostNavigationCallbacks callbacks
    ) {
        final String[][] promos = new String[][]{
                {"👑 ELITE CLUB", "Exclusive SVIP Frame & Agora Room Boost • Tap to Join"},
                {"🛍️ POINT MALL", "Wait for You to Redeem Luxury Mounts & Badges"},
                {"🎡 QLUVX CARNIVAL NIGHT", "Win Up to 50,000 Coins in Voice PK Rooms"}
        };
        final int idx = activePromoBannerIndex % promos.length;

        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        int padH = UiStyleHelper.dp(activity, 14);
        int padV = UiStyleHelper.dp(activity, 10);
        banner.setPadding(padH, padV, padH, padV);
        banner.setBackground(UiStyleHelper.gradientBox(
                activity,
                idx == 0 ? Color.rgb(76, 29, 149) : (idx == 1 ? Color.rgb(157, 23, 77) : Color.rgb(15, 118, 110)),
                idx == 0 ? Color.rgb(192, 38, 211) : (idx == 1 ? Color.rgb(234, 88, 12) : Color.rgb(37, 99, 235)),
                14f,
                Color.rgb(253, 224, 71),
                1f
        ));

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(activity);
        title.setText(promos[idx][0]);
        title.setTextColor(Color.rgb(253, 224, 71));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

        TextView sub = new TextView(activity);
        sub.setText(promos[idx][1]);
        sub.setTextColor(Color.WHITE);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);

        textCol.addView(title);
        textCol.addView(sub);
        banner.addView(textCol);

        TextView dots = new TextView(activity);
        dots.setText((idx == 0 ? "● ○ ○" : (idx == 1 ? "○ ● ○" : "○ ○ ●")) + "  ❯");
        dots.setTextColor(Color.WHITE);
        dots.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        banner.addView(dots);

        banner.setOnClickListener(v -> {
            activePromoBannerIndex = (activePromoBannerIndex + 1) % promos.length;
            callbacks.onRefreshScreen();
        });

        return banner;
    }

    private static void showNotificationsDialog(
            final Activity activity,
            final QluvxSocialManager social,
            final HostNavigationCallbacks callbacks
    ) {
        String[] notifs = new String[]{
                "💖 Aryan started a Live Video PK stream — Tap to watch!",
                "🎙️ Rohan invited you to Seat 2 in '✨ दिल से बात करो ✨'",
                "🎁 Zoya sent you a Love Castle x1 (+500 Coins)",
                "👑 VIP Bonus: Your daily +200 Coins reward is ready in Wallet!"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔔 Notifications Center")
                .setItems(notifs, (d, which) -> {
                    if (which == 0) {
                        callbacks.onOpenLiveVideoRoom("Aryan Live", "qluvx_live_aryan", "Aryan", "12345678", R.drawable.img_avatar_0);
                    } else if (which == 1) {
                        callbacks.onOpenVoiceRoom("✨ दिल से बात करो ✨", "qluvx_voice_dilse");
                    } else if (which == 2) {
                        callbacks.onSelectMenu(4); // Chat
                    } else {
                        callbacks.onSelectMenu(6); // Wallet
                    }
                })
                .setPositiveButton("Mark All Read", null)
                .show();
    }

    // =========================================================================
    // 2. PROFILE SCREEN (Screen 4 of Mockup + All 15 Requested Profile Buttons)
    // =========================================================================
    public static void renderProfileScreen(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final HostNavigationCallbacks callbacks
    ) {
        final QluvxSocialManager.UserProfile me = social.getCurrentUser();
        final boolean isBlocked = blockedUserIds.contains(me.userId);

        // 2A. Large Portrait Hero Banner with Thumbnail Strip (matching Screen 4 of mockup)
        FrameLayout heroFrame = new FrameLayout(activity);
        int heroH = UiStyleHelper.dp(activity, 240);
        heroFrame.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 20));
            }
        });
        heroFrame.setClipToOutline(true);

        ImageView heroImg = new ImageView(activity);
        heroImg.setImageResource(me.avatarResId != 0 ? me.avatarResId : R.drawable.img_avatar_0);
        heroImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        heroFrame.addView(heroImg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heroH));

        // Thumbnail Strip on bottom-left of Hero
        LinearLayout thumbStrip = new LinearLayout(activity);
        thumbStrip.setOrientation(LinearLayout.HORIZONTAL);
        thumbStrip.setPadding(UiStyleHelper.dp(activity, 12), UiStyleHelper.dp(activity, 12), UiStyleHelper.dp(activity, 12), UiStyleHelper.dp(activity, 12));
        thumbStrip.setBackground(UiStyleHelper.bottomScrimGradient(activity, 20f));

        int[] albumThumbs = new int[]{R.drawable.img_avatar_0, R.drawable.img_avatar_1, R.drawable.img_avatar_2};
        for (int t = 0; t < albumThumbs.length; t++) {
            final int thumbRes = albumThumbs[t];
            ImageView ivT = new ImageView(activity);
            ivT.setImageResource(thumbRes);
            ivT.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int tSz = UiStyleHelper.dp(activity, 48);
            ivT.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_BG_DEEP, 10f, Color.WHITE, 1.5f));
            ivT.setOnClickListener(v -> {
                heroImg.setImageResource(thumbRes);
                me.avatarResId = thumbRes;
            });
            thumbStrip.addView(ivT, UiStyleHelper.params(tSz, tSz, 0, 0, UiStyleHelper.dp(activity, 8), 0));
        }

        View stripSpacer = new View(activity);
        thumbStrip.addView(stripSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        // Edit Profile Button on Hero
        TextView btnEditProfile = UiStyleHelper.createStatusChip(
                activity,
                "✏️ Edit Profile",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                Color.WHITE
        );
        btnEditProfile.setOnClickListener(v -> callbacks.onShowEditProfileDialog());
        thumbStrip.addView(btnEditProfile);

        heroFrame.addView(thumbStrip, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        container.addView(heroFrame, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        // 2B. Identity & Bio Card
        LinearLayout infoCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_BORDER_SUBTLE);

        LinearLayout nameRow = new LinearLayout(activity);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(activity);
        tvName.setText(me.displayName + " ☑️ ♂");
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView lvBadge = UiStyleHelper.createStatusChip(
                activity,
                "💎 Lv.32",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                0
        );
        lvBadge.setOnClickListener(v -> showLevelDetailsDialog(activity));

        nameRow.addView(tvName);
        nameRow.addView(lvBadge);
        infoCard.addView(nameRow);

        TextView tvIdRow = new TextView(activity);
        tvIdRow.setText("ID: " + me.userId + "   ● Online   •   👥 " + me.followersCount + " Followers");
        tvIdRow.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
        tvIdRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tvIdRow.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 8));
        infoCard.addView(tvIdRow);

        // Location / Age / Language Chips
        LinearLayout metaChips = new LinearLayout(activity);
        metaChips.setOrientation(LinearLayout.HORIZONTAL);
        String[] chipLabels = new String[]{"📍 " + me.country, "👤 " + me.age, "🌐 English / Hindi"};
        for (String cl : chipLabels) {
            TextView c = UiStyleHelper.createStatusChip(activity, cl, UiStyleHelper.COLOR_CARD_ELEVATED, UiStyleHelper.COLOR_TEXT_SECONDARY, 0);
            metaChips.addView(c, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        }
        infoCard.addView(metaChips, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

        TextView tvBio = new TextView(activity);
        tvBio.setText(me.bio != null && !me.bio.isEmpty() ? me.bio : "Chill, friendly and open to new friends 💕");
        tvBio.setTextColor(Color.WHITE);
        tvBio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvBio.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));
        infoCard.addView(tvBio);

        // Hashtags row
        HorizontalScrollView tagScroll = new HorizontalScrollView(activity);
        tagScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tagRow = new LinearLayout(activity);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] tags = new String[]{"#Friends", "#Music", "#Travel", "#Chat", "#Game"};
        for (String tag : tags) {
            TextView tChip = UiStyleHelper.createStatusChip(activity, tag, UiStyleHelper.COLOR_BG_DEEP, UiStyleHelper.COLOR_AGORA_CYAN, UiStyleHelper.COLOR_BORDER_SUBTLE);
            tagRow.addView(tChip, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        }
        tagScroll.addView(tagRow);
        infoCard.addView(tagScroll, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        // 2C. Primary Profile Action Buttons: [ ＋ Follow ] [ Unfollow ] [ 💬 Message ]
        LinearLayout ctaRow = new LinearLayout(activity);
        ctaRow.setOrientation(LinearLayout.HORIZONTAL);
        ctaRow.setWeightSum(3f);

        TextView btnFollow = UiStyleHelper.createControlBarButton(
                activity,
                me.isFollowing ? "✓ Following" : "＋ Follow",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                0,
                v -> {
                    if (!me.isFollowing) {
                        me.isFollowing = true;
                        me.followersCount++;
                    }
                    Toast.makeText(activity, "💖 Followed " + me.displayName + "!", Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                }
        );
        btnFollow.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnUnfollow = UiStyleHelper.createControlBarButton(
                activity,
                "Unfollow",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_SECONDARY,
                UiStyleHelper.COLOR_BORDER_SUBTLE,
                v -> {
                    if (me.isFollowing) {
                        me.isFollowing = false;
                        me.followersCount = Math.max(0, me.followersCount - 1);
                    }
                    Toast.makeText(activity, "Unfollowed " + me.displayName, Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                }
        );
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        uLp.setMargins(UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8), 0);
        btnUnfollow.setLayoutParams(uLp);

        TextView btnMessage = UiStyleHelper.createControlBarButton(
                activity,
                "💬 Message",
                UiStyleHelper.COLOR_ROYAL_PURPLE,
                Color.WHITE,
                UiStyleHelper.COLOR_AGORA_CYAN,
                v -> callbacks.onSelectMenu(4) // Opens Chat
        );
        btnMessage.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ctaRow.addView(btnFollow);
        ctaRow.addView(btnUnfollow);
        ctaRow.addView(btnMessage);
        infoCard.addView(ctaRow);

        container.addView(infoCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        // 2D. Profile Sub-Tabs: Profile | Open Album | Open Gifts | Open Moments
        LinearLayout subTabRow = new LinearLayout(activity);
        subTabRow.setOrientation(LinearLayout.HORIZONTAL);
        String[][] pTabs = new String[][]{
                {"Profile", "Profile"},
                {"Album", "📸 Open Album"},
                {"Gifts", "🎁 Open Gifts"},
                {"Moment", "✨ Open Moments"}
        };
        for (final String[] pt : pTabs) {
            boolean active = pt[0].equalsIgnoreCase(activeProfileSubTab);
            TextView tabChip = UiStyleHelper.createStatusChip(
                    activity,
                    pt[1],
                    active ? UiStyleHelper.COLOR_NEON_PINK : UiStyleHelper.COLOR_CARD_BG,
                    Color.WHITE,
                    active ? Color.WHITE : UiStyleHelper.COLOR_BORDER_SUBTLE
            );
            tabChip.setOnClickListener(v -> {
                activeProfileSubTab = pt[0];
                callbacks.onRefreshScreen();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(0, 0, UiStyleHelper.dp(activity, 6), 0);
            subTabRow.addView(tabChip, lp);
        }
        container.addView(subTabRow, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        // Sub-Tab Content Box
        if ("Album".equalsIgnoreCase(activeProfileSubTab)) {
            LinearLayout albumCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_NEON_PINK);
            TextView aTitle = new TextView(activity);
            aTitle.setText("📸 Photo Album (6 HD Portraits)");
            aTitle.setTextColor(Color.WHITE);
            aTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            aTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            albumCard.addView(aTitle, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

            LinearLayout row1 = new LinearLayout(activity);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            for (int a = 0; a < 3; a++) {
                ImageView iv = new ImageView(activity);
                iv.setImageResource(VoiceRoomModels.getAvatarResId(a));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiStyleHelper.dp(activity, 110), 1f);
                if (a > 0) lp.setMargins(UiStyleHelper.dp(activity, 6), 0, 0, 0);
                row1.addView(iv, lp);
            }
            albumCard.addView(row1);
            container.addView(albumCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
        } else if ("Gifts".equalsIgnoreCase(activeProfileSubTab)) {
            LinearLayout giftsCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_WARNING_AMBER);
            TextView gTitle = new TextView(activity);
            gTitle.setText("🎁 Gifts Wall (28 Gifts Received • Tap to Send Gift)");
            gTitle.setTextColor(UiStyleHelper.COLOR_WARNING_AMBER);
            gTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            gTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            giftsCard.addView(gTitle);

            TextView gWall = new TextView(activity);
            gWall.setText("🌹 Rose x18   •   🏰 Love Castle x3   •   🚀 Super Rocket x5   •   👑 Golden Crown x2");
            gWall.setTextColor(Color.WHITE);
            gWall.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            gWall.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 10));
            giftsCard.addView(gWall);

            TextView openGiftPanelBtn = UiStyleHelper.createControlBarButton(
                    activity,
                    "🎁 Open Gift Panel",
                    UiStyleHelper.COLOR_NEON_PINK,
                    Color.WHITE,
                    0,
                    v -> UiStyleHelper.showUnifiedGiftPanelDialog(activity, economy, me.displayName, me.displayName, (gift, mult, tx) -> callbacks.onRefreshScreen())
            );
            giftsCard.addView(openGiftPanelBtn);
            container.addView(giftsCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
        } else if ("Moment".equalsIgnoreCase(activeProfileSubTab)) {
            LinearLayout momentsCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_NEON_PINK);
            TextView mTitle = new TextView(activity);
            mTitle.setText("✨ My Moments Feed");
            mTitle.setTextColor(Color.WHITE);
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            mTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            momentsCard.addView(mTitle);

            TextView mPost1 = new TextView(activity);
            mPost1.setText("• \"" + myCustomStoryStatus + "\"\n  ❤️ 124 Likes  •  💬 18 Comments  •  Just now");
            mPost1.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            mPost1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            mPost1.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));
            momentsCard.addView(mPost1);

            TextView mPost2 = new TextView(activity);
            mPost2.setText("• \"Hosting late night 12-seat voice party! Come join Seat 2 🎙️👑\"\n  ❤️ 286 Likes  •  💬 42 Comments  •  2h ago");
            mPost2.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            mPost2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            mPost2.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
            momentsCard.addView(mPost2);

            TextView btnPostMoment = UiStyleHelper.createControlBarButton(
                    activity,
                    "✨ + Post New Moment",
                    UiStyleHelper.COLOR_NEON_PINK,
                    Color.WHITE,
                    0,
                    v -> showMyStoryAndAddStoryDialog(activity, callbacks)
            );
            momentsCard.addView(btnPostMoment);
            container.addView(momentsCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
        }

        // 2E. Interactive List Rows: My Level | VIP Level | My Badges | Wallet | Settings
        LinearLayout rowsCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_BORDER_SUBTLE);

        rowsCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                "🛡️ My Level",
                "Creator & Wealth Progress • Next reward at Lv.35",
                "👑 Lv.32 ›",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                v -> showLevelDetailsDialog(activity)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

        rowsCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                "👑 VIP Level",
                "Active Status: " + economy.getVipStatusLabel() + " • Frame: " + me.profileFrame,
                "👑 SVIP ›",
                Color.argb(95, 245, 158, 11),
                UiStyleHelper.COLOR_WARNING_AMBER,
                v -> callbacks.onSelectMenu(8) // Opens VIP/SVIP screen
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

        rowsCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                "🏅 My Badges",
                "🛡️ Top Anchor  •  💎 PK Champion  •  👑 Royal Guardian  •  🔥 Verified",
                "4 Badges ›",
                UiStyleHelper.COLOR_ROYAL_PURPLE,
                Color.WHITE,
                v -> showMyBadgesDialog(activity)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

        rowsCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                "🪙 Wallet",
                "Balance: " + economy.getCoinBalance() + " Coins • Recharge & Ledger",
                "🪙 " + economy.getCoinBalance() + " ›",
                Color.argb(95, 16, 185, 129),
                UiStyleHelper.COLOR_LIVE_GREEN,
                v -> callbacks.onSelectMenu(6) // Opens Wallet screen
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

        rowsCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                "⚙️ Settings",
                "Privacy, Notifications, Permissions, Wallpaper & Account",
                "Open ›",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                v -> callbacks.onSelectMenu(11) // Opens Settings screen
        ));

        container.addView(rowsCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        // 2F. Bottom Report / Block / Unblock Row (matching bottom of Screen 4 in mockup)
        LinearLayout modRow = new LinearLayout(activity);
        modRow.setOrientation(LinearLayout.HORIZONTAL);
        modRow.setWeightSum(3f);

        TextView btnReport = UiStyleHelper.createControlBarButton(
                activity,
                "⚠️ Report",
                UiStyleHelper.COLOR_CARD_BG,
                UiStyleHelper.COLOR_DANGER_RED,
                UiStyleHelper.COLOR_DANGER_RED,
                v -> {
                    VoiceRoomModels.RoomRepository.getInstance().addModerationReport("Profile Report: User " + me.userId + " (" + me.displayName + ")");
                    economy.logAudit("Profile report submitted for " + me.userId);
                    Toast.makeText(activity, "✅ Report submitted to Admin Moderation Queue.", Toast.LENGTH_SHORT).show();
                }
        );
        btnReport.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnBlock = UiStyleHelper.createControlBarButton(
                activity,
                "🚫 Block",
                isBlocked ? UiStyleHelper.COLOR_DANGER_RED : UiStyleHelper.COLOR_CARD_BG,
                Color.WHITE,
                UiStyleHelper.COLOR_BORDER_SUBTLE,
                v -> {
                    blockedUserIds.add(me.userId);
                    Toast.makeText(activity, "🚫 User blocked.", Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                }
        );
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bLp.setMargins(UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8), 0);
        btnBlock.setLayoutParams(bLp);

        TextView btnUnblock = UiStyleHelper.createControlBarButton(
                activity,
                "✅ Unblock",
                !isBlocked ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_CARD_BG,
                Color.WHITE,
                UiStyleHelper.COLOR_LIVE_GREEN,
                v -> {
                    blockedUserIds.remove(me.userId);
                    Toast.makeText(activity, "✅ User unblocked.", Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                }
        );
        btnUnblock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        modRow.addView(btnReport);
        modRow.addView(btnBlock);
        modRow.addView(btnUnblock);
        container.addView(modRow, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 14)));
    }

    private static void showLevelDetailsDialog(Activity activity) {
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🛡️ My Level — Lv.32 (Creator & Wealth)")
                .setMessage("• Current Level: Lv.32 (28,450 / 30,000 EXP)\n• Entrance Effect: Neon Pink Starlight\n• Room Seat Priority: Enabled\n• Host PK Bonus: +15% Gift Multiplier")
                .setPositiveButton("Awesome", null)
                .show();
    }

    private static void showMyBadgesDialog(Activity activity) {
        String[] badges = new String[]{
                "🛡️ Lv.32 Star Creator Badge (Equipped)",
                "👑 SVIP Royal Crown Emblem (Equipped)",
                "⚔️ PK Arena 10-Win Streak Medal",
                "💎 Verified Voice Anchor Badge"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🏅 My Badges Collection")
                .setItems(badges, (d, which) ->
                        Toast.makeText(activity, "Equipped Badge: " + badges[which], Toast.LENGTH_SHORT).show()
                )
                .setPositiveButton("Done", null)
                .show();
    }

    // =========================================================================
    // 3. WALLET SCREEN (Coin Balance, Recharge, Transaction History, Purchase History)
    // =========================================================================
    public static void renderWalletScreen(
            final Activity activity,
            final LinearLayout container,
            final GiftEconomyManager economy,
            final HostNavigationCallbacks callbacks
    ) {
        LinearLayout balanceCard = UiStyleHelper.createGradientCardContainer(
                activity,
                Color.rgb(46, 16, 84),
                Color.rgb(88, 28, 135),
                UiStyleHelper.COLOR_WARNING_AMBER
        );

        // Coin Balance Button / Header
        TextView btnCoinBalance = UiStyleHelper.createControlBarButton(
                activity,
                "🪙 Coin Balance: " + economy.getCoinBalance() + " Coins (Tap to Refresh)",
                Color.argb(95, 245, 158, 11),
                UiStyleHelper.COLOR_WARNING_AMBER,
                UiStyleHelper.COLOR_WARNING_AMBER,
                v -> {
                    Toast.makeText(activity, "🪙 Verified Wallet Balance: " + economy.getCoinBalance() + " Coins", Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                }
        );
        balanceCard.addView(btnCoinBalance, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));

        TextView tvVerifyStatus = new TextView(activity);
        tvVerifyStatus.setText("🔒 Backend Ledger Verification: Active • VIP: " + economy.getVipStatusLabel());
        tvVerifyStatus.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
        tvVerifyStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tvVerifyStatus.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        balanceCard.addView(tvVerifyStatus);

        // Primary Recharge Button + Coin Packs
        int[][] packs = new int[][]{
                {500, 99}, {1500, 249}, {5000, 799}, {12000, 1799}
        };
        String[] packTitles = new String[]{
                "⚡ Recharge +500 Coins (Starter Pack — ₹99)",
                "⚡ Recharge +1,500 Coins (Popular Pack — ₹249)",
                "⚡ Recharge +5,000 Coins (Pro Host Pack — ₹799)",
                "⚡ Recharge +12,000 Coins (Royal SVIP Pack — ₹1,799)"
        };
        for (int i = 0; i < packs.length; i++) {
            final int coins = packs[i][0];
            final String pTitle = packTitles[i];
            TextView btnRechargePack = UiStyleHelper.createControlBarButton(
                    activity,
                    pTitle,
                    UiStyleHelper.COLOR_NEON_PINK,
                    Color.WHITE,
                    0,
                    v -> {
                        String idempotencyKey = "rc_" + System.currentTimeMillis();
                        economy.rechargeVerifiedPack(coins, pTitle, idempotencyKey);
                        Toast.makeText(activity, "✅ Verified Recharge: +" + coins + " Coins added to Wallet!", Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    }
            );
            balanceCard.addView(btnRechargePack, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));
        }

        container.addView(balanceCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        // Sub-Tabs: [ Transaction History ] | [ Purchase History ]
        LinearLayout historyTabRow = new LinearLayout(activity);
        historyTabRow.setOrientation(LinearLayout.HORIZONTAL);
        historyTabRow.setWeightSum(2f);

        boolean isTxTab = "Transactions".equalsIgnoreCase(activeWalletSubTab);

        TextView btnTxHistory = UiStyleHelper.createControlBarButton(
                activity,
                "📜 Transaction History",
                isTxTab ? UiStyleHelper.COLOR_NEON_PINK : UiStyleHelper.COLOR_CARD_BG,
                Color.WHITE,
                UiStyleHelper.COLOR_BORDER_SUBTLE,
                v -> {
                    activeWalletSubTab = "Transactions";
                    callbacks.onRefreshScreen();
                }
        );
        btnTxHistory.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnPurchaseHistory = UiStyleHelper.createControlBarButton(
                activity,
                "🧾 Purchase History",
                !isTxTab ? UiStyleHelper.COLOR_NEON_PINK : UiStyleHelper.COLOR_CARD_BG,
                Color.WHITE,
                UiStyleHelper.COLOR_BORDER_SUBTLE,
                v -> {
                    activeWalletSubTab = "Purchases";
                    callbacks.onRefreshScreen();
                }
        );
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pLp.setMargins(UiStyleHelper.dp(activity, 8), 0, 0, 0);
        btnPurchaseHistory.setLayoutParams(pLp);

        historyTabRow.addView(btnTxHistory);
        historyTabRow.addView(btnPurchaseHistory);
        container.addView(historyTabRow, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));

        List<GiftEconomyManager.WalletTransaction> allTxs = economy.getTransactions();
        List<GiftEconomyManager.WalletTransaction> filteredTxs = new ArrayList<>();
        for (GiftEconomyManager.WalletTransaction tx : allTxs) {
            if (isTxTab) {
                filteredTxs.add(tx);
            } else if ("RECHARGE".equalsIgnoreCase(tx.type) || tx.deltaCoins > 0) {
                filteredTxs.add(tx);
            }
        }

        if (filteredTxs.isEmpty()) {
            container.addView(UiStyleHelper.createStateBanner(
                    activity,
                    isTxTab ? "No Wallet Transactions Yet" : "No Coin Purchases Yet",
                    "Tap any '⚡ Recharge' pack above to verify and record a purchase in the ledger.",
                    UiStyleHelper.COLOR_AGORA_CYAN
            ));
        } else {
            int pad = UiStyleHelper.dp(activity, 12);
            for (GiftEconomyManager.WalletTransaction tx : filteredTxs) {
                TextView row = new TextView(activity);
                row.setText("[" + tx.timestamp + "] " + tx.txId + " (" + tx.type + ")\n"
                        + tx.description + "\nDelta: " + (tx.deltaCoins >= 0 ? "+" : "") + tx.deltaCoins
                        + " Coins  •  Balance After: 🪙 " + tx.balanceAfter);
                row.setTextColor(tx.deltaCoins >= 0 ? UiStyleHelper.COLOR_LIVE_GREEN : Color.WHITE);
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                row.setPadding(pad, pad, pad, pad);
                row.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_CARD_BG, 14f, UiStyleHelper.COLOR_BORDER_SUBTLE, 1f));
                container.addView(row, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));
            }
        }
    }

    private static void clipViewOval(View v) {
        if (v == null) return;
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int sz = Math.min(view.getWidth(), view.getHeight());
                outline.setOval(0, 0, sz, sz);
            }
        });
        v.setClipToOutline(true);
    }
}
