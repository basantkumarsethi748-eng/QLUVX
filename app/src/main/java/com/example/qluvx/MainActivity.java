package com.example.qluvx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.List;

/**
 * QLUVX Master Hub Activity (A-to-Z All 12 Menus + Dynamic Wallpaper & Theme Set Studio +
 * Overhauled Home Page, Glossy Gradient Buttons, Live Message Box, 12-Seat Voice & Video PK):
 * 1. Home — Hero Banner, 6-Button Hub, Live Wallpaper & Theme Bar, Quick Message Box, Radar, 3-Col Nearby & Party Rooms.
 * 2. Discover — Search users and rooms, nearby users, countries, categories and filters.
 * 3. Live — Agora voice rooms with up to 12 microphone seats, host crown, listeners, invitations, mute/unmute, seat permissions and room chat.
 * 4. Live Video — Video stage, comments, viewer count, reactions, wallpapers and PK battles.
 * 5. Chat (Message Box) — Visual inbox cards, customizable chat wallpaper, left/right avatar bubbles, quick replies, auto-reply, voice notes & gifts.
 * 6. Profile — User ID, profile photo, bio, followers, theme studio, settings and account management.
 * 7. Wallet — Coins, recharge, transaction history and gift spending.
 * 8. Gifts — Gift catalog, normal and premium gifts, animations and combo x1/x5/x10.
 * 9. VIP/SVIP — Memberships, badges, profile frames and benefits.
 * 10. Game Center — Ludo for 2/4 players, Carrom and 8 Ball Pool.
 * 11. Owner/Admin Studio — User management, reports, moderation, gift management, transaction review, VIP management, analytics, settings and audit logs.
 * 12. Settings — Privacy, notifications, language, permissions, support and account deletion (via SettingsScreenController).
 */
public class MainActivity extends Activity implements SettingsScreenController.SettingsHostCallbacks, QluvxMasterScreensController.HostNavigationCallbacks {

    private static final int REQ_PICK_CHAT_IMAGE = 4201;

    private static final String[] MENU_TITLES_EN = new String[]{
            "🏠 Home",
            "🧭 Discover",
            "🎙️ 12-Seat",
            "🎥 Video PK",
            "💬 Msg Box",
            "👤 Profile",
            "🪙 Wallet",
            "🎁 Gifts",
            "👑 VIP/SVIP",
            "🎮 Games",
            "🛡️ Admin",
            "⚙️ Settings"
    };

    private static final String[] MENU_TITLES_HI = new String[]{
            "🏠 होम",
            "🧭 डिस्कवर",
            "🎙️ 12-सीट",
            "🎥 वीडियो PK",
            "💬 मैसेज बॉक्स",
            "👤 प्रोफ़ाइल",
            "🪙 वॉलेट",
            "🎁 गिफ्ट्स",
            "👑 VIP/SVIP",
            "🎮 गेम्स",
            "🛡️ एडमिन",
            "⚙️ सेटिंग्स"
    };

    private static final String[] DISCOVER_COUNTRIES = new String[]{
            "All", "India", "USA", "UAE", "UK", "Indonesia", "Brazil"
    };

    private static final String[] AUTO_REPLIES = new String[]{
            "Hey! Great to hear from you 😊 Come join my 12-Seat Voice Room!",
            "Haha bilkul! Aaj ka live room aur wallpaper theme bohot mast lag raha hai 🔥",
            "Thank you so much! Let's do a 1v1 Voice or Video PK match soon 🎙️⚡",
            "Awesome! Sending you lots of good vibes and a Flame Tap 🔥❤️"
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VoiceRoomModels.RoomRepository roomRepo = VoiceRoomModels.RoomRepository.getInstance();
    private GiftEconomyManager economy;
    private QluvxSocialManager social;

    private FrameLayout mainOuterFrame;
    private ImageView ivMainWallpaper;
    private View topAppBar;
    private View topMenuScroll;
    private View bottomQuickNav;
    private LinearLayout menuTabsContainer;
    private LinearLayout contentContainer;
    private TextView tvBrandSubtitle;
    private TextView btnTopThemeWallpaper;
    private TextView tvTopWalletChip;
    private TextView btnTopCreateRoom;
    private TextView navBottomHome;
    private TextView navBottomDiscover;
    private TextView navBottomVoice;
    private TextView navBottomChat;
    private TextView navBottomProfile;

    private int currentMenuIndex = 0; // 0..11 corresponding to Menus 1..12
    private String selectedHomeSection = "Recommended";
    private String discoverSearchQuery = "";
    private String selectedDiscoverCountry = "All";
    private boolean discoverNearbyOnly = false;
    private QluvxSocialManager.ChatConversation activeChatConversation;
    private int selectedGiftComboMultiplier = 1;
    private int autoReplyCursor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiStyleHelper.loadSavedTheme(this);
        economy = GiftEconomyManager.getInstance(this);
        social = QluvxSocialManager.getInstance(this);
        if (!social.getConversations().isEmpty()) {
            activeChatConversation = social.getConversations().get(0);
        }

        UiStyleHelper.configureSafeActivityWindow(this);
        setContentView(R.layout.activity_main);

        mainOuterFrame = findViewById(R.id.main_outer_frame);
        ivMainWallpaper = findViewById(R.id.iv_main_wallpaper);
        topAppBar = findViewById(R.id.top_app_bar);
        topMenuScroll = findViewById(R.id.top_menu_scroll);
        bottomQuickNav = findViewById(R.id.bottom_quick_nav);
        menuTabsContainer = findViewById(R.id.menu_tabs_container);
        contentContainer = findViewById(R.id.main_menu_content_container);
        tvBrandSubtitle = findViewById(R.id.tv_brand_subtitle);
        btnTopThemeWallpaper = findViewById(R.id.btn_top_theme_wallpaper);
        tvTopWalletChip = findViewById(R.id.tv_top_wallet_chip);
        btnTopCreateRoom = findViewById(R.id.btn_top_create_room);
        navBottomHome = findViewById(R.id.nav_bottom_home);
        navBottomDiscover = findViewById(R.id.nav_bottom_discover);
        navBottomVoice = findViewById(R.id.nav_bottom_voice);
        navBottomChat = findViewById(R.id.nav_bottom_chat);
        navBottomProfile = findViewById(R.id.nav_bottom_profile);

        TextView btnTopVip = findViewById(R.id.btn_top_vip);
        TextView btnTopSearch = findViewById(R.id.btn_top_search);
        TextView btnTopNotifications = findViewById(R.id.btn_top_notifications);

        if (btnTopVip != null) {
            btnTopVip.setOnClickListener(v -> selectMenu(8)); // Menu 9: VIP/SVIP
        }
        if (btnTopSearch != null) {
            btnTopSearch.setOnClickListener(v -> selectMenu(1)); // Menu 2: Discover & Search
        }
        if (btnTopNotifications != null) {
            btnTopNotifications.setOnClickListener(v -> showNotificationsCenterDialog());
        }
        if (btnTopThemeWallpaper != null) {
            btnTopThemeWallpaper.setOnClickListener(v ->
                    UiStyleHelper.showThemeAndWallpaperStudioDialog(this, this::renderActiveMenu)
            );
        }
        tvTopWalletChip.setOnClickListener(v -> selectMenu(6)); // Menu 7: Wallet
        btnTopCreateRoom.setOnClickListener(v -> showCreateRoomDialog());

        // BOTTOM NAVIGATION: 1. Home, 2. Discover, 3. Chat, 4. Live, 5. Me
        navBottomHome.setOnClickListener(v -> selectMenu(0));
        navBottomDiscover.setOnClickListener(v -> selectMenu(1));
        navBottomChat.setOnClickListener(v -> selectMenu(4));
        navBottomVoice.setOnClickListener(v -> selectMenu(2));
        navBottomProfile.setOnClickListener(v -> selectMenu(5));

        selectMenu(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiStyleHelper.configureSafeActivityWindow(this);
        if (contentContainer != null && economy != null) {
            renderActiveMenu();
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (currentMenuIndex != 0) {
            selectMenu(0);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onRefreshUiRequested() {
        renderActiveMenu();
    }

    @Override
    public void onShowAuthDialogRequested() {
        showAuthDialog();
    }

    @Override
    public void onOpenVoiceRoom(String title, String channelName) {
        openVoiceRoomActivity(title, channelName);
    }

    @Override
    public void onSelectMenu(int menuIndex) {
        selectMenu(menuIndex);
    }

    @Override
    public void onOpenLiveVideoRoom(String roomTitle, String channelName, String hostName, String hostId, int hostAvatarRes) {
        Intent intent = new Intent(this, LiveVideoActivity.class);
        intent.putExtra(LiveVideoActivity.EXTRA_VIDEO_TITLE, roomTitle);
        intent.putExtra(LiveVideoActivity.EXTRA_VIDEO_CHANNEL, channelName);
        intent.putExtra(LiveVideoActivity.EXTRA_START_IN_SETUP, "Pre-LIVE Setup".equalsIgnoreCase(hostName));
        startActivity(intent);
    }

    @Override
    public void onOpenDirectChatWithPeer(QluvxSocialManager.UserProfile user) {
        if (user != null) {
            activeChatConversation = social.getOrCreateConversation(user);
        }
        selectMenu(4);
    }

    @Override
    public void onShowCreateVoiceRoomDialog() {
        showCreateRoomDialog();
    }

    @Override
    public void onShowEditProfileDialog() {
        showEditProfileModalDialog();
    }

    @Override
    public void onRefreshScreen() {
        renderActiveMenu();
    }

    private void showNotificationsCenterDialog() {
        String[] items = new String[]{
                "🔔 Ananya started a Live Video PK Stream • Tap to Watch",
                "🎙️ DJ Riya invited you to Seat 2 in Late Night Vibes",
                "🎁 Kabir sent you a Luxury Rocket Gift (+500 Coins)",
                "❤️ Simran & 4 others followed your QLUVX profile",
                "👑 VIP Level 6 Daily Reward (+200 Bonus Coins) is ready!"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔔 QLUVX Notifications Center")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        onOpenLiveVideoRoom("Ananya Live PK Stage", "qluvx_live_ananya", "Ananya", "882104", R.drawable.img_avatar_0);
                    } else if (which == 1) {
                        openVoiceRoomActivity("Late Night Vibes", BuildConfig.AGORA_DEFAULT_CHANNEL);
                    } else if (which == 2 || which == 4) {
                        selectMenu(6);
                    } else {
                        selectMenu(5);
                    }
                })
                .setPositiveButton("Mark All Read", (d, w) ->
                        Toast.makeText(this, "All notifications marked as read ✓", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SettingsScreenController.REQ_SETTINGS_PERMISSIONS) {
            renderActiveMenu();
        }
    }

    private void selectMenu(int index) {
        currentMenuIndex = Math.max(0, Math.min(MENU_TITLES_EN.length - 1, index));
        renderActiveMenu();
    }

    private void applyGlobalWallpaperAndChromeTheme() {
        UiStyleHelper.loadSavedTheme(this);
        if (mainOuterFrame != null) {
            mainOuterFrame.setBackgroundColor(Color.rgb(14, 15, 20));
        }
        boolean isPrimaryFiveTab = (currentMenuIndex == 0 || currentMenuIndex == 1
                || currentMenuIndex == 2 || currentMenuIndex == 4 || currentMenuIndex == 5);
        if (ivMainWallpaper != null) {
            ivMainWallpaper.setImageResource(UiStyleHelper.getActiveWallpaperResId(this));
            ivMainWallpaper.setAlpha(isPrimaryFiveTab ? 0.14f : UiStyleHelper.getActiveWallpaperAlpha(this));
        }
        int barColor = Color.argb(232,
                Color.red(UiStyleHelper.COLOR_BG_STAGE_TOP),
                Color.green(UiStyleHelper.COLOR_BG_STAGE_TOP),
                Color.blue(UiStyleHelper.COLOR_BG_STAGE_TOP));
        if (topAppBar != null) {
            topAppBar.setBackgroundColor(barColor);
            topAppBar.setVisibility(isPrimaryFiveTab ? View.GONE : View.VISIBLE);
        }
        if (topMenuScroll != null) {
            topMenuScroll.setBackgroundColor(barColor);
            topMenuScroll.setVisibility(isPrimaryFiveTab ? View.GONE : View.VISIBLE);
        }
        View topDivider = findViewById(R.id.top_divider_line);
        if (topDivider != null) {
            topDivider.setVisibility(isPrimaryFiveTab ? View.GONE : View.VISIBLE);
        }
        if (bottomQuickNav != null) {
            bottomQuickNav.setBackground(UiStyleHelper.gradientBox(
                    this,
                    QluvxFiveTabHubController.getLuxuryGradStart(),
                    QluvxFiveTabHubController.getLuxuryGradEnd(),
                    28f,
                    QluvxFiveTabHubController.getLuxuryBorderAccent(),
                    1.2f
            ));
        }

        if (btnTopThemeWallpaper != null) {
            UiStyleHelper.ThemePreset preset = UiStyleHelper.getActiveThemePreset(this);
            btnTopThemeWallpaper.setText(preset.emoji + " Theme");
            btnTopThemeWallpaper.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
            btnTopThemeWallpaper.setBackground(UiStyleHelper.rippleButton(
                    this,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    14f,
                    UiStyleHelper.COLOR_AGORA_CYAN,
                    1f
            ));
        }
        if (tvTopWalletChip != null) {
            tvTopWalletChip.setBackground(UiStyleHelper.rippleButton(
                    this,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    14f,
                    UiStyleHelper.COLOR_WARNING_AMBER,
                    1f
            ));
        }
        if (btnTopCreateRoom != null) {
            btnTopCreateRoom.setBackground(UiStyleHelper.rippleGradientButton(
                    this,
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    UiStyleHelper.COLOR_INDIGO_ACCENT,
                    14f
            ));
        }
    }

    private void renderActiveMenu() {
        applyGlobalWallpaperAndChromeTheme();

        if (tvBrandSubtitle != null) {
            UiStyleHelper.ThemePreset p = UiStyleHelper.getActiveThemePreset(this);
            tvBrandSubtitle.setText(p.emoji + " " + p.title + " • " + social.tr(
                    getString(R.string.app_tagline),
                    getString(R.string.app_tagline_hi)
            ));
        }
        if (btnTopCreateRoom != null) {
            btnTopCreateRoom.setText(social.tr("🎙️ + Room", "🎙️ + रूम"));
        }
        tvTopWalletChip.setText("🪙 " + economy.getCoinBalance());
        buildTopMenuChips();
        updateBottomNavHighlight();
        contentContainer.removeAllViews();

        switch (currentMenuIndex) {
            case 0:
                renderMenu1Home();
                break;
            case 1:
                renderMenu2Discover();
                break;
            case 2:
                renderMenu3LiveVoiceRooms();
                break;
            case 3:
                renderMenu4LiveVideo();
                break;
            case 4:
                renderMenu5Chat();
                break;
            case 5:
                renderMenu6Profile();
                break;
            case 6:
                renderMenu7Wallet();
                break;
            case 7:
                renderMenu8Gifts();
                break;
            case 8:
                renderMenu9VipSvip();
                break;
            case 9:
                renderMenu10GameCenter();
                break;
            case 10:
                renderMenu11AdminStudio();
                break;
            case 11:
                renderMenu12Settings();
                break;
        }
    }

    private void buildTopMenuChips() {
        menuTabsContainer.removeAllViews();
        String[] titles = social.isHindi() ? MENU_TITLES_HI : MENU_TITLES_EN;
        for (int i = 0; i < titles.length; i++) {
            final int menuIdx = i;
            boolean active = (i == currentMenuIndex);
            TextView chip = UiStyleHelper.createStatusChip(
                    this,
                    titles[i],
                    active ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                    active ? Color.WHITE : UiStyleHelper.COLOR_TEXT_SECONDARY,
                    active ? UiStyleHelper.COLOR_AGORA_CYAN : UiStyleHelper.COLOR_BORDER_SUBTLE
            );
            chip.setOnClickListener(v -> selectMenu(menuIdx));
            menuTabsContainer.addView(chip, UiStyleHelper.params(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, UiStyleHelper.dp(this, 8), 0
            ));
        }
    }

    private void updateBottomNavHighlight() {
        int activeText = Color.WHITE;
        int idleText = Color.rgb(203, 213, 225);
        int gradStart = QluvxFiveTabHubController.getLuxuryButtonStart();
        int gradEnd = QluvxFiveTabHubController.getLuxuryButtonEnd();

        navBottomHome.setText(social.tr("🏠\nHome", "🏠\nहोम"));
        navBottomDiscover.setText(social.tr("🧭\nDiscover", "🧭\nडिस्कवर"));
        navBottomChat.setText(social.tr("💬 9+\nChat", "💬 9+\nचैट"));
        navBottomVoice.setText(social.tr("📹\nLive", "📹\nलाइव"));
        navBottomProfile.setText(social.tr("👤\nMe", "👤\nप्रोफ़ाइल"));

        navBottomHome.setTextColor(currentMenuIndex == 0 ? activeText : idleText);
        navBottomHome.setBackground(currentMenuIndex == 0
                ? UiStyleHelper.gradientBox(this, gradStart, gradEnd, 20f, Color.rgb(253, 224, 71), 1.2f)
                : UiStyleHelper.roundedBox(this, Color.TRANSPARENT, 20f, 0, 0));

        navBottomDiscover.setTextColor(currentMenuIndex == 1 ? activeText : idleText);
        navBottomDiscover.setBackground(currentMenuIndex == 1
                ? UiStyleHelper.gradientBox(this, gradStart, gradEnd, 20f, Color.rgb(253, 224, 71), 1.2f)
                : UiStyleHelper.roundedBox(this, Color.TRANSPARENT, 20f, 0, 0));

        navBottomChat.setTextColor(currentMenuIndex == 4 ? activeText : idleText);
        navBottomChat.setBackground(currentMenuIndex == 4
                ? UiStyleHelper.gradientBox(this, gradStart, gradEnd, 20f, Color.rgb(253, 224, 71), 1.2f)
                : UiStyleHelper.roundedBox(this, Color.TRANSPARENT, 20f, 0, 0));

        boolean isLiveTab = (currentMenuIndex == 2 || currentMenuIndex == 3);
        navBottomVoice.setTextColor(isLiveTab ? activeText : idleText);
        navBottomVoice.setBackground(isLiveTab
                ? UiStyleHelper.gradientBox(this, gradStart, gradEnd, 20f, Color.rgb(253, 224, 71), 1.2f)
                : UiStyleHelper.roundedBox(this, Color.argb(45, 255, 255, 255), 20f, QluvxFiveTabHubController.getLuxuryBorderAccent(), 0.8f));

        navBottomProfile.setTextColor(currentMenuIndex == 5 ? activeText : idleText);
        navBottomProfile.setBackground(currentMenuIndex == 5
                ? UiStyleHelper.gradientBox(this, gradStart, gradEnd, 20f, Color.rgb(253, 224, 71), 1.2f)
                : UiStyleHelper.roundedBox(this, Color.TRANSPARENT, 20f, 0, 0));
    }

    // =========================================================================
    // MENU 1: HOME PAGE ("mome page") — QLUVX LIVE & VOICE Master Hub
    //         + All 21 Home Screen Buttons, Story Bar, Ranking Banner & Live Grid
    // =========================================================================
    private void renderMenu1Home() {
        QluvxFiveTabHubController.renderHomeTab(
                this,
                contentContainer,
                social,
                economy,
                roomRepo,
                this
        );
    }

    // =========================================================================
    // MENU 2: DISCOVER ([Family] [Group] [Quick Chat] + Live Camera, Post, Photo Gallery)
    // =========================================================================
    private void renderMenu2Discover() {
        QluvxFiveTabHubController.renderDiscoverTab(
                this,
                contentContainer,
                social,
                economy,
                roomRepo,
                this
        );
    }

    // =========================================================================
    // MENU 3: LIVE (LIVE | VOICE + Top All IDs + Recommended / Global Country / Following 1-by-1 IDs)
    // =========================================================================
    private void renderMenu3LiveVoiceRooms() {
        QluvxFiveTabHubController.renderLiveHubTab(
                this,
                contentContainer,
                social,
                economy,
                roomRepo,
                this
        );
    }

    // =========================================================================
    // MENU 4: LIVE VIDEO (Video stage, comments, viewer count, reactions and PK battles)
    // =========================================================================
    private void renderMenu4LiveVideo() {
        addSectionLabel("🎥 Live Video Stage & Real-Time PK Battles");
        renderLiveVideoCardsSection();
    }

    private void renderLiveVideoCardsSection() {
        String[][] videoStreams = new String[][]{
                {"QLUVX Live Video & PK Arena", "qluvx_live_video_pk", "Aarav vs Zoya • Live PK Battle • 👁️ 142 Viewers"},
                {"Acoustic Guitar & Singing Live", "qluvx_video_music_01", "Putri Ayu • Music Stage • 👁️ 218 Viewers"},
                {"Late Night Talk & Q&A Stage", "qluvx_video_talk_02", "Sophia Sterling • Interactive Video • 👁️ 96 Viewers"}
        };
        int[] covers = new int[]{R.drawable.bg_channel_0, R.drawable.bg_channel_1, R.drawable.bg_channel_2};

        for (int idx = 0; idx < videoStreams.length; idx++) {
            String[] stream = videoStreams[idx];
            final String title = stream[0];
            final String channel = stream[1];
            final String meta = stream[2];

            LinearLayout card = UiStyleHelper.createCardContainer(this, UiStyleHelper.COLOR_BORDER_SUBTLE);

            FrameLayout bannerWrap = new FrameLayout(this);
            bannerWrap.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(MainActivity.this, 14));
                }
            });
            bannerWrap.setClipToOutline(true);

            ImageView banner = new ImageView(this);
            banner.setImageResource(covers[idx % covers.length]);
            banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
            bannerWrap.addView(banner, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            LinearLayout stageOverlay = new LinearLayout(this);
            stageOverlay.setOrientation(LinearLayout.HORIZONTAL);
            stageOverlay.setGravity(Gravity.CENTER_VERTICAL);
            stageOverlay.setPadding(UiStyleHelper.dp(this, 12), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 12), UiStyleHelper.dp(this, 10));
            stageOverlay.setBackground(UiStyleHelper.bottomScrimGradient(this, 14f));

            View hostAv = UiStyleHelper.createCircularAvatar(this, VoiceRoomModels.getAvatarResId(idx + 1), 46, UiStyleHelper.COLOR_LIVE_GREEN, true);
            View pkAv = UiStyleHelper.createCircularAvatar(this, VoiceRoomModels.getAvatarResId(idx + 4), 46, UiStyleHelper.COLOR_DANGER_RED, true);

            TextView vsBadge = UiStyleHelper.createStatusChip(this, "⚔️ LIVE PK", Color.argb(180, 225, 29, 72), Color.WHITE, 0);

            stageOverlay.addView(hostAv);
            stageOverlay.addView(vsBadge, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 8), 0, UiStyleHelper.dp(this, 8), 0));
            stageOverlay.addView(pkAv);

            bannerWrap.addView(stageOverlay, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            ));

            card.addView(bannerWrap, UiStyleHelper.params(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    UiStyleHelper.dp(this, 136),
                    0, 0, 0, UiStyleHelper.dp(this, 10)
            ));

            TextView badge = UiStyleHelper.createStatusChip(
                    this,
                    "🔴 LIVE VIDEO + PK BATTLE",
                    Color.argb(75, 244, 63, 94),
                    UiStyleHelper.COLOR_DANGER_RED,
                    UiStyleHelper.COLOR_DANGER_RED
            );

            TextView tvTitle = new TextView(this);
            tvTitle.setText(title);
            tvTitle.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            tvTitle.setPadding(0, UiStyleHelper.dp(this, 8), 0, UiStyleHelper.dp(this, 4));

            TextView tvMeta = new TextView(this);
            tvMeta.setText(meta + " • Channel: " + channel);
            tvMeta.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            tvMeta.setPadding(0, 0, 0, UiStyleHelper.dp(this, 12));

            TextView joinBtn = UiStyleHelper.createControlBarButton(
                    this,
                    "🎥 Enter Live Video & PK Stage",
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    Color.WHITE,
                    0,
                    v -> {
                        Intent intent = new Intent(this, LiveVideoActivity.class);
                        intent.putExtra(LiveVideoActivity.EXTRA_VIDEO_TITLE, title);
                        intent.putExtra(LiveVideoActivity.EXTRA_VIDEO_CHANNEL, channel);
                        startActivity(intent);
                    }
            );

            card.addView(badge, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(tvTitle);
            card.addView(tvMeta);
            card.addView(joinBtn);
            contentContainer.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 12)));
        }
    }

    // =========================================================================
    // MENU 5: MESSAGE BOX ("message box") — Visual Inbox Cards, Custom Chat Wallpaper,
    //         Left/Right Avatar Chat Bubbles, 1-Tap Quick Replies, Rich Media & Auto-Reply
    // =========================================================================
    private void renderMenu5Chat() {
        QluvxFiveTabHubController.renderChatTab(
                this,
                contentContainer,
                social,
                economy,
                this
        );
    }

    private void sendDirectMessageAndTriggerReply(String type, String content, String meta) {
        if (activeChatConversation == null) return;
        activeChatConversation.messages.add(new QluvxSocialManager.DirectMessage(
                social.getCurrentUser().displayName, true, type, content, meta
        ));
        renderActiveMenu();
        schedulePeerSmartReply(activeChatConversation);
    }

    private void schedulePeerSmartReply(final QluvxSocialManager.ChatConversation targetConv) {
        if (targetConv == null) return;
        final String replyText = AUTO_REPLIES[autoReplyCursor % AUTO_REPLIES.length];
        autoReplyCursor++;
        mainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            targetConv.messages.add(new QluvxSocialManager.DirectMessage(
                    targetConv.peerName,
                    false,
                    "TEXT",
                    replyText,
                    ""
            ));
            if (currentMenuIndex == 4 || currentMenuIndex == 0) {
                renderActiveMenu();
            }
        }, 850L);
    }

    private void playVoiceNoteTone() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 180);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_CHAT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null && activeChatConversation != null) {
                sendDirectMessageAndTriggerReply(
                        "IMAGE",
                        "🖼️ Image Attachment: " + uri.getLastPathSegment(),
                        uri.toString()
                );
            }
        } else if (requestCode == QluvxFiveTabHubController.REQ_PICK_PROFILE_DP && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                QluvxFiveTabHubController.onProfileDpPicked(this, social, uri, this::renderActiveMenu);
            }
        } else if (requestCode == QluvxFiveTabHubController.REQ_PICK_GALLERY_PHOTO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                QluvxFiveTabHubController.onGalleryPhotoPicked(this, social, uri, this::renderActiveMenu);
            }
        }
    }

    // =========================================================================
    // MENU 6: PROFILE & AUTH (DP Change + All Profile Details + Wallet + Services)
    // =========================================================================
    private void renderMenu6Profile() {
        QluvxFiveTabHubController.renderMeProfileTab(
                this,
                contentContainer,
                social,
                economy,
                this
        );
    }

    private void showEditProfileModalDialog() {
        final QluvxSocialManager.UserProfile me = social.getCurrentUser();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        form.setPadding(pad, pad, pad, pad);

        final EditText etName = new EditText(this);
        etName.setText(me.displayName);
        etName.setHint("Display Name");
        etName.setSingleLine(true);

        final EditText etBio = new EditText(this);
        etBio.setText(me.bio);
        etBio.setHint("Profile Bio");
        etBio.setSingleLine(true);

        final EditText etCountry = new EditText(this);
        etCountry.setText(me.country);
        etCountry.setHint("Country");
        etCountry.setSingleLine(true);

        form.addView(etName);
        form.addView(etBio);
        form.addView(etCountry);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("✏️ Edit QLUVX Profile")
                .setView(form)
                .setPositiveButton("Save Profile", (d, w) -> {
                    social.saveProfileChanges(
                            etName.getText().toString(),
                            etBio.getText().toString(),
                            etCountry.getText().toString(),
                            me.avatarEmoji
                    );
                    Toast.makeText(this, "✅ Profile updated!", Toast.LENGTH_SHORT).show();
                    renderActiveMenu();
                })
                .setNeutralButton("Account Auth", (d, w) -> showAuthDialog())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAuthDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        form.setPadding(pad, pad, pad, pad);

        final EditText nameIn = new EditText(this);
        nameIn.setHint("Display Name");
        nameIn.setText(social.getCurrentUser().displayName);
        nameIn.setSingleLine(true);

        final EditText emailIn = new EditText(this);
        emailIn.setHint("Email or Phone (e.g. user@qluvx.app)");
        emailIn.setText(social.getCurrentUser().emailOrPhone);
        emailIn.setSingleLine(true);

        form.addView(nameIn);
        form.addView(emailIn);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔐 QLUVX Account Authentication")
                .setView(form)
                .setPositiveButton("Sign In (Firebase Auth)", (d, w) -> {
                    social.signInUser(nameIn.getText().toString(), emailIn.getText().toString(), "Firebase Auth • Verified");
                    Toast.makeText(this, "Signed in as " + social.getCurrentUser().displayName, Toast.LENGTH_SHORT).show();
                    renderActiveMenu();
                })
                .setNeutralButton("Sign Out", (d, w) -> {
                    social.signOutUser();
                    Toast.makeText(this, "Signed out.", Toast.LENGTH_SHORT).show();
                    renderActiveMenu();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // MENU 7: WALLET (Coins, recharge, transaction history and gift spending)
    // =========================================================================
    private void renderMenu7Wallet() {
        QluvxMasterScreensController.renderWalletScreen(
                this,
                contentContainer,
                economy,
                this
        );
    }

    // =========================================================================
    // MENU 8: GIFTS (Gift catalog, All / Popular / Luxury, Combo x1/x5/x10, Select Gift, Send Gift, Close Panel)
    // =========================================================================
    private void renderMenu8Gifts() {
        addSectionLabel("🎁 Gift Panel • All, Popular, Luxury & Combo x1 / x5 / x10");

        TextView openInteractivePanelBtn = UiStyleHelper.createControlBarButton(
                this,
                "🎁 Open Interactive Gift Panel (All / Popular / Luxury / Combo x1/x5/x10)",
                UiStyleHelper.COLOR_AGORA_BLUE,
                Color.WHITE,
                0,
                v -> UiStyleHelper.showUnifiedGiftPanelDialog(
                        this,
                        economy,
                        social.getCurrentUser().displayName,
                        "Room Host (Seat 1)",
                        (gift, combo, tx) -> renderActiveMenu()
                )
        );
        contentContainer.addView(openInteractivePanelBtn, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, 0, 0, UiStyleHelper.dp(this, 12)
        ));

        LinearLayout comboRow = new LinearLayout(this);
        comboRow.setOrientation(LinearLayout.HORIZONTAL);
        int[] mults = new int[]{1, 5, 10};
        for (int m : mults) {
            final int multVal = m;
            boolean active = (selectedGiftComboMultiplier == multVal);
            TextView chip = UiStyleHelper.createStatusChip(
                    this,
                    "🔥 Combo x" + multVal,
                    active ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_BG,
                    active ? Color.WHITE : UiStyleHelper.COLOR_TEXT_SECONDARY,
                    active ? UiStyleHelper.COLOR_AGORA_CYAN : UiStyleHelper.COLOR_CARD_ELEVATED
            );
            chip.setOnClickListener(v -> {
                selectedGiftComboMultiplier = multVal;
                renderActiveMenu();
            });
            comboRow.addView(chip, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 8), 0));
        }
        contentContainer.addView(comboRow, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 14)));

        for (GiftEconomyManager.GiftItem gift : economy.getGiftsByCategory("All")) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            int p = UiStyleHelper.dp(this, 14);
            card.setPadding(p, p, p, p);
            card.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_BG, 14f, UiStyleHelper.COLOR_CARD_ELEVATED, 1f));

            int totalCost = gift.coinPrice * selectedGiftComboMultiplier;
            TextView info = new TextView(this);
            info.setText(gift.iconLabel + "  " + gift.name + " (" + gift.category + ")\n🪙 "
                    + gift.coinPrice + " Coins each  •  Combo x" + selectedGiftComboMultiplier + " = " + totalCost + " Coins");
            info.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView sendBtn = UiStyleHelper.createControlBarButton(
                    this,
                    "Send x" + selectedGiftComboMultiplier,
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    Color.WHITE,
                    0,
                    v -> {
                        GiftEconomyManager.WalletTransaction tx = economy.sendGiftCombo(
                                gift,
                                selectedGiftComboMultiplier,
                                social.getCurrentUser().displayName,
                                "Room Host (Seat 1)",
                                null
                        );
                        if (tx != null) {
                            Toast.makeText(this, "🎉 Sent " + gift.iconLabel + " " + gift.name + " x" + selectedGiftComboMultiplier + "!", Toast.LENGTH_SHORT).show();
                            renderActiveMenu();
                        } else {
                            Toast.makeText(this, "Insufficient coins! Recharge in Wallet.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            card.addView(info);
            card.addView(sendBtn);
            contentContainer.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 10)));
        }
    }

    // =========================================================================
    // MENU 9: VIP/SVIP (Memberships, badges, profile frames and benefits)
    // =========================================================================
    private void renderMenu9VipSvip() {
        addSectionLabel("👑 VIP / SVIP Memberships, Badges, Profile Frames & Benefits");

        for (QluvxSocialManager.VipTierOption tier : social.getVipCatalog()) {
            LinearLayout card = UiStyleHelper.createCardContainer(this, tier.accentColor);

            TextView title = new TextView(this);
            title.setText(tier.badgeIcon + "  " + tier.title + "  •  🪙 " + tier.monthlyCoins + " Coins");
            title.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView frame = new TextView(this);
            frame.setText("Exclusive Avatar Frame: " + tier.frameName);
            frame.setTextColor(UiStyleHelper.COLOR_WARNING_AMBER);
            frame.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            frame.setPadding(0, UiStyleHelper.dp(this, 4), 0, UiStyleHelper.dp(this, 8));

            StringBuilder benefitsText = new StringBuilder();
            for (String b : tier.benefits) {
                benefitsText.append("✓ ").append(b).append("\n");
            }
            TextView benefits = new TextView(this);
            benefits.setText(benefitsText.toString().trim());
            benefits.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            benefits.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            benefits.setPadding(0, 0, 0, UiStyleHelper.dp(this, 12));

            TextView activateBtn = UiStyleHelper.createControlBarButton(
                    this,
                    "⭐ Activate " + tier.title,
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    Color.WHITE,
                    0,
                    v -> {
                        boolean ok = social.activateVipTier(this, tier);
                        if (ok) {
                            Toast.makeText(this, "Activated " + tier.title + " & " + tier.frameName + "!", Toast.LENGTH_SHORT).show();
                            renderActiveMenu();
                        } else {
                            Toast.makeText(this, "Need " + tier.monthlyCoins + " Coins! Recharge in Wallet.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            card.addView(title);
            card.addView(frame);
            card.addView(benefits);
            card.addView(activateBtn);
            contentContainer.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 12)));
        }
    }

    // =========================================================================
    // MENU 10: GAME CENTER (Ludo for 2/4 players, Carrom and 8 Ball Pool)
    // =========================================================================
    private void renderMenu10GameCenter() {
        addSectionLabel("🎮 QLUVX Game Center (Ludo 2/4P, Carrom & 8 Ball Pool)");
        renderGameCenterQuickCards();
    }

    private void renderGameCenterQuickCards() {
        String[][] games = new String[][]{
                {"🎲 Ludo Royal (2 / 4 Players)", "Interactive 15x15 Ludo board, dice roller, 2P/4P pass-and-play & voice room link", "0"},
                {"🎯 Carrom Club Pro", "Wooden Carrom board with Red Queen, Black/White coins, slingshot aiming & pocket physics", "1"},
                {"🎱 8 Ball Pool Arena", "Billiards table with cue stick aiming, numbered rack balls (1–8) & 6-pocket physics", "2"}
        };

        for (String[] g : games) {
            final int gameIdx = Integer.parseInt(g[2]);
            LinearLayout card = UiStyleHelper.createCardContainer(this, UiStyleHelper.COLOR_CARD_ELEVATED);

            TextView title = new TextView(this);
            title.setText(g[0]);
            title.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView desc = new TextView(this);
            desc.setText(g[1]);
            desc.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            desc.setPadding(0, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 12));

            TextView playBtn = UiStyleHelper.createControlBarButton(
                    this,
                    "▶️ Play Now",
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    Color.WHITE,
                    0,
                    v -> {
                        Intent intent = new Intent(this, GameCenterActivity.class);
                        intent.putExtra(GameCenterActivity.EXTRA_INITIAL_GAME, gameIdx);
                        startActivity(intent);
                    }
            );

            card.addView(title);
            card.addView(desc);
            card.addView(playBtn);
            contentContainer.addView(card, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 12)));
        }
    }

    // =========================================================================
    // MENU 11: OWNER/ADMIN STUDIO
    // =========================================================================
    private void renderMenu11AdminStudio() {
        addSectionLabel("🛡️ Owner / Admin Studio (RBAC Console)");

        LinearLayout card = UiStyleHelper.createCardContainer(this, UiStyleHelper.COLOR_LIVE_GREEN);

        TextView title = new TextView(this);
        title.setText("QLUVX Owner & Super Admin Control Center");
        title.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView desc = new TextView(this);
        desc.setText("Role: " + social.getCurrentUser().systemRole
                + "\n• User Management (Ban/Unban, Role assignment)"
                + "\n• Room Moderation & Reports Queue (Lock/Unlock, 1/4/8/12 Seat Capacity)"
                + "\n• Gift Catalog Pricing & Enable/Disable"
                + "\n• Wallet Transaction Ledger & Authorized Refunds"
                + "\n• VIP/SVIP Granting & Platform Analytics"
                + "\n• Security Settings & Immutable Audit Logs (" + economy.getAdminAuditLogs().size() + " entries)");
        desc.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        desc.setPadding(0, UiStyleHelper.dp(this, 8), 0, UiStyleHelper.dp(this, 14));

        TextView launchBtn = UiStyleHelper.createControlBarButton(
                this,
                "🛡️ Open Full Owner/Admin Studio Console",
                UiStyleHelper.COLOR_AGORA_BLUE,
                Color.WHITE,
                0,
                v -> startActivity(new Intent(this, AdminStudioActivity.class))
        );

        card.addView(title);
        card.addView(desc);
        card.addView(launchBtn);
        contentContainer.addView(card);
    }

    // =========================================================================
    // MENU 12: SETTINGS (Delegated to modular SettingsScreenController)
    // =========================================================================
    private void renderMenu12Settings() {
        contentContainer.addView(
                UiStyleHelper.buildInlineWallpaperThemeBar(this, this::renderActiveMenu),
                UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 14))
        );
        SettingsScreenController.renderSettingsScreen(this, contentContainer, social, economy, this);
    }

    // =========================================================================
    // SHARED HELPERS: ROOM CARDS, USER CARDS, DIALOGS
    // =========================================================================
    private void addSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setPadding(0, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 10));
        contentContainer.addView(tv);
    }

    private View buildUserCard(final QluvxSocialManager.UserProfile u) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int p = UiStyleHelper.dp(this, 12);
        card.setPadding(p, p, p, p);
        card.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_BG, 16f, UiStyleHelper.COLOR_BORDER_SUBTLE, 1f));
        card.setOnClickListener(v -> {
            activeChatConversation = social.getOrCreateConversation(u);
            selectMenu(4);
        });

        View av = UiStyleHelper.createCircularAvatar(
                this,
                u.avatarResId != 0 ? u.avatarResId : R.drawable.img_avatar_0,
                54,
                u.isOnline ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_AGORA_CYAN,
                u.isOnline
        );

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(UiStyleHelper.dp(this, 12), 0, UiStyleHelper.dp(this, 8), 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(this);
        name.setText(u.displayName + ", " + u.age + " ✓ • 📍 " + u.distanceKm);
        name.setSingleLine(true);
        name.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView specs = new TextView(this);
        specs.setText(u.heightCm + " cm • " + u.weightKg + " kg • " + u.zodiacSign + " • " + u.country);
        specs.setSingleLine(true);
        specs.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        specs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);

        TextView bio = new TextView(this);
        bio.setText(u.bio);
        bio.setSingleLine(true);
        bio.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        bio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        bio.setPadding(0, UiStyleHelper.dp(this, 2), 0, 0);

        info.addView(name);
        info.addView(specs);
        info.addView(bio);

        TextView followBtn = UiStyleHelper.createStatusChip(
                this,
                u.isFollowing ? "✓" : "+ Follow",
                u.isFollowing ? UiStyleHelper.COLOR_CARD_ELEVATED : UiStyleHelper.COLOR_AGORA_BLUE,
                Color.WHITE,
                0
        );
        followBtn.setOnClickListener(v -> {
            social.toggleFollowUser(u);
            renderActiveMenu();
        });

        TextView dmBtn = UiStyleHelper.createStatusChip(
                this,
                "💬",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                0
        );
        dmBtn.setOnClickListener(v -> {
            activeChatConversation = social.getOrCreateConversation(u);
            selectMenu(4);
        });

        card.addView(av);
        card.addView(info);
        card.addView(followBtn);
        card.addView(dmBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, 0, 0));
        return card;
    }

    private View buildRoomCard(VoiceRoomModels.RoomInfo room) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiStyleHelper.dp(this, 14);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(UiStyleHelper.rippleButton(
                this,
                UiStyleHelper.COLOR_CARD_BG,
                16f,
                UiStyleHelper.COLOR_CARD_ELEVATED,
                1f
        ));
        card.setOnClickListener(v -> onRoomCardClicked(room));

        ImageView coverImg = new ImageView(this);
        int coverRes = room.coverResId != 0 ? room.coverResId : R.drawable.img_channel_0;
        coverImg.setImageResource(coverRes);
        coverImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverImg.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(MainActivity.this, 12));
            }
        });
        coverImg.setClipToOutline(true);
        int coverSize = UiStyleHelper.dp(this, 68);
        LinearLayout.LayoutParams coverLp = new LinearLayout.LayoutParams(coverSize, coverSize);
        coverLp.setMargins(0, 0, UiStyleHelper.dp(this, 12), 0);
        card.addView(coverImg, coverLp);

        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView catBadge = UiStyleHelper.createStatusChip(
                this,
                (room.isPrivateLocked ? "🔒 " : "🌐 ") + room.category,
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                0
        );
        TextView seatsBadge = UiStyleHelper.createStatusChip(
                this,
                "🎙️ " + room.getOccupiedSeatCount() + "/" + room.activeSeatCapacity + " Seats",
                Color.argb(45, 16, 185, 129),
                UiStyleHelper.COLOR_LIVE_GREEN,
                0
        );
        TextView onlineBadge = UiStyleHelper.createStatusChip(
                this,
                "👥 " + room.listenerCount,
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_SECONDARY,
                0
        );
        topRow.addView(catBadge);
        topRow.addView(seatsBadge, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, 0, 0));
        topRow.addView(onlineBadge, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, 0, 0));
        rightCol.addView(topRow);

        TextView title = new TextView(this);
        title.setText(room.title);
        title.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setPadding(0, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 2));
        rightCol.addView(title);

        TextView sub = new TextView(this);
        sub.setText(room.subtitle + " • CH: " + room.channelName + " • Anchor: " + room.hostName);
        sub.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        rightCol.addView(sub);

        card.addView(rightCol);
        return card;
    }

    private void openVoiceRoomActivity(String roomTitle, String channelName) {
        Intent intent = new Intent(this, VoiceRoomActivity.class);
        intent.putExtra(VoiceRoomActivity.EXTRA_ROOM_TITLE, roomTitle);
        intent.putExtra(VoiceRoomActivity.EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(VoiceRoomActivity.EXTRA_LOCAL_UID, roomRepo.localUid);
        startActivity(intent);
    }

    private void onRoomCardClicked(VoiceRoomModels.RoomInfo room) {
        if (room.isPrivateLocked && room.roomPassword != null && !room.roomPassword.isEmpty()) {
            final EditText pinInput = new EditText(this);
            pinInput.setHint("Enter 4-digit Room PIN (Hint: " + room.roomPassword + ")");
            pinInput.setSingleLine(true);
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("🔒 Private Voice Room")
                    .setView(pinInput)
                    .setPositiveButton("Unlock & Join", (d, w) -> {
                        String entered = pinInput.getText().toString().trim();
                        if (room.roomPassword.equals(entered)) {
                            openVoiceRoomActivity(room.title, room.channelName);
                        } else {
                            Toast.makeText(this, "Incorrect Room PIN!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            openVoiceRoomActivity(room.title, room.channelName);
        }
    }

    private void showCreateRoomDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 18);
        form.setPadding(pad, pad, pad, pad);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Room Title (e.g. Late Night Music & Chill)");
        titleInput.setSingleLine(true);

        final EditText channelInput = new EditText(this);
        channelInput.setHint("Agora Channel Name");
        channelInput.setSingleLine(true);
        channelInput.setText(BuildConfig.AGORA_DEFAULT_CHANNEL);

        final EditText topicInput = new EditText(this);
        topicInput.setHint("Category (Music, Casual Chat, Gaming, Friends)");
        topicInput.setSingleLine(true);
        topicInput.setText("Casual Chat");

        final EditText pinInput = new EditText(this);
        pinInput.setHint("Optional 4-digit PIN for Private Room");
        pinInput.setSingleLine(true);

        form.addView(titleInput);
        form.addView(channelInput);
        form.addView(topicInput);
        form.addView(pinInput);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Create 12-Seat Agora Voice Room")
                .setView(form)
                .setPositiveButton("Create & Join", (d, w) -> {
                    String pin = pinInput.getText().toString().trim();
                    VoiceRoomModels.RoomInfo room = roomRepo.createCustomRoom(
                            titleInput.getText().toString(),
                            channelInput.getText().toString(),
                            topicInput.getText().toString().trim(),
                            social.getSettingLanguage(),
                            12,
                            !pin.isEmpty(),
                            pin
                    );
                    openVoiceRoomActivity(room.title, room.channelName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
