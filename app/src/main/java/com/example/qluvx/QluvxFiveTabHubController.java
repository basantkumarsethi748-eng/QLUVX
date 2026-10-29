package com.example.qluvx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dedicated controller implementing the exact 5-Tab Bottom Navigation specifications:
 * 1. HOME: Nearby | Online tabs + 3-column ("3 line par") Nearby ID grid showing Photo, Name, ID, and Location
 * 2. DISCOVER: [Family] [Group] [Quick Chat] + [Live Camera] [Post] [Photo Gallery] + Moments & Gallery feed
 * 3. CHAT: Top single-line ID strip + Below vertical single-ID list with Pending message badge & timestamp
 *          + Tap any ID to open full Message Box with Location + ID header and bottom Camera, Album,
 *          Location Send, One-Time Photo Send, and Video Call options
 * 4. LIVE: LIVE | VOICE switch + Top single-line All IDs strip + Recommended | Global (Country selector) | Following
 *          + All filter + Vertical single-column ("ek sdehe nache tarf ek ek id show") list for all modes
 * 5. ME: Instant DP Change (Gallery or HD Avatars + VIP Frame) + All Profile Details + Complete Profile Ecosystem
 */
public class QluvxFiveTabHubController {

    public static final int REQ_PICK_PROFILE_DP = 4201;
    public static final int REQ_PICK_GALLERY_PHOTO = 4202;
    public static final int REQ_PICK_CHAT_ALBUM = 4203;
    public static final int REQ_PICK_CHAT_ONETIME = 4204;

    public static class NearbyUserCardItem {
        public final String userId;
        public final String name;
        public final String location;
        public final String distance;
        public final String levelLabel;
        public final boolean isOnline;
        public final int avatarResId;
        public final String channelName;
        public boolean isFollowing;

        public NearbyUserCardItem(
                String userId,
                String name,
                String location,
                String distance,
                String levelLabel,
                boolean isOnline,
                int avatarResId,
                String channelName,
                boolean isFollowing
        ) {
            this.userId = userId;
            this.name = name;
            this.location = location;
            this.distance = distance;
            this.levelLabel = levelLabel;
            this.isOnline = isOnline;
            this.avatarResId = avatarResId;
            this.channelName = channelName;
            this.isFollowing = isFollowing;
        }
    }

    public static class DiscoverPostItem {
        public final String authorName;
        public final String authorId;
        public final String location;
        public final String text;
        public final String timeAgo;
        public final int photoResId;
        public int likesCount;
        public int commentsCount;
        public boolean isLiked;

        public DiscoverPostItem(
                String authorName,
                String authorId,
                String location,
                String text,
                String timeAgo,
                int photoResId,
                int likesCount,
                int commentsCount
        ) {
            this.authorName = authorName;
            this.authorId = authorId;
            this.location = location;
            this.text = text;
            this.timeAgo = timeAgo;
            this.photoResId = photoResId;
            this.likesCount = likesCount;
            this.commentsCount = commentsCount;
        }
    }

    // State for Tab 1: Home (Nearby | Online + 3-column grid)
    private static String homeTopMode = "Nearby"; // "Nearby" or "Online"
    private static String homeSubFilter = "All";  // "All", "PK", "Hot", "New", "Nearby"
    private static List<NearbyUserCardItem> nearbyItems;

    // State for Tab 2: Discover ([Family] [Group] [Quick Chat] + Live Camera / Post / Photo Gallery)
    private static String activeDiscoverTab = "All"; // "All", "Family", "Group", "QuickChat", "Gallery"
    private static List<DiscoverPostItem> discoverPosts;
    private static final List<Integer> galleryPhotos = new ArrayList<>();
    private static final List<Uri> customGalleryUris = new ArrayList<>();

    // State for Tab 3: Chat (Pending counts, locations, one-time photos)
    private static final Map<String, Integer> pendingUnreadCounts = new HashMap<>();
    private static final Map<String, String> peerLocations = new HashMap<>();
    private static final Map<String, String> pendingTimestamps = new HashMap<>();
    private static final Set<String> openedOneTimeMessages = new HashSet<>();

    // State for Tab 4: Live (LIVE | VOICE + Top Single-Line IDs + Vertical 1-by-1 ID list)
    private static boolean isLiveVoiceMode = false; // false = LIVE, true = VOICE
    private static String livePrimaryTab = "Recommended"; // "Recommended", "Global", "Following"
    private static String liveSubFilter = "All"; // "All", "PK", "Hot", "New", "Nearby"
    private static String liveSelectedCountry = "Global";

    // State for Tab 5: Me (Custom DP URI, Diamond balance, Family name)
    private static Uri customProfileDpUri = null;
    private static int diamondBalance = 18450;
    private static String equippedFamilyName = "👑 Royal QLUVX Empire";
    private static String equippedMountName = "🏎️ Neon Cyber Roadster";

    // Luxury Visual Design Mode: 0 = Royal Gold VIP (Default), 1 = Rose Neon, 2 = Cyber Aqua, 3 = Emerald Club
    public static int luxuryDesignMode = 0;

    public static int getLuxuryGradStart() {
        switch (luxuryDesignMode) {
            case 1: return Color.rgb(68, 16, 62);
            case 2: return Color.rgb(15, 38, 74);
            case 3: return Color.rgb(12, 54, 42);
            default: return Color.rgb(58, 32, 12); // Royal Gold VIP
        }
    }

    public static int getLuxuryGradEnd() {
        switch (luxuryDesignMode) {
            case 1: return Color.rgb(26, 14, 42);
            case 2: return Color.rgb(12, 22, 42);
            case 3: return Color.rgb(12, 28, 26);
            default: return Color.rgb(26, 16, 28); // Royal Gold VIP
        }
    }

    public static int getLuxuryBorderAccent() {
        switch (luxuryDesignMode) {
            case 1: return Color.rgb(244, 114, 182);
            case 2: return Color.rgb(56, 189, 248);
            case 3: return Color.rgb(52, 211, 153);
            default: return Color.rgb(251, 191, 36); // Royal Gold VIP
        }
    }

    public static int getLuxuryButtonStart() {
        switch (luxuryDesignMode) {
            case 1: return Color.rgb(236, 72, 153);
            case 2: return Color.rgb(37, 99, 235);
            case 3: return Color.rgb(16, 185, 129);
            default: return Color.rgb(217, 119, 6); // Royal Gold VIP
        }
    }

    public static int getLuxuryButtonEnd() {
        switch (luxuryDesignMode) {
            case 1: return Color.rgb(124, 58, 237);
            case 2: return Color.rgb(6, 182, 212);
            case 3: return Color.rgb(5, 150, 105);
            default: return Color.rgb(225, 29, 72); // Gold-Crimson Royal
        }
    }

    public static void renderLuxuryDesignHeaderBar(
            final Activity activity,
            final LinearLayout container,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.VERTICAL);
        int p10 = UiStyleHelper.dp(activity, 10);
        bar.setPadding(p10, UiStyleHelper.dp(activity, 8), p10, UiStyleHelper.dp(activity, 8));
        bar.setBackground(UiStyleHelper.gradientBox(
                activity,
                getLuxuryGradStart(),
                getLuxuryGradEnd(),
                16f,
                getLuxuryBorderAccent(),
                1.2f
        ));

        // Top Row: QLUVX ROYAL Logo + Coins Pill
        LinearLayout brandRow = new LinearLayout(activity);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(activity);
        logo.setText("👑 QLUVX ROYAL");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        logo.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        logo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        brandRow.addView(logo);

        TextView coinsChip = new TextView(activity);
        coinsChip.setText("🪙 " + economy.getCoinBalance());
        coinsChip.setTextColor(Color.rgb(253, 224, 71));
        coinsChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        coinsChip.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        coinsChip.setPadding(UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 4));
        coinsChip.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(130, 0, 0, 0), 12f, Color.rgb(251, 191, 36), 1f));
        coinsChip.setOnClickListener(v -> callbacks.onSelectMenu(6));
        brandRow.addView(coinsChip);
        bar.addView(brandRow);

        // Second Row: 4 Instant Design Switcher Pills
        HorizontalScrollView designScroll = new HorizontalScrollView(activity);
        designScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout designRow = new LinearLayout(activity);
        designRow.setOrientation(LinearLayout.HORIZONTAL);
        designRow.setPadding(0, UiStyleHelper.dp(activity, 7), 0, 0);

        String[] designs = new String[]{
                "👑 Gold VIP",
                "💖 Rose Neon",
                "💎 Cyber Aqua",
                "✨ Emerald Club"
        };
        int[] designWallpapers = new int[]{1, 0, 5, 3};

        for (int i = 0; i < designs.length; i++) {
            final int dIdx = i;
            boolean active = (luxuryDesignMode == i);
            TextView pill = new TextView(activity);
            pill.setText(designs[i]);
            pill.setTextColor(active ? Color.rgb(15, 23, 42) : Color.WHITE);
            pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            pill.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            pill.setPadding(UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 10), UiStyleHelper.dp(activity, 5));
            pill.setBackground(UiStyleHelper.roundedBox(
                    activity,
                    active ? Color.rgb(253, 224, 71) : Color.argb(110, 15, 23, 42),
                    12f,
                    active ? Color.WHITE : getLuxuryBorderAccent(),
                    1f
            ));
            pill.setOnClickListener(v -> {
                luxuryDesignMode = dIdx;
                UiStyleHelper.applyThemePreset(activity, designWallpapers[dIdx] % UiStyleHelper.THEME_PRESETS.length);
                Toast.makeText(activity, "🎨 Design Applied: " + designs[dIdx], Toast.LENGTH_SHORT).show();
                callbacks.onRefreshScreen();
            });
            designRow.addView(pill, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
        }
        designScroll.addView(designRow);
        bar.addView(designScroll);

        container.addView(bar, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10)));
    }

    private static final String[][] GLOBAL_COUNTRIES = new String[][]{
            {"🌐", "Global"}, {"🇮🇳", "India"},
            {"🇮🇩", "Indonesia"}, {"🇰🇷", "South Korea"},
            {"🇵🇭", "Philippines"}, {"🇻🇳", "Vietnam"},
            {"🇯🇵", "Japan"}, {"🇲🇾", "Malaysia"},
            {"🇹🇭", "Thailand"}, {"🇹🇼", "Taiwan"},
            {"🇺🇸", "United States"}, {"🇵🇰", "Pakistan"},
            {"🇸🇬", "Singapore"}, {"🇧🇩", "Bangladesh"},
            {"🇦🇪", "United Arab Emirates"}, {"🇸🇦", "Saudi Arabia"},
            {"🇹🇷", "Turkey"}, {"🇭🇰", "Hong Kong"},
            {"🇳🇵", "Nepal"}, {"🇱🇰", "Sri Lanka"}
    };

    public static void setCustomProfileDpUri(Activity activity, Uri uri) {
        if (uri != null) {
            customProfileDpUri = uri;
            Toast.makeText(activity, "✅ Profile DP Updated!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void addCustomGalleryUri(Activity activity, Uri uri) {
        if (uri != null) {
            customGalleryUris.add(0, uri);
            activeDiscoverTab = "Gallery";
            Toast.makeText(activity, "🖼️ Photo added to Discover Photo Gallery!", Toast.LENGTH_SHORT).show();
        }
    }

    private static List<NearbyUserCardItem> getNearbyUsers() {
        if (nearbyItems == null) {
            nearbyItems = new ArrayList<>();
            nearbyItems.add(new NearbyUserCardItem("102938", "Aryan ✨", "Mumbai, India", "0.4 km", "LV 32", true, R.drawable.img_avatar_0, "qluvx_live_aryan", true));
            nearbyItems.add(new NearbyUserCardItem("204918", "sushmita", "Delhi, India", "0.8 km", "LV 28", true, R.drawable.img_avatar_1, "qluvx_live_sushmita", true));
            nearbyItems.add(new NearbyUserCardItem("309481", "🌺 Ishani", "Kolkata, India", "1.1 km", "LV 41", true, R.drawable.img_avatar_2, "qluvx_live_ishani", false));
            nearbyItems.add(new NearbyUserCardItem("401928", "Rohan PK", "Bengaluru, India", "1.5 km", "LV 25", true, R.drawable.img_avatar_3, "qluvx_live_rohan", false));
            nearbyItems.add(new NearbyUserCardItem("508172", "🦋 Komolika", "Pune, India", "1.9 km", "LV 36", true, R.drawable.img_avatar_4, "qluvx_live_komolika", true));
            nearbyItems.add(new NearbyUserCardItem("609283", "Zoya Khan", "Hyderabad, India", "2.2 km", "LV 30", true, R.drawable.img_avatar_5, "qluvx_live_zoya", true));
            nearbyItems.add(new NearbyUserCardItem("701829", "Kabir Music", "Jaipur, India", "2.6 km", "LV 22", true, R.drawable.img_avatar_0, "qluvx_live_kabir", false));
            nearbyItems.add(new NearbyUserCardItem("803912", "Neha Sharma", "Chandigarh, India", "3.0 km", "LV 29", false, R.drawable.img_avatar_1, "qluvx_live_neha", false));
            nearbyItems.add(new NearbyUserCardItem("904811", " 도윤 Doyun", "Seoul, South Korea", "Global", "LV 38", true, R.drawable.img_avatar_2, "qluvx_live_doyun", false));
            nearbyItems.add(new NearbyUserCardItem("915822", "Putri Cantik", "Jakarta, Indonesia", "Global", "LV 27", true, R.drawable.img_avatar_3, "qluvx_live_putri", false));
            nearbyItems.add(new NearbyUserCardItem("926833", "Simran 💖", "Lucknow, India", "3.4 km", "LV 31", true, R.drawable.img_avatar_4, "qluvx_live_simran", true));
            nearbyItems.add(new NearbyUserCardItem("937844", "Vikram सिंह", "Ahmedabad, India", "3.8 km", "LV 24", false, R.drawable.img_avatar_5, "qluvx_live_vikram", false));
        }
        return nearbyItems;
    }

    private static List<DiscoverPostItem> getDiscoverPosts() {
        if (discoverPosts == null) {
            discoverPosts = new ArrayList<>();
            discoverPosts.add(new DiscoverPostItem(
                    "sushmita 👑", "ID: 204918", "📍 Mumbai, India",
                    "Live PK Night starting now! Join my room & send roses 🌹🔥",
                    "2m ago", R.drawable.img_avatar_1, 342, 48
            ));
            discoverPosts.add(new DiscoverPostItem(
                    "🌺 Ishani", "ID: 309481", "📍 Delhi, India",
                    "New photo from today's shoot ✨ Drop a ❤️ if we should do a Voice Party!",
                    "15m ago", R.drawable.img_avatar_2, 518, 76
            ));
            discoverPosts.add(new DiscoverPostItem(
                    "Aryan ✨", "ID: 102938", "📍 Bengaluru, India",
                    "QLUVX Royal Family is recruiting active hosts & guardians 🏰👑",
                    "1h ago", R.drawable.img_avatar_0, 289, 34
            ));
        }
        if (galleryPhotos.isEmpty()) {
            galleryPhotos.add(R.drawable.img_avatar_0);
            galleryPhotos.add(R.drawable.img_avatar_1);
            galleryPhotos.add(R.drawable.img_avatar_2);
            galleryPhotos.add(R.drawable.img_avatar_3);
            galleryPhotos.add(R.drawable.img_avatar_4);
            galleryPhotos.add(R.drawable.img_avatar_5);
        }
        return discoverPosts;
    }

    private static void ensureChatMetadataSeeded(QluvxSocialManager social) {
        if (!peerLocations.isEmpty()) return;
        String[] sampleLocs = new String[]{
                "Mumbai, India • 0.8 km",
                "Delhi, India • 1.4 km",
                "Bengaluru, India • 2.1 km",
                "Kolkata, India • 2.9 km",
                "Hyderabad, India • 3.5 km",
                "Pune, India • 4.2 km"
        };
        String[] sampleTimes = new String[]{
                "10:45 AM", "2m ago", "14m ago", "1h ago", "Yesterday", "Yesterday"
        };
        int[] sampleUnread = new int[]{3, 2, 1, 5, 0, 1};

        List<QluvxSocialManager.ChatConversation> convs = social.getConversations();
        for (int i = 0; i < convs.size(); i++) {
            QluvxSocialManager.ChatConversation c = convs.get(i);
            peerLocations.put(c.peerUserId, sampleLocs[i % sampleLocs.length]);
            pendingTimestamps.put(c.peerUserId, sampleTimes[i % sampleTimes.length]);
            pendingUnreadCounts.put(c.peerUserId, sampleUnread[i % sampleUnread.length]);
        }
    }

    // =========================================================================
    // TAB 1: HOME BUTTON
    // - Nearby | Online switch
    // - 3-column grid ("3 line par near by I'd show location name sath")
    // - All Home buttons & ranking banners preserved
    // =========================================================================
    public static void renderHomeTab(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final VoiceRoomModels.RoomRepository roomRepo,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        renderLuxuryDesignHeaderBar(activity, container, economy, callbacks);

        // 1. Luxury Segmented Bar: [ 📍 Nearby ] [ 🟢 Online ] + [ 🔍 ] [ 🔔 ] [ 📹+ Live ]
        LinearLayout topBar = new LinearLayout(activity);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));

        String[] homeModes = new String[]{"Nearby", "Online"};
        for (final String hm : homeModes) {
            boolean active = hm.equalsIgnoreCase(homeTopMode);
            TextView tv = new TextView(activity);
            tv.setText(hm.equals("Nearby") ? "📍 Nearby" : "🟢 Online");
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            tv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            tv.setPadding(UiStyleHelper.dp(activity, 14), UiStyleHelper.dp(activity, 7), UiStyleHelper.dp(activity, 14), UiStyleHelper.dp(activity, 7));
            tv.setBackground(active
                    ? UiStyleHelper.gradientBox(activity, getLuxuryButtonStart(), getLuxuryButtonEnd(), 16f, Color.WHITE, 1.2f)
                    : UiStyleHelper.roundedBox(activity, Color.rgb(28, 32, 46), 16f, getLuxuryBorderAccent(), 0.8f));
            tv.setOnClickListener(v -> {
                homeTopMode = hm;
                callbacks.onRefreshScreen();
            });
            topBar.addView(tv, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 8), 0));
        }

        View spacer = new View(activity);
        topBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView btnSearch = UiStyleHelper.createStatusChip(activity, "🔍", Color.rgb(30, 36, 52), Color.WHITE, getLuxuryBorderAccent());
        btnSearch.setOnClickListener(v -> callbacks.onSelectMenu(1));
        topBar.addView(btnSearch, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));

        TextView btnCamLive = UiStyleHelper.createStatusChip(activity, "📹+ Live", getLuxuryButtonStart(), Color.WHITE, Color.WHITE);
        btnCamLive.setOnClickListener(v ->
                callbacks.onOpenLiveVideoRoom("Your LIVE Title", "qluvx_live_video_pk", "Pre-LIVE Setup", "10001", R.drawable.img_avatar_0)
        );
        topBar.addView(btnCamLive);

        container.addView(topBar);

        // 2. 3-Column Grid ("3 line par near by I'd show location name sath")
        List<NearbyUserCardItem> list = new ArrayList<>();
        for (NearbyUserCardItem item : getNearbyUsers()) {
            if ("Online".equalsIgnoreCase(homeTopMode) && !item.isOnline) continue;
            list.add(item);
        }

        for (int i = 0; i < list.size(); i += 3) {
            LinearLayout row3 = new LinearLayout(activity);
            row3.setOrientation(LinearLayout.HORIZONTAL);
            row3.setWeightSum(3f);
            if (i > 0) {
                row3.setPadding(0, UiStyleHelper.dp(activity, 8), 0, 0);
            }

            for (int col = 0; col < 3; col++) {
                int idx = i + col;
                if (idx < list.size()) {
                    final NearbyUserCardItem user = list.get(idx);
                    View card = buildThreeColumnNearbyIdCard(activity, user, social, callbacks);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(
                            col == 0 ? 0 : UiStyleHelper.dp(activity, 4),
                            0,
                            col == 2 ? 0 : UiStyleHelper.dp(activity, 4),
                            0
                    );
                    row3.addView(card, lp);
                } else {
                    View empty = new View(activity);
                    row3.addView(empty, new LinearLayout.LayoutParams(0, 1, 1f));
                }
            }
            container.addView(row3);
        }
    }

    private static View buildThreeColumnNearbyIdCard(
            final Activity activity,
            final NearbyUserCardItem user,
            final QluvxSocialManager social,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiStyleHelper.gradientBox(
                activity,
                getLuxuryGradStart(),
                getLuxuryGradEnd(),
                15f,
                getLuxuryBorderAccent(),
                1.2f
        ));

        // Top Portrait Image with Online Dot & Distance Badge
        FrameLayout imgWrap = new FrameLayout(activity);
        int imgH = UiStyleHelper.dp(activity, 122);
        imgWrap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imgH));
        imgWrap.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 14));
            }
        });
        imgWrap.setClipToOutline(true);

        ImageView iv = new ImageView(activity);
        iv.setImageResource(user.avatarResId);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgWrap.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imgH));

        // Top-left Online Dot + Distance
        TextView onlinePill = new TextView(activity);
        onlinePill.setText((user.isOnline ? "🟢 " : "⚪ ") + user.distance);
        onlinePill.setTextColor(Color.WHITE);
        onlinePill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        onlinePill.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        onlinePill.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2));
        onlinePill.setBackground(UiStyleHelper.roundedBox(activity, Color.argb(190, 10, 14, 24), 8f, getLuxuryBorderAccent(), 0.7f));
        FrameLayout.LayoutParams opLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        int m4 = UiStyleHelper.dp(activity, 4);
        opLp.setMargins(m4, m4, m4, m4);
        imgWrap.addView(onlinePill, opLp);

        // Top-right Follow (+) badge
        TextView followDot = new TextView(activity);
        followDot.setText(user.isFollowing ? "✓" : "＋");
        followDot.setTextColor(Color.WHITE);
        followDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        followDot.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        followDot.setGravity(Gravity.CENTER);
        int fSz = UiStyleHelper.dp(activity, 22);
        followDot.setBackground(UiStyleHelper.circle(activity, user.isFollowing ? UiStyleHelper.COLOR_LIVE_GREEN : getLuxuryButtonStart(), Color.WHITE, 1f));
        FrameLayout.LayoutParams fLp = new FrameLayout.LayoutParams(fSz, fSz, Gravity.TOP | Gravity.END);
        fLp.setMargins(m4, m4, m4, m4);
        followDot.setOnClickListener(v -> {
            user.isFollowing = !user.isFollowing;
            Toast.makeText(activity, user.isFollowing ? ("Followed " + user.name) : ("Unfollowed " + user.name), Toast.LENGTH_SHORT).show();
            callbacks.onRefreshScreen();
        });
        imgWrap.addView(followDot, fLp);

        card.addView(imgWrap);

        // Bottom Info Box: Name + Golden ID Badge + Location + 1-Tap Action Pill
        LinearLayout infoBox = new LinearLayout(activity);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        int p7 = UiStyleHelper.dp(activity, 7);
        infoBox.setPadding(p7, p7, p7, p7);

        TextView tvName = new TextView(activity);
        tvName.setText(user.name);
        tvName.setSingleLine(true);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        infoBox.addView(tvName);

        TextView tvId = new TextView(activity);
        tvId.setText("ID: " + user.userId);
        tvId.setSingleLine(true);
        tvId.setTextColor(Color.rgb(253, 224, 71));
        tvId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvId.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvId.setPadding(0, UiStyleHelper.dp(activity, 1), 0, UiStyleHelper.dp(activity, 1));
        infoBox.addView(tvId);

        TextView tvLoc = new TextView(activity);
        tvLoc.setText("📍 " + user.location);
        tvLoc.setSingleLine(true);
        tvLoc.setTextColor(Color.rgb(226, 232, 240));
        tvLoc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        infoBox.addView(tvLoc);

        TextView btnOpen = new TextView(activity);
        btnOpen.setText("📹 Live • 💬 Chat");
        btnOpen.setGravity(Gravity.CENTER);
        btnOpen.setSingleLine(true);
        btnOpen.setTextColor(Color.WHITE);
        btnOpen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        btnOpen.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        btnOpen.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4));
        btnOpen.setBackground(UiStyleHelper.gradientBox(activity, getLuxuryButtonStart(), getLuxuryButtonEnd(), 8f, 0, 0));
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.setMargins(0, UiStyleHelper.dp(activity, 5), 0, 0);
        infoBox.addView(btnOpen, actLp);

        card.addView(infoBox);

        card.setOnClickListener(v -> {
            String[] actions = new String[]{
                    "📹 Watch " + user.name + " Live Stream",
                    "💬 Open Message Box with " + user.name + " (ID: " + user.userId + ")",
                    "🎙️ Join " + user.name + "'s Voice Room"
            };
            new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(user.name + " • ID: " + user.userId + "\n📍 " + user.location)
                    .setItems(actions, (d, which) -> {
                        if (which == 0) {
                            callbacks.onOpenLiveVideoRoom(user.name + " Live", user.channelName, user.name, user.userId, user.avatarResId);
                        } else if (which == 1) {
                            List<QluvxSocialManager.UserProfile> all = social.getAllCommunityUsers();
                            if (!all.isEmpty()) {
                                callbacks.onOpenDirectChatWithPeer(all.get(0));
                            } else {
                                callbacks.onSelectMenu(4);
                            }
                        } else {
                            callbacks.onOpenVoiceRoom(user.name + " Voice Party", user.channelName);
                        }
                    })
                    .setPositiveButton("Close", null)
                    .show();
        });

        return card;
    }

    // =========================================================================
    // TAB 2: DISCOVER BUTTON
    // - [ Family ] [ Group ] [ Quick Chat ]
    // - [ Live Camera ] [ Post ] [ Photo Gallery ]
    // =========================================================================
    public static void renderDiscoverTab(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final VoiceRoomModels.RoomRepository roomRepo,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        renderLuxuryDesignHeaderBar(activity, container, economy, callbacks);
        getDiscoverPosts();

        // 1. Top 3 Primary Cards: [ 🏰 Family ]  [ 👥 Group ]  [ ⚡ Quick Chat ]
        LinearLayout topThreeRow = new LinearLayout(activity);
        topThreeRow.setOrientation(LinearLayout.HORIZONTAL);
        topThreeRow.setWeightSum(3f);
        topThreeRow.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 10));

        String[][] topCards = new String[][]{
                {"🏰", "Family", "Royal Clans", "Family"},
                {"👥", "Group", "Party Rooms", "Group"},
                {"⚡", "Quick Chat", "Instant Match", "QuickChat"}
        };
        int[][] topGradColors = new int[][]{
                {Color.rgb(124, 45, 18), Color.rgb(217, 119, 6), Color.rgb(253, 224, 71)},
                {Color.rgb(30, 58, 138), Color.rgb(37, 99, 235), Color.rgb(56, 189, 248)},
                {Color.rgb(131, 24, 67), Color.rgb(219, 39, 119), Color.rgb(244, 114, 182)}
        };

        for (int i = 0; i < topCards.length; i++) {
            final String[] tc = topCards[i];
            final int[] gc = topGradColors[i];
            boolean selected = tc[3].equalsIgnoreCase(activeDiscoverTab);

            LinearLayout box = new LinearLayout(activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            int pad = UiStyleHelper.dp(activity, 10);
            box.setPadding(pad, pad, pad, pad);
            box.setBackground(UiStyleHelper.gradientBox(activity, gc[0], gc[1], 14f, selected ? Color.WHITE : gc[2], selected ? 2f : 1f));

            TextView icon = new TextView(activity);
            icon.setText(tc[0]);
            icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            icon.setGravity(Gravity.CENTER);
            box.addView(icon);

            TextView title = new TextView(activity);
            title.setText(tc[1]);
            title.setTextColor(Color.WHITE);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            box.addView(title);

            TextView sub = new TextView(activity);
            sub.setText(tc[2]);
            sub.setTextColor(Color.argb(220, 255, 255, 255));
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            box.addView(sub);

            box.setOnClickListener(v -> {
                activeDiscoverTab = tc[3];
                callbacks.onRefreshScreen();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(i == 0 ? 0 : UiStyleHelper.dp(activity, 4), 0, i == 2 ? 0 : UiStyleHelper.dp(activity, 4), 0);
            topThreeRow.addView(box, lp);
        }
        container.addView(topThreeRow);

        // 2. Second Action Bar: [ 📹 Live Camera ]  [ ✍️ Post ]  [ 🖼️ Photo Gallery ]
        LinearLayout mediaActionRow = new LinearLayout(activity);
        mediaActionRow.setOrientation(LinearLayout.HORIZONTAL);
        mediaActionRow.setWeightSum(3f);
        mediaActionRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 12));

        TextView btnLiveCam = buildDiscoverActionButton(activity, "📹 Live Camera", Color.rgb(236, 72, 153), v ->
                callbacks.onOpenLiveVideoRoom("Your LIVE Title", "qluvx_live_video_pk", "Pre-LIVE Setup", "10001", R.drawable.img_avatar_0)
        );
        TextView btnPost = buildDiscoverActionButton(activity, "✍️ Post", Color.rgb(124, 58, 237), v ->
                showCreateDiscoverPostDialog(activity, social, callbacks)
        );
        TextView btnGallery = buildDiscoverActionButton(activity, "🖼️ Photo Gallery", Color.rgb(16, 185, 129), v -> {
            activeDiscoverTab = "Gallery";
            callbacks.onRefreshScreen();
        });

        LinearLayout.LayoutParams mLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mLp1.setMargins(0, 0, UiStyleHelper.dp(activity, 4), 0);
        LinearLayout.LayoutParams mLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mLp2.setMargins(UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4), 0);
        LinearLayout.LayoutParams mLp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mLp3.setMargins(UiStyleHelper.dp(activity, 4), 0, 0, 0);

        mediaActionRow.addView(btnLiveCam, mLp1);
        mediaActionRow.addView(btnPost, mLp2);
        mediaActionRow.addView(btnGallery, mLp3);
        container.addView(mediaActionRow);

        // 3. Render Selected Discover Section
        if ("Family".equalsIgnoreCase(activeDiscoverTab)) {
            renderDiscoverFamilySection(activity, container, callbacks);
        } else if ("Group".equalsIgnoreCase(activeDiscoverTab)) {
            renderDiscoverGroupSection(activity, container, roomRepo, callbacks);
        } else if ("QuickChat".equalsIgnoreCase(activeDiscoverTab)) {
            renderDiscoverQuickChatSection(activity, container, social, callbacks);
        } else if ("Gallery".equalsIgnoreCase(activeDiscoverTab)) {
            renderDiscoverPhotoGallerySection(activity, container, callbacks);
        }

        // 4. Moments & Photo Posts Feed (Always visible below so user can browse posts & gallery)
        LinearLayout feedHeader = new LinearLayout(activity);
        feedHeader.setOrientation(LinearLayout.HORIZONTAL);
        feedHeader.setGravity(Gravity.CENTER_VERTICAL);
        feedHeader.setPadding(0, UiStyleHelper.dp(activity, 6), 0, UiStyleHelper.dp(activity, 8));

        TextView tvFeedTitle = new TextView(activity);
        tvFeedTitle.setText("✨ Discover Moments, Live Camera & Photo Posts");
        tvFeedTitle.setTextColor(Color.WHITE);
        tvFeedTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tvFeedTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvFeedTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        feedHeader.addView(tvFeedTitle);
        container.addView(feedHeader);

        for (final DiscoverPostItem post : getDiscoverPosts()) {
            LinearLayout pCard = new LinearLayout(activity);
            pCard.setOrientation(LinearLayout.VERTICAL);
            int pad = UiStyleHelper.dp(activity, 12);
            pCard.setPadding(pad, pad, pad, pad);
            pCard.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(22, 24, 34), 14f, Color.rgb(48, 54, 74), 1f));

            LinearLayout authorRow = new LinearLayout(activity);
            authorRow.setOrientation(LinearLayout.HORIZONTAL);
            authorRow.setGravity(Gravity.CENTER_VERTICAL);

            View av = UiStyleHelper.createCircularAvatar(activity, post.photoResId, 42, UiStyleHelper.COLOR_NEON_PINK, false);
            authorRow.addView(av);

            LinearLayout meta = new LinearLayout(activity);
            meta.setOrientation(LinearLayout.VERTICAL);
            meta.setPadding(UiStyleHelper.dp(activity, 10), 0, 0, 0);
            meta.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView nm = new TextView(activity);
            nm.setText(post.authorName + "  •  " + post.authorId);
            nm.setTextColor(Color.WHITE);
            nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            nm.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

            TextView loc = new TextView(activity);
            loc.setText(post.location + "  •  " + post.timeAgo);
            loc.setTextColor(Color.rgb(56, 189, 248));
            loc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

            meta.addView(nm);
            meta.addView(loc);
            authorRow.addView(meta);
            pCard.addView(authorRow);

            TextView body = new TextView(activity);
            body.setText(post.text);
            body.setTextColor(Color.rgb(226, 232, 240));
            body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            body.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));
            pCard.addView(body);

            ImageView ivPost = new ImageView(activity);
            ivPost.setImageResource(post.photoResId);
            ivPost.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivPost.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 12));
                }
            });
            ivPost.setClipToOutline(true);
            pCard.addView(ivPost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(activity, 170)));

            LinearLayout reactRow = new LinearLayout(activity);
            reactRow.setOrientation(LinearLayout.HORIZONTAL);
            reactRow.setPadding(0, UiStyleHelper.dp(activity, 8), 0, 0);

            TextView btnLike = UiStyleHelper.createStatusChip(
                    activity,
                    (post.isLiked ? "❤️ " : "🤍 ") + post.likesCount + " Likes",
                    post.isLiked ? Color.argb(80, 236, 72, 153) : Color.rgb(30, 34, 48),
                    Color.WHITE,
                    0
            );
            btnLike.setOnClickListener(v -> {
                post.isLiked = !post.isLiked;
                post.likesCount += post.isLiked ? 1 : -1;
                callbacks.onRefreshScreen();
            });

            TextView btnComment = UiStyleHelper.createStatusChip(
                    activity,
                    "💬 " + post.commentsCount + " Comments",
                    Color.rgb(30, 34, 48),
                    Color.rgb(203, 213, 225),
                    0
            );
            btnComment.setOnClickListener(v -> {
                post.commentsCount++;
                Toast.makeText(activity, "💬 Comment posted on " + post.authorName + "'s post!", Toast.LENGTH_SHORT).show();
                callbacks.onRefreshScreen();
            });

            reactRow.addView(btnLike);
            reactRow.addView(btnComment, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 8), 0, 0, 0));
            pCard.addView(reactRow);

            container.addView(pCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10)));
        }
    }

    private static TextView buildDiscoverActionButton(Activity activity, String label, int bgColor, View.OnClickListener listener) {
        TextView tv = new TextView(activity);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tv.setGravity(Gravity.CENTER);
        int padV = UiStyleHelper.dp(activity, 10);
        tv.setPadding(UiStyleHelper.dp(activity, 6), padV, UiStyleHelper.dp(activity, 6), padV);
        tv.setBackground(UiStyleHelper.roundedBox(activity, bgColor, 12f, Color.WHITE, 0.8f));
        tv.setOnClickListener(listener);
        return tv;
    }

    private static void renderDiscoverFamilySection(
            final Activity activity,
            final LinearLayout container,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout card = UiStyleHelper.createCardContainer(activity, Color.rgb(251, 191, 36));
        TextView header = new TextView(activity);
        header.setText("🏰 QLUVX Families & Royal Clans");
        header.setTextColor(Color.rgb(253, 224, 71));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        header.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        card.addView(header);

        String[][] families = new String[][]{
                {"👑 Royal QLUVX Empire", "ID: FAM-101 • 148 Members • Rank #1", "Join Family"},
                {"⚡ Nawabzade Clan", "ID: FAM-204 • 112 Members • Rank #2", "Join Family"},
                {"🐉 Dragon PK Warriors", "ID: FAM-309 • 96 Members • Rank #3", "Join Family"}
        };
        for (int i = 0; i < families.length; i++) {
            final String[] fam = families[i];
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));

            View av = UiStyleHelper.createCircularAvatar(activity, VoiceRoomModels.getAvatarResId(i), 42, Color.rgb(251, 191, 36), false);
            row.addView(av);

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 8), 0);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t = new TextView(activity);
            t.setText(fam[0]);
            t.setTextColor(Color.WHITE);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView s = new TextView(activity);
            s.setText(fam[1]);
            s.setTextColor(Color.rgb(203, 213, 225));
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

            col.addView(t);
            col.addView(s);
            row.addView(col);

            TextView btnJoin = UiStyleHelper.createStatusChip(activity, fam[2], Color.rgb(217, 119, 6), Color.WHITE, 0);
            btnJoin.setOnClickListener(v -> {
                equippedFamilyName = fam[0];
                Toast.makeText(activity, "🏰 Joined " + fam[0] + "!", Toast.LENGTH_SHORT).show();
                callbacks.onRefreshScreen();
            });
            row.addView(btnJoin);
            card.addView(row);
        }
        container.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
    }

    private static void renderDiscoverGroupSection(
            final Activity activity,
            final LinearLayout container,
            final VoiceRoomModels.RoomRepository roomRepo,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout card = UiStyleHelper.createCardContainer(activity, Color.rgb(56, 189, 248));
        TextView header = new TextView(activity);
        header.setText("👥 Active Voice & Chat Groups");
        header.setTextColor(Color.rgb(56, 189, 248));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        header.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        card.addView(header);

        String[][] groups = new String[][]{
                {"🎙️ Late Night Friends Group", "ID: GRP-8801 • 42 Online • Mumbai, India", "qluvx_voice_chinnu"},
                {"🎵 Bollywood & Desi Beats Group", "ID: GRP-7742 • 38 Online • Delhi, India", "qluvx_voice_fukrey"},
                {"🎮 Gaming & PK Squad Group", "ID: GRP-6619 • 29 Online • Global", "qluvx_voice_warzone"}
        };
        for (int i = 0; i < groups.length; i++) {
            final String[] g = groups[i];
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));

            View av = UiStyleHelper.createCircularAvatar(activity, VoiceRoomModels.getAvatarResId(i + 1), 42, Color.rgb(56, 189, 248), false);
            row.addView(av);

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 8), 0);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t = new TextView(activity);
            t.setText(g[0]);
            t.setTextColor(Color.WHITE);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView s = new TextView(activity);
            s.setText(g[1]);
            s.setTextColor(Color.rgb(203, 213, 225));
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

            col.addView(t);
            col.addView(s);
            row.addView(col);

            TextView btnEnter = UiStyleHelper.createStatusChip(activity, "Join Group", Color.rgb(37, 99, 235), Color.WHITE, 0);
            btnEnter.setOnClickListener(v -> callbacks.onOpenVoiceRoom(g[0], g[2]));
            row.addView(btnEnter);
            card.addView(row);
        }
        container.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
    }

    private static void renderDiscoverQuickChatSection(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout card = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_NEON_PINK);
        TextView header = new TextView(activity);
        header.setText("⚡ Quick Chat — Instant Online Match");
        header.setTextColor(Color.rgb(244, 114, 182));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        header.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        card.addView(header);

        List<QluvxSocialManager.UserProfile> users = social.getAllCommunityUsers();
        for (int i = 0; i < Math.min(4, users.size()); i++) {
            final QluvxSocialManager.UserProfile u = users.get(i);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));

            View av = UiStyleHelper.createCircularAvatar(activity, VoiceRoomModels.getAvatarResId(i), 42, UiStyleHelper.COLOR_LIVE_GREEN, false);
            row.addView(av);

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 8), 0);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t = new TextView(activity);
            t.setText(u.displayName + " • ID: " + u.userId);
            t.setTextColor(Color.WHITE);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView s = new TextView(activity);
            s.setText("🟢 Online Now • 📍 " + u.country);
            s.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

            col.addView(t);
            col.addView(s);
            row.addView(col);

            TextView btnChat = UiStyleHelper.createStatusChip(activity, "💬 Quick Chat", UiStyleHelper.COLOR_NEON_PINK, Color.WHITE, 0);
            btnChat.setOnClickListener(v -> callbacks.onOpenDirectChatWithPeer(u));
            row.addView(btnChat);
            card.addView(row);
        }
        container.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
    }

    private static void renderDiscoverPhotoGallerySection(
            final Activity activity,
            final LinearLayout container,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout card = UiStyleHelper.createCardContainer(activity, Color.rgb(16, 185, 129));

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));

        TextView title = new TextView(activity);
        title.setText("🖼️ Photo Gallery (" + (galleryPhotos.size() + customGalleryUris.size()) + " Photos)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnUpload = UiStyleHelper.createStatusChip(activity, "＋ Add from Gallery", Color.rgb(16, 185, 129), Color.WHITE, 0);
        btnUpload.setOnClickListener(v -> {
            try {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                activity.startActivityForResult(pick, REQ_PICK_GALLERY_PHOTO);
            } catch (Exception e) {
                galleryPhotos.add(0, R.drawable.img_avatar_2);
                callbacks.onRefreshScreen();
            }
        });

        topRow.addView(title);
        topRow.addView(btnUpload);
        card.addView(topRow);

        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(3);
        int cellIdx = 0;

        for (Uri uri : customGalleryUris) {
            ImageView iv = new ImageView(activity);
            iv.setImageURI(uri);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = UiStyleHelper.dp(activity, 102);
            lp.columnSpec = GridLayout.spec(cellIdx % 3, 1f);
            lp.rowSpec = GridLayout.spec(cellIdx / 3);
            int m = UiStyleHelper.dp(activity, 3);
            lp.setMargins(m, m, m, m);
            grid.addView(iv, lp);
            cellIdx++;
        }

        for (final int resId : galleryPhotos) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(resId);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setOnClickListener(v -> Toast.makeText(activity, "🖼️ Viewing HD Gallery Photo", Toast.LENGTH_SHORT).show());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = UiStyleHelper.dp(activity, 102);
            lp.columnSpec = GridLayout.spec(cellIdx % 3, 1f);
            lp.rowSpec = GridLayout.spec(cellIdx / 3);
            int m = UiStyleHelper.dp(activity, 3);
            lp.setMargins(m, m, m, m);
            grid.addView(iv, lp);
            cellIdx++;
        }

        card.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 12)));
    }

    private static void showCreateDiscoverPostDialog(
            final Activity activity,
            final QluvxSocialManager social,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        box.setPadding(pad, pad, pad, pad);

        final EditText etText = new EditText(activity);
        etText.setHint("Write your post caption...");
        box.addView(etText);

        final EditText etLoc = new EditText(activity);
        etLoc.setHint("Location (e.g. Mumbai, India)");
        etLoc.setText("📍 " + social.getCurrentUser().country);
        box.addView(etLoc);

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("✍️ Create New Post & Photo")
                .setView(box)
                .setPositiveButton("Publish Post", (d, w) -> {
                    String txt = etText.getText().toString().trim();
                    if (txt.isEmpty()) txt = "Sharing good vibes on QLUVX ✨📸";
                    String loc = etLoc.getText().toString().trim();
                    if (loc.isEmpty()) loc = "📍 India";
                    getDiscoverPosts().add(0, new DiscoverPostItem(
                            social.getCurrentUser().displayName,
                            "ID: " + social.getCurrentUser().userId,
                            loc,
                            txt,
                            "Just now",
                            social.getCurrentUser().avatarResId != 0 ? social.getCurrentUser().avatarResId : R.drawable.img_avatar_0,
                            1,
                            0
                    ));
                    Toast.makeText(activity, "✅ Post published to Discover feed!", Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // TAB 3: CHAT BUTTON
    // - Upar single line ID show
    // - Niche single ID all show + Pending message time show
    // - ID tap karne par Message Box khulega (Location + ID sath, niche Camera, Album,
    //   Location Send, One-Time Photo Send, Video Call)
    // =========================================================================
    public static void renderChatTab(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        renderLuxuryDesignHeaderBar(activity, container, economy, callbacks);
        ensureChatMetadataSeeded(social);
        final List<QluvxSocialManager.ChatConversation> allConvs = social.getConversations();

        // 1. TOP SINGLE LINE ID SHOW ("Upar single line id show")
        TextView topStripLabel = new TextView(activity);
        topStripLabel.setText("🟢 Online IDs (Tap any ID to open Message Box)");
        topStripLabel.setTextColor(Color.rgb(148, 163, 184));
        topStripLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        topStripLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        topStripLabel.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 6));
        container.addView(topStripLabel);

        HorizontalScrollView topSingleLineScroll = new HorizontalScrollView(activity);
        topSingleLineScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout topSingleLineRow = new LinearLayout(activity);
        topSingleLineRow.setOrientation(LinearLayout.HORIZONTAL);
        topSingleLineRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 12));

        for (int i = 0; i < allConvs.size(); i++) {
            final QluvxSocialManager.ChatConversation conv = allConvs.get(i);
            final int avRes = VoiceRoomModels.getAvatarResId(i + 1);

            LinearLayout idPill = new LinearLayout(activity);
            idPill.setOrientation(LinearLayout.VERTICAL);
            idPill.setGravity(Gravity.CENTER_HORIZONTAL);
            int p8 = UiStyleHelper.dp(activity, 8);
            idPill.setPadding(p8, p8, p8, p8);
            idPill.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(24, 27, 38), 14f, Color.rgb(56, 189, 248), 1f));

            View av = UiStyleHelper.createCircularAvatar(activity, avRes, 48, UiStyleHelper.COLOR_LIVE_GREEN, false);
            idPill.addView(av);

            TextView tvName = new TextView(activity);
            tvName.setText(conv.peerName);
            tvName.setSingleLine(true);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            tvName.setPadding(0, UiStyleHelper.dp(activity, 4), 0, 0);
            idPill.addView(tvName);

            TextView tvId = new TextView(activity);
            tvId.setText("ID: " + conv.peerUserId);
            tvId.setSingleLine(true);
            tvId.setTextColor(Color.rgb(56, 189, 248));
            tvId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            idPill.addView(tvId);

            idPill.setOnClickListener(v -> {
                pendingUnreadCounts.put(conv.peerUserId, 0);
                showFullMessageBoxDialog(activity, conv, avRes, social, economy, callbacks);
                callbacks.onRefreshScreen();
            });

            topSingleLineRow.addView(idPill, UiStyleHelper.params(
                    UiStyleHelper.dp(activity, 92),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, UiStyleHelper.dp(activity, 8), 0
            ));
        }
        topSingleLineScroll.addView(topSingleLineRow);
        container.addView(topSingleLineScroll);

        // 2. BELOW SINGLE ID ALL SHOW + PENDING MESSAGE TIME SHOW ("Niche single I'd all show", "Pending message time show")
        TextView listHeader = new TextView(activity);
        listHeader.setText("💬 All Messages & Pending Chats (Tap ID to open Message Box)");
        listHeader.setTextColor(Color.WHITE);
        listHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        listHeader.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        listHeader.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 8));
        container.addView(listHeader);

        for (int i = 0; i < allConvs.size(); i++) {
            final QluvxSocialManager.ChatConversation conv = allConvs.get(i);
            final int avRes = VoiceRoomModels.getAvatarResId(i + 1);
            final String loc = peerLocations.containsKey(conv.peerUserId) ? peerLocations.get(conv.peerUserId) : "Mumbai, India • 1.2 km";
            final String timeStr = pendingTimestamps.containsKey(conv.peerUserId) ? pendingTimestamps.get(conv.peerUserId) : "Just now";
            final int unread = pendingUnreadCounts.containsKey(conv.peerUserId) ? pendingUnreadCounts.get(conv.peerUserId) : 0;

            String lastMsg = "Tap to start chatting...";
            if (!conv.messages.isEmpty()) {
                lastMsg = conv.messages.get(conv.messages.size() - 1).content;
            }

            LinearLayout rowCard = new LinearLayout(activity);
            rowCard.setOrientation(LinearLayout.HORIZONTAL);
            rowCard.setGravity(Gravity.CENTER_VERTICAL);
            int pad = UiStyleHelper.dp(activity, 12);
            rowCard.setPadding(pad, pad, pad, pad);
            rowCard.setBackground(UiStyleHelper.gradientBox(
                    activity,
                    getLuxuryGradStart(),
                    getLuxuryGradEnd(),
                    16f,
                    unread > 0 ? Color.rgb(253, 224, 71) : getLuxuryBorderAccent(),
                    unread > 0 ? 1.5f : 1f
            ));

            View av = UiStyleHelper.createCircularAvatar(activity, avRes, 52, UiStyleHelper.COLOR_LIVE_GREEN, false);
            rowCard.addView(av);

            LinearLayout midCol = new LinearLayout(activity);
            midCol.setOrientation(LinearLayout.VERTICAL);
            midCol.setPadding(UiStyleHelper.dp(activity, 12), 0, UiStyleHelper.dp(activity, 8), 0);
            midCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNameAndId = new TextView(activity);
            tvNameAndId.setText(conv.peerName + "   •   ID: " + conv.peerUserId);
            tvNameAndId.setSingleLine(true);
            tvNameAndId.setTextColor(Color.WHITE);
            tvNameAndId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            tvNameAndId.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            midCol.addView(tvNameAndId);

            TextView tvLocation = new TextView(activity);
            tvLocation.setText("📍 " + loc);
            tvLocation.setSingleLine(true);
            tvLocation.setTextColor(Color.rgb(56, 189, 248));
            tvLocation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvLocation.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 2));
            midCol.addView(tvLocation);

            TextView tvPreview = new TextView(activity);
            tvPreview.setText(lastMsg);
            tvPreview.setSingleLine(true);
            tvPreview.setTextColor(unread > 0 ? Color.WHITE : Color.rgb(148, 163, 184));
            tvPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            midCol.addView(tvPreview);

            rowCard.addView(midCol);

            // Right Column: Pending Message Time + Unread Badge
            LinearLayout rightCol = new LinearLayout(activity);
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            TextView tvTime = new TextView(activity);
            tvTime.setText("🕒 " + timeStr);
            tvTime.setTextColor(unread > 0 ? Color.rgb(253, 224, 71) : Color.rgb(148, 163, 184));
            tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            tvTime.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            rightCol.addView(tvTime);

            TextView badge = new TextView(activity);
            badge.setText(unread > 0 ? (unread + " Pending") : "Open 💬");
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            badge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            badge.setPadding(UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 3), UiStyleHelper.dp(activity, 8), UiStyleHelper.dp(activity, 3));
            badge.setBackground(UiStyleHelper.roundedBox(
                    activity,
                    unread > 0 ? Color.rgb(239, 68, 68) : Color.rgb(37, 99, 235),
                    10f,
                    0,
                    0
            ));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(0, UiStyleHelper.dp(activity, 5), 0, 0);
            rightCol.addView(badge, bLp);

            rowCard.addView(rightCol);

            // Clicking the ID row opens the full Message Box Dialog!
            rowCard.setOnClickListener(v -> {
                pendingUnreadCounts.put(conv.peerUserId, 0);
                showFullMessageBoxDialog(activity, conv, avRes, social, economy, callbacks);
                callbacks.onRefreshScreen();
            });

            container.addView(rowCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 8)));
        }
    }

    /**
     * Full Message Box Dialog when tapping any User ID in Chat:
     * - Shows Peer Name + User ID + Location at the top + Video Call button
     * - Shows all messages (including One-Time Photos & Location Pins)
     * - Bottom action bar with:
     *   [ 📷 Camera ] [ 🖼️ Album ] [ 📍 Location ] [ 🔥 1-Time Photo ] [ 📹 Video Call ] [ 🎤 Voice ] [ 🎁 Gift ]
     */
    public static void showFullMessageBoxDialog(
            final Activity activity,
            final QluvxSocialManager.ChatConversation conv,
            final int peerAvatarRes,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        final String loc = peerLocations.containsKey(conv.peerUserId) ? peerLocations.get(conv.peerUserId) : "Mumbai, India • 1.2 km";

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 12);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(16, 18, 28), 18f, UiStyleHelper.COLOR_AGORA_CYAN, 1.2f));

        // 1. Header with Avatar + Name + ID + Location + Video Call + Close
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));

        View av = UiStyleHelper.createCircularAvatar(activity, peerAvatarRes, 44, UiStyleHelper.COLOR_LIVE_GREEN, false);
        header.addView(av);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 6), 0);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(activity);
        tvName.setText(conv.peerName + "  •  ID: " + conv.peerUserId);
        tvName.setSingleLine(true);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

        TextView tvLoc = new TextView(activity);
        tvLoc.setText("📍 " + loc + "  •  🟢 Online");
        tvLoc.setSingleLine(true);
        tvLoc.setTextColor(Color.rgb(56, 189, 248));
        tvLoc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

        titleCol.addView(tvName);
        titleCol.addView(tvLoc);
        header.addView(titleCol);

        TextView btnVideoCallTop = UiStyleHelper.createStatusChip(activity, "📹 Video Call", UiStyleHelper.COLOR_LIVE_GREEN, Color.WHITE, 0);
        btnVideoCallTop.setOnClickListener(v -> {
            dialog.dismiss();
            callbacks.onOpenLiveVideoRoom("Video Call • " + conv.peerName, "qluvx_call_" + conv.peerUserId, conv.peerName, conv.peerUserId, peerAvatarRes);
        });
        header.addView(btnVideoCallTop, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));

        TextView btnClose = UiStyleHelper.createStatusChip(activity, "✕", Color.rgb(51, 65, 85), Color.WHITE, 0);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        header.addView(btnClose);

        root.addView(header);

        // 2. Scrollable Messages Area
        final ScrollView msgScroll = new ScrollView(activity);
        final LinearLayout msgContainer = new LinearLayout(activity);
        msgContainer.setOrientation(LinearLayout.VERTICAL);
        msgScroll.addView(msgContainer);
        root.addView(msgScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(activity, 290)));

        final Runnable[] refreshMessages = new Runnable[1];
        refreshMessages[0] = () -> {
            msgContainer.removeAllViews();
            String nowTime = new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());
            for (int m = 0; m < conv.messages.size(); m++) {
                final QluvxSocialManager.DirectMessage msg = conv.messages.get(m);
                final String msgKey = conv.peerUserId + "_" + m;

                LinearLayout bRow = new LinearLayout(activity);
                bRow.setOrientation(LinearLayout.HORIZONTAL);
                bRow.setGravity(msg.fromMe ? Gravity.END : Gravity.START);

                LinearLayout bubble = new LinearLayout(activity);
                bubble.setOrientation(LinearLayout.VERTICAL);
                int bPad = UiStyleHelper.dp(activity, 10);
                bubble.setPadding(bPad, UiStyleHelper.dp(activity, 7), bPad, UiStyleHelper.dp(activity, 7));
                bubble.setBackground(UiStyleHelper.roundedBox(
                        activity,
                        msg.fromMe ? Color.rgb(37, 99, 235) : Color.rgb(30, 41, 59),
                        14f,
                        "ONETIME".equals(msg.messageType) ? UiStyleHelper.COLOR_NEON_PINK : 0,
                        "ONETIME".equals(msg.messageType) ? 1.2f : 0f
                ));

                TextView senderMeta = new TextView(activity);
                senderMeta.setText((msg.fromMe ? "You" : msg.senderName) + "  •  " + nowTime);
                senderMeta.setTextColor(Color.argb(190, 255, 255, 255));
                senderMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
                bubble.addView(senderMeta);

                final TextView contentTv = new TextView(activity);
                if ("ONETIME".equals(msg.messageType)) {
                    boolean opened = openedOneTimeMessages.contains(msgKey);
                    contentTv.setText(opened
                            ? "💨 One-Time Photo Expired (Viewed Once)"
                            : "🔥 One-Time Photo • Tap to View Once 🔒");
                    contentTv.setTextColor(opened ? Color.rgb(148, 163, 184) : Color.rgb(253, 224, 71));
                    contentTv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
                    bubble.setOnClickListener(v -> {
                        if (!openedOneTimeMessages.contains(msgKey)) {
                            openedOneTimeMessages.add(msgKey);
                            showOneTimePhotoViewerDialog(activity, peerAvatarRes, refreshMessages[0]);
                        } else {
                            Toast.makeText(activity, "💨 This one-time photo has already been viewed and expired!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    contentTv.setText(msg.content);
                    contentTv.setTextColor(Color.WHITE);
                }
                contentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                contentTv.setPadding(0, UiStyleHelper.dp(activity, 3), 0, 0);
                bubble.addView(contentTv);

                bRow.addView(bubble, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                msgContainer.addView(bRow, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 6)));
            }
            msgScroll.post(() -> msgScroll.fullScroll(View.FOCUS_DOWN));
        };
        refreshMessages[0].run();

        // 3. Bottom Rich Media Options ("niche cemra album location send option one time photo send video call")
        HorizontalScrollView bottomToolsScroll = new HorizontalScrollView(activity);
        bottomToolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout toolsRow = new LinearLayout(activity);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 8));

        // 📷 Camera
        TextView btnCamera = UiStyleHelper.createStatusChip(activity, "📷 Camera", Color.rgb(30, 41, 59), Color.WHITE, Color.rgb(56, 189, 248));
        btnCamera.setOnClickListener(v -> {
            conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "IMAGE", "📷 Sent Instant Camera Photo (HD_Cam_" + (System.currentTimeMillis() % 1000) + ".jpg)", "camera"));
            pendingTimestamps.put(conv.peerUserId, "Just now");
            refreshMessages[0].run();
            callbacks.onRefreshScreen();
        });

        // 🖼️ Album
        TextView btnAlbum = UiStyleHelper.createStatusChip(activity, "🖼️ Album", Color.rgb(30, 41, 59), Color.WHITE, Color.rgb(168, 85, 247));
        btnAlbum.setOnClickListener(v -> {
            conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "IMAGE", "🖼️ Sent Photo from Album (Gallery_IMG.jpg)", "album"));
            pendingTimestamps.put(conv.peerUserId, "Just now");
            refreshMessages[0].run();
            callbacks.onRefreshScreen();
        });

        // 📍 Location Send
        TextView btnLocSend = UiStyleHelper.createStatusChip(activity, "📍 Location", Color.rgb(30, 41, 59), Color.rgb(52, 211, 153), Color.rgb(52, 211, 153));
        btnLocSend.setOnClickListener(v -> {
            conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "LOCATION", "📍 Shared Live Location: " + social.getCurrentUser().country + " (19.0760° N, 72.8777° E)", "gps"));
            pendingTimestamps.put(conv.peerUserId, "Just now");
            refreshMessages[0].run();
            callbacks.onRefreshScreen();
        });

        // 🔥 One-Time Photo Send
        TextView btnOneTime = UiStyleHelper.createStatusChip(activity, "🔥 1-Time Photo", Color.argb(90, 236, 72, 153), Color.rgb(253, 224, 71), UiStyleHelper.COLOR_NEON_PINK);
        btnOneTime.setOnClickListener(v -> {
            conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "ONETIME", "🔥 One-Time Photo (Tap to View Once)", "1x"));
            pendingTimestamps.put(conv.peerUserId, "Just now");
            refreshMessages[0].run();
            callbacks.onRefreshScreen();
        });

        // 📹 Video Call
        TextView btnVidCall = UiStyleHelper.createStatusChip(activity, "📹 Video Call", Color.argb(90, 16, 185, 129), Color.WHITE, UiStyleHelper.COLOR_LIVE_GREEN);
        btnVidCall.setOnClickListener(v -> {
            dialog.dismiss();
            callbacks.onOpenLiveVideoRoom("Video Call • " + conv.peerName, "qluvx_call_" + conv.peerUserId, conv.peerName, conv.peerUserId, peerAvatarRes);
        });

        // 🎤 Voice & 🎁 Gift
        TextView btnVoice = UiStyleHelper.createStatusChip(activity, "🎤 Voice", Color.rgb(30, 41, 59), Color.WHITE, 0);
        btnVoice.setOnClickListener(v -> {
            conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "VOICE", "🎤 Voice Note (0:06 • HD Audio)", "0:06"));
            pendingTimestamps.put(conv.peerUserId, "Just now");
            refreshMessages[0].run();
            callbacks.onRefreshScreen();
        });

        TextView btnGift = UiStyleHelper.createStatusChip(activity, "🎁 Gift", Color.rgb(30, 41, 59), Color.rgb(244, 114, 182), 0);
        btnGift.setOnClickListener(v ->
                UiStyleHelper.showUnifiedGiftPanelDialog(activity, economy, social.getCurrentUser().displayName, conv.peerName, (gift, combo, tx) -> {
                    conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "GIFT", "🎁 Sent " + gift.iconLabel + " " + gift.name + " x" + combo, "gift"));
                    refreshMessages[0].run();
                    callbacks.onRefreshScreen();
                })
        );

        int m6 = UiStyleHelper.dp(activity, 6);
        toolsRow.addView(btnCamera, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, m6, 0));
        toolsRow.addView(btnAlbum, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, m6, 0));
        toolsRow.addView(btnLocSend, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, m6, 0));
        toolsRow.addView(btnOneTime, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, m6, 0));
        toolsRow.addView(btnVidCall, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, m6, 0));
        toolsRow.addView(btnVoice, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, m6, 0));
        toolsRow.addView(btnGift);
        bottomToolsScroll.addView(toolsRow);
        root.addView(bottomToolsScroll);

        // 4. Text Composer Row
        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        final EditText etMsg = new EditText(activity);
        etMsg.setHint("Message " + conv.peerName + " (ID: " + conv.peerUserId + ")...");
        etMsg.setSingleLine(true);
        etMsg.setTextColor(Color.WHITE);
        etMsg.setHintTextColor(Color.rgb(148, 163, 184));
        etMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        int ip = UiStyleHelper.dp(activity, 10);
        etMsg.setPadding(ip, ip, ip, ip);
        etMsg.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(24, 28, 40), 12f, Color.rgb(56, 189, 248), 1f));
        etMsg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnSend = UiStyleHelper.createControlBarButton(
                activity,
                "Send ➤",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                0,
                v -> {
                    String txt = etMsg.getText().toString().trim();
                    if (!txt.isEmpty()) {
                        etMsg.setText("");
                        conv.messages.add(new QluvxSocialManager.DirectMessage(social.getCurrentUser().displayName, true, "TEXT", txt, ""));
                        pendingTimestamps.put(conv.peerUserId, "Just now");
                        refreshMessages[0].run();
                        callbacks.onRefreshScreen();
                    }
                }
        );

        inputRow.addView(etMsg);
        inputRow.addView(btnSend, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 8), 0, 0, 0));
        root.addView(inputRow);

        dialog.setView(root);
        dialog.show();
    }

    private static void showOneTimePhotoViewerDialog(Activity activity, int photoRes, final Runnable onDismissCallback) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int pad = UiStyleHelper.dp(activity, 16);
        box.setPadding(pad, pad, pad, pad);

        ImageView iv = new ImageView(activity);
        iv.setImageResource(photoRes);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(iv, new LinearLayout.LayoutParams(UiStyleHelper.dp(activity, 240), UiStyleHelper.dp(activity, 280)));

        TextView notice = new TextView(activity);
        notice.setText("🔥 Viewing One-Time Photo — Will self-destruct on close!");
        notice.setTextColor(Color.rgb(253, 224, 71));
        notice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        notice.setPadding(0, UiStyleHelper.dp(activity, 10), 0, 0);
        box.addView(notice);

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔥 One-Time Photo")
                .setView(box)
                .setOnDismissListener(d -> {
                    if (onDismissCallback != null) onDismissCallback.run();
                })
                .setPositiveButton("Close & Destroy", null)
                .show();
    }

    // =========================================================================
    // TAB 4: LIVE BUTTON
    // - LIVE | VOICE switch
    // - Upar ek line ID show all
    // - Recommended | Global (touch select country) | Following
    // - All filter
    // - Ek sdehe nache tarf ek ek ID show (Vertical 1-by-1 single-column list)
    // =========================================================================
    public static void renderLiveHubTab(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final VoiceRoomModels.RoomRepository roomRepo,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        renderLuxuryDesignHeaderBar(activity, container, economy, callbacks);

        // 1. Top Mode Switch: [ LIVE ]  [ VOICE ] + [ 📹+ Go Live / 🎙️+ Create Voice ]
        LinearLayout topModeRow = new LinearLayout(activity);
        topModeRow.setOrientation(LinearLayout.HORIZONTAL);
        topModeRow.setGravity(Gravity.CENTER_VERTICAL);
        topModeRow.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 8));

        TextView tvLive = new TextView(activity);
        tvLive.setText("LIVE");
        tvLive.setTextColor(!isLiveVoiceMode ? Color.WHITE : Color.rgb(148, 163, 184));
        tvLive.setTextSize(TypedValue.COMPLEX_UNIT_SP, !isLiveVoiceMode ? 21f : 16f);
        tvLive.setTypeface(Typeface.create("sans-serif-black", !isLiveVoiceMode ? Typeface.BOLD : Typeface.NORMAL));
        tvLive.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 16), UiStyleHelper.dp(activity, 4));
        tvLive.setOnClickListener(v -> {
            isLiveVoiceMode = false;
            callbacks.onRefreshScreen();
        });

        TextView tvVoice = new TextView(activity);
        tvVoice.setText("VOICE");
        tvVoice.setTextColor(isLiveVoiceMode ? Color.WHITE : Color.rgb(148, 163, 184));
        tvVoice.setTextSize(TypedValue.COMPLEX_UNIT_SP, isLiveVoiceMode ? 21f : 16f);
        tvVoice.setTypeface(Typeface.create("sans-serif-black", isLiveVoiceMode ? Typeface.BOLD : Typeface.NORMAL));
        tvVoice.setPadding(UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 14), UiStyleHelper.dp(activity, 4));
        tvVoice.setOnClickListener(v -> {
            isLiveVoiceMode = true;
            callbacks.onRefreshScreen();
        });

        topModeRow.addView(tvLive);
        topModeRow.addView(tvVoice);

        View sp = new View(activity);
        topModeRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView btnGoLiveOrVoice = UiStyleHelper.createStatusChip(
                activity,
                isLiveVoiceMode ? "🎙️+ Create Voice" : "📹+ Go Live",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                Color.WHITE
        );
        btnGoLiveOrVoice.setOnClickListener(v -> {
            if (isLiveVoiceMode) {
                callbacks.onShowCreateVoiceRoomDialog();
            } else {
                callbacks.onOpenLiveVideoRoom("Your LIVE Title", "qluvx_live_video_pk", "Pre-LIVE Setup", "10001", R.drawable.img_avatar_0);
            }
        });
        topModeRow.addView(btnGoLiveOrVoice);
        container.addView(topModeRow);

        // 2. TOP SINGLE LINE ID SHOW ALL ("Upar ek line id show all")
        HorizontalScrollView topIdsScroll = new HorizontalScrollView(activity);
        topIdsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout topIdsRow = new LinearLayout(activity);
        topIdsRow.setOrientation(LinearLayout.HORIZONTAL);
        topIdsRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));

        List<QluvxMasterScreensController.LiveCreatorItem> allCreators = QluvxMasterScreensController.getLiveCreators();
        for (int i = 0; i < allCreators.size(); i++) {
            final QluvxMasterScreensController.LiveCreatorItem c = allCreators.get(i);
            LinearLayout pill = new LinearLayout(activity);
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER_HORIZONTAL);
            pill.setPadding(0, 0, UiStyleHelper.dp(activity, 10), 0);

            View av = UiStyleHelper.createCircularAvatar(activity, c.avatarResId, 52, UiStyleHelper.COLOR_NEON_PINK, false);
            pill.addView(av);

            TextView badge = new TextView(activity);
            badge.setText("ID:" + c.id.substring(0, Math.min(5, c.id.length())));
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
            badge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            badge.setPadding(UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 1), UiStyleHelper.dp(activity, 5), UiStyleHelper.dp(activity, 1));
            badge.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_NEON_PINK, 6f, 0, 0));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(0, -UiStyleHelper.dp(activity, 7), 0, 0);
            pill.addView(badge, bLp);

            TextView nm = new TextView(activity);
            nm.setText(c.name);
            nm.setSingleLine(true);
            nm.setTextColor(Color.WHITE);
            nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            pill.addView(nm, new LinearLayout.LayoutParams(UiStyleHelper.dp(activity, 64), ViewGroup.LayoutParams.WRAP_CONTENT));

            pill.setOnClickListener(v -> {
                if (isLiveVoiceMode) {
                    callbacks.onOpenVoiceRoom(c.name + " Voice", c.channelName);
                } else {
                    callbacks.onOpenLiveVideoRoom(c.name + " Live", c.channelName, c.name, c.id, c.avatarResId);
                }
            });
            topIdsRow.addView(pill);
        }
        topIdsScroll.addView(topIdsRow);
        container.addView(topIdsScroll);

        // 3. Primary Tabs: [ Recommended ]  [ Global ▾ (Select Country) ]  [ Following ]
        LinearLayout primaryRow = new LinearLayout(activity);
        primaryRow.setOrientation(LinearLayout.HORIZONTAL);
        primaryRow.setGravity(Gravity.CENTER_VERTICAL);
        primaryRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 6));

        String[] pTabs = new String[]{"Recommended", "Global", "Following"};
        for (final String pt : pTabs) {
            boolean active = pt.equalsIgnoreCase(livePrimaryTab);
            TextView tv = new TextView(activity);
            if ("Global".equals(pt)) {
                tv.setText("🌐 " + ("Global".equalsIgnoreCase(liveSelectedCountry) ? "Global ▾" : (liveSelectedCountry + " ▾")));
            } else if ("Following".equals(pt)) {
                tv.setText("💖 Following Friends");
            } else {
                tv.setText("🔥 Recommended");
            }
            tv.setTextColor(active ? Color.WHITE : Color.rgb(148, 163, 184));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, active ? 15.5f : 13.5f);
            tv.setTypeface(Typeface.create("sans-serif-black", active ? Typeface.BOLD : Typeface.NORMAL));
            tv.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 14), UiStyleHelper.dp(activity, 4));
            tv.setOnClickListener(v -> {
                if ("Global".equals(pt)) {
                    livePrimaryTab = "Global";
                    showLiveCountryPickerDialog(activity, callbacks);
                } else {
                    livePrimaryTab = pt;
                    callbacks.onRefreshScreen();
                }
            });
            primaryRow.addView(tv);
        }
        container.addView(primaryRow);

        // 4. Filter Pills starting with [ All ]
        HorizontalScrollView subFilterScroll = new HorizontalScrollView(activity);
        subFilterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout subFilterRow = new LinearLayout(activity);
        subFilterRow.setOrientation(LinearLayout.HORIZONTAL);
        subFilterRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));

        String[] subPills = new String[]{"All", "PK", "Hot", "New", "Nearby"};
        for (final String spName : subPills) {
            boolean active = spName.equalsIgnoreCase(liveSubFilter);
            TextView pill = UiStyleHelper.createStatusChip(
                    activity,
                    spName,
                    active ? Color.rgb(236, 72, 153) : Color.rgb(28, 30, 42),
                    Color.WHITE,
                    active ? Color.WHITE : Color.rgb(65, 72, 94)
            );
            pill.setOnClickListener(v -> {
                liveSubFilter = spName;
                callbacks.onRefreshScreen();
            });
            subFilterRow.addView(pill, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 8), 0));
        }
        subFilterScroll.addView(subFilterRow);
        container.addView(subFilterScroll);

        // 5. VERTICAL ONE-BY-ONE ID LIST ("Ek sdehe nache tarf ek ek id show" for Recommended, Global Country, and Following Friends)
        List<QluvxMasterScreensController.LiveCreatorItem> verticalList = new ArrayList<>();
        for (QluvxMasterScreensController.LiveCreatorItem item : allCreators) {
            if ("Following".equalsIgnoreCase(livePrimaryTab) && !item.isFollowing) {
                continue;
            }
            if ("Global".equalsIgnoreCase(livePrimaryTab) && !"Global".equalsIgnoreCase(liveSelectedCountry)) {
                if (!item.country.toLowerCase(Locale.US).contains(liveSelectedCountry.toLowerCase(Locale.US))) {
                    continue;
                }
            }
            if (!"All".equalsIgnoreCase(liveSubFilter) && !liveSubFilter.equalsIgnoreCase(item.filterCategory)) {
                continue;
            }
            verticalList.add(item);
        }
        if (verticalList.isEmpty()) {
            verticalList.addAll(allCreators);
        }

        for (int i = 0; i < verticalList.size(); i++) {
            final QluvxMasterScreensController.LiveCreatorItem creator = verticalList.get(i);
            View singleRowCard = buildVerticalSingleCreatorRowCard(activity, creator, i, callbacks);
            container.addView(singleRowCard, UiStyleHelper.params(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, 0, UiStyleHelper.dp(activity, 10)
            ));
        }
    }

    private static View buildVerticalSingleCreatorRowCard(
            final Activity activity,
            final QluvxMasterScreensController.LiveCreatorItem creator,
            final int index,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiStyleHelper.dp(activity, 12);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(UiStyleHelper.gradientBox(
                activity,
                getLuxuryGradStart(),
                getLuxuryGradEnd(),
                16f,
                getLuxuryBorderAccent(),
                1.2f
        ));

        // Left 82dp Rounded Square Portrait
        FrameLayout thumbWrap = new FrameLayout(activity);
        int sz = UiStyleHelper.dp(activity, 82);
        thumbWrap.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        thumbWrap.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(activity, 12));
            }
        });
        thumbWrap.setClipToOutline(true);

        ImageView iv = new ImageView(activity);
        iv.setImageResource(creator.avatarResId);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbWrap.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView modeBadge = new TextView(activity);
        modeBadge.setText(isLiveVoiceMode ? ("ılı " + creator.viewersLabel) : (creator.topBadge != null && !creator.topBadge.isEmpty() ? creator.topBadge : "LIVE"));
        modeBadge.setTextColor(Color.WHITE);
        modeBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        modeBadge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        modeBadge.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2));
        modeBadge.setBackground(UiStyleHelper.roundedBox(activity, isLiveVoiceMode ? Color.rgb(16, 185, 129) : UiStyleHelper.COLOR_NEON_PINK, 6f, 0, 0));
        thumbWrap.addView(modeBadge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START));

        card.addView(thumbWrap);

        // Middle Column: Name + ID + Country Location + Level + Agora Participants
        LinearLayout midCol = new LinearLayout(activity);
        midCol.setOrientation(LinearLayout.VERTICAL);
        midCol.setPadding(UiStyleHelper.dp(activity, 12), 0, UiStyleHelper.dp(activity, 8), 0);
        midCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(activity);
        tvName.setText(creator.name);
        tvName.setSingleLine(true);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        midCol.addView(tvName);

        TextView tvIdAndLevel = new TextView(activity);
        tvIdAndLevel.setText("ID: " + creator.id + "   •   💎 " + creator.levelLabel);
        tvIdAndLevel.setSingleLine(true);
        tvIdAndLevel.setTextColor(Color.rgb(56, 189, 248));
        tvIdAndLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvIdAndLevel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvIdAndLevel.setPadding(0, UiStyleHelper.dp(activity, 2), 0, UiStyleHelper.dp(activity, 2));
        midCol.addView(tvIdAndLevel);

        TextView tvLoc = new TextView(activity);
        tvLoc.setText("📍 " + creator.country + "   •   🏆 " + creator.viewersLabel + " watching");
        tvLoc.setSingleLine(true);
        tvLoc.setTextColor(Color.rgb(203, 213, 225));
        tvLoc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        midCol.addView(tvLoc);

        card.addView(midCol);

        // Right Column: Follow Button + Join/Watch Button
        LinearLayout rightCol = new LinearLayout(activity);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        TextView btnFollow = UiStyleHelper.createStatusChip(
                activity,
                creator.isFollowing ? "✓ Friend" : "＋ Follow",
                creator.isFollowing ? Color.rgb(16, 185, 129) : Color.rgb(45, 52, 70),
                Color.WHITE,
                0
        );
        btnFollow.setOnClickListener(v -> {
            creator.isFollowing = !creator.isFollowing;
            Toast.makeText(activity, creator.isFollowing ? ("💖 Added " + creator.name + " to Following!") : ("Unfollowed " + creator.name), Toast.LENGTH_SHORT).show();
            callbacks.onRefreshScreen();
        });
        rightCol.addView(btnFollow);

        TextView btnWatch = UiStyleHelper.createStatusChip(
                activity,
                isLiveVoiceMode ? "🎙️ Join" : "📹 Watch",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                0
        );
        View.OnClickListener openRoomClick = v -> {
            if (isLiveVoiceMode) {
                callbacks.onOpenVoiceRoom(creator.name + " Voice Room", creator.channelName);
            } else {
                callbacks.onOpenLiveVideoRoom(creator.name + " Live", creator.channelName, creator.name, creator.id, creator.avatarResId);
            }
        };
        btnWatch.setOnClickListener(openRoomClick);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wLp.setMargins(0, UiStyleHelper.dp(activity, 6), 0, 0);
        rightCol.addView(btnWatch, wLp);

        card.addView(rightCol);
        card.setOnClickListener(openRoomClick);

        return card;
    }

    private static void showLiveCountryPickerDialog(
            final Activity activity,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        final AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(20, 22, 32), 20f, Color.rgb(56, 189, 248), 1.2f));

        TextView title = new TextView(activity);
        title.setText("🌐 Select Country (Vertical ID List Below)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        title.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        root.addView(title);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout gridWrap = new LinearLayout(activity);
        gridWrap.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < GLOBAL_COUNTRIES.length; i += 2) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(2f);
            for (int c = 0; c < 2; c++) {
                int idx = i + c;
                if (idx < GLOBAL_COUNTRIES.length) {
                    final String flag = GLOBAL_COUNTRIES[idx][0];
                    final String cName = GLOBAL_COUNTRIES[idx][1];
                    boolean active = cName.equalsIgnoreCase(liveSelectedCountry);

                    TextView cell = new TextView(activity);
                    cell.setText(flag + "  " + cName);
                    cell.setTextColor(Color.WHITE);
                    cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                    cell.setTypeface(Typeface.create("sans-serif-medium", active ? Typeface.BOLD : Typeface.NORMAL));
                    int p10 = UiStyleHelper.dp(activity, 10);
                    cell.setPadding(p10, p10, p10, p10);
                    cell.setBackground(UiStyleHelper.roundedBox(
                            activity,
                            active ? Color.rgb(37, 99, 235) : Color.rgb(30, 34, 48),
                            10f,
                            active ? Color.WHITE : 0,
                            1f
                    ));
                    cell.setOnClickListener(v -> {
                        liveSelectedCountry = cName;
                        dialog.dismiss();
                        callbacks.onRefreshScreen();
                    });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(c == 0 ? 0 : UiStyleHelper.dp(activity, 4), 0, c == 1 ? 0 : UiStyleHelper.dp(activity, 4), UiStyleHelper.dp(activity, 8));
                    row.addView(cell, lp);
                }
            }
            gridWrap.addView(row);
        }

        scroll.addView(gridWrap);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(activity, 320)));
        dialog.setView(root);
        dialog.show();
    }

    // =========================================================================
    // TAB 5: ME BUTTON (All Profile Details + DP Change + Complete Profile Features)
    // =========================================================================
    public static void renderMeProfileTab(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        renderLuxuryDesignHeaderBar(activity, container, economy, callbacks);
        final QluvxSocialManager.UserProfile me = social.getCurrentUser();

        // 1. TOP PROFILE DP CHANGE & IDENTITY HERO CARD ("All prfile details dp chenge")
        LinearLayout dpHeroCard = new LinearLayout(activity);
        dpHeroCard.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 14);
        dpHeroCard.setPadding(pad, pad, pad, pad);
        dpHeroCard.setBackground(UiStyleHelper.gradientBox(
                activity,
                getLuxuryGradStart(),
                getLuxuryGradEnd(),
                18f,
                getLuxuryBorderAccent(),
                1.4f
        ));

        LinearLayout topIdentityRow = new LinearLayout(activity);
        topIdentityRow.setOrientation(LinearLayout.HORIZONTAL);
        topIdentityRow.setGravity(Gravity.CENTER_VERTICAL);

        // Circular DP with Camera Change Overlay
        FrameLayout dpWrap = new FrameLayout(activity);
        int dpSz = UiStyleHelper.dp(activity, 84);
        dpWrap.setLayoutParams(new LinearLayout.LayoutParams(dpSz, dpSz));

        ImageView ivDp = new ImageView(activity);
        if (customProfileDpUri != null) {
            ivDp.setImageURI(customProfileDpUri);
        } else {
            ivDp.setImageResource(me.avatarResId != 0 ? me.avatarResId : R.drawable.img_avatar_0);
        }
        ivDp.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivDp.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        ivDp.setClipToOutline(true);
        ivDp.setBackground(UiStyleHelper.circle(activity, Color.rgb(30, 41, 59), UiStyleHelper.COLOR_NEON_PINK, 2.5f));
        dpWrap.addView(ivDp, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView camBadge = new TextView(activity);
        camBadge.setText("📷 DP");
        camBadge.setTextColor(Color.WHITE);
        camBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        camBadge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        camBadge.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2));
        camBadge.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_NEON_PINK, 8f, Color.WHITE, 1f));
        dpWrap.addView(camBadge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

        dpWrap.setOnClickListener(v -> showChangeDpStudioDialog(activity, me, callbacks));
        topIdentityRow.addView(dpWrap);

        // Right Identity Column: Name, ID + Copy, Location, VIP Frame, Edit Profile
        LinearLayout idCol = new LinearLayout(activity);
        idCol.setOrientation(LinearLayout.VERTICAL);
        idCol.setPadding(UiStyleHelper.dp(activity, 14), 0, 0, 0);
        idCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(activity);
        tvName.setText(me.displayName + " ☑️  💎 Lv.32");
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.5f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        idCol.addView(tvName);

        LinearLayout idCopyRow = new LinearLayout(activity);
        idCopyRow.setOrientation(LinearLayout.HORIZONTAL);
        idCopyRow.setGravity(Gravity.CENTER_VERTICAL);
        idCopyRow.setPadding(0, UiStyleHelper.dp(activity, 3), 0, UiStyleHelper.dp(activity, 3));

        TextView tvUserId = new TextView(activity);
        tvUserId.setText("ID: " + me.userId + "  ");
        tvUserId.setTextColor(Color.rgb(56, 189, 248));
        tvUserId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tvUserId.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView btnCopyId = new TextView(activity);
        btnCopyId.setText("📋 Copy");
        btnCopyId.setTextColor(Color.WHITE);
        btnCopyId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        btnCopyId.setPadding(UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 6), UiStyleHelper.dp(activity, 2));
        btnCopyId.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(51, 65, 85), 6f, 0, 0));
        btnCopyId.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("QLUVX ID", me.userId));
            }
            Toast.makeText(activity, "📋 Copied QLUVX ID: " + me.userId, Toast.LENGTH_SHORT).show();
        });

        idCopyRow.addView(tvUserId);
        idCopyRow.addView(btnCopyId);
        idCol.addView(idCopyRow);

        TextView tvLocation = new TextView(activity);
        tvLocation.setText("📍 " + me.country + "  •  " + equippedFamilyName);
        tvLocation.setTextColor(Color.rgb(203, 213, 225));
        tvLocation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        idCol.addView(tvLocation);

        LinearLayout dpBtnsRow = new LinearLayout(activity);
        dpBtnsRow.setOrientation(LinearLayout.HORIZONTAL);
        dpBtnsRow.setPadding(0, UiStyleHelper.dp(activity, 6), 0, 0);

        TextView btnChangeDp = UiStyleHelper.createStatusChip(activity, "📷 Change DP", UiStyleHelper.COLOR_NEON_PINK, Color.WHITE, 0);
        btnChangeDp.setOnClickListener(v -> showChangeDpStudioDialog(activity, me, callbacks));

        TextView btnEditInfo = UiStyleHelper.createStatusChip(activity, "✏️ Edit Profile", Color.rgb(37, 99, 235), Color.WHITE, 0);
        btnEditInfo.setOnClickListener(v -> callbacks.onShowEditProfileDialog());

        dpBtnsRow.addView(btnChangeDp);
        dpBtnsRow.addView(btnEditInfo, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(activity, 6), 0, 0, 0));
        idCol.addView(dpBtnsRow);

        topIdentityRow.addView(idCol);
        dpHeroCard.addView(topIdentityRow);

        // Quick 6-Avatar Instant DP Selector Strip
        HorizontalScrollView quickDpScroll = new HorizontalScrollView(activity);
        quickDpScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quickDpRow = new LinearLayout(activity);
        quickDpRow.setOrientation(LinearLayout.HORIZONTAL);
        quickDpRow.setGravity(Gravity.CENTER_VERTICAL);
        quickDpRow.setPadding(0, UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 6));

        TextView quickDpLabel = new TextView(activity);
        quickDpLabel.setText("Instant DP: ");
        quickDpLabel.setTextColor(Color.rgb(203, 213, 225));
        quickDpLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        quickDpRow.addView(quickDpLabel);

        for (int a = 0; a < 6; a++) {
            final int avRes = VoiceRoomModels.getAvatarResId(a);
            boolean activeDp = (customProfileDpUri == null && me.avatarResId == avRes);
            View av = UiStyleHelper.createCircularAvatar(activity, avRes, 36, activeDp ? Color.rgb(253, 224, 71) : Color.rgb(100, 116, 139), false);
            av.setOnClickListener(v -> {
                customProfileDpUri = null;
                me.avatarResId = avRes;
                Toast.makeText(activity, "✅ Profile DP Changed!", Toast.LENGTH_SHORT).show();
                callbacks.onRefreshScreen();
            });
            quickDpRow.addView(av, UiStyleHelper.params(UiStyleHelper.dp(activity, 36), UiStyleHelper.dp(activity, 36), UiStyleHelper.dp(activity, 6), 0, 0, 0));
        }
        quickDpScroll.addView(quickDpRow);
        dpHeroCard.addView(quickDpScroll);

        // 4-Column Social Stats Bar: Following | Followers | Friends | Visitors
        LinearLayout statsRow = new LinearLayout(activity);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(4f);
        statsRow.setPadding(0, UiStyleHelper.dp(activity, 8), 0, 0);

        String[][] stats = new String[][]{
                {String.valueOf(me.followingCount + 142), "Following"},
                {String.valueOf(me.followersCount + 1280), "Followers"},
                {"86", "Friends"},
                {"342", "Visitors"}
        };
        for (final String[] st : stats) {
            LinearLayout sCol = new LinearLayout(activity);
            sCol.setOrientation(LinearLayout.VERTICAL);
            sCol.setGravity(Gravity.CENTER);
            TextView vTv = new TextView(activity);
            vTv.setText(st[0]);
            vTv.setTextColor(Color.WHITE);
            vTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            vTv.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            TextView lTv = new TextView(activity);
            lTv.setText(st[1]);
            lTv.setTextColor(Color.rgb(148, 163, 184));
            lTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            sCol.addView(vTv);
            sCol.addView(lTv);
            sCol.setOnClickListener(v -> Toast.makeText(activity, st[1] + ": " + st[0], Toast.LENGTH_SHORT).show());
            statsRow.addView(sCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        dpHeroCard.addView(statsRow);

        container.addView(dpHeroCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10)));

        // 2. DUAL WALLET BAR: [ 🪙 Coins Balance + Recharge ]  [ 💎 Diamonds + Exchange ]
        LinearLayout walletRow = new LinearLayout(activity);
        walletRow.setOrientation(LinearLayout.HORIZONTAL);
        walletRow.setWeightSum(2f);
        walletRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));

        LinearLayout coinsCard = new LinearLayout(activity);
        coinsCard.setOrientation(LinearLayout.VERTICAL);
        int wPad = UiStyleHelper.dp(activity, 12);
        coinsCard.setPadding(wPad, wPad, wPad, wPad);
        coinsCard.setBackground(UiStyleHelper.gradientBox(activity, Color.rgb(66, 38, 12), Color.rgb(120, 68, 14), 14f, Color.rgb(251, 191, 36), 1f));
        TextView tvCoins = new TextView(activity);
        tvCoins.setText("🪙 " + economy.getCoinBalance() + " Coins");
        tvCoins.setTextColor(Color.rgb(253, 224, 71));
        tvCoins.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        tvCoins.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        TextView tvCoinsSub = new TextView(activity);
        tvCoinsSub.setText("⚡ Tap to Recharge Coins ›");
        tvCoinsSub.setTextColor(Color.WHITE);
        tvCoinsSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        coinsCard.addView(tvCoins);
        coinsCard.addView(tvCoinsSub);
        coinsCard.setOnClickListener(v -> callbacks.onSelectMenu(6));

        LinearLayout diamondsCard = new LinearLayout(activity);
        diamondsCard.setOrientation(LinearLayout.VERTICAL);
        diamondsCard.setPadding(wPad, wPad, wPad, wPad);
        diamondsCard.setBackground(UiStyleHelper.gradientBox(activity, Color.rgb(56, 20, 72), Color.rgb(108, 28, 118), 14f, Color.rgb(244, 114, 182), 1f));
        TextView tvDiamonds = new TextView(activity);
        tvDiamonds.setText("💎 " + diamondBalance + " Diamonds");
        tvDiamonds.setTextColor(Color.rgb(244, 114, 182));
        tvDiamonds.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        tvDiamonds.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        TextView tvDiamondsSub = new TextView(activity);
        tvDiamondsSub.setText("💸 Exchange to Coins ›");
        tvDiamondsSub.setTextColor(Color.WHITE);
        tvDiamondsSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        diamondsCard.addView(tvDiamonds);
        diamondsCard.addView(tvDiamondsSub);
        diamondsCard.setOnClickListener(v -> {
            if (diamondBalance >= 1000) {
                diamondBalance -= 1000;
                economy.rechargeVerifiedPack(500, "Diamond Exchange (+500 Coins)", "ex_" + System.currentTimeMillis());
                Toast.makeText(activity, "💎 Exchanged 1,000 Diamonds for +500 Coins!", Toast.LENGTH_SHORT).show();
                callbacks.onRefreshScreen();
            }
        });

        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cLp.setMargins(0, 0, UiStyleHelper.dp(activity, 5), 0);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dLp.setMargins(UiStyleHelper.dp(activity, 5), 0, 0, 0);
        walletRow.addView(coinsCard, cLp);
        walletRow.addView(diamondsCard, dLp);
        container.addView(walletRow);

        // 3. 8-ICON PROFILE SERVICES GRID ("Sab prfile jo hota hai sab haona chahya")
        LinearLayout servicesCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_BORDER_SUBTLE);
        TextView srvHeader = new TextView(activity);
        srvHeader.setText("✨ Profile Services & Royal Center");
        srvHeader.setTextColor(Color.WHITE);
        srvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        srvHeader.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        srvHeader.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 8));
        servicesCard.addView(srvHeader);

        String[][] gridServices = new String[][]{
                {"👑", "VIP Center"},
                {"🛡️", "My Level"},
                {"🏰", "My Family"},
                {"🎒", "Bag & Store"},
                {"🏅", "My Badges"},
                {"🛡️", "Guardian"},
                {"🎁", "Invite Earn"},
                {"🎨", "Theme Set"}
        };
        for (int r = 0; r < 2; r++) {
            LinearLayout gRow = new LinearLayout(activity);
            gRow.setOrientation(LinearLayout.HORIZONTAL);
            gRow.setWeightSum(4f);
            if (r > 0) gRow.setPadding(0, UiStyleHelper.dp(activity, 8), 0, 0);
            for (int c = 0; c < 4; c++) {
                final int idx = r * 4 + c;
                final String[] item = gridServices[idx];
                LinearLayout cell = new LinearLayout(activity);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                int p8 = UiStyleHelper.dp(activity, 8);
                cell.setPadding(p8, p8, p8, p8);
                cell.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(26, 30, 44), 12f, 0, 0));

                TextView em = new TextView(activity);
                em.setText(item[0]);
                em.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                em.setGravity(Gravity.CENTER);

                TextView lb = new TextView(activity);
                lb.setText(item[1]);
                lb.setSingleLine(true);
                lb.setTextColor(Color.WHITE);
                lb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
                lb.setPadding(0, UiStyleHelper.dp(activity, 3), 0, 0);

                cell.addView(em);
                cell.addView(lb);
                cell.setOnClickListener(v -> {
                    if (idx == 0) {
                        callbacks.onSelectMenu(8); // VIP
                    } else if (idx == 3) {
                        showBagAndStoreDialog(activity, me, callbacks);
                    } else if (idx == 6) {
                        economy.rechargeVerifiedPack(200, "Invite Friend Reward", "inv_" + System.currentTimeMillis());
                        Toast.makeText(activity, "🎁 Invite Link Copied! +200 Bonus Coins credited!", Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    } else if (idx == 7) {
                        UiStyleHelper.showThemeAndWallpaperStudioDialog(activity, callbacks::onRefreshScreen);
                    } else {
                        Toast.makeText(activity, item[0] + " " + item[1] + " — Active on your QLUVX Profile", Toast.LENGTH_SHORT).show();
                    }
                });

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(UiStyleHelper.dp(activity, 3), 0, UiStyleHelper.dp(activity, 3), 0);
                gRow.addView(cell, lp);
            }
            servicesCard.addView(gRow);
        }
        container.addView(servicesCard, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(activity, 10)));

        // 4. Clean Simple Profile Menu List (No duplicate profile headers!)
        LinearLayout menuListCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_BORDER_SUBTLE);
        String[][] menuItems = new String[][]{
                {"🖼️", "My Photo Album", "View & upload profile photos"},
                {"🎁", "My Gifts Wall", "See all gifts received from fans"},
                {"✨", "My Moments & Posts", "Manage your Discover posts"},
                {"💰", "Coin Wallet & Recharge", "Balance: " + economy.getCoinBalance() + " Coins"},
                {"👑", "VIP & SVIP Privileges", "Unlock royal frames & mounts"},
                {"🛡️", "Admin Control Panel", "Manage users, rooms & reports"},
                {"⚙️", "Settings & Privacy", "Language, block list & account"}
        };
        for (int i = 0; i < menuItems.length; i++) {
            final int idx = i;
            final String[] mi = menuItems[i];
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p10 = UiStyleHelper.dp(activity, 11);
            row.setPadding(p10, p10, p10, p10);
            if (i > 0) {
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.setMargins(0, UiStyleHelper.dp(activity, 6), 0, 0);
                row.setLayoutParams(rLp);
            }
            row.setBackground(UiStyleHelper.roundedBox(activity, Color.rgb(24, 28, 40), 12f, Color.rgb(45, 52, 70), 1f));

            TextView iconTv = new TextView(activity);
            iconTv.setText(mi[0]);
            iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            row.addView(iconTv);

            LinearLayout textCol = new LinearLayout(activity);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(UiStyleHelper.dp(activity, 12), 0, UiStyleHelper.dp(activity, 8), 0);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t1 = new TextView(activity);
            t1.setText(mi[1]);
            t1.setTextColor(Color.WHITE);
            t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            t1.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

            TextView t2 = new TextView(activity);
            t2.setText(mi[2]);
            t2.setTextColor(Color.rgb(148, 163, 184));
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);

            textCol.addView(t1);
            textCol.addView(t2);
            row.addView(textCol);

            TextView arrow = new TextView(activity);
            arrow.setText("›");
            arrow.setTextColor(Color.rgb(148, 163, 184));
            arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            row.addView(arrow);

            row.setOnClickListener(v -> {
                if (idx == 0) {
                    activeDiscoverTab = "Gallery";
                    callbacks.onSelectMenu(1);
                } else if (idx == 1) {
                    callbacks.onSelectMenu(7);
                } else if (idx == 2) {
                    activeDiscoverTab = "Moments";
                    callbacks.onSelectMenu(1);
                } else if (idx == 3) {
                    callbacks.onSelectMenu(6);
                } else if (idx == 4) {
                    callbacks.onSelectMenu(8);
                } else if (idx == 5) {
                    callbacks.onSelectMenu(10);
                } else {
                    callbacks.onSelectMenu(11);
                }
            });
            menuListCard.addView(row);
        }
        container.addView(menuListCard);
    }

    private static void showChangeDpStudioDialog(
            final Activity activity,
            final QluvxSocialManager.UserProfile me,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        String[] options = new String[]{
                "🖼️ Choose DP Photo from Phone Gallery / Album",
                "📸 Studio Portrait 1 (Royal Host)",
                "📸 Studio Portrait 2 (Neon Star)",
                "📸 Studio Portrait 3 (Glamour Queen)",
                "📸 Studio Portrait 4 (DJ Vibe)",
                "📸 Studio Portrait 5 (Cyber Punk)",
                "📸 Studio Portrait 6 (VIP Gold)",
                "👑 Equip Golden Dragon VIP Frame",
                "💖 Equip Neon Pink Crown Frame"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("📷 Change Profile DP & Avatar Frame")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        try {
                            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            pick.addCategory(Intent.CATEGORY_OPENABLE);
                            pick.setType("image/*");
                            activity.startActivityForResult(pick, REQ_PICK_PROFILE_DP);
                        } catch (Exception e) {
                            me.avatarResId = R.drawable.img_avatar_1;
                            callbacks.onRefreshScreen();
                        }
                    } else if (which >= 1 && which <= 6) {
                        customProfileDpUri = null;
                        me.avatarResId = VoiceRoomModels.getAvatarResId(which - 1);
                        Toast.makeText(activity, "✅ Profile DP updated to Studio Portrait #" + which, Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    } else if (which == 7) {
                        me.profileFrame = "Golden Dragon VIP Frame";
                        Toast.makeText(activity, "👑 Equipped Golden Dragon VIP Frame!", Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    } else {
                        me.profileFrame = "Neon Pink Crown Frame";
                        Toast.makeText(activity, "💖 Equipped Neon Pink Crown Frame!", Toast.LENGTH_SHORT).show();
                        callbacks.onRefreshScreen();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private static void showBagAndStoreDialog(
            final Activity activity,
            final QluvxSocialManager.UserProfile me,
            final QluvxMasterScreensController.HostNavigationCallbacks callbacks
    ) {
        String[] items = new String[]{
                "🏎️ Entrance Mount: Neon Cyber Roadster (Equipped)",
                "🚁 Entrance Mount: Royal Golden Helicopter",
                "👑 Avatar Frame: SVIP Emperor Crown",
                "💬 Chat Bubble: Neon Pink Starlight",
                "🎙️ Mic Wave Ring: Emerald Pulse Wave"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎒 My Bag & VIP Store (" + equippedMountName + ")")
                .setItems(items, (d, which) -> {
                    equippedMountName = items[which];
                    Toast.makeText(activity, "✅ Equipped: " + items[which], Toast.LENGTH_SHORT).show();
                    callbacks.onRefreshScreen();
                })
                .setPositiveButton("Done", null)
                .show();
    }

    public static void onProfileDpPicked(
            final Activity activity,
            final QluvxSocialManager social,
            final Uri uri,
            final Runnable onRefresh
    ) {
        if (uri != null) {
            customProfileDpUri = uri;
            Toast.makeText(activity, "✅ Profile DP Updated from Gallery!", Toast.LENGTH_SHORT).show();
            if (onRefresh != null) onRefresh.run();
        }
    }

    public static void onGalleryPhotoPicked(
            final Activity activity,
            final QluvxSocialManager social,
            final Uri uri,
            final Runnable onRefresh
    ) {
        if (uri != null) {
            discoverPosts.add(0, new DiscoverPostItem(
                    social.getCurrentUser().displayName,
                    social.getCurrentUser().userId,
                    social.getCurrentUser().country,
                    "🖼️ Shared new photo from Gallery (" + uri.getLastPathSegment() + ") ✨ #QLUVX",
                    "Just now",
                    R.drawable.img_avatar_0,
                    12,
                    1
            ));
            Toast.makeText(activity, "🖼️ Photo posted to Discover Gallery!", Toast.LENGTH_SHORT).show();
            if (onRefresh != null) onRefresh.run();
        }
    }
}
