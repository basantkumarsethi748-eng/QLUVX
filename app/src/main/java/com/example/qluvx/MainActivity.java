package com.example.qluvx;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements
        AgoraVoiceManager.VoiceEventListener,
        RoomDialogsHelper.RoomActionCallbacks,
        RoomGamesView.GameSyncBridge {

    private static final int REQ_RECORD_AUDIO = 101;
    private static final int REQ_CAMERA_AND_AUDIO = 102;

    private final VoiceRoomModels.RoomRepository repository = new VoiceRoomModels.RoomRepository();
    private final EconomyManager economyManager = new EconomyManager();
    private AgoraVoiceManager voiceManager;
    private final Handler animHandler = new Handler(Looper.getMainLooper());

    private FrameLayout rootContainer;
    private String selectedCategory = "All";

    // Active Live Room state
    private VoiceRoomModels.RoomInfo currentRoom = null;
    private int currentLocalSeatIndex = -1; // -1 = Listener (Audience), 0..11 = Speaker seat
    private int pendingSeatIndexAfterPermission = -1;
    private boolean isGamesPanelVisible = false;

    private FrameLayout stageFrame;
    private LinearLayout connectionStatusPill;
    private VoiceWaveViews.ConnectionStatusDotView connectionDotView;
    private TextView connectionBadgeView;
    private TextView diagnosticBannerView;
    private TextView stageHeaderMetricsView;
    private TextView roomNoticeView;
    private TextView speakerRouteBtn;
    private TextView remoteAudioMuteBtn;
    private TextView micToggleBtn;
    private TextView seatActionBtn;
    private TextView cameraToggleBtn;
    private TextView micGainBtn;
    private LinearLayout videoStageContainer;
    private FrameLayout localVideoHolder;
    private FrameLayout remoteVideoHolder;
    private TextView remoteVideoPlaceholderText;
    private RoomGamesView roomGamesView;
    private LinearLayout chatMessagesContainer;
    private ScrollView chatScrollView;
    private VoiceWaveViews.LiveSpectrumView liveSpectrumView;
    private VoiceWaveViews.FloatingReactionsView reactionsOverlay;
    private final List<VoiceWaveViews.SeatAvatarView> activeSeatViews = new ArrayList<>();
    private final List<TextView> activeSeatNameViews = new ArrayList<>();
    private final List<TextView> activeSeatSubViews = new ArrayList<>();

    private final Runnable stageAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentRoom != null) {
                if (connectionDotView != null) {
                    connectionDotView.stepPulse();
                }
                int maxRealVolume = 0;
                for (int i = 0; i < currentRoom.seats.size(); i++) {
                    VoiceRoomModels.SpeakerSeat seat = currentRoom.seats.get(i);
                    if (seat.isOccupied && !seat.isMuted && seat.speakingVolume > maxRealVolume) {
                        maxRealVolume = seat.speakingVolume;
                    }
                    if (i < activeSeatViews.size()) {
                        activeSeatViews.get(i).stepAnimation();
                    }
                }
                if (liveSpectrumView != null) {
                    liveSpectrumView.updateVolume(maxRealVolume);
                }
                if (roomGamesView != null && isGamesPanelVisible) {
                    roomGamesView.stepPhysicsAndSyncUI();
                }
                if (reactionsOverlay != null) {
                    reactionsOverlay.stepParticles();
                }
            }
            animHandler.postDelayed(this, 50);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(UiStyleHelper.COLOR_BG_DEEP);
        window.setNavigationBarColor(UiStyleHelper.COLOR_BG_DEEP);

        voiceManager = new AgoraVoiceManager(this, this);

        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(UiStyleHelper.COLOR_BG_DEEP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootContainer.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets sysBars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom);
                return insets;
            });
        }

        setContentView(rootContainer);
        showLobbyScreen();
        animHandler.post(stageAnimationRunnable);
    }

    private void showLobbyScreen() {
        if (currentRoom != null) {
            leaveCurrentRoomInternal();
        }
        rootContainer.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        content.setPadding(pad, pad, pad, UiStyleHelper.dp(this, 28));

        // 1. Top Brand Header + Wallet + Profile Button
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout brandCol = new LinearLayout(this);
        brandCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        brandCol.setLayoutParams(brandLp);

        TextView title = new TextView(this);
        title.setText("QLUVX");
        title.setTextColor(UiStyleHelper.COLOR_NEON_PINK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextView subtitle = new TextView(this);
        subtitle.setText("AGORA 12-SEAT VOICE • LIVE VIDEO • GAMES");
        subtitle.setTextColor(UiStyleHelper.COLOR_CYBER_CYAN);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        subtitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        brandCol.addView(title);
        brandCol.addView(subtitle);

        TextView walletBtn = UiStyleHelper.createPillButton(
                this,
                "🪙 " + economyManager.getCoinBalance(),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.rgb(255, 215, 64),
                v -> RoomDialogsHelper.showWalletHistoryDialog(this, economyManager)
        );

        TextView profileBtn = UiStyleHelper.createPillButton(
                this,
                "👤 " + repository.localUserName,
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> showEditProfileDialog()
        );

        topRow.addView(brandCol);
        topRow.addView(walletBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));
        topRow.addView(profileBtn);
        content.addView(topRow);

        // 2. Agora RTC SDK Pre-Flight Verification Card
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        int cardPad = UiStyleHelper.dp(this, 14);
        statusCard.setPadding(cardPad, cardPad, cardPad, cardPad);
        boolean appIdValid = AgoraVoiceManager.isAgoraAppIdValidFormat();
        boolean hasNet = voiceManager.hasInternetConnection();
        boolean hasMic = voiceManager.hasMicPermission();
        boolean hasCam = voiceManager.hasCameraPermission();
        boolean hasToken = !AgoraVoiceManager.getConfiguredTempToken().isEmpty()
                || !AgoraVoiceManager.getConfiguredTokenServerUrl().isEmpty()
                || (voiceManager.getSessionOverrideToken() != null && !voiceManager.getSessionOverrideToken().isEmpty());

        statusCard.setBackground(UiStyleHelper.roundedBox(
                this,
                Color.rgb(16, 20, 34),
                14f,
                appIdValid ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_MUTED_RED,
                1f
        ));

        TextView sdkTitle = new TextView(this);
        sdkTitle.setText(appIdValid
                ? ("Agora Full RTC SDK v" + voiceManager.getSdkVersionString() + " • App ID: " + AgoraVoiceManager.getConfiguredAppIdPreview())
                : "⚠️ Agora App ID Required (" + AgoraVoiceManager.getConfiguredAppIdPreview() + ")");
        sdkTitle.setTextColor(appIdValid ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_MUTED_RED);
        sdkTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sdkTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextView sdkDesc = new TextView(this);
        String checkSummary = "Internet: " + (hasNet ? "Online" : "Offline")
                + " • Mic: " + (hasMic ? "Granted" : "Prompt")
                + " • Cam: " + (hasCam ? "Granted" : "Prompt")
                + " • Auth: " + (hasToken ? "Token Set" : "App-ID Mode");
        sdkDesc.setText(checkSummary + "\nSupports 12-Seat Voice Rooms, Live Video Publishing, Gifts & Economy, and Ludo / Carrom / 8 Ball Pool.");
        sdkDesc.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        sdkDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        sdkDesc.setPadding(0, UiStyleHelper.dp(this, 4), 0, 0);

        statusCard.addView(sdkTitle);
        statusCard.addView(sdkDesc);
        content.addView(statusCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 14), 0, UiStyleHelper.dp(this, 14)
        ));

        // 3. Action Buttons Row: Create Live Room + Join Channel / Token Setup
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView createRoomBtn = new TextView(this);
        createRoomBtn.setText("🎙️📹 + Create Voice/Video Room");
        createRoomBtn.setTextColor(Color.WHITE);
        createRoomBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        createRoomBtn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        createRoomBtn.setGravity(Gravity.CENTER);
        createRoomBtn.setMinHeight(UiStyleHelper.dp(this, 52));
        createRoomBtn.setBackground(UiStyleHelper.rippleGradientButton(
                this,
                UiStyleHelper.COLOR_NEON_PINK,
                UiStyleHelper.COLOR_ELECTRIC_VIOLET,
                16f
        ));
        createRoomBtn.setContentDescription("Create Live Voice or Video Room");
        createRoomBtn.setOnClickListener(v -> showCreateOrJoinDialog(true));

        TextView joinChannelBtn = new TextView(this);
        joinChannelBtn.setText("🔑 Join / Token Setup");
        joinChannelBtn.setTextColor(UiStyleHelper.COLOR_CYBER_CYAN);
        joinChannelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        joinChannelBtn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        joinChannelBtn.setGravity(Gravity.CENTER);
        joinChannelBtn.setMinHeight(UiStyleHelper.dp(this, 52));
        joinChannelBtn.setBackground(UiStyleHelper.rippleButton(
                this,
                UiStyleHelper.COLOR_CARD_ELEVATED,
                16f,
                UiStyleHelper.COLOR_CYBER_CYAN,
                1.2f
        ));
        joinChannelBtn.setContentDescription("Join Room by Agora Channel ID or configure token");
        joinChannelBtn.setOnClickListener(v -> showCreateOrJoinDialog(false));

        LinearLayout.LayoutParams btn1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f);
        btn1Lp.setMargins(0, 0, UiStyleHelper.dp(this, 8), 0);
        LinearLayout.LayoutParams btn2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);

        actionsRow.addView(createRoomBtn, btn1Lp);
        actionsRow.addView(joinChannelBtn, btn2Lp);
        content.addView(actionsRow);

        // 4. Category Filter Chips
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] categories = new String[]{"All", "Music", "Video", "Chill", "Gaming"};
        for (String cat : categories) {
            boolean active = cat.equalsIgnoreCase(selectedCategory);
            TextView chip = UiStyleHelper.createPillButton(
                    this,
                    active ? ("✨ " + cat) : cat,
                    active ? UiStyleHelper.COLOR_NEON_PINK : UiStyleHelper.COLOR_CARD_BG,
                    Color.WHITE,
                    v -> {
                        selectedCategory = cat;
                        showLobbyScreen();
                    }
            );
            chipRow.addView(chip, UiStyleHelper.params(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, UiStyleHelper.dp(this, 8), 0
            ));
        }
        chipScroll.addView(chipRow);
        content.addView(chipScroll, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 16), 0, UiStyleHelper.dp(this, 12)
        ));

        // 5. Section Header
        TextView sectionHeader = new TextView(this);
        sectionHeader.setText("📡 AGORA VOICE & LIVE VIDEO ROOMS");
        sectionHeader.setTextColor(Color.WHITE);
        sectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sectionHeader.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        content.addView(sectionHeader, UiStyleHelper.params(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 4), 0, UiStyleHelper.dp(this, 10)
        ));

        // 6. Room Cards List
        List<VoiceRoomModels.RoomInfo> rooms = repository.getRooms(selectedCategory);
        for (VoiceRoomModels.RoomInfo room : rooms) {
            content.addView(buildLobbyRoomCard(room), UiStyleHelper.params(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, 0, UiStyleHelper.dp(this, 14)
            ));
        }

        scroll.addView(content);
        rootContainer.addView(scroll);
    }

    private View buildLobbyRoomCard(VoiceRoomModels.RoomInfo room) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(UiStyleHelper.rippleButton(
                this,
                room.wallpaper != null ? room.wallpaper.topGradientColor : UiStyleHelper.COLOR_CARD_BG,
                18f,
                Color.argb(95, Color.red(room.accentColor), Color.green(room.accentColor), Color.blue(room.accentColor)),
                1.2f
        ));
        card.setOnClickListener(v -> enterVoiceRoom(room, false));

        LinearLayout tagsRow = new LinearLayout(this);
        tagsRow.setOrientation(LinearLayout.HORIZONTAL);
        tagsRow.setGravity(Gravity.CENTER_VERTICAL);

        boolean isVideo = room.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO;
        TextView modeTag = UiStyleHelper.createTagBadge(
                this,
                isVideo ? "📹 LIVE VIDEO" : "🎙️ 12-SEAT VOICE",
                isVideo ? Color.argb(55, 0, 229, 255) : Color.argb(55, 255, 55, 180),
                isVideo ? UiStyleHelper.COLOR_CYBER_CYAN : UiStyleHelper.COLOR_NEON_PINK
        );
        TextView coverTag = UiStyleHelper.createTagBadge(this, room.coverTagline, UiStyleHelper.COLOR_CARD_ELEVATED, Color.WHITE);
        TextView channelTag = UiStyleHelper.createTagBadge(this, "CH: " + room.channelName, UiStyleHelper.COLOR_CARD_ELEVATED, UiStyleHelper.COLOR_LIVE_GREEN);

        tagsRow.addView(modeTag);
        tagsRow.addView(coverTag, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, 0, 0));
        tagsRow.addView(channelTag, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, 0, 0));
        card.addView(tagsRow);

        TextView roomTitle = new TextView(this);
        roomTitle.setText(room.title);
        roomTitle.setTextColor(Color.WHITE);
        roomTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        roomTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        roomTitle.setPadding(0, UiStyleHelper.dp(this, 8), 0, UiStyleHelper.dp(this, 4));
        card.addView(roomTitle);

        TextView notice = new TextView(this);
        notice.setText(room.welcomeNotice);
        notice.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        notice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        notice.setMaxLines(2);
        notice.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(notice);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomRow.setPadding(0, UiStyleHelper.dp(this, 12), 0, 0);

        TextView statsText = new TextView(this);
        statsText.setText("12 Seats • Gifts • Ludo / Carrom / 8-Ball");
        statsText.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        statsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statsText.setLayoutParams(statsLp);

        TextView joinBtn = UiStyleHelper.createPillButton(
                this,
                "Enter Room →",
                room.accentColor,
                Color.BLACK,
                v -> enterVoiceRoom(room, false)
        );

        bottomRow.addView(statsText);
        bottomRow.addView(joinBtn);
        card.addView(bottomRow);

        return card;
    }

    private void enterVoiceRoom(VoiceRoomModels.RoomInfo room, boolean startOnHostSeat) {
        this.currentRoom = room;
        this.currentRoom.clearAllSeats();
        this.currentLocalSeatIndex = -1;
        this.pendingSeatIndexAfterPermission = -1;
        this.isGamesPanelVisible = false;
        this.activeSeatViews.clear();
        this.activeSeatNameViews.clear();
        this.activeSeatSubViews.clear();

        renderLiveVoiceRoomScreen();

        boolean isVideoMode = (room.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO);
        if (startOnHostSeat) {
            if (!voiceManager.hasMicPermission() || (isVideoMode && !voiceManager.hasCameraPermission())) {
                pendingSeatIndexAfterPermission = 0;
                requestRoomPermissions(isVideoMode);
                voiceManager.joinRoom(room.channelName, false, isVideoMode);
            } else {
                currentLocalSeatIndex = 0;
                voiceManager.joinRoom(room.channelName, true, isVideoMode);
            }
        } else {
            voiceManager.joinRoom(room.channelName, false, isVideoMode);
        }
    }

    private void requestRoomPermissions(boolean includeCamera) {
        if (includeCamera) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, REQ_CAMERA_AND_AUDIO);
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO || requestCode == REQ_CAMERA_AND_AUDIO) {
            boolean micOk = voiceManager.hasMicPermission();
            boolean camOk = voiceManager.hasCameraPermission();
            if (micOk) {
                appendRoomChatMessage("System", UiStyleHelper.COLOR_LIVE_GREEN, "PERM",
                        "Microphone permission granted" + (camOk ? " + Camera permission granted." : "."), true);
                if (currentRoom != null && pendingSeatIndexAfterPermission >= 0) {
                    int targetSeat = pendingSeatIndexAfterPermission;
                    pendingSeatIndexAfterPermission = -1;
                    moveLocalUserToSeat(targetSeat);
                }
                if (currentRoom != null && currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO && camOk && voiceManager.isBroadcaster()) {
                    voiceManager.setLocalVideoPublishing(true);
                    refreshVideoStageViews();
                }
            } else {
                pendingSeatIndexAfterPermission = -1;
                appendRoomChatMessage("System", UiStyleHelper.COLOR_MUTED_RED, "PERM",
                        "Microphone permission denied. Listening in Audience mode.", true);
                Toast.makeText(this, "Microphone permission is required to speak on stage.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void renderLiveVoiceRoomScreen() {
        if (currentRoom == null) return;
        rootContainer.removeAllViews();
        activeSeatViews.clear();
        activeSeatNameViews.clear();
        activeSeatSubViews.clear();

        stageFrame = new FrameLayout(this);
        applyRoomWallpaperBackground();

        ScrollView mainVerticalScroll = new ScrollView(this);
        mainVerticalScroll.setFillViewport(true);

        LinearLayout mainCol = new LinearLayout(this);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 10);
        mainCol.setPadding(pad, pad, pad, pad);

        // 1. Room Top Header Bar with Real-Time Visual Connection Status Indicator
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView roomTitleView = new TextView(this);
        roomTitleView.setText(currentRoom.title + " • [" + currentRoom.coverTagline + "]");
        roomTitleView.setTextColor(Color.WHITE);
        roomTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        roomTitleView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        roomTitleView.setSingleLine(true);
        roomTitleView.setEllipsize(TextUtils.TruncateAt.END);

        connectionStatusPill = new LinearLayout(this);
        connectionStatusPill.setOrientation(LinearLayout.HORIZONTAL);
        connectionStatusPill.setGravity(Gravity.CENTER_VERTICAL);
        connectionStatusPill.setPadding(
                UiStyleHelper.dp(this, 8),
                UiStyleHelper.dp(this, 3),
                UiStyleHelper.dp(this, 10),
                UiStyleHelper.dp(this, 3)
        );
        connectionStatusPill.setContentDescription("Real-time Agora connection status indicator. Tap for connection diagnostics.");
        connectionStatusPill.setOnClickListener(v -> showConnectionDiagnosticsDialog());

        connectionDotView = new VoiceWaveViews.ConnectionStatusDotView(this);
        int dotSize = UiStyleHelper.dp(this, 14);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotLp.setMargins(0, 0, UiStyleHelper.dp(this, 6), 0);

        connectionBadgeView = new TextView(this);
        connectionBadgeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        connectionBadgeView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        connectionBadgeView.setSingleLine(true);
        connectionBadgeView.setEllipsize(TextUtils.TruncateAt.END);

        connectionStatusPill.addView(connectionDotView, dotLp);
        connectionStatusPill.addView(connectionBadgeView);

        titleCol.addView(roomTitleView);
        titleCol.addView(connectionStatusPill, UiStyleHelper.params(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 3), 0, 0
        ));

        speakerRouteBtn = UiStyleHelper.createPillButton(
                this,
                voiceManager.isSpeakerphoneOn() ? "🔊 Speaker" : "🔈 Earpiece",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> {
                    boolean next = !voiceManager.isSpeakerphoneOn();
                    voiceManager.setSpeakerphoneEnabled(next);
                    speakerRouteBtn.setText(next ? "🔊 Speaker" : "🔈 Earpiece");
                }
        );

        TextView leaveBtn = UiStyleHelper.createPillButton(
                this,
                "✕ Leave",
                Color.argb(65, 255, 64, 96),
                UiStyleHelper.COLOR_MUTED_RED,
                v -> showLobbyScreen()
        );

        headerRow.addView(titleCol);
        headerRow.addView(speakerRouteBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 6), 0));
        headerRow.addView(leaveBtn);
        mainCol.addView(headerRow);

        // Live Diagnostic Banner
        diagnosticBannerView = new TextView(this);
        diagnosticBannerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        int diagPad = UiStyleHelper.dp(this, 8);
        diagnosticBannerView.setPadding(diagPad, UiStyleHelper.dp(this, 4), diagPad, UiStyleHelper.dp(this, 4));
        diagnosticBannerView.setOnClickListener(v -> showConnectionDiagnosticsDialog());
        mainCol.addView(diagnosticBannerView, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 5), 0, 0
        ));

        // 2. Live Video Stage (Visible when RoomMode == LIVE_VIDEO)
        videoStageContainer = new LinearLayout(this);
        videoStageContainer.setOrientation(LinearLayout.VERTICAL);
        videoStageContainer.setPadding(diagPad, diagPad, diagPad, diagPad);
        videoStageContainer.setBackground(UiStyleHelper.roundedBox(this, Color.argb(200, 14, 18, 34), 16f, UiStyleHelper.COLOR_CYBER_CYAN, 1.2f));
        videoStageContainer.setVisibility(currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO ? View.VISIBLE : View.GONE);

        LinearLayout videoHeaderRow = new LinearLayout(this);
        videoHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        videoHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView vidTitle = new TextView(this);
        vidTitle.setText("📹 AGORA LIVE VIDEO STAGE (Local + Remote Video)");
        vidTitle.setTextColor(UiStyleHelper.COLOR_CYBER_CYAN);
        vidTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        vidTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        vidTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        cameraToggleBtn = UiStyleHelper.createPillButton(
                this,
                voiceManager.isLocalVideoEnabled() ? "📹 Cam On" : "🚫 Cam Off",
                voiceManager.isLocalVideoEnabled() ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> toggleLocalCameraPublishing()
        );

        TextView switchCamBtn = UiStyleHelper.createPillButton(
                this,
                "🔄 Flip Cam",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> voiceManager.switchCameraDirection()
        );

        videoHeaderRow.addView(vidTitle);
        videoHeaderRow.addView(cameraToggleBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));
        videoHeaderRow.addView(switchCamBtn);
        videoStageContainer.addView(videoHeaderRow);

        LinearLayout videoPanesRow = new LinearLayout(this);
        videoPanesRow.setOrientation(LinearLayout.HORIZONTAL);
        videoPanesRow.setPadding(0, UiStyleHelper.dp(this, 6), 0, 0);

        localVideoHolder = new FrameLayout(this);
        localVideoHolder.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(10, 10, 18), 12f, Color.argb(80, 255, 55, 180), 1f));

        remoteVideoHolder = new FrameLayout(this);
        remoteVideoHolder.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(10, 10, 18), 12f, Color.argb(80, 0, 229, 255), 1f));

        remoteVideoPlaceholderText = new TextView(this);
        remoteVideoPlaceholderText.setText("Remote Video\n(Waiting for remote video stream)");
        remoteVideoPlaceholderText.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        remoteVideoPlaceholderText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        remoteVideoPlaceholderText.setGravity(Gravity.CENTER);
        remoteVideoHolder.addView(remoteVideoPlaceholderText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        int paneH = UiStyleHelper.dp(this, 130);
        LinearLayout.LayoutParams vp1 = new LinearLayout.LayoutParams(0, paneH, 1f);
        vp1.setMargins(0, 0, UiStyleHelper.dp(this, 6), 0);
        LinearLayout.LayoutParams vp2 = new LinearLayout.LayoutParams(0, paneH, 1f);

        videoPanesRow.addView(localVideoHolder, vp1);
        videoPanesRow.addView(remoteVideoHolder, vp2);
        videoStageContainer.addView(videoPanesRow);
        mainCol.addView(videoStageContainer, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 6), 0, 0
        ));

        // 3. Host (Seat 0) & Co-Host (Seat 1) Stage Card + Spectrum Visualizer
        LinearLayout hostStageCard = new LinearLayout(this);
        hostStageCard.setOrientation(LinearLayout.HORIZONTAL);
        hostStageCard.setGravity(Gravity.CENTER_VERTICAL);
        int stagePad = UiStyleHelper.dp(this, 8);
        hostStageCard.setPadding(stagePad, stagePad, stagePad, stagePad);
        hostStageCard.setBackground(UiStyleHelper.roundedBox(
                this,
                Color.argb(190, 24, 20, 44),
                16f,
                Color.argb(90, 255, 55, 180),
                1f
        ));

        // Build Seat 0 (Host) and Seat 1 (Co-Host) first so activeSeatViews indices 0 and 1 match!
        View hostSeatCell = buildSeatCellView(0, true);
        View coHostSeatCell = buildSeatCellView(1, true);

        hostStageCard.addView(hostSeatCell, new LinearLayout.LayoutParams(
                UiStyleHelper.dp(this, 78),
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        hostStageCard.addView(coHostSeatCell, new LinearLayout.LayoutParams(
                UiStyleHelper.dp(this, 78),
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout hostInfoCol = new LinearLayout(this);
        hostInfoCol.setOrientation(LinearLayout.VERTICAL);
        hostInfoCol.setPadding(UiStyleHelper.dp(this, 8), 0, 0, 0);
        hostInfoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        stageHeaderMetricsView = new TextView(this);
        stageHeaderMetricsView.setTextColor(UiStyleHelper.COLOR_NEON_PINK);
        stageHeaderMetricsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        stageHeaderMetricsView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        roomNoticeView = new TextView(this);
        roomNoticeView.setText(currentRoom.welcomeNotice);
        roomNoticeView.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        roomNoticeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        roomNoticeView.setMaxLines(2);
        roomNoticeView.setEllipsize(TextUtils.TruncateAt.END);
        roomNoticeView.setPadding(0, UiStyleHelper.dp(this, 2), 0, UiStyleHelper.dp(this, 4));

        liveSpectrumView = new VoiceWaveViews.LiveSpectrumView(this);
        hostInfoCol.addView(stageHeaderMetricsView);
        hostInfoCol.addView(roomNoticeView);
        hostInfoCol.addView(liveSpectrumView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiStyleHelper.dp(this, 22)
        ));

        hostStageCard.addView(hostInfoCol);
        mainCol.addView(hostStageCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 6)
        ));

        // 4. Remaining 10 Speaker Seats Grid (Seats 2..11 -> Total 12 Microphone Seats!)
        GridLayout seatsGrid = new GridLayout(this);
        seatsGrid.setColumnCount(5);
        seatsGrid.setRowCount(2);
        seatsGrid.setBackground(UiStyleHelper.roundedBox(this, Color.argb(140, 16, 16, 28), 16f, Color.argb(40, 255, 255, 255), 1f));
        int gridPad = UiStyleHelper.dp(this, 4);
        seatsGrid.setPadding(gridPad, gridPad, gridPad, gridPad);

        for (int seatIdx = 2; seatIdx < VoiceRoomModels.TOTAL_ROOM_SEATS; seatIdx++) {
            View cell = buildSeatCellView(seatIdx, false);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec((seatIdx - 2) % 5, 1f);
            glp.rowSpec = GridLayout.spec((seatIdx - 2) / 5);
            seatsGrid.addView(cell, glp);
        }
        mainCol.addView(seatsGrid, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, 0, 0, UiStyleHelper.dp(this, 6)
        ));

        // 5. Interactive Multiplayer Games Hub (Ludo 2P/4P, Carrom, 8 Ball Pool) - Toggled via Feature Bar
        roomGamesView = new RoomGamesView(this, this);
        roomGamesView.setVisibility(isGamesPanelVisible ? View.VISIBLE : View.GONE);
        mainCol.addView(roomGamesView, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, 0, 0, UiStyleHelper.dp(this, 6)
        ));

        // 6. Feature & Controls Strip (Video Toggle, Games Hub, Gifts & Wallet, Participants/Hand-Raise, Wallpaper Theme, Mic Gain, Diagnostics)
        HorizontalScrollView fxScroll = new HorizontalScrollView(this);
        fxScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout fxRow = new LinearLayout(this);
        fxRow.setOrientation(LinearLayout.HORIZONTAL);
        fxRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView giftsBtn = UiStyleHelper.createPillButton(
                this,
                "🎁 Gifts & Wallet",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                v -> RoomDialogsHelper.showGiftAndEconomyDialog(this, currentRoom, economyManager, voiceManager, repository.localUserName, this)
        );
        fxRow.addView(giftsBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        TextView gamesBtn = UiStyleHelper.createPillButton(
                this,
                isGamesPanelVisible ? "🎮 Hide Games" : "🎮 Games (Ludo/Carrom/Pool)",
                UiStyleHelper.COLOR_ELECTRIC_VIOLET,
                Color.WHITE,
                v -> {
                    isGamesPanelVisible = !isGamesPanelVisible;
                    if (roomGamesView != null) {
                        roomGamesView.setVisibility(isGamesPanelVisible ? View.VISIBLE : View.GONE);
                    }
                    ((TextView) v).setText(isGamesPanelVisible ? "🎮 Hide Games" : "🎮 Games (Ludo/Carrom/Pool)");
                }
        );
        fxRow.addView(gamesBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        TextView participantsBtn = UiStyleHelper.createPillButton(
                this,
                "👥 Seats & Hand-Raise",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_CYBER_CYAN,
                v -> RoomDialogsHelper.showParticipantsAndHandRaiseDialog(this, currentRoom, voiceManager, currentLocalSeatIndex, repository.localUserName, this)
        );
        fxRow.addView(participantsBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        TextView videoModeBtn = UiStyleHelper.createPillButton(
                this,
                currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO ? "🎙️ Switch to Voice Mode" : "📹 Enable Live Video",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> toggleRoomVideoMode()
        );
        fxRow.addView(videoModeBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        TextView themeBtn = UiStyleHelper.createPillButton(
                this,
                "🎨 Wallpaper & Cover",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> RoomDialogsHelper.showRoomDisplaySettingsDialog(this, currentRoom, voiceManager, this)
        );
        fxRow.addView(themeBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        micGainBtn = UiStyleHelper.createPillButton(
                this,
                "🎚️ Gain: " + voiceManager.getMicSignalGain() + "%",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> {
                    int current = voiceManager.getMicSignalGain();
                    int nextGain = current == 100 ? 150 : (current == 150 ? 200 : 100);
                    voiceManager.setMicSignalGain(nextGain);
                    micGainBtn.setText("🎚️ Gain: " + nextGain + "%");
                }
        );
        fxRow.addView(micGainBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        remoteAudioMuteBtn = UiStyleHelper.createPillButton(
                this,
                voiceManager.isRemoteAudioMuted() ? "🔇 Room Muted" : "🎧 Room Audio On",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                voiceManager.isRemoteAudioMuted() ? UiStyleHelper.COLOR_MUTED_RED : UiStyleHelper.COLOR_CYBER_CYAN,
                v -> {
                    boolean nextMute = !voiceManager.isRemoteAudioMuted();
                    voiceManager.setAllRemoteAudioMuted(nextMute);
                    remoteAudioMuteBtn.setText(nextMute ? "🔇 Room Muted" : "🎧 Room Audio On");
                    remoteAudioMuteBtn.setTextColor(nextMute ? UiStyleHelper.COLOR_MUTED_RED : UiStyleHelper.COLOR_CYBER_CYAN);
                }
        );
        fxRow.addView(remoteAudioMuteBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        TextView modBtn = UiStyleHelper.createPillButton(
                this,
                "🛡️ Report / Mod",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> RoomDialogsHelper.showModerationLogDialog(this, currentRoom)
        );
        fxRow.addView(modBtn);

        fxScroll.addView(fxRow);
        mainCol.addView(fxScroll, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, 0, 0, UiStyleHelper.dp(this, 6)
        ));

        // 7. Live Room Chat & Real-Time Agora Event Log
        chatScrollView = new ScrollView(this);
        chatScrollView.setBackground(UiStyleHelper.roundedBox(this, Color.argb(150, 12, 12, 22), 14f, Color.argb(35, 255, 255, 255), 1f));
        int chatPad = UiStyleHelper.dp(this, 8);
        chatScrollView.setPadding(chatPad, chatPad, chatPad, chatPad);

        chatMessagesContainer = new LinearLayout(this);
        chatMessagesContainer.setOrientation(LinearLayout.VERTICAL);
        chatScrollView.addView(chatMessagesContainer);

        for (VoiceRoomModels.ChatMessage msg : currentRoom.messages) {
            addChatBubbleView(msg);
        }
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));

        LinearLayout.LayoutParams chatLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiStyleHelper.dp(this, 150),
                1f
        );
        chatLp.setMargins(0, 0, 0, UiStyleHelper.dp(this, 6));
        mainCol.addView(chatScrollView, chatLp);

        // 8. Bottom Controls & Chat Input Dock
        LinearLayout bottomDock = new LinearLayout(this);
        bottomDock.setOrientation(LinearLayout.VERTICAL);

        LinearLayout controlRow = new LinearLayout(this);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);
        controlRow.setGravity(Gravity.CENTER_VERTICAL);

        micToggleBtn = UiStyleHelper.createPillButton(
                this,
                getMicButtonText(),
                getMicButtonColor(),
                Color.WHITE,
                v -> onMicToggleClicked()
        );

        seatActionBtn = UiStyleHelper.createPillButton(
                this,
                currentLocalSeatIndex >= 0 ? "⬇ Step Down" : "🎙️ Take Mic",
                currentLocalSeatIndex >= 0 ? UiStyleHelper.COLOR_CARD_ELEVATED : UiStyleHelper.COLOR_ELECTRIC_VIOLET,
                Color.WHITE,
                v -> onSeatActionClicked()
        );

        TextView handRaiseBtn = UiStyleHelper.createPillButton(
                this,
                "✋",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> RoomDialogsHelper.showParticipantsAndHandRaiseDialog(this, currentRoom, voiceManager, currentLocalSeatIndex, repository.localUserName, this)
        );

        controlRow.addView(micToggleBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));
        controlRow.addView(seatActionBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));
        controlRow.addView(handRaiseBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));

        String[] quickEmojis = new String[]{"🔥", "💖", "🎉", "👑"};
        for (String emoji : quickEmojis) {
            TextView emojiBtn = UiStyleHelper.createPillButton(
                    this,
                    emoji,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    Color.WHITE,
                    v -> {
                        if (reactionsOverlay != null) {
                            reactionsOverlay.spawnReaction(emoji);
                        }
                        try {
                            JSONObject pkt = new JSONObject();
                            pkt.put("type", "REACTION");
                            pkt.put("emoji", emoji);
                            voiceManager.broadcastDataPacket(pkt);
                        } catch (Throwable ignored) {
                        }
                    }
            );
            emojiBtn.setPadding(UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 8), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 8));
            controlRow.addView(emojiBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 4), 0));
        }

        bottomDock.addView(controlRow, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 6)));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText chatInput = new EditText(this);
        chatInput.setHint("Send real-time room chat…");
        chatInput.setHintTextColor(Color.argb(130, 200, 200, 230));
        chatInput.setTextColor(Color.WHITE);
        chatInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        chatInput.setSingleLine(true);
        chatInput.setPadding(UiStyleHelper.dp(this, 14), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 14), UiStyleHelper.dp(this, 10));
        chatInput.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_ELEVATED, 22f, Color.argb(50, 255, 255, 255), 1f));

        TextView sendBtn = UiStyleHelper.createPillButton(
                this,
                "Send",
                UiStyleHelper.COLOR_NEON_PINK,
                Color.WHITE,
                v -> {
                    String msg = chatInput.getText().toString().trim();
                    if (!msg.isEmpty()) {
                        chatInput.setText("");
                        String roleBadge = currentLocalSeatIndex == 0 ? "HOST"
                                : (currentLocalSeatIndex == 1 ? "CO-HOST"
                                : (currentLocalSeatIndex > 1 ? "SPEAKER" : "AUDIENCE"));
                        appendRoomChatMessage(
                                repository.localUserName,
                                repository.localAvatarColor,
                                roleBadge,
                                msg,
                                false
                        );
                        try {
                            JSONObject pkt = new JSONObject();
                            pkt.put("type", "CHAT");
                            pkt.put("sender", repository.localUserName);
                            pkt.put("role", roleBadge);
                            pkt.put("text", msg);
                            voiceManager.broadcastDataPacket(pkt);
                        } catch (Throwable ignored) {
                        }
                    }
                }
        );

        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputLp.setMargins(0, 0, UiStyleHelper.dp(this, 8), 0);
        inputRow.addView(chatInput, inputLp);
        inputRow.addView(sendBtn);
        bottomDock.addView(inputRow);

        mainCol.addView(bottomDock);
        mainVerticalScroll.addView(mainCol);
        stageFrame.addView(mainVerticalScroll);

        reactionsOverlay = new VoiceWaveViews.FloatingReactionsView(this);
        reactionsOverlay.setClickable(false);
        reactionsOverlay.setFocusable(false);
        stageFrame.addView(reactionsOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        rootContainer.addView(stageFrame);

        updateConnectionStatusUI(
                voiceManager.getCurrentStatus(),
                voiceManager.getCurrentStatusText(),
                voiceManager.getLastRealRttMs(),
                voiceManager.getLastDiagnosticDetail()
        );
        refreshVideoStageViews();
    }

    private void applyRoomWallpaperBackground() {
        if (stageFrame == null || currentRoom == null) return;
        VoiceRoomModels.WallpaperTheme wp = currentRoom.wallpaper != null
                ? currentRoom.wallpaper
                : VoiceRoomModels.WALLPAPER_THEMES[0];
        stageFrame.setBackground(UiStyleHelper.gradientBox(
                this,
                wp.topGradientColor,
                wp.bottomGradientColor,
                0f, 0, 0
        ));
    }

    private void toggleRoomVideoMode() {
        if (currentRoom == null) return;
        boolean nextVideo = (currentRoom.mode != VoiceRoomModels.RoomMode.LIVE_VIDEO);
        currentRoom.mode = nextVideo ? VoiceRoomModels.RoomMode.LIVE_VIDEO : VoiceRoomModels.RoomMode.VOICE_ROOM;
        if (nextVideo && !voiceManager.hasCameraPermission()) {
            requestRoomPermissions(true);
        }
        renderLiveVoiceRoomScreen();
        if (nextVideo && voiceManager.isBroadcaster() && voiceManager.hasCameraPermission()) {
            voiceManager.setLocalVideoPublishing(true);
            refreshVideoStageViews();
        }
    }

    private void toggleLocalCameraPublishing() {
        if (currentRoom == null) return;
        if (!voiceManager.hasCameraPermission() || !voiceManager.hasMicPermission()) {
            requestRoomPermissions(true);
            return;
        }
        if (!voiceManager.isBroadcaster()) {
            Toast.makeText(this, "Take a microphone/video seat first to publish camera video.", Toast.LENGTH_SHORT).show();
            onSeatActionClicked();
            return;
        }
        boolean nextCam = !voiceManager.isLocalVideoEnabled();
        voiceManager.setLocalVideoPublishing(nextCam);
        refreshVideoStageViews();
    }

    private void refreshVideoStageViews() {
        if (currentRoom == null || localVideoHolder == null || remoteVideoHolder == null) return;
        if (cameraToggleBtn != null) {
            boolean camOn = voiceManager.isLocalVideoEnabled();
            cameraToggleBtn.setText(camOn ? "📹 Cam On" : "🚫 Cam Off");
            cameraToggleBtn.setBackground(UiStyleHelper.rippleButton(
                    this,
                    camOn ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_CARD_ELEVATED,
                    24f, Color.argb(55, 255, 255, 255), 1f
            ));
        }
        localVideoHolder.removeAllViews();
        if (voiceManager.isLocalVideoEnabled() && voiceManager.hasCameraPermission()) {
            SurfaceView localSv = voiceManager.createAndBindLocalVideoView(this);
            if (localSv != null) {
                localVideoHolder.addView(localSv, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
            }
        } else {
            TextView localOff = new TextView(this);
            localOff.setText("Local Camera Off\n(Tap '📹 Cam On' while seated as Broadcaster)");
            localOff.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            localOff.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            localOff.setGravity(Gravity.CENTER);
            localVideoHolder.addView(localOff, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        List<Integer> remotes = voiceManager.getRemoteUidList();
        remoteVideoHolder.removeAllViews();
        if (!remotes.isEmpty()) {
            int firstRemoteUid = remotes.get(0);
            SurfaceView remoteSv = voiceManager.createAndBindRemoteVideoView(this, firstRemoteUid);
            if (remoteSv != null) {
                remoteVideoHolder.addView(remoteSv, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                return;
            }
        }
        remoteVideoHolder.addView(remoteVideoPlaceholderText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private void updateStageHeaderMetrics() {
        if (stageHeaderMetricsView == null || currentRoom == null) return;
        boolean connected = voiceManager.isChannelConnected();
        int totalOnline = (connected ? 1 : 0) + voiceManager.getRemoteUserCount();
        stageHeaderMetricsView.setText("👑 CH: " + currentRoom.channelName
                + " • " + currentRoom.getActiveSpeakerCount() + "/12 SEATS • "
                + (connected ? (totalOnline + " ONLINE") : "OFFLINE"));
    }

    private void updateConnectionStatusUI(
            AgoraVoiceManager.ConnectionStatus status,
            String statusText,
            int realRttMs,
            String diagnosticDetail
    ) {
        if (connectionDotView == null || connectionBadgeView == null || connectionStatusPill == null) {
            return;
        }
        connectionDotView.setConnectionStatus(status);
        int statusColor = VoiceWaveViews.ConnectionStatusDotView.getStatusColor(status);

        String pingSuffix = (realRttMs > 0 && status == AgoraVoiceManager.ConnectionStatus.CONNECTED)
                ? (" • " + realRttMs + " ms")
                : "";
        connectionBadgeView.setText(statusText + pingSuffix);
        connectionBadgeView.setTextColor(statusColor);

        int r = Color.red(statusColor);
        int g = Color.green(statusColor);
        int b = Color.blue(statusColor);
        connectionStatusPill.setBackground(UiStyleHelper.rippleButton(
                this,
                Color.argb(38, r, g, b),
                14f,
                Color.argb(110, r, g, b),
                1f
        ));

        if (diagnosticBannerView != null) {
            if (status == AgoraVoiceManager.ConnectionStatus.CONNECTED) {
                diagnosticBannerView.setText("🟢 Agora Connected: " + diagnosticDetail + " (Tap for RTC Diagnostics)");
                diagnosticBannerView.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
                diagnosticBannerView.setBackground(UiStyleHelper.roundedBox(this, Color.argb(30, 0, 230, 118), 10f, Color.argb(80, 0, 230, 118), 1f));
            } else if (status == AgoraVoiceManager.ConnectionStatus.CONNECTING) {
                diagnosticBannerView.setText("🟡 " + diagnosticDetail);
                diagnosticBannerView.setTextColor(Color.rgb(255, 215, 64));
                diagnosticBannerView.setBackground(UiStyleHelper.roundedBox(this, Color.argb(30, 255, 215, 64), 10f, Color.argb(80, 255, 215, 64), 1f));
            } else {
                diagnosticBannerView.setText("🔴 " + diagnosticDetail + " [Tap to configure Channel/Token or Retry]");
                diagnosticBannerView.setTextColor(Color.rgb(255, 110, 130));
                diagnosticBannerView.setBackground(UiStyleHelper.roundedBox(this, Color.argb(40, 255, 64, 96), 10f, Color.argb(110, 255, 64, 96), 1f));
            }
        }
        updateStageHeaderMetrics();
    }

    private void showConnectionDiagnosticsDialog() {
        if (currentRoom == null) return;
        AgoraVoiceManager.ConnectionStatus status = voiceManager.getCurrentStatus();
        String stateBadge;
        if (status == AgoraVoiceManager.ConnectionStatus.CONNECTED) {
            stateBadge = "🟢 CONNECTED (Confirmed by Agora SDK)";
        } else if (status == AgoraVoiceManager.ConnectionStatus.CONNECTING) {
            stateBadge = "🟡 CONNECTING / RECONNECTING";
        } else {
            stateBadge = "🔴 DISCONNECTED / FAILED";
        }

        int rtt = voiceManager.getLastRealRttMs();
        String rttStr = (rtt > 0 && status == AgoraVoiceManager.ConnectionStatus.CONNECTED)
                ? (rtt + " ms (Real Agora RtcStats)")
                : "Not available (Requires active connection)";

        String details = "Connection State: " + stateBadge
                + "\nStatus Label: " + voiceManager.getCurrentStatusText()
                + "\nDiagnostic Detail: " + voiceManager.getLastDiagnosticDetail()
                + "\n\nAgora SDK Version: " + voiceManager.getSdkVersionString()
                + "\nApp ID: " + AgoraVoiceManager.getConfiguredAppIdPreview()
                + "\nChannel Name: " + currentRoom.channelName
                + "\nRoom Mode: " + (currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO ? "Live Video + Voice" : "12-Seat Voice Room")
                + "\nAssigned Local UID: " + (voiceManager.getAssignedLocalUid() != 0 ? voiceManager.getAssignedLocalUid() : "0 (Pending Join)")
                + "\nRemote Users in Channel: " + voiceManager.getRemoteUserCount()
                + "\nClient Role: " + (voiceManager.isBroadcaster() ? "BROADCASTER (Publishing)" : "AUDIENCE (Listener)")
                + "\nMic / Cam Permissions: Mic=" + (voiceManager.hasMicPermission() ? "Granted" : "Denied")
                + ", Cam=" + (voiceManager.hasCameraPermission() ? "Granted" : "Denied")
                + "\nInternet Connectivity: " + (voiceManager.hasInternetConnection() ? "Online" : "Offline")
                + "\nAudio Route: " + (voiceManager.isSpeakerphoneOn() ? "Speakerphone" : "Earpiece")
                + "\nReal Latency (Last-Mile RTT): " + rttStr
                + "\nMedia Bitrate: TX " + voiceManager.getLastTxKbps() + " kbps / RX " + voiceManager.getLastRxKbps() + " kbps";

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("📡 Agora RTC Diagnostics")
                .setMessage(details)
                .setPositiveButton("Reconnect RTC", (d, w) -> {
                    if (currentRoom != null) {
                        voiceManager.joinRoom(
                                currentRoom.channelName,
                                currentLocalSeatIndex >= 0,
                                currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO
                        );
                    }
                })
                .setNeutralButton("Channel / Token Setup", (d, w) -> showChannelAndTokenOverrideDialog())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showChannelAndTokenOverrideDialog() {
        if (currentRoom == null) return;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 20);
        form.setPadding(pad, pad, pad, pad);

        TextView helpText = new TextView(this);
        helpText.setText("If your Agora project has Primary Certificate enabled, ensure the Channel Name matches the exact channel name your RTC Token was generated for (with UID=0).");
        helpText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        helpText.setPadding(0, 0, 0, UiStyleHelper.dp(this, 10));
        form.addView(helpText);

        final EditText channelInput = new EditText(this);
        channelInput.setHint("Exact Agora Channel Name");
        channelInput.setSingleLine(true);
        channelInput.setText(currentRoom.channelName);
        form.addView(channelInput);

        final EditText tokenInput = new EditText(this);
        tokenInput.setHint("Optional Temp Token (leave blank to use .env or App-ID mode)");
        tokenInput.setSingleLine(true);
        if (voiceManager.getSessionOverrideToken() != null) {
            tokenInput.setText(voiceManager.getSessionOverrideToken());
        }
        form.addView(tokenInput);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔑 Configure Channel & RTC Token")
                .setView(form)
                .setPositiveButton("Apply & Rejoin", (d, w) -> {
                    String newChannel = AgoraVoiceManager.sanitizeChannelName(channelInput.getText().toString());
                    if (!newChannel.isEmpty()) {
                        currentRoom.channelName = newChannel;
                    }
                    voiceManager.setSessionOverrideToken(tokenInput.getText().toString());
                    currentRoom.clearAllSeats();
                    refreshAllSeatsUI();
                    voiceManager.joinRoom(
                            currentRoom.channelName,
                            currentLocalSeatIndex >= 0,
                            currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private View buildSeatCellView(final int seatIndex, boolean isStageTopCell) {
        final VoiceRoomModels.SpeakerSeat seat = currentRoom.seats.get(seatIndex);
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiStyleHelper.dp(this, 3);
        cell.setPadding(pad, pad, pad, pad);
        cell.setMinimumHeight(UiStyleHelper.dp(this, 48));
        cell.setBackground(UiStyleHelper.rippleButton(this, Color.TRANSPARENT, 10f, 0, 0));

        VoiceWaveViews.SeatAvatarView avatarView = new VoiceWaveViews.SeatAvatarView(this);
        boolean isLocal = seat.isOccupied && seatIndex == currentLocalSeatIndex;
        avatarView.bindSeat(seat, isLocal);

        int avatarSize = UiStyleHelper.dp(this, isStageTopCell ? 56 : 48);
        cell.addView(avatarView, new LinearLayout.LayoutParams(avatarSize, avatarSize));

        TextView nameView = new TextView(this);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        nameView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setGravity(Gravity.CENTER);

        TextView subView = new TextView(this);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        subView.setSingleLine(true);
        subView.setGravity(Gravity.CENTER);

        activeSeatViews.add(avatarView);
        activeSeatNameViews.add(nameView);
        activeSeatSubViews.add(subView);
        updateSingleSeatLabels(seatIndex);

        cell.addView(nameView);
        cell.addView(subView);

        cell.setOnClickListener(v -> onSeatCellTapped(seatIndex));
        return cell;
    }

    private void updateSingleSeatLabels(int seatIndex) {
        if (currentRoom == null || seatIndex >= activeSeatNameViews.size()) return;
        VoiceRoomModels.SpeakerSeat seat = currentRoom.seats.get(seatIndex);
        boolean isLocal = seat.isOccupied && seatIndex == currentLocalSeatIndex;

        TextView nameView = activeSeatNameViews.get(seatIndex);
        TextView subView = activeSeatSubViews.get(seatIndex);
        activeSeatViews.get(seatIndex).bindSeat(seat, isLocal);

        if (seat.isOccupied) {
            nameView.setText(isLocal ? (repository.localUserName + " (You)") : seat.userName);
            nameView.setTextColor(isLocal ? UiStyleHelper.COLOR_LIVE_GREEN : Color.WHITE);
            subView.setText(seat.isMuted ? "🔇 Muted" : ("🎙️ " + seat.roleTag));
            subView.setTextColor(seat.isMuted ? UiStyleHelper.COLOR_MUTED_RED : UiStyleHelper.COLOR_CYBER_CYAN);
        } else if (seat.isLocked) {
            nameView.setText(VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(seatIndex));
            nameView.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            subView.setText("🔒 Locked");
            subView.setTextColor(UiStyleHelper.COLOR_MUTED_RED);
        } else {
            nameView.setText(VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(seatIndex));
            nameView.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            subView.setText("Open Mic");
            subView.setTextColor(Color.argb(160, 180, 185, 220));
        }
    }

    private void refreshAllSeatsUI() {
        if (currentRoom == null) return;
        for (int i = 0; i < currentRoom.seats.size() && i < activeSeatViews.size(); i++) {
            updateSingleSeatLabels(i);
        }
        if (micToggleBtn != null) {
            micToggleBtn.setText(getMicButtonText());
            micToggleBtn.setBackground(UiStyleHelper.rippleButton(this, getMicButtonColor(), 24f, Color.argb(55, 255, 255, 255), 1f));
        }
        if (seatActionBtn != null) {
            seatActionBtn.setText(currentLocalSeatIndex >= 0 ? "⬇ Step Down" : "🎙️ Take Mic");
            seatActionBtn.setBackground(UiStyleHelper.rippleButton(
                    this,
                    currentLocalSeatIndex >= 0 ? UiStyleHelper.COLOR_CARD_ELEVATED : UiStyleHelper.COLOR_ELECTRIC_VIOLET,
                    24f, Color.argb(55, 255, 255, 255), 1f
            ));
        }
        updateStageHeaderMetrics();
    }

    private void onSeatCellTapped(int seatIndex) {
        if (currentRoom == null) return;
        VoiceRoomModels.SpeakerSeat seat = currentRoom.seats.get(seatIndex);

        if (!seat.isOccupied) {
            String[] emptySeatOptions = new String[]{
                    "🎙️ Take Seat " + seatIndex + " (" + VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(seatIndex) + ")",
                    seat.isLocked ? "🔓 Unlock Seat " + seatIndex : "🔒 Lock Seat " + seatIndex,
                    "✋ Raise Hand for Seat " + seatIndex
            };
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(seatIndex) + " Controls")
                    .setItems(emptySeatOptions, (d, which) -> {
                        if (which == 0) {
                            if (seat.isLocked) {
                                Toast.makeText(this, "This seat is locked! Unlock it first.", Toast.LENGTH_SHORT).show();
                            } else {
                                moveLocalUserToSeat(seatIndex);
                            }
                        } else if (which == 1) {
                            seat.isLocked = !seat.isLocked;
                            updateSingleSeatLabels(seatIndex);
                        } else if (which == 2) {
                            currentRoom.handRaises.add(0, new VoiceRoomModels.HandRaiseRequest(
                                    voiceManager.getAssignedLocalUid(), repository.localUserName, seatIndex, "RAISED"
                            ));
                            appendRoomChatMessage("System", UiStyleHelper.COLOR_NEON_PINK, "QUEUE",
                                    repository.localUserName + " raised hand for Seat " + seatIndex + ".", true);
                        }
                    })
                    .show();
        } else if (seatIndex == currentLocalSeatIndex) {
            String[] options = new String[]{
                    seat.isMuted ? "🎙️ Unmute Microphone" : "🔇 Mute Microphone",
                    "👑 Toggle Role Tag (HOST / CO-HOST / SPEAKER)",
                    "⬇ Step Down to Audience (Listener)"
            };
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Your Mic Seat (UID " + seat.uid + ")")
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            onMicToggleClicked();
                        } else if (which == 1) {
                            seat.roleTag = "HOST".equals(seat.roleTag) ? "CO-HOST" : ("CO-HOST".equals(seat.roleTag) ? "SPEAKER" : "HOST");
                            updateSingleSeatLabels(seatIndex);
                        } else if (which == 2) {
                            stepDownFromSeat();
                        }
                    })
                    .show();
        } else {
            String[] options = new String[]{
                    seat.isMuted ? ("🔊 Unmute Remote UID " + seat.uid) : ("🔇 Mute Remote UID " + seat.uid),
                    "👑 Promote to Co-Host",
                    "🚫 Remove from Seat (Move to Listeners)",
                    "🚩 Report Participant"
            };
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(seat.userName + " (UID " + seat.uid + ")")
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            onSeatPermissionAction(seat.isMuted ? "UNMUTE_SEAT" : "MUTE_SEAT", seatIndex, seat.uid, seat.userName);
                        } else if (which == 1) {
                            seat.roleTag = "CO-HOST";
                            updateSingleSeatLabels(seatIndex);
                        } else if (which == 2) {
                            onSeatPermissionAction("REMOVE_FROM_SEAT", seatIndex, seat.uid, seat.userName);
                        } else if (which == 3) {
                            RoomDialogsHelper.showModerationLogDialog(this, currentRoom);
                        }
                    })
                    .show();
        }
    }

    private void moveLocalUserToSeat(int targetSeatIndex) {
        if (currentRoom == null) return;

        boolean isVideoMode = (currentRoom.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO);
        if (!voiceManager.hasMicPermission() || (isVideoMode && !voiceManager.hasCameraPermission())) {
            pendingSeatIndexAfterPermission = targetSeatIndex;
            requestRoomPermissions(isVideoMode);
            return;
        }

        if (!voiceManager.isChannelConnected()) {
            currentLocalSeatIndex = targetSeatIndex;
            Toast.makeText(this, "Connecting to Agora channel first…", Toast.LENGTH_SHORT).show();
            voiceManager.joinRoom(currentRoom.channelName, true, isVideoMode);
            return;
        }

        if (currentLocalSeatIndex >= 0 && currentLocalSeatIndex < currentRoom.seats.size()) {
            currentRoom.seats.get(currentLocalSeatIndex).vacate();
        }

        boolean switched = voiceManager.switchRole(true);
        if (switched) {
            VoiceRoomModels.SpeakerSeat target = currentRoom.seats.get(targetSeatIndex);
            target.assignUser(
                    voiceManager.getAssignedLocalUid(),
                    repository.localUserName,
                    repository.localAvatarColor,
                    false,
                    VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(targetSeatIndex)
            );
            currentLocalSeatIndex = targetSeatIndex;
            refreshAllSeatsUI();
        }
    }

    private void stepDownFromSeat() {
        if (currentRoom == null || currentLocalSeatIndex < 0) return;
        int prevSeat = currentLocalSeatIndex;
        currentRoom.seats.get(prevSeat).vacate();
        currentLocalSeatIndex = -1;
        voiceManager.switchRole(false);
        refreshAllSeatsUI();
        refreshVideoStageViews();
    }

    private void onMicToggleClicked() {
        if (currentRoom == null) return;
        if (currentLocalSeatIndex < 0 || !voiceManager.isBroadcaster()) {
            onSeatActionClicked();
            return;
        }
        VoiceRoomModels.SpeakerSeat mySeat = currentRoom.seats.get(currentLocalSeatIndex);
        boolean nextMuted = !mySeat.isMuted;
        mySeat.isMuted = nextMuted;
        if (nextMuted) mySeat.speakingVolume = 0;
        voiceManager.setLocalMuted(nextMuted);
        refreshAllSeatsUI();
    }

    private void onSeatActionClicked() {
        if (currentRoom == null) return;
        if (currentLocalSeatIndex >= 0) {
            stepDownFromSeat();
        } else {
            for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
                if (!seat.isOccupied && !seat.isLocked) {
                    moveLocalUserToSeat(seat.seatIndex);
                    return;
                }
            }
            Toast.makeText(this, "All 12 mic seats are currently occupied or locked!", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMicButtonText() {
        if (currentLocalSeatIndex < 0 || !voiceManager.isBroadcaster()) return "🎧 Listener Mode";
        VoiceRoomModels.SpeakerSeat mySeat = currentRoom.seats.get(currentLocalSeatIndex);
        return mySeat.isMuted ? "🔇 Unmute Mic" : "🎙️ Mic Live";
    }

    private int getMicButtonColor() {
        if (currentLocalSeatIndex < 0 || !voiceManager.isBroadcaster()) return UiStyleHelper.COLOR_CARD_ELEVATED;
        VoiceRoomModels.SpeakerSeat mySeat = currentRoom.seats.get(currentLocalSeatIndex);
        return mySeat.isMuted ? UiStyleHelper.COLOR_MUTED_RED : UiStyleHelper.COLOR_LIVE_GREEN;
    }

    private void appendRoomChatMessage(String sender, int color, String badge, String text, boolean isSystem) {
        if (currentRoom == null) return;
        VoiceRoomModels.ChatMessage msg = new VoiceRoomModels.ChatMessage(sender, color, badge, text, isSystem);
        currentRoom.messages.add(msg);
        if (chatMessagesContainer != null) {
            addChatBubbleView(msg);
            if (chatScrollView != null) {
                chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
            }
        }
    }

    private void addChatBubbleView(VoiceRoomModels.ChatMessage msg) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiStyleHelper.dp(this, 8), UiStyleHelper.dp(this, 4), UiStyleHelper.dp(this, 8), UiStyleHelper.dp(this, 4));
        row.setBackground(UiStyleHelper.roundedBox(
                this,
                msg.isSystem ? Color.argb(55, 0, 229, 255) : Color.argb(120, 26, 26, 44),
                10f, 0, 0
        ));

        TextView badge = UiStyleHelper.createTagBadge(this, msg.roleBadge, msg.senderColor, Color.BLACK);
        TextView content = new TextView(this);
        content.setText(msg.isSystem ? msg.message : (msg.senderName + ": " + msg.message));
        content.setTextColor(msg.isSystem ? UiStyleHelper.COLOR_CYBER_CYAN : Color.WHITE);
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        content.setPadding(UiStyleHelper.dp(this, 8), 0, 0, 0);

        row.addView(badge);
        row.addView(content);
        chatMessagesContainer.addView(row, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, 0, 0, UiStyleHelper.dp(this, 4)
        ));
    }

    private void showCreateOrJoinDialog(boolean isCreateMode) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 20);
        form.setPadding(pad, pad, pad, pad);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Room Title (e.g. QLUVX Live Stage)");
        titleInput.setSingleLine(true);

        final EditText channelInput = new EditText(this);
        channelInput.setHint("Exact Agora Channel Name");
        channelInput.setSingleLine(true);
        channelInput.setText(AgoraVoiceManager.getDefaultChannelName());

        final EditText tokenInput = new EditText(this);
        tokenInput.setHint("Optional Temp Token (if not set in Secrets)");
        tokenInput.setSingleLine(true);
        if (voiceManager.getSessionOverrideToken() != null) {
            tokenInput.setText(voiceManager.getSessionOverrideToken());
        }

        if (isCreateMode) {
            form.addView(titleInput);
        }
        form.addView(channelInput);
        form.addView(tokenInput);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(isCreateMode ? "🎙️📹 Create Agora Voice or Video Room" : "🔑 Join Agora Channel / Token")
                .setView(form)
                .setPositiveButton(isCreateMode ? "🎙️ Create 12-Seat Voice" : "Join Voice Channel", (d, w) -> {
                    String title = isCreateMode ? titleInput.getText().toString() : "";
                    String channel = channelInput.getText().toString();
                    voiceManager.setSessionOverrideToken(tokenInput.getText().toString());
                    VoiceRoomModels.RoomInfo room = repository.createOrFindRoom(title, channel, "Music", VoiceRoomModels.RoomMode.VOICE_ROOM);
                    enterVoiceRoom(room, isCreateMode);
                })
                .setNeutralButton("📹 Join as Live Video", (d, w) -> {
                    String title = isCreateMode ? titleInput.getText().toString() : "";
                    String channel = channelInput.getText().toString();
                    voiceManager.setSessionOverrideToken(tokenInput.getText().toString());
                    VoiceRoomModels.RoomInfo room = repository.createOrFindRoom(title, channel, "Video", VoiceRoomModels.RoomMode.LIVE_VIDEO);
                    enterVoiceRoom(room, isCreateMode);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditProfileDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 20);
        form.setPadding(pad, pad, pad, pad);

        final EditText nameInput = new EditText(this);
        nameInput.setText(repository.localUserName);
        nameInput.setHint("Enter your display name");
        nameInput.setSingleLine(true);
        form.addView(nameInput);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("👤 Your QLUVX Profile")
                .setView(form)
                .setPositiveButton("Save", (d, w) -> {
                    String nextName = nameInput.getText().toString().trim();
                    if (!nextName.isEmpty()) {
                        repository.localUserName = nextName;
                        showLobbyScreen();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveCurrentRoomInternal() {
        if (currentRoom != null) {
            currentRoom.clearAllSeats();
            currentRoom = null;
            currentLocalSeatIndex = -1;
            pendingSeatIndexAfterPermission = -1;
        }
        voiceManager.leaveVoiceRoom();
    }

    @Override
    public void onBackPressed() {
        if (currentRoom != null) {
            showLobbyScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animHandler.removeCallbacks(stageAnimationRunnable);
        if (voiceManager != null) {
            voiceManager.destroy();
        }
    }

    // --- RoomDialogsHelper.RoomActionCallbacks ---

    @Override
    public void onGiftSentToRoom(EconomyManager.GiftItem gift, String recipientName, String validationMode) {
        if (reactionsOverlay != null) {
            reactionsOverlay.spawnReaction(gift.emoji);
            reactionsOverlay.spawnReaction("✨");
        }
        appendRoomChatMessage(
                repository.localUserName,
                Color.rgb(255, 215, 64),
                "GIFT",
                "sent " + gift.emoji + " " + gift.name + " (" + gift.priceCoins + " Coins) to " + recipientName + " [" + validationMode + "]",
                false
        );
    }

    @Override
    public void onRoomDisplayUpdated() {
        if (currentRoom == null) return;
        renderLiveVoiceRoomScreen();
    }

    @Override
    public void onSeatPermissionAction(String actionType, int targetSeatIndex, int targetUid, String targetName) {
        if (currentRoom == null) return;
        if ("ACCEPT_HAND_RAISE".equals(actionType) || "INVITE_TO_MIC".equals(actionType)) {
            if (targetUid == voiceManager.getAssignedLocalUid()) {
                moveLocalUserToSeat(targetSeatIndex >= 0 ? targetSeatIndex : 2);
            } else {
                for (int i = 2; i < currentRoom.seats.size(); i++) {
                    VoiceRoomModels.SpeakerSeat s = currentRoom.seats.get(i);
                    if (!s.isOccupied && !s.isLocked) {
                        s.assignUser(targetUid, targetName, UiStyleHelper.COLOR_CYBER_CYAN, false, "SPEAKER");
                        break;
                    }
                }
            }
            appendRoomChatMessage("Host", UiStyleHelper.COLOR_LIVE_GREEN, "SEAT",
                    "Approved/Invited " + targetName + " (UID " + targetUid + ") to microphone stage.", true);
        } else if ("REJECT_HAND_RAISE".equals(actionType)) {
            appendRoomChatMessage("Host", UiStyleHelper.COLOR_MUTED_RED, "SEAT",
                    "Declined hand-raise request from " + targetName + ".", true);
        } else if ("MUTE_SEAT".equals(actionType) || "UNMUTE_SEAT".equals(actionType)) {
            boolean mute = "MUTE_SEAT".equals(actionType);
            if (targetSeatIndex >= 0 && targetSeatIndex < currentRoom.seats.size()) {
                VoiceRoomModels.SpeakerSeat s = currentRoom.seats.get(targetSeatIndex);
                s.isMuted = mute;
                if (mute) s.speakingVolume = 0;
            }
            if (targetUid == voiceManager.getAssignedLocalUid()) {
                voiceManager.setLocalMuted(mute);
            } else {
                voiceManager.setRemoteUserMuted(targetUid, mute);
            }
        } else if ("REMOVE_FROM_SEAT".equals(actionType)) {
            if (targetSeatIndex == currentLocalSeatIndex) {
                stepDownFromSeat();
            } else if (targetSeatIndex >= 0 && targetSeatIndex < currentRoom.seats.size()) {
                currentRoom.seats.get(targetSeatIndex).vacate();
            }
            appendRoomChatMessage("Host", UiStyleHelper.COLOR_MUTED_RED, "SEAT",
                    "Removed " + targetName + " from Seat " + targetSeatIndex + " to Audience.", true);
        }
        try {
            JSONObject pkt = new JSONObject();
            pkt.put("type", "SEAT_CONTROL");
            pkt.put("action", actionType);
            pkt.put("seatIndex", targetSeatIndex);
            pkt.put("targetUid", targetUid);
            pkt.put("targetName", targetName);
            voiceManager.broadcastDataPacket(pkt);
        } catch (Throwable ignored) {
        }
        refreshAllSeatsUI();
    }

    @Override
    public void onModerationActionLogged(String logEntry) {
        appendRoomChatMessage("Room", UiStyleHelper.COLOR_NEON_PINK, "EVENT", logEntry, true);
    }

    // --- RoomGamesView.GameSyncBridge ---

    @Override
    public boolean sendGameActionOverAgora(JSONObject actionJson) {
        return voiceManager != null && voiceManager.broadcastDataPacket(actionJson);
    }

    @Override
    public boolean isAgoraChannelConnected() {
        return voiceManager != null && voiceManager.isChannelConnected();
    }

    @Override
    public void onGameLogEvent(String message) {
        appendRoomChatMessage("Games", UiStyleHelper.COLOR_ELECTRIC_VIOLET, "GAME", message, true);
    }

    // --- AgoraVoiceManager.VoiceEventListener callbacks ---

    @Override
    public void onConnectionStateChanged(
            AgoraVoiceManager.ConnectionStatus status,
            String statusText,
            int realRttMs,
            String diagnosticDetail
    ) {
        updateConnectionStatusUI(status, statusText, realRttMs, diagnosticDetail);
        if (status == AgoraVoiceManager.ConnectionStatus.DISCONNECTED && currentRoom != null) {
            for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
                seat.speakingVolume = 0;
            }
            refreshAllSeatsUI();
        }
    }

    @Override
    public void onLocalJoinedSuccess(String channel, int assignedUid, int elapsedMs) {
        repository.assignedLocalUid = assignedUid;
        if (currentRoom != null && voiceManager.isBroadcaster()) {
            int seatIdx = currentLocalSeatIndex >= 0 ? currentLocalSeatIndex : 0;
            currentLocalSeatIndex = seatIdx;
            currentRoom.seats.get(seatIdx).assignUser(
                    assignedUid,
                    repository.localUserName,
                    repository.localAvatarColor,
                    voiceManager.isLocalMuted(),
                    VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(seatIdx)
            );
        }
        refreshAllSeatsUI();
        refreshVideoStageViews();
    }

    @Override
    public void onRemoteUserJoined(int uid) {
        if (currentRoom == null) return;
        for (VoiceRoomModels.SpeakerSeat existing : currentRoom.seats) {
            if (existing.isOccupied && existing.uid == uid) {
                refreshAllSeatsUI();
                refreshVideoStageViews();
                return;
            }
        }
        for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
            if (!seat.isOccupied && !seat.isLocked) {
                seat.assignUser(uid, "User " + uid, UiStyleHelper.COLOR_CYBER_CYAN, false,
                        VoiceRoomModels.SpeakerSeat.defaultRoleForIndex(seat.seatIndex));
                refreshAllSeatsUI();
                refreshVideoStageViews();
                break;
            }
        }
    }

    @Override
    public void onRemoteUserLeft(int uid, int reason) {
        if (currentRoom == null) return;
        for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
            if (seat.isOccupied && seat.uid == uid) {
                seat.vacate();
                break;
            }
        }
        refreshAllSeatsUI();
        refreshVideoStageViews();
    }

    @Override
    public void onRemoteUserMuteChanged(int uid, boolean muted) {
        if (currentRoom == null) return;
        for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
            if (seat.isOccupied && seat.uid == uid) {
                seat.isMuted = muted;
                if (muted) seat.speakingVolume = 0;
                refreshAllSeatsUI();
                break;
            }
        }
    }

    @Override
    public void onRemoteVideoStateChanged(int uid, boolean videoActive) {
        if (currentRoom == null) return;
        for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
            if (seat.isOccupied && seat.uid == uid) {
                seat.isVideoEnabled = videoActive;
                break;
            }
        }
        refreshVideoStageViews();
    }

    @Override
    public void onLocalVolumeUpdated(int volume255) {
        if (currentRoom != null && currentLocalSeatIndex >= 0 && currentLocalSeatIndex < currentRoom.seats.size()) {
            VoiceRoomModels.SpeakerSeat mySeat = currentRoom.seats.get(currentLocalSeatIndex);
            mySeat.speakingVolume = mySeat.isMuted ? 0 : volume255;
        }
    }

    @Override
    public void onRemoteVolumeUpdated(int uid, int volume255) {
        if (currentRoom == null) return;
        for (VoiceRoomModels.SpeakerSeat seat : currentRoom.seats) {
            if (seat.isOccupied && seat.uid == uid) {
                seat.speakingVolume = seat.isMuted ? 0 : volume255;
                break;
            }
        }
    }

    @Override
    public void onDataStreamPacketReceived(int fromUid, String jsonPayload) {
        if (currentRoom == null || jsonPayload == null) return;
        try {
            JSONObject json = new JSONObject(jsonPayload);
            String type = json.optString("type", "");
            if ("CHAT".equals(type)) {
                String sender = json.optString("sender", "User " + fromUid);
                String role = json.optString("role", "PEER");
                String text = json.optString("text", "");
                appendRoomChatMessage(sender, UiStyleHelper.COLOR_CYBER_CYAN, role, text, false);
            } else if ("GIFT_EVENT".equals(type)) {
                String key = json.optString("idempotencyKey", "");
                String emoji = json.optString("emoji", "🎁");
                String gName = json.optString("giftName", "Gift");
                int coins = json.optInt("coins", 0);
                String sender = json.optString("sender", "User " + fromUid);
                String rec = json.optString("recipient", "Stage");
                economyManager.recordIncomingRemoteGift(key, emoji, gName, coins, sender);
                if (reactionsOverlay != null) {
                    reactionsOverlay.spawnReaction(emoji);
                }
                appendRoomChatMessage(sender, Color.rgb(255, 215, 64), "GIFT",
                        "sent " + emoji + " " + gName + " (" + coins + " Coins) to " + rec, false);
            } else if ("REACTION".equals(type)) {
                String emoji = json.optString("emoji", "🔥");
                if (reactionsOverlay != null) {
                    reactionsOverlay.spawnReaction(emoji);
                }
            } else if ("HAND_RAISE".equals(type)) {
                String uName = json.optString("userName", "User " + fromUid);
                currentRoom.handRaises.add(0, new VoiceRoomModels.HandRaiseRequest(fromUid, uName, -1, "RAISED"));
                appendRoomChatMessage("System", UiStyleHelper.COLOR_NEON_PINK, "QUEUE",
                        "✋ " + uName + " (UID " + fromUid + ") raised hand for a mic seat.", true);
            } else if ("GAME_ACTION".equals(type)) {
                if (roomGamesView != null) {
                    roomGamesView.applyRemoteGameAction(json);
                }
            } else if ("WALLPAPER_UPDATE".equals(type)) {
                String wpId = json.optString("wallpaperId", "");
                for (VoiceRoomModels.WallpaperTheme theme : VoiceRoomModels.WALLPAPER_THEMES) {
                    if (theme.id.equals(wpId)) {
                        currentRoom.wallpaper = theme;
                        currentRoom.accentColor = theme.accentColor;
                        break;
                    }
                }
                currentRoom.title = json.optString("title", currentRoom.title);
                currentRoom.coverTagline = json.optString("cover", currentRoom.coverTagline);
                currentRoom.welcomeNotice = json.optString("notice", currentRoom.welcomeNotice);
                applyRoomWallpaperBackground();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onRtcEventLog(String logMessage) {
        appendRoomChatMessage("Agora SDK", UiStyleHelper.COLOR_CYBER_CYAN, "RTC", logMessage, true);
    }
}
