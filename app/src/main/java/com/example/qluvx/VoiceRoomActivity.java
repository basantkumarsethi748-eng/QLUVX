package com.example.qluvx;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class VoiceRoomActivity extends Activity {
    private static final String TAG = "VoiceRoomActivity";
    private static final int REQ_AUDIO_PERMISSIONS = 201;

    public static final String EXTRA_ROOM_TITLE = "extra_room_title";
    public static final String EXTRA_CHANNEL_NAME = "extra_channel_name";
    public static final String EXTRA_LOCAL_UID = "extra_local_uid";

    private static final int[] SEAT_VIEW_IDS = new int[]{
            R.id.seat_1, R.id.seat_2, R.id.seat_3, R.id.seat_4,
            R.id.seat_5, R.id.seat_6, R.id.seat_7, R.id.seat_8,
            R.id.seat_9, R.id.seat_10, R.id.seat_11, R.id.seat_12
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final VoiceRoomModels.RoomRepository repository = VoiceRoomModels.RoomRepository.getInstance();
    private GiftEconomyManager economy;

    // Agora RTC Engine & Configuration from build.gradle (BuildConfig)
    private RtcEngine rtcEngine;
    private boolean isEngineInitialized = false;
    private boolean isJoinedChannel = false;
    private boolean isBroadcaster = true;
    private boolean isLocalMuted = false;
    private boolean isSpeakerphoneOn = true;

    // Agora Chatroom Audio Mixing & Voice Changer State
    private boolean isAudioMixingActive = false;
    private MediaPlayer localMixingPlayer;
    private int currentVoiceChangerMode = 0; // 0 = Voice Effect (Reverb), 1 = Voice Beautify (Changer)
    private int selectedVoiceEffectIndex = 0;
    private int selectedVoiceBeautifyIndex = 0;
    private String activeVoiceEffectLabel = "Default";
    private int currentBgThemeIndex = 0;

    private VoiceRoomModels.RoomInfo currentRoom;
    private String roomTitle;
    private String channelName;
    private int localUid;
    private int currentLocalSeatIndex = 0;
    private int currentRttMs = 18;
    private long lastChatTimestampMs = 0L;

    private final List<VoiceRoomModels.SpeakerSeat> seats = new ArrayList<>(VoiceRoomModels.TOTAL_SEATS);
    private final List<SeatView> seatViews = new ArrayList<>(VoiceRoomModels.TOTAL_SEATS);

    // Header & Room Info Views
    private ImageView roomChannelBgImage;
    private TextView roomTitleView;
    private TextView channelSubView;
    private TextView audioMixingBtn;
    private TextView connectionStateChip;
    private TextView rttLatencyChip;
    private TextView seatCountChip;
    private TextView onlineCountChip;
    private TextView roomAnnouncementView;
    private TextView activeSpeakerBanner;

    // PK Battle Views
    private LinearLayout pkBattleBar;
    private TextView pkScoreLeftView;
    private TextView pkTimerView;
    private TextView pkScoreRightView;

    // Real-Time Chat Views
    private ScrollView chatScrollView;
    private LinearLayout chatMessagesContainer;
    private EditText chatInputEdit;

    // Bottom Control Bar Views
    private TextView micToggleBtn;
    private TextView audioOutputBtn;
    private TextView voiceChangerBtn;
    private TextView requestSpeakBtn;
    private TextView giftPanelBtn;
    private TextView pkBattleBtn;
    private TextView leaveRoomBtn;

    // GiftPopView Animated Overlay Views
    private LinearLayout giftPopBanner;
    private ImageView giftSenderAvatarView;
    private TextView giftSenderNameView;
    private TextView giftActionTipView;

    private int animationTick = 0;

    private final Runnable stageWaveRunnable = new Runnable() {
        @Override
        public void run() {
            animationTick++;
            if (animationTick % 4 == 0) {
                for (int i = 0; i < seats.size(); i++) {
                    VoiceRoomModels.SpeakerSeat seat = seats.get(i);
                    if (seat.isOccupied && !seat.isMuted) {
                        if (seat.uid == localUid && !isJoinedChannel) {
                            seat.speakingVolume = 42 + random.nextInt(95);
                            seat.isSpeakingVad = true;
                            updateSeatUI(i);
                        } else if (seat.uid != localUid && random.nextInt(10) < 5) {
                            seat.speakingVolume = 45 + random.nextInt(145);
                            seat.isSpeakingVad = true;
                            updateSeatUI(i);
                        } else if (seat.uid != localUid) {
                            seat.speakingVolume = 0;
                            seat.isSpeakingVad = false;
                            updateSeatUI(i);
                        }
                    }
                }
                updateActiveSpeakerBanner();
            }

            if (animationTick % 20 == 0) {
                if (currentRoom != null && currentRoom.pkBattle.isActive) {
                    currentRoom.pkBattle.remainingSeconds = Math.max(0, currentRoom.pkBattle.remainingSeconds - 1);
                    if (currentRoom.pkBattle.remainingSeconds == 0) {
                        currentRoom.pkBattle.remainingSeconds = 180;
                    }
                    updatePkBattleUI();
                }
            }

            if (animationTick % 40 == 0 && !isJoinedChannel && rttLatencyChip != null) {
                int jitterRtt = Math.max(12, Math.min(45, currentRttMs + (random.nextInt(7) - 3)));
                updateRttIndicator(jitterRtt);
            }

            for (int i = 0; i < seatViews.size(); i++) {
                seatViews.get(i).stepActiveSpeakerAnimation();
            }
            mainHandler.postDelayed(this, 50);
        }
    };

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            mainHandler.post(() -> {
                isJoinedChannel = true;
                updateConnectionStateUI(Constants.CONNECTION_STATE_CONNECTED, "CONNECTED", true);
                updateRttIndicator(Math.max(12, Math.min(elapsed, 95)));
            });
        }

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            mainHandler.post(() -> {
                String label = AgoraVoiceManager.mapConnectionStateLabel(state);
                boolean connected = (state == Constants.CONNECTION_STATE_CONNECTED
                        || state == Constants.CONNECTION_STATE_FAILED);
                updateConnectionStateUI(state, label, connected);
            });
        }

        @Override
        public void onAudioMixingStateChanged(int state, int reason) {
            mainHandler.post(() -> {
                if (state == Constants.AUDIO_MIXING_STATE_STOPPED || state == Constants.AUDIO_MIXING_STATE_FAILED) {
                    isAudioMixingActive = false;
                    updateAudioMixingButtonUI();
                }
            });
        }

        @Override
        public void onTokenPrivilegeWillExpire(String token) {
            TokenServiceClient.requestRtcToken(
                    channelName,
                    localUid,
                    isBroadcaster,
                    (newToken, fromBackendServer) -> {
                        if (rtcEngine != null && newToken != null && !newToken.isEmpty()) {
                            try {
                                rtcEngine.renewToken(newToken);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
            );
        }

        @Override
        public void onRtcStats(RtcStats stats) {
            if (stats == null) return;
            final int rtt = stats.lastmileDelay > 0
                    ? stats.lastmileDelay
                    : (stats.gatewayRtt > 0 ? stats.gatewayRtt : 18);
            mainHandler.post(() -> updateRttIndicator(rtt));
        }

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            if (speakers == null) return;
            mainHandler.post(() -> {
                for (AudioVolumeInfo info : speakers) {
                    if (info.uid == 0) {
                        if (currentLocalSeatIndex >= 0 && currentLocalSeatIndex < seats.size()) {
                            VoiceRoomModels.SpeakerSeat localSeat = seats.get(currentLocalSeatIndex);
                            localSeat.speakingVolume = isLocalMuted ? 0 : info.volume;
                            localSeat.isSpeakingVad = !isLocalMuted && (info.vad == 1 || info.volume > 8);
                            updateSeatUI(currentLocalSeatIndex);
                        }
                    } else {
                        for (int i = 0; i < seats.size(); i++) {
                            VoiceRoomModels.SpeakerSeat seat = seats.get(i);
                            if (seat.isOccupied && seat.uid == info.uid) {
                                seat.speakingVolume = seat.isMuted ? 0 : info.volume;
                                seat.isSpeakingVad = !seat.isMuted && (info.vad == 1 || info.volume > 8);
                                updateSeatUI(i);
                                break;
                            }
                        }
                    }
                }
                updateActiveSpeakerBanner();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            mainHandler.post(() -> {
                for (int i = 0; i < seats.size(); i++) {
                    VoiceRoomModels.SpeakerSeat seat = seats.get(i);
                    if (!seat.isOccupied && !seat.isLocked) {
                        seat.assignUser(uid, "User " + (uid % 1000), UiStyleHelper.COLOR_AGORA_BLUE, false, "Seat " + (i + 1));
                        appendRoomMessage("System", "MODERATOR", "👋 User " + (uid % 1000) + " joined Seat " + (i + 1), true);
                        refreshAllSeatsAndControls();
                        break;
                    }
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            mainHandler.post(() -> {
                for (int i = 0; i < seats.size(); i++) {
                    VoiceRoomModels.SpeakerSeat seat = seats.get(i);
                    if (seat.isOccupied && seat.uid == uid) {
                        String oldName = seat.userName;
                        seat.vacate();
                        appendRoomMessage("System", "MODERATOR", "🚪 " + oldName + " left the seat", true);
                        refreshAllSeatsAndControls();
                        break;
                    }
                }
            });
        }

        @Override
        public void onUserMuteAudio(int uid, boolean muted) {
            mainHandler.post(() -> {
                for (int i = 0; i < seats.size(); i++) {
                    VoiceRoomModels.SpeakerSeat seat = seats.get(i);
                    if (seat.isOccupied && seat.uid == uid) {
                        seat.isMuted = muted;
                        if (muted) seat.speakingVolume = 0;
                        updateSeatUI(i);
                        break;
                    }
                }
            });
        }

        @Override
        public void onError(int err) {
            Log.w(TAG, "Agora RTC Engine code: " + err);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        economy = GiftEconomyManager.getInstance(this);

        UiStyleHelper.configureSafeActivityWindow(this);
        installSafeBackgroundThreadGuard();
        setContentView(R.layout.activity_voice_room);

        Intent intent = getIntent();
        String defaultChannel = getBuildConfigDefaultChannel();
        channelName = (intent != null && intent.hasExtra(EXTRA_CHANNEL_NAME))
                ? intent.getStringExtra(EXTRA_CHANNEL_NAME)
                : defaultChannel;
        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = defaultChannel;
        }
        roomTitle = (intent != null && intent.hasExtra(EXTRA_ROOM_TITLE))
                ? intent.getStringExtra(EXTRA_ROOM_TITLE)
                : "QLUVX Official Agora Voice Room";
        localUid = (intent != null)
                ? intent.getIntExtra(EXTRA_LOCAL_UID, repository.localUid)
                : repository.localUid;

        currentRoom = repository.createOrFindRoom(roomTitle, channelName);
        if (currentRoom != null) {
            currentRoom.pkBattle.isActive = true;
            if (currentRoom.pkBattle.hostScore == 0 && currentRoom.pkBattle.challengerScore == 0) {
                currentRoom.pkBattle.hostScore = 1450;
                currentRoom.pkBattle.challengerScore = 1180;
            }
            if (currentRoom.speakerRequests.isEmpty()) {
                currentRoom.speakerRequests.add("Riya (Requesting Seat)");
                currentRoom.speakerRequests.add("Arjun (Requesting Seat)");
            }
        }

        initSeatsModel();
        bindXmlViews();

        if (hasRequiredPermissions()) {
            initializeAndJoinAgoraVoiceChannel();
        } else {
            updateConnectionStateUI(Constants.CONNECTION_STATE_CONNECTED, "CONNECTED (STAGE)", true);
            updateRttIndicator(18);
        }

        mainHandler.post(stageWaveRunnable);
    }

    private String getBuildConfigDefaultChannel() {
        try {
            String ch = BuildConfig.AGORA_DEFAULT_CHANNEL;
            if (ch != null && !ch.trim().isEmpty()) {
                return ch.trim();
            }
        } catch (Throwable ignored) {
        }
        return "qluvx_live_1";
    }

    private String getBuildConfigAppId() {
        try {
            String id = BuildConfig.AGORA_APP_ID;
            if (id != null && !id.trim().isEmpty()
                    && !id.trim().equals("YOUR_AGORA_APP_ID")
                    && id.trim().length() >= 16) {
                return id.trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void initSeatsModel() {
        seats.clear();
        if (currentRoom != null && currentRoom.seats.size() == VoiceRoomModels.TOTAL_SEATS) {
            seats.addAll(currentRoom.seats);
        } else {
            for (int i = 0; i < VoiceRoomModels.TOTAL_SEATS; i++) {
                seats.add(new VoiceRoomModels.SpeakerSeat(i));
            }
        }
        seats.get(0).assignUser(localUid, "You (Anchor)", UiStyleHelper.COLOR_AGORA_BLUE, false, "Host");
        currentLocalSeatIndex = 0;

        if (!seats.get(1).isOccupied) {
            seats.get(1).assignUser(202, "Aarav", Color.rgb(14, 165, 233), false, "Co-Host");
        }
        if (!seats.get(2).isOccupied) {
            seats.get(2).assignUser(203, "Zoya", Color.rgb(99, 102, 241), false, "Speaker");
        }
        if (!seats.get(3).isOccupied) {
            seats.get(3).assignUser(204, "Kabir", Color.rgb(16, 185, 129), true, "Speaker");
        }
    }

    private void bindXmlViews() {
        roomChannelBgImage = findViewById(R.id.iv_room_channel_bg);
        roomTitleView = findViewById(R.id.tv_room_title);
        channelSubView = findViewById(R.id.tv_channel_sub);
        audioMixingBtn = findViewById(R.id.btn_audio_mixing);
        connectionStateChip = findViewById(R.id.chip_connection_state);
        rttLatencyChip = findViewById(R.id.chip_rtt_latency);
        seatCountChip = findViewById(R.id.chip_seat_count);
        onlineCountChip = findViewById(R.id.chip_online_count);
        roomAnnouncementView = findViewById(R.id.tv_room_announcement);
        activeSpeakerBanner = findViewById(R.id.tv_active_speaker_banner);

        giftPopBanner = findViewById(R.id.gift_pop_banner);
        giftSenderAvatarView = findViewById(R.id.iv_gift_sender_avatar);
        giftSenderNameView = findViewById(R.id.tv_gift_sender_name);
        giftActionTipView = findViewById(R.id.tv_gift_action_tip);

        if (giftSenderAvatarView != null) {
            giftSenderAvatarView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int size = Math.min(view.getWidth(), view.getHeight());
                    outline.setOval(0, 0, size, size);
                }
            });
            giftSenderAvatarView.setClipToOutline(true);
        }

        if (roomChannelBgImage != null) {
            int wpRes = UiStyleHelper.getActiveWallpaperResId(this);
            if (currentRoom != null && currentRoom.backgroundResId != 0 && currentBgThemeIndex > 0) {
                wpRes = currentRoom.backgroundResId;
            }
            roomChannelBgImage.setImageResource(wpRes);
            roomChannelBgImage.setAlpha(Math.max(0.45f, UiStyleHelper.getWallpaperAlpha(this)));
        }

        pkBattleBar = findViewById(R.id.pk_battle_bar);
        pkScoreLeftView = findViewById(R.id.tv_pk_score_left);
        pkTimerView = findViewById(R.id.tv_pk_timer);
        pkScoreRightView = findViewById(R.id.tv_pk_score_right);

        chatScrollView = findViewById(R.id.chat_scroll_view);
        chatMessagesContainer = findViewById(R.id.chat_messages_container);
        chatInputEdit = findViewById(R.id.et_chat_input);

        LinearLayout quickPillsContainer = findViewById(R.id.voice_quick_chat_pills);
        if (quickPillsContainer != null) {
            populateVoiceQuickChatPills(quickPillsContainer);
        }

        TextView sendChatBtn = findViewById(R.id.btn_send_chat);
        if (sendChatBtn != null) {
            sendChatBtn.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_AGORA_BLUE, 10f, Color.argb(140, 255, 255, 255), 1f));
            sendChatBtn.setOnClickListener(v -> onSendChatClicked());
        }

        TextView backBtn = findViewById(R.id.btn_back_lobby);
        if (backBtn != null) {
            backBtn.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 10f, 0, 0));
            backBtn.setOnClickListener(v -> confirmLeaveRoomDialog());
        }

        if (audioMixingBtn != null) {
            updateAudioMixingButtonUI();
            audioMixingBtn.setOnClickListener(v -> toggleAudioMixing());
        }

        if (onlineCountChip != null) {
            onlineCountChip.setBackground(UiStyleHelper.rippleButton(this, Color.argb(40, 56, 189, 248), 8f, 0, 0));
            onlineCountChip.setOnClickListener(v -> showChannelMemberListDialog());
        }

        TextView shareBtn = findViewById(R.id.btn_share_room);
        if (shareBtn != null) {
            shareBtn.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 10f, 0, 0));
            shareBtn.setOnClickListener(v -> shareRoomLink());
        }

        TextView switchRoomsBtn = findViewById(R.id.btn_switch_rooms);
        if (switchRoomsBtn != null) {
            switchRoomsBtn.setBackground(UiStyleHelper.rippleButton(
                    this,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    10f,
                    Color.argb(55, 255, 255, 255),
                    1f
            ));
            switchRoomsBtn.setOnClickListener(v -> showRoomSettingsAndModerationDialog());
        }

        seatViews.clear();
        for (int i = 0; i < SEAT_VIEW_IDS.length; i++) {
            final int seatIdx = i;
            SeatView sv = findViewById(SEAT_VIEW_IDS[i]);
            sv.setBackground(UiStyleHelper.rippleButton(this, Color.TRANSPARENT, 12f, 0, 0));
            sv.setOnClickListener(v -> onSeatClicked(seatIdx));
            seatViews.add(sv);
        }

        micToggleBtn = findViewById(R.id.btn_mic_toggle);
        audioOutputBtn = findViewById(R.id.btn_audio_output);
        voiceChangerBtn = findViewById(R.id.btn_voice_changer);
        requestSpeakBtn = findViewById(R.id.btn_request_speak);
        giftPanelBtn = findViewById(R.id.btn_gift_panel);
        pkBattleBtn = findViewById(R.id.btn_pk_battle);
        leaveRoomBtn = findViewById(R.id.btn_leave_room);

        micToggleBtn.setOnClickListener(v -> onMicToggleClicked());

        audioOutputBtn.setBackground(UiStyleHelper.rippleButton(
                this,
                UiStyleHelper.COLOR_CARD_ELEVATED,
                12f,
                Color.argb(70, 14, 165, 233),
                1f
        ));
        audioOutputBtn.setOnClickListener(v -> onAudioOutputSwitchClicked());

        if (voiceChangerBtn != null) {
            voiceChangerBtn.setBackground(UiStyleHelper.rippleButton(
                    this,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    12f,
                    Color.argb(80, 56, 189, 248),
                    1f
            ));
            voiceChangerBtn.setOnClickListener(v -> showVoiceChangerAndBeautifyDialog());
        }

        if (requestSpeakBtn != null) {
            requestSpeakBtn.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, 0, 0));
            requestSpeakBtn.setOnClickListener(v -> showSpeakerRequestQueueDialog());
        }

        if (giftPanelBtn != null) {
            giftPanelBtn.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, 0, 0));
            giftPanelBtn.setOnClickListener(v -> showGiftPanelDialog());
        }

        if (pkBattleBtn != null) {
            pkBattleBtn.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, 0, 0));
            pkBattleBtn.setOnClickListener(v -> showPkBattleControlDialog());
        }

        leaveRoomBtn.setBackground(UiStyleHelper.rippleButton(
                this,
                Color.argb(45, 239, 68, 68),
                12f,
                Color.argb(120, 239, 68, 68),
                1f
        ));
        leaveRoomBtn.setOnClickListener(v -> confirmLeaveRoomDialog());

        updateRoomHeaderMetadata();
        updatePkBattleUI();
        renderChatMessages();
        refreshAllSeatsAndControls();
    }

    /**
     * Ported from Agora Chatroom RtcManager.startAudioMixing("/assets/mixing.mp3", false, false, 1)
     * with local MediaPlayer fallback on assets/mixing.mp3 when testing in Stage mode.
     */
    private void toggleAudioMixing() {
        isAudioMixingActive = !isAudioMixingActive;
        if (isAudioMixingActive) {
            boolean startedViaRtc = false;
            if (rtcEngine != null && isJoinedChannel) {
                try {
                    rtcEngine.startAudioMixing("/assets/mixing.mp3", false, 1, 0);
                    rtcEngine.adjustAudioMixingVolume(15);
                    startedViaRtc = true;
                } catch (Throwable t) {
                    Log.w(TAG, "RTC startAudioMixing fallback to MediaPlayer", t);
                }
            }
            if (!startedViaRtc) {
                startLocalAssetMixingPlayer();
            }
            appendRoomMessage("System", "AUDIO MIXING", "🎵 Started background audio mixing (/assets/mixing.mp3 • Vol 15%)", true);
            Toast.makeText(this, "🎵 Audio Mixing ON (mixing.mp3)", Toast.LENGTH_SHORT).show();
        } else {
            stopAllAudioMixing();
            appendRoomMessage("System", "AUDIO MIXING", "⏹️ Stopped background audio mixing", true);
            Toast.makeText(this, "⏹️ Audio Mixing OFF", Toast.LENGTH_SHORT).show();
        }
        updateAudioMixingButtonUI();
    }

    private void startLocalAssetMixingPlayer() {
        stopLocalAssetMixingPlayer();
        try {
            AssetFileDescriptor afd = getAssets().openFd("mixing.mp3");
            localMixingPlayer = new MediaPlayer();
            localMixingPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            localMixingPlayer.setVolume(0.25f, 0.25f);
            localMixingPlayer.setOnCompletionListener(mp -> {
                isAudioMixingActive = false;
                updateAudioMixingButtonUI();
            });
            localMixingPlayer.prepare();
            localMixingPlayer.start();
        } catch (Throwable t) {
            Log.w(TAG, "Unable to play local mixing.mp3", t);
        }
    }

    private void stopLocalAssetMixingPlayer() {
        if (localMixingPlayer != null) {
            try {
                if (localMixingPlayer.isPlaying()) {
                    localMixingPlayer.stop();
                }
                localMixingPlayer.release();
            } catch (Throwable ignored) {
            }
            localMixingPlayer = null;
        }
    }

    private void stopAllAudioMixing() {
        if (rtcEngine != null && isEngineInitialized) {
            try {
                rtcEngine.stopAudioMixing();
            } catch (Throwable ignored) {
            }
        }
        stopLocalAssetMixingPlayer();
    }

    private void updateAudioMixingButtonUI() {
        if (audioMixingBtn == null) return;
        if (isAudioMixingActive) {
            audioMixingBtn.setText("🎵 Mixing ON");
            audioMixingBtn.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
            audioMixingBtn.setBackground(UiStyleHelper.rippleButton(
                    this,
                    Color.argb(45, 16, 185, 129),
                    10f,
                    Color.argb(140, 16, 185, 129),
                    1f
            ));
        } else {
            audioMixingBtn.setText("🎵 Mixing");
            audioMixingBtn.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
            audioMixingBtn.setBackground(UiStyleHelper.rippleButton(
                    this,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    10f,
                    Color.argb(60, 56, 189, 248),
                    1f
            ));
        }
    }

    /**
     * Ported from Agora Chatroom VoiceChangerDialog.java & RtcManager.setVoiceChanger / setReverbPreset.
     * Supports both "Voice Effect" (Default, KTV, Live, Uncle, Girl, Studio, Pop, R&B, Phonograph)
     * and "Voice Beautify" (Default, Thick, Low, Round, Falsetto, Full, Clear, Resounding, Loud, OpenAir).
     */
    private void showVoiceChangerAndBeautifyDialog() {
        final String[] effectKeys = getResources().getStringArray(R.array.voice_effect_keys);
        final int[] effectValues = getResources().getIntArray(R.array.voice_effect_values);
        final String[] beautifyKeys = getResources().getStringArray(R.array.voice_beautify_keys);
        final int[] beautifyValues = getResources().getIntArray(R.array.voice_beautify_values);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        root.setPadding(pad, pad, pad, pad);

        final TextView activeStatus = new TextView(this);
        activeStatus.setText("🎭 Active Preset: " + activeVoiceEffectLabel);
        activeStatus.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        activeStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        activeStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        root.addView(activeStatus);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, UiStyleHelper.dp(this, 10), 0, UiStyleHelper.dp(this, 10));

        final TextView tabEffect = new TextView(this);
        tabEffect.setText(getString(R.string.voice_effect) + " (9)");
        tabEffect.setGravity(Gravity.CENTER);
        tabEffect.setMinHeight(UiStyleHelper.dp(this, 40));
        tabEffect.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tabEffect.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        final TextView tabBeautify = new TextView(this);
        tabBeautify.setText(getString(R.string.beautify_voice) + " (10)");
        tabBeautify.setGravity(Gravity.CENTER);
        tabBeautify.setMinHeight(UiStyleHelper.dp(this, 40));
        tabBeautify.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tabBeautify.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        tabRow.addView(tabEffect, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View spacer = new View(this);
        tabRow.addView(spacer, new LinearLayout.LayoutParams(UiStyleHelper.dp(this, 8), 1));
        tabRow.addView(tabBeautify, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(tabRow);

        final GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Runnable[] renderGrid = new Runnable[1];
        renderGrid[0] = () -> {
            boolean isEffectTab = (currentVoiceChangerMode == 0);
            tabEffect.setTextColor(isEffectTab ? Color.WHITE : UiStyleHelper.COLOR_TEXT_SECONDARY);
            tabEffect.setBackground(UiStyleHelper.rippleButton(
                    this,
                    isEffectTab ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                    10f, 0, 0
            ));
            tabBeautify.setTextColor(!isEffectTab ? Color.WHITE : UiStyleHelper.COLOR_TEXT_SECONDARY);
            tabBeautify.setBackground(UiStyleHelper.rippleButton(
                    this,
                    !isEffectTab ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                    10f, 0, 0
            ));

            grid.removeAllViews();
            String[] keys = isEffectTab ? effectKeys : beautifyKeys;
            int[] values = isEffectTab ? effectValues : beautifyValues;
            int selectedIdx = isEffectTab ? selectedVoiceEffectIndex : selectedVoiceBeautifyIndex;

            for (int i = 0; i < keys.length; i++) {
                final int idx = i;
                final String key = keys[i];
                final int morphVal = values[i];
                boolean isSelected = (i == selectedIdx);

                TextView cell = new TextView(this);
                cell.setText(key);
                cell.setGravity(Gravity.CENTER);
                cell.setMinHeight(UiStyleHelper.dp(this, 44));
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                cell.setTypeface(Typeface.create("sans-serif-medium", isSelected ? Typeface.BOLD : Typeface.NORMAL));
                cell.setTextColor(isSelected ? Color.WHITE : UiStyleHelper.COLOR_TEXT_PRIMARY);
                cell.setBackground(UiStyleHelper.rippleButton(
                        this,
                        isSelected ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                        10f,
                        isSelected ? UiStyleHelper.COLOR_AGORA_CYAN : Color.argb(40, 255, 255, 255),
                        1f
                ));

                cell.setOnClickListener(v -> {
                    if (isEffectTab) {
                        selectedVoiceEffectIndex = idx;
                        selectedVoiceBeautifyIndex = 0;
                        applyAgoraReverbPreset(morphVal, key);
                    } else {
                        selectedVoiceBeautifyIndex = idx;
                        selectedVoiceEffectIndex = 0;
                        applyAgoraVoiceChanger(morphVal, key);
                    }
                    activeStatus.setText("🎭 Active Preset: " + activeVoiceEffectLabel);
                    renderGrid[0].run();
                });

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.columnSpec = GridLayout.spec(i % 3, 1f);
                lp.rowSpec = GridLayout.spec(i / 3);
                int m = UiStyleHelper.dp(this, 4);
                lp.setMargins(m, m, m, m);
                grid.addView(cell, lp);
            }
        };

        tabEffect.setOnClickListener(v -> {
            currentVoiceChangerMode = 0;
            renderGrid[0].run();
        });
        tabBeautify.setOnClickListener(v -> {
            currentVoiceChangerMode = 1;
            renderGrid[0].run();
        });

        renderGrid[0].run();

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎭 Agora Voice Effect & Voice Beautify")
                .setView(root)
                .setPositiveButton("Done", null)
                .show();
    }

    private void applyAgoraReverbPreset(int presetVal, String label) {
        this.activeVoiceEffectLabel = label.equals("Default") ? "Default" : ("Effect: " + label);
        if (rtcEngine != null && isEngineInitialized) {
            try {
                rtcEngine.setParameters(String.format(Locale.US, "{\"che.audio.morph.reverb_preset\": %d}", presetVal));
            } catch (Throwable t) {
                Log.w(TAG, "Error setting reverb_preset", t);
            }
        }
        updateVoiceChangerButtonUI();
        appendRoomMessage("You", "VOICE EFFECT", "🎭 Switched Voice Effect to: " + label + " (preset=" + presetVal + ")", true);
    }

    private void applyAgoraVoiceChanger(int changerVal, String label) {
        this.activeVoiceEffectLabel = label.equals("Default") ? "Default" : ("Beautify: " + label);
        if (rtcEngine != null && isEngineInitialized) {
            try {
                rtcEngine.setParameters(String.format(Locale.US, "{\"che.audio.morph.voice_changer\": %d}", changerVal));
            } catch (Throwable t) {
                Log.w(TAG, "Error setting voice_changer", t);
            }
        }
        updateVoiceChangerButtonUI();
        appendRoomMessage("You", "VOICE BEAUTIFY", "✨ Switched Voice Beautify to: " + label + " (morph=" + changerVal + ")", true);
    }

    private void updateVoiceChangerButtonUI() {
        if (voiceChangerBtn == null) return;
        boolean hasCustomEffect = !activeVoiceEffectLabel.equals("Default");
        voiceChangerBtn.setText(hasCustomEffect ? ("🎭 " + activeVoiceEffectLabel.replace("Effect: ", "").replace("Beautify: ", "")) : "🎭 Changer");
        voiceChangerBtn.setTextColor(hasCustomEffect ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_AGORA_CYAN);
    }

    private boolean showAgoraParticipantsAsGrid = false;

    /**
     * Ported from Agora Chatroom MemberListDialog.java & MemberListAdapter.java.
     * Displays participants currently connected to the Agora channel in either a List or a 3-column Grid,
     * with their avatar portraits, UID, Anchor/Seat badge, toAudience/toBroadcast role toggle, and Turn on/off mic controls.
     */
    private void showChannelMemberListDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 14);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(0, 0, 0, UiStyleHelper.dp(this, 8));

        final TextView headerCount = new TextView(this);
        headerCount.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        headerCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        headerCount.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        headerCount.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerBar.addView(headerCount);

        final TextView viewToggleBtn = new TextView(this);
        viewToggleBtn.setText(showAgoraParticipantsAsGrid ? "☰ List View" : "⊞ Grid View");
        viewToggleBtn.setTextColor(Color.WHITE);
        viewToggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        viewToggleBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        viewToggleBtn.setPadding(UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 5), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 5));
        viewToggleBtn.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_AGORA_BLUE, 10f, 0, 0));
        headerBar.addView(viewToggleBtn);

        root.addView(headerBar);

        ScrollView scroll = new ScrollView(this);
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(this, 310)));

        final String[] sampleAudienceNames = new String[]{
                "Zhao Fuqiang", "Qian Minzhu", "Sun Wenming", "Li Hexie", "Zhou Ziyou", "Wu Pingdeng"
        };

        final Runnable[] refreshMemberRows = new Runnable[1];
        refreshMemberRows[0] = () -> {
            listContainer.removeAllViews();
            viewToggleBtn.setText(showAgoraParticipantsAsGrid ? "☰ List View" : "⊞ Grid View");
            int totalShown = 0;

            if (showAgoraParticipantsAsGrid) {
                GridLayout grid = new GridLayout(this);
                grid.setColumnCount(3);
                int gIdx = 0;

                for (int i = 0; i < seats.size(); i++) {
                    final int seatIdx = i;
                    final VoiceRoomModels.SpeakerSeat s = seats.get(i);
                    if (!s.isOccupied) continue;
                    totalShown++;

                    LinearLayout cell = new LinearLayout(this);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(Gravity.CENTER_HORIZONTAL);
                    int cPad = UiStyleHelper.dp(this, 8);
                    cell.setPadding(cPad, cPad, cPad, cPad);
                    cell.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, s.isMuted ? Color.rgb(71, 85, 105) : UiStyleHelper.COLOR_LIVE_GREEN, 1f));

                    View av = UiStyleHelper.createCircularAvatar(
                            this,
                            s.avatarResId != 0 ? s.avatarResId : VoiceRoomModels.getAvatarResId(i),
                            44,
                            s.isMuted ? Color.rgb(100, 116, 139) : UiStyleHelper.COLOR_LIVE_GREEN,
                            i == 0
                    );
                    cell.addView(av);

                    TextView nm = new TextView(this);
                    nm.setText(s.userName);
                    nm.setSingleLine(true);
                    nm.setTextColor(Color.WHITE);
                    nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                    nm.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    nm.setPadding(0, UiStyleHelper.dp(this, 4), 0, 0);
                    cell.addView(nm);

                    TextView role = new TextView(this);
                    role.setText((i == 0 ? "👑 Host" : ("Seat " + (i + 1))) + " • UID " + s.uid);
                    role.setSingleLine(true);
                    role.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
                    role.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
                    cell.addView(role);

                    cell.setOnClickListener(v -> {
                        if (s.uid == localUid) {
                            onMicToggleClicked();
                        } else {
                            s.isMuted = !s.isMuted;
                            if (s.isMuted) s.speakingVolume = 0;
                            updateSeatUI(seatIdx);
                        }
                        refreshMemberRows[0].run();
                    });

                    GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                    lp.width = 0;
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    lp.columnSpec = GridLayout.spec(gIdx % 3, 1f);
                    lp.rowSpec = GridLayout.spec(gIdx / 3);
                    int m = UiStyleHelper.dp(this, 4);
                    lp.setMargins(m, m, m, m);
                    grid.addView(cell, lp);
                    gIdx++;
                }

                for (int a = 0; a < sampleAudienceNames.length; a++) {
                    final String audName = sampleAudienceNames[a];
                    final int audUid = 800 + a;
                    boolean alreadySeated = false;
                    for (VoiceRoomModels.SpeakerSeat s : seats) {
                        if (s.isOccupied && s.userName.equalsIgnoreCase(audName)) {
                            alreadySeated = true;
                            break;
                        }
                    }
                    if (alreadySeated) continue;
                    totalShown++;

                    LinearLayout cell = new LinearLayout(this);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(Gravity.CENTER_HORIZONTAL);
                    int cPad = UiStyleHelper.dp(this, 8);
                    cell.setPadding(cPad, cPad, cPad, cPad);
                    cell.setBackground(UiStyleHelper.roundedBox(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, Color.rgb(51, 65, 85), 1f));

                    View av = UiStyleHelper.createCircularAvatar(this, VoiceRoomModels.getAvatarResId(a + 5), 44, Color.rgb(100, 116, 139), false);
                    cell.addView(av);

                    TextView nm = new TextView(this);
                    nm.setText(audName);
                    nm.setSingleLine(true);
                    nm.setTextColor(Color.WHITE);
                    nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                    nm.setPadding(0, UiStyleHelper.dp(this, 4), 0, 0);
                    cell.addView(nm);

                    TextView role = new TextView(this);
                    role.setText("🎧 UID " + audUid);
                    role.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
                    role.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
                    cell.addView(role);

                    GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                    lp.width = 0;
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    lp.columnSpec = GridLayout.spec(gIdx % 3, 1f);
                    lp.rowSpec = GridLayout.spec(gIdx / 3);
                    int m = UiStyleHelper.dp(this, 4);
                    lp.setMargins(m, m, m, m);
                    grid.addView(cell, lp);
                    gIdx++;
                }

                listContainer.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                headerCount.setText("📡 Connected in Agora (" + totalShown + ")");
                return;
            }

            // 1. Show all occupied seats (Broadcasters / Anchor) in List mode
            for (int i = 0; i < seats.size(); i++) {
                final int seatIdx = i;
                final VoiceRoomModels.SpeakerSeat s = seats.get(i);
                if (!s.isOccupied) continue;
                totalShown++;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, 0, 0f));
                int rPad = UiStyleHelper.dp(this, 8);
                row.setPadding(rPad, rPad, rPad, rPad);

                ImageView avatar = new ImageView(this);
                avatar.setImageResource(s.avatarResId != 0 ? s.avatarResId : VoiceRoomModels.getAvatarResId(i));
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                avatar.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int sz = Math.min(view.getWidth(), view.getHeight());
                        outline.setOval(0, 0, sz, sz);
                    }
                });
                avatar.setClipToOutline(true);
                int avSize = UiStyleHelper.dp(this, 36);
                row.addView(avatar, new LinearLayout.LayoutParams(avSize, avSize));

                TextView nameTv = new TextView(this);
                String anchorTag = (i == 0) ? getString(R.string.anchor) : (" (Seat " + (i + 1) + ")");
                nameTv.setText(s.userName + anchorTag + " • UID " + s.uid);
                nameTv.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
                nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                nameTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                nameTv.setPadding(UiStyleHelper.dp(this, 8), 0, UiStyleHelper.dp(this, 6), 0);
                row.addView(nameTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                if (i != 0) {
                    TextView roleBtn = UiStyleHelper.createStatusChip(
                            this,
                            getString(R.string.to_audience),
                            Color.argb(45, 245, 158, 11),
                            UiStyleHelper.COLOR_WARNING_AMBER,
                            0
                    );
                    roleBtn.setOnClickListener(v -> {
                        String oldUser = s.userName;
                        s.vacate();
                        appendRoomMessage("System", "Anchor", "⬇️ Moved " + oldUser + " to Audience", true);
                        refreshAllSeatsAndControls();
                        refreshMemberRows[0].run();
                    });
                    row.addView(roleBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));
                }

                TextView muteBtn = UiStyleHelper.createStatusChip(
                        this,
                        s.isMuted ? getString(R.string.turn_on_mic) : getString(R.string.turn_off_mic),
                        s.isMuted ? Color.argb(45, 239, 68, 68) : Color.argb(45, 16, 185, 129),
                        s.isMuted ? UiStyleHelper.COLOR_DANGER_RED : UiStyleHelper.COLOR_LIVE_GREEN,
                        0
                );
                muteBtn.setOnClickListener(v -> {
                    if (s.uid == localUid) {
                        onMicToggleClicked();
                    } else {
                        s.isMuted = !s.isMuted;
                        if (s.isMuted) s.speakingVolume = 0;
                        updateSeatUI(seatIdx);
                    }
                    refreshMemberRows[0].run();
                });
                row.addView(muteBtn);

                listContainer.addView(row, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 6)));
            }

            // 2. Show Audience Members with "toBroadcast" button
            for (int a = 0; a < sampleAudienceNames.length; a++) {
                final String audName = sampleAudienceNames[a];
                final int audUid = 800 + a;
                boolean alreadySeated = false;
                for (VoiceRoomModels.SpeakerSeat s : seats) {
                    if (s.isOccupied && s.userName.equalsIgnoreCase(audName)) {
                        alreadySeated = true;
                        break;
                    }
                }
                if (alreadySeated) continue;
                totalShown++;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, 0, 0f));
                int rPad = UiStyleHelper.dp(this, 8);
                row.setPadding(rPad, rPad, rPad, rPad);

                ImageView avatar = new ImageView(this);
                avatar.setImageResource(VoiceRoomModels.getAvatarResId(a + 5));
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                avatar.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int sz = Math.min(view.getWidth(), view.getHeight());
                        outline.setOval(0, 0, sz, sz);
                    }
                });
                avatar.setClipToOutline(true);
                int avSize = UiStyleHelper.dp(this, 36);
                row.addView(avatar, new LinearLayout.LayoutParams(avSize, avSize));

                TextView nameTv = new TextView(this);
                nameTv.setText(audName + " (Audience • UID " + audUid + ")");
                nameTv.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
                nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                nameTv.setPadding(UiStyleHelper.dp(this, 8), 0, UiStyleHelper.dp(this, 6), 0);
                row.addView(nameTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView toBroadcastBtn = UiStyleHelper.createStatusChip(
                        this,
                        getString(R.string.to_broadcast),
                        Color.argb(45, 56, 189, 248),
                        UiStyleHelper.COLOR_AGORA_CYAN,
                        0
                );
                toBroadcastBtn.setOnClickListener(v -> {
                    for (int i = 0; i < seats.size(); i++) {
                        VoiceRoomModels.SpeakerSeat emptySeat = seats.get(i);
                        if (!emptySeat.isOccupied && !emptySeat.isLocked) {
                            emptySeat.assignUser(audUid, audName, UiStyleHelper.COLOR_AGORA_BLUE, false, "Speaker");
                            appendRoomMessage("System", "Anchor", "🎙️ Invited " + audName + " to Broadcast on Seat " + (i + 1), true);
                            refreshAllSeatsAndControls();
                            refreshMemberRows[0].run();
                            return;
                        }
                    }
                    Toast.makeText(this, "No open seats available!", Toast.LENGTH_SHORT).show();
                });
                row.addView(toBroadcastBtn);

                listContainer.addView(row, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 6)));
            }

            headerCount.setText(getString(R.string.channel_member_list, totalShown));
        };

        viewToggleBtn.setOnClickListener(v -> {
            showAgoraParticipantsAsGrid = !showAgoraParticipantsAsGrid;
            refreshMemberRows[0].run();
        });

        refreshMemberRows[0].run();

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("📡 Agora Channel Participants (List / Grid)")
                .setView(root)
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Ported from Agora Chatroom GiftPopView.show(String userId)
     * Slides up and fades out the gift banner with the sender's circular avatar and rocket gift icon.
     */
    private void triggerGiftPopViewAnimation(String senderName, int avatarResId, String giftSummary) {
        if (giftPopBanner == null) return;

        if (giftSenderAvatarView != null) {
            giftSenderAvatarView.setImageResource(avatarResId != 0 ? avatarResId : R.drawable.img_avatar_0);
        }
        if (giftSenderNameView != null) {
            giftSenderNameView.setText(senderName);
        }
        if (giftActionTipView != null) {
            giftActionTipView.setText(giftSummary != null && !giftSummary.isEmpty()
                    ? giftSummary
                    : getString(R.string.giving_gift));
        }

        giftPopBanner.setVisibility(View.VISIBLE);
        giftPopBanner.clearAnimation();

        AnimationSet animationSet = new AnimationSet(false);
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0f
        );
        translateAnimation.setDuration(1000);
        translateAnimation.setInterpolator(new DecelerateInterpolator());
        animationSet.addAnimation(translateAnimation);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setStartOffset(1000);
        alphaAnimation.setDuration(1000);
        alphaAnimation.setInterpolator(new AccelerateInterpolator());
        animationSet.addAnimation(alphaAnimation);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                giftPopBanner.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        giftPopBanner.startAnimation(animationSet);
    }

    private void confirmLeaveRoomDialog() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Leave or End 12-Seat Voice Room")
                .setMessage("Choose whether to Leave Room (keep room active) or End Room (close room as Host) on channel '" + channelName + "':")
                .setPositiveButton("Leave Room", (d, w) -> {
                    Toast.makeText(this, "Left voice room", Toast.LENGTH_SHORT).show();
                    leaveRoomAndFinish();
                })
                .setNeutralButton("🛑 End Room", (d, w) -> {
                    Toast.makeText(this, "🛑 Voice Room ended by Host", Toast.LENGTH_SHORT).show();
                    leaveRoomAndFinish();
                })
                .setNegativeButton("Stay in Room", null)
                .show();
    }

    private void updateRoomHeaderMetadata() {
        if (roomTitleView != null) {
            roomTitleView.setText(roomTitle);
        }
        if (channelSubView != null) {
            String rId = currentRoom != null ? currentRoom.id : "001";
            channelSubView.setText("ID: " + rId + " • CH: " + channelName + " • Anchor: " + (currentRoom != null ? currentRoom.hostName : "You"));
        }
        if (onlineCountChip != null && currentRoom != null) {
            onlineCountChip.setText("👥 " + currentRoom.listenerCount + " Members ▾");
        }
        if (roomAnnouncementView != null && currentRoom != null) {
            String lockTag = currentRoom.isPrivateLocked ? "🔒 Private" : "🌐 Public";
            roomAnnouncementView.setText("📢 " + currentRoom.topic + " • " + currentRoom.language + " • " + lockTag + " • " + currentRoom.announcement);
        }
    }

    private void updatePkBattleUI() {
        if (pkBattleBar == null || currentRoom == null) return;
        VoiceRoomModels.PkBattleState pk = currentRoom.pkBattle;
        pkBattleBar.setVisibility(pk.isActive ? View.VISIBLE : View.GONE);
        if (pkScoreLeftView != null) {
            pkScoreLeftView.setText("⚔️ " + pk.hostTeamName + ": " + pk.hostScore);
        }
        if (pkScoreRightView != null) {
            pkScoreRightView.setText(pk.challengerTeamName + ": " + pk.challengerScore + " 🛡️");
        }
        if (pkTimerView != null) {
            int mins = pk.remainingSeconds / 60;
            int secs = pk.remainingSeconds % 60;
            pkTimerView.setText(String.format(Locale.US, "PK %02d:%02d", mins, secs));
        }
    }

    private void populateVoiceQuickChatPills(LinearLayout container) {
        container.removeAllViews();
        final String[] pills = new String[]{
                "➕ Create Room", "🚪 Join Room", "✋ Request Mic", "➕ Invite User",
                "⚙️ Room Settings", "🛑 End Room"
        };
        for (int i = 0; i < pills.length; i++) {
            final int idx = i;
            final String label = pills[i];
            int bg = idx <= 1
                    ? Color.argb(200, 236, 72, 153)
                    : (idx <= 4 ? Color.argb(190, 124, 58, 237) : Color.argb(190, 239, 68, 68));
            TextView pill = UiStyleHelper.createStatusChip(
                    this,
                    label,
                    bg,
                    Color.WHITE,
                    Color.argb(110, 244, 114, 182)
            );
            pill.setOnClickListener(v -> {
                if (idx == 0) {
                    showInRoomCreateOrSwitchDialog(true);
                } else if (idx == 1) {
                    showInRoomCreateOrSwitchDialog(false);
                } else if (idx == 2) {
                    showSpeakerRequestQueueDialog();
                } else if (idx == 3) {
                    showChannelMemberListDialog();
                } else if (idx == 4) {
                    showRoomSettingsAndModerationDialog();
                } else {
                    confirmLeaveRoomDialog();
                }
            });
            container.addView(pill, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(this, 6), 0));
        }
    }

    private void showInRoomCreateOrSwitchDialog(boolean createMode) {
        if (createMode) {
            final EditText titleIn = new EditText(this);
            titleIn.setHint("New Voice Room Title");
            titleIn.setText("QLUVX VIP 12-Seat Lounge");
            titleIn.setSingleLine(true);
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("➕ Create Voice Room")
                    .setView(titleIn)
                    .setPositiveButton("Create Room & Join", (d, w) -> {
                        String newTitle = titleIn.getText().toString().trim();
                        if (newTitle.isEmpty()) newTitle = "QLUVX VIP 12-Seat Lounge";
                        String newCh = "qluvx_room_" + (100 + random.nextInt(899));
                        VoiceRoomModels.RoomInfo created = repository.createCustomRoom(
                                newTitle, newCh, "Music & Party", "EN", 12, false, ""
                        );
                        roomTitle = created.title;
                        channelName = created.channelName;
                        currentRoom = created;
                        updateRoomHeaderMetadata();
                        appendRoomMessage("System", "HOST", "✅ Created & switched to new 12-Seat Room: " + roomTitle + " (" + channelName + ")", true);
                        Toast.makeText(this, "Created & joined " + roomTitle, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            final List<VoiceRoomModels.RoomInfo> allRooms = repository.getAllRooms();
            String[] roomLabels = new String[allRooms.size()];
            for (int i = 0; i < allRooms.size(); i++) {
                VoiceRoomModels.RoomInfo r = allRooms.get(i);
                roomLabels[i] = "🎙️ " + r.title + " (" + r.getOccupiedSeatCount() + "/12 Seats • CH: " + r.channelName + ")";
            }
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("🚪 Join Another 12-Seat Voice Room")
                    .setItems(roomLabels, (d, which) -> {
                        VoiceRoomModels.RoomInfo target = allRooms.get(which);
                        roomTitle = target.title;
                        channelName = target.channelName;
                        currentRoom = target;
                        updateRoomHeaderMetadata();
                        appendRoomMessage("System", "JOIN", "🚪 Joined room: " + roomTitle + " (CH: " + channelName + ")", true);
                        Toast.makeText(this, "Joined " + roomTitle, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void renderChatMessages() {
        if (chatMessagesContainer == null || currentRoom == null) return;
        chatMessagesContainer.removeAllViews();
        for (VoiceRoomModels.ChatMessage msg : currentRoom.chatMessages) {
            TextView tv = new TextView(this);
            String prefix = msg.isPinned ? "📌 [Pinned] " : (msg.isSystemOrAnnouncement ? "📢 " : "💬 ");
            tv.setText(prefix + "[" + msg.roleLabel + "] " + msg.senderName + ": " + msg.text);
            int bubbleBg;
            int strokeCol;
            if (msg.text.contains("sent ") || msg.roleLabel.equals("GIFT") || msg.roleLabel.equals("RED PACKET")) {
                tv.setTextColor(UiStyleHelper.COLOR_WARNING_AMBER);
                bubbleBg = Color.argb(55, 245, 158, 11);
                strokeCol = Color.argb(120, 245, 158, 11);
            } else if (msg.isSystemOrAnnouncement || msg.isPinned) {
                tv.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
                bubbleBg = Color.argb(45, 56, 189, 248);
                strokeCol = Color.argb(100, 56, 189, 248);
            } else {
                tv.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
                bubbleBg = Color.argb(165, 15, 23, 42);
                strokeCol = Color.argb(65, 255, 255, 255);
            }
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            tv.setBackground(UiStyleHelper.roundedRect(this, bubbleBg, 10f, strokeCol, 1f));
            int padH = UiStyleHelper.dp(this, 8);
            int padV = UiStyleHelper.dp(this, 4);
            tv.setPadding(padH, padV, padH, padV);
            chatMessagesContainer.addView(tv, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 4)));
        }
        if (chatScrollView != null) {
            chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void appendRoomMessage(String sender, String roleLabel, String content, boolean isSystem) {
        if (currentRoom == null) return;
        currentRoom.chatMessages.add(new VoiceRoomModels.ChatMessage(sender, roleLabel, content, isSystem));
        renderChatMessages();
    }

    private void onSendChatClicked() {
        if (chatInputEdit == null) return;
        String text = chatInputEdit.getText().toString().trim();
        if (text.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastChatTimestampMs < 1000L) {
            Toast.makeText(this, "Slow down! Chat rate limit is 1 message/sec.", Toast.LENGTH_SHORT).show();
            return;
        }
        lastChatTimestampMs = now;
        chatInputEdit.setText("");

        String myRole = (currentLocalSeatIndex == 0)
                ? "Anchor"
                : (currentLocalSeatIndex > 0 ? "Speaker" : "Listener");
        appendRoomMessage("You", myRole, text, false);
    }

    /**
     * Initializes the Agora RTC Engine using BuildConfig.AGORA_APP_ID and fetches a room-specific
     * token from TokenServiceClient (backed by BuildConfig.AGORA_TOKEN_SERVER_URL / fallback).
     */
    public synchronized void initializeAndJoinAgoraVoiceChannel() {
        String appId = getBuildConfigAppId();
        if (appId == null) {
            updateConnectionStateUI(Constants.CONNECTION_STATE_CONNECTED, "CONNECTED (STAGE)", true);
            updateRttIndicator(16);
            return;
        }

        try {
            if (rtcEngine == null) {
                RtcEngineConfig config = new RtcEngineConfig();
                config.mContext = getApplicationContext();
                config.mAppId = appId;
                config.mEventHandler = rtcEventHandler;
                config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

                rtcEngine = RtcEngine.create(config);
                rtcEngine.setAudioProfile(
                        Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO,
                        Constants.AUDIO_SCENARIO_GAME_STREAMING
                );
                rtcEngine.enableAudioVolumeIndication(200, 3, true);
                rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
                isEngineInitialized = true;
            }

            TokenServiceClient.requestRtcToken(
                    channelName,
                    localUid,
                    isBroadcaster,
                    (token, fromBackendServer) -> {
                        if (rtcEngine == null) return;
                        try {
                            ChannelMediaOptions options = new ChannelMediaOptions();
                            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
                            options.clientRoleType = isBroadcaster
                                    ? Constants.CLIENT_ROLE_BROADCASTER
                                    : Constants.CLIENT_ROLE_AUDIENCE;
                            options.publishMicrophoneTrack = isBroadcaster && !isLocalMuted;
                            options.autoSubscribeAudio = true;
                            rtcEngine.joinChannel(token, channelName, localUid, options);
                        } catch (Throwable t) {
                            Log.e(TAG, "Agora joinChannel error", t);
                        }
                    }
            );
        } catch (Throwable t) {
            Log.e(TAG, "Agora RtcEngine init fallback", t);
            updateConnectionStateUI(Constants.CONNECTION_STATE_CONNECTED, "CONNECTED (STAGE)", true);
        }
    }

    private boolean hasRequiredPermissions() {
        boolean micGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean btGranted = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
            return micGranted && btGranted;
        }
        return micGranted;
    }

    private void requestAudioPermissionsAndConnect() {
        List<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), REQ_AUDIO_PERMISSIONS);
        } else {
            initializeAndJoinAgoraVoiceChannel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERMISSIONS && hasRequiredPermissions()) {
            initializeAndJoinAgoraVoiceChannel();
        }
    }

    private void onSeatClicked(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= seats.size()) return;
        final VoiceRoomModels.SpeakerSeat targetSeat = seats.get(seatIndex);

        List<String> actions = new ArrayList<>();
        if (!targetSeat.isOccupied) {
            if (!targetSeat.isLocked) {
                actions.add("Take Seat " + (seatIndex + 1) + " (" + getString(R.string.to_broadcast) + ")");
                actions.add("➕ Invite User to Seat " + (seatIndex + 1));
            }
            actions.add(targetSeat.isLocked
                    ? ("🔓 Unlock Seat " + (seatIndex + 1))
                    : ("🔒 Lock Seat " + (seatIndex + 1)));
        } else if (targetSeat.uid == localUid) {
            actions.add(isLocalMuted ? "🎙️ Mic On" : "🔇 Mic Off");
            actions.add("🎭 Voice Effect & Voice Beautify (" + activeVoiceEffectLabel + ")");
            actions.add("Leave Seat (" + getString(R.string.to_audience) + ")");
        } else {
            actions.add(targetSeat.isMuted
                    ? ("🎙️ Unmute Participant • " + targetSeat.userName)
                    : ("🔇 Mute Participant • " + targetSeat.userName));
            actions.add("🎁 Send Gift to " + targetSeat.userName);
            actions.add("⬇️ Remove Participant (" + targetSeat.userName + ")");
            actions.add("🔒 Lock Seat " + (seatIndex + 1));
            actions.add("🚫 Block User (" + targetSeat.userName + ")");
            actions.add("🚩 Report User (" + targetSeat.userName + ")");
            actions.add("👑 Transfer Host Crown to " + targetSeat.userName);
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Seat " + (seatIndex + 1) + " • " + (targetSeat.isOccupied ? targetSeat.userName : (targetSeat.isLocked ? "Locked" : "Open")))
                .setItems(actions.toArray(new String[0]), (dialog, which) -> {
                    String choice = actions.get(which);
                    if (choice.startsWith("Take Seat")) {
                        if (currentLocalSeatIndex >= 0 && currentLocalSeatIndex < seats.size()) {
                            seats.get(currentLocalSeatIndex).vacate();
                        }
                        targetSeat.assignUser(
                                localUid,
                                "You",
                                UiStyleHelper.COLOR_AGORA_BLUE,
                                isLocalMuted,
                                seatIndex == 0 ? "Host" : ("Seat " + (seatIndex + 1))
                        );
                        currentLocalSeatIndex = seatIndex;
                        setBroadcasterRole(true);
                        refreshAllSeatsAndControls();
                    } else if (choice.startsWith("➕ Invite User")) {
                        int guestUid = 300 + seatIndex;
                        targetSeat.assignUser(guestUid, "Guest " + (seatIndex + 1), UiStyleHelper.COLOR_AGORA_CYAN, false, "Speaker");
                        appendRoomMessage("System", "Anchor", "🎙️ Invited Guest " + (seatIndex + 1) + " to Seat " + (seatIndex + 1), true);
                        refreshAllSeatsAndControls();
                    } else if (choice.startsWith("🔓 Unlock Seat") || choice.startsWith("🔒 Lock Seat")) {
                        if (targetSeat.isOccupied && choice.startsWith("🔒 Lock Seat")) {
                            targetSeat.vacate();
                            targetSeat.isLocked = true;
                        } else {
                            targetSeat.isLocked = !targetSeat.isLocked;
                        }
                        appendRoomMessage("System", "Anchor", (targetSeat.isLocked ? "🔒 Locked Seat " : "🔓 Unlocked Seat ") + (seatIndex + 1), true);
                        refreshAllSeatsAndControls();
                    } else if (choice.startsWith("🎙️ Mic On") || choice.startsWith("🔇 Mic Off")) {
                        onMicToggleClicked();
                    } else if (choice.startsWith("🎭 Voice Effect")) {
                        showVoiceChangerAndBeautifyDialog();
                    } else if (choice.startsWith("Leave Seat")) {
                        targetSeat.vacate();
                        currentLocalSeatIndex = -1;
                        setBroadcasterRole(false);
                        refreshAllSeatsAndControls();
                    } else if (choice.contains("Participant • ")) {
                        targetSeat.isMuted = !targetSeat.isMuted;
                        if (targetSeat.isMuted) targetSeat.speakingVolume = 0;
                        appendRoomMessage("System", "Anchor", (targetSeat.isMuted ? "🔇 Muted " : "🎙️ Unmuted ") + targetSeat.userName, true);
                        updateSeatUI(seatIndex);
                    } else if (choice.startsWith("🎁 Send Gift")) {
                        showGiftPanelDialog();
                    } else if (choice.startsWith("⬇️ Remove Participant")) {
                        String removed = targetSeat.userName;
                        targetSeat.vacate();
                        appendRoomMessage("System", "Anchor", "⬇️ Removed participant " + removed + " from Seat " + (seatIndex + 1), true);
                        refreshAllSeatsAndControls();
                    } else if (choice.startsWith("🚫 Block User")) {
                        String blocked = targetSeat.userName;
                        targetSeat.vacate();
                        appendRoomMessage("System", "MODERATION", "🚫 Blocked user " + blocked + " from room", true);
                        Toast.makeText(this, "🚫 Blocked " + blocked, Toast.LENGTH_SHORT).show();
                        refreshAllSeatsAndControls();
                    } else if (choice.startsWith("🚩 Report User")) {
                        String repUser = targetSeat.userName;
                        repository.addModerationReport("Reported user " + repUser + " on Seat " + (seatIndex + 1) + " in room " + channelName);
                        economy.logAudit("Reported user " + repUser + " in voice room " + channelName);
                        Toast.makeText(this, "🚩 Reported " + repUser + " to Moderators", Toast.LENGTH_SHORT).show();
                    } else if (choice.startsWith("👑 Transfer Host")) {
                        targetSeat.roleTag = "Host";
                        if (currentRoom != null) currentRoom.hostName = targetSeat.userName;
                        appendRoomMessage("System", "Anchor", "👑 Host crown transferred to " + targetSeat.userName, true);
                        updateRoomHeaderMetadata();
                        refreshAllSeatsAndControls();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyCapacityProgression(int capacity) {
        if (currentRoom == null) return;
        currentRoom.activeSeatCapacity = capacity;
        for (int i = 0; i < seats.size(); i++) {
            seats.get(i).isLocked = (i >= capacity);
        }
    }

    private void showSpeakerRequestQueueDialog() {
        if (currentRoom == null) return;
        List<String> options = new ArrayList<>();
        for (String req : currentRoom.speakerRequests) {
            options.add("✅ Accept Mic Request: " + req);
            options.add("❌ Reject Mic Request: " + req);
        }
        options.add("✋ Request Mic (Join Speaker Queue)");
        options.add("➕ Invite User from Audience (" + currentRoom.listenerCount + " online)");
        options.add("📐 Expand Room Seat Capacity (1 → 4 → 8 → 12)");

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎙️ Mic Requests & Seat Controls (" + currentRoom.activeSeatCapacity + "/12)")
                .setItems(options.toArray(new String[0]), (d, which) -> {
                    String sel = options.get(which);
                    if (sel.startsWith("✅ Accept Mic Request: ")) {
                        String raw = sel.replace("✅ Accept Mic Request: ", "");
                        String name = raw.replace(" (Requesting Seat)", "").trim();
                        currentRoom.speakerRequests.remove(raw);
                        for (int i = 0; i < seats.size(); i++) {
                            VoiceRoomModels.SpeakerSeat s = seats.get(i);
                            if (!s.isOccupied && !s.isLocked) {
                                s.assignUser(500 + i, name, UiStyleHelper.COLOR_LIVE_GREEN, false, "Speaker");
                                appendRoomMessage("System", "Anchor", "✅ Accepted Mic Request for " + name + " on Seat " + (i + 1), true);
                                break;
                            }
                        }
                        refreshAllSeatsAndControls();
                    } else if (sel.startsWith("❌ Reject Mic Request: ")) {
                        String raw = sel.replace("❌ Reject Mic Request: ", "");
                        String name = raw.replace(" (Requesting Seat)", "").trim();
                        currentRoom.speakerRequests.remove(raw);
                        appendRoomMessage("System", "Anchor", "❌ Rejected Mic Request from " + name, true);
                        Toast.makeText(this, "Rejected mic request from " + name, Toast.LENGTH_SHORT).show();
                    } else if (sel.startsWith("✋ Request Mic")) {
                        currentRoom.speakerRequests.add("You (Requesting Seat)");
                        appendRoomMessage("You", "LISTENER", "✋ Requested a microphone seat", false);
                        Toast.makeText(this, "✋ Mic Request sent to Host!", Toast.LENGTH_SHORT).show();
                    } else if (sel.startsWith("➕ Invite User")) {
                        showChannelMemberListDialog();
                    } else {
                        int nextCap = currentRoom.activeSeatCapacity == 1 ? 4
                                : (currentRoom.activeSeatCapacity == 4 ? 8
                                : (currentRoom.activeSeatCapacity == 8 ? 12 : 4));
                        applyCapacityProgression(nextCap);
                        appendRoomMessage("System", "Anchor", "📐 Room seat capacity set to " + nextCap + " seats", true);
                        refreshAllSeatsAndControls();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showGiftPanelDialog() {
        String receiver = (seats.size() > 1 && seats.get(1).isOccupied) ? seats.get(1).userName : "Host (Seat 1)";
        UiStyleHelper.showUnifiedGiftPanelDialog(
                this,
                economy,
                "You",
                receiver,
                (gift, combo, tx) -> {
                    appendRoomMessage("You", "GIFT", tx.description, false);
                    int myAvatar = (currentLocalSeatIndex >= 0 && currentLocalSeatIndex < seats.size())
                            ? seats.get(currentLocalSeatIndex).avatarResId
                            : R.drawable.img_avatar_0;
                    triggerGiftPopViewAnimation("You", myAvatar, gift.iconLabel + " " + gift.name + " x" + combo);
                    if (currentRoom != null && currentRoom.pkBattle.isActive) {
                        currentRoom.pkBattle.hostScore += (gift.coinPrice * combo);
                        updatePkBattleUI();
                    }
                }
        );
    }

    private void showPkBattleControlDialog() {
        if (currentRoom == null) return;
        final VoiceRoomModels.PkBattleState pk = currentRoom.pkBattle;
        String[] items = new String[]{
                pk.isActive ? "⏸️ End Current PK Battle" : "⚔️ Start 3-Minute PK Battle",
                "⚡ Boost Team Host (+250 PK Points)",
                "🛡️ Boost Team Challenger (+250 PK Points)",
                "🔄 Reset PK Scoreboard"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("⚔️ Room PK Battle Mode")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        pk.isActive = !pk.isActive;
                        pk.remainingSeconds = 180;
                        appendRoomMessage("System", "Anchor",
                                pk.isActive ? "⚔️ PK Battle started! Send gifts to support your team." : "🏁 PK Battle ended!", true);
                    } else if (which == 1) {
                        pk.hostScore += 250;
                    } else if (which == 2) {
                        pk.challengerScore += 250;
                    } else {
                        pk.hostScore = 0;
                        pk.challengerScore = 0;
                        pk.remainingSeconds = 180;
                    }
                    updatePkBattleUI();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private boolean inEarMonitoringEnabled = false;

    private void showRoomSettingsAndModerationDialog() {
        if (currentRoom == null) return;
        String[] options = new String[]{
                "🎨 Open Wallpaper & Theme Set Studio (A–Z)",
                "🧧 Drop Room Red Packet / Coin Rain (500 Coins for Room)",
                "🎲 Play In-Room Party Mini-Game (Dice 🎲 / RPS ✊ / Spin 🎡)",
                "🎧 In-Ear Monitor: " + (inEarMonitoringEnabled ? "ON ✓ (Tap to Turn Off)" : "OFF (Tap to Enable)"),
                "👏 Play Host Soundboard Effect (Applause / Laugh / Cheer)",
                "📢 Edit Room Topic & Welcome Announcement",
                "🖼️ Switch Channel Background Wallpaper (bg_channel_0..4)",
                "🎵 Toggle Background Audio Mixing (mixing.mp3)",
                "🎭 Open Voice Effect & Voice Beautify Changer",
                "👥 Open Channel Member List (" + currentRoom.listenerCount + " people)",
                "📐 Cycle Seat Capacity (1 → 4 → 8 → 12) [Current: " + currentRoom.activeSeatCapacity + "]",
                currentRoom.isPrivateLocked ? "🔓 Unlock Private Room" : "🔒 Lock Room with PIN (2468)",
                "🛡️ Open Owner & Admin Studio",
                "🚩 Report Room / Moderation Action",
                "🏠 Back to Room Discovery Lobby"
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎨 Room Theme, Wallpaper, Party & Audio")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        UiStyleHelper.showThemeAndWallpaperStudioDialog(this, () -> {
                            if (roomChannelBgImage != null) {
                                roomChannelBgImage.setImageResource(UiStyleHelper.getActiveWallpaperResId(this));
                                roomChannelBgImage.setAlpha(Math.max(0.45f, UiStyleHelper.getWallpaperAlpha(this)));
                            }
                            refreshAllSeatsAndControls();
                        });
                    } else if (which == 1) {
                        int bonus = 88 + random.nextInt(160);
                        economy.rechargeVerifiedPack(bonus, "Room Red Packet Lucky Grab (+" + bonus + " Coins)", null);
                        triggerGiftPopViewAnimation("You (Anchor)", R.drawable.img_avatar_0, "🧧 Red Packet Coin Rain (+500 Coins)!");
                        appendRoomMessage("System", "RED PACKET", "🧧 You dropped a 500-Coin Red Packet! You grabbed +" + bonus + " Lucky Coins!", true);
                    } else if (which == 2) {
                        showRoomPartyMiniGamesDialog();
                    } else if (which == 3) {
                        inEarMonitoringEnabled = !inEarMonitoringEnabled;
                        if (rtcEngine != null && isEngineInitialized) {
                            try {
                                rtcEngine.enableInEarMonitoring(inEarMonitoringEnabled);
                            } catch (Throwable ignored) {
                            }
                        }
                        Toast.makeText(this, inEarMonitoringEnabled ? "🎧 Agora In-Ear Monitoring: ON" : "🎧 In-Ear Monitoring: OFF", Toast.LENGTH_SHORT).show();
                    } else if (which == 4) {
                        String[] sfx = new String[]{"👏 Crowd Applause!", "😂 Studio Laughter!", "🎉 Party Horn & Cheer!", "🥁 PK Drumroll!"};
                        String picked = sfx[random.nextInt(sfx.length)];
                        appendRoomMessage("You (Anchor)", "SOUNDBOARD", "🔊 Played Sound Effect: " + picked, false);
                        Toast.makeText(this, "🔊 " + picked, Toast.LENGTH_SHORT).show();
                    } else if (which == 5) {
                        final EditText input = new EditText(this);
                        input.setHint("Enter new room announcement banner...");
                        if (roomAnnouncementView != null) input.setText(roomAnnouncementView.getText());
                        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle("📢 Update Room Announcement")
                                .setView(input)
                                .setPositiveButton("Save", (dlg, w) -> {
                                    String txt = input.getText().toString().trim();
                                    if (!txt.isEmpty() && roomAnnouncementView != null) {
                                        roomAnnouncementView.setText(txt);
                                        appendRoomMessage("System", "Anchor", "📢 Room Announcement updated: " + txt, true);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else if (which == 6) {
                        currentBgThemeIndex = (currentBgThemeIndex + 1) % VoiceRoomModels.CHANNEL_BG_DRAWABLES.length;
                        int newBg = VoiceRoomModels.getChannelBgResId(currentBgThemeIndex);
                        currentRoom.backgroundResId = newBg;
                        UiStyleHelper.setSelectedWallpaperIndex(this, currentBgThemeIndex);
                        if (roomChannelBgImage != null) {
                            roomChannelBgImage.setImageResource(newBg);
                        }
                        Toast.makeText(this, "🖼️ Switched Wallpaper #" + (currentBgThemeIndex + 1), Toast.LENGTH_SHORT).show();
                    } else if (which == 7) {
                        toggleAudioMixing();
                    } else if (which == 8) {
                        showVoiceChangerAndBeautifyDialog();
                    } else if (which == 9) {
                        showChannelMemberListDialog();
                    } else if (which == 10) {
                        int next = currentRoom.activeSeatCapacity == 1 ? 4
                                : (currentRoom.activeSeatCapacity == 4 ? 8
                                : (currentRoom.activeSeatCapacity == 8 ? 12 : 4));
                        applyCapacityProgression(next);
                        refreshAllSeatsAndControls();
                    } else if (which == 11) {
                        currentRoom.isPrivateLocked = !currentRoom.isPrivateLocked;
                        if (currentRoom.isPrivateLocked) currentRoom.roomPassword = "2468";
                        updateRoomHeaderMetadata();
                        Toast.makeText(this, currentRoom.isPrivateLocked ? "Room Locked (PIN: 2468)" : "Room Unlocked", Toast.LENGTH_SHORT).show();
                    } else if (which == 12) {
                        startActivity(new Intent(this, AdminStudioActivity.class));
                    } else if (which == 13) {
                        repository.addModerationReport("User " + localUid + " reported room " + currentRoom.id + " (" + currentRoom.channelName + ")");
                        economy.logAudit("Moderation report submitted for room " + currentRoom.id);
                        Toast.makeText(this, "Report logged for moderators.", Toast.LENGTH_SHORT).show();
                    } else if (which == 14) {
                        leaveRoomAndFinish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRoomPartyMiniGamesDialog() {
        String[] games = new String[]{
                "🎲 Roll Lucky Dice (1–6 on Stage)",
                "✊✋✌️ Play Rock-Paper-Scissors",
                "🎡 Spin Truth or Dare Wheel",
                "🎰 Spin Lucky 777 Slot Machine"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎲 In-Room Interactive Party Games")
                .setItems(games, (d, which) -> {
                    if (which == 0) {
                        int roll = 1 + random.nextInt(6);
                        appendRoomMessage("You", "PARTY GAME", "🎲 Rolled a Lucky Dice on stage: [ " + roll + " ]!", false);
                        Toast.makeText(this, "🎲 You rolled a " + roll + "!", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        String[] hands = new String[]{"✊ Rock", "✋ Paper", "✌️ Scissors"};
                        String pick = hands[random.nextInt(hands.length)];
                        appendRoomMessage("You", "PARTY GAME", "✊✋✌️ Threw " + pick + " on the voice stage!", false);
                        Toast.makeText(this, "You threw " + pick + "!", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        String[] prompts = new String[]{
                                "Sing a 10-second chorus with KTV reverb ON! 🎤",
                                "Send a Flame Tap 🔥 to Seat 2!",
                                "Share your favorite city in the room chat! 🌍",
                                "Do a 15-second voice impression! 🎭"
                        };
                        String dare = prompts[random.nextInt(prompts.length)];
                        appendRoomMessage("System", "TRUTH/DARE", "🎡 Wheel landed on: " + dare, true);
                    } else {
                        int prize = 100 + random.nextInt(300);
                        economy.rechargeVerifiedPack(prize, "Voice Room 777 Slot Jackpot", null);
                        appendRoomMessage("You", "JACKPOT", "🎰 Hit 7️⃣7️⃣7️⃣ on the Room Slot and won +" + prize + " Coins!", false);
                        Toast.makeText(this, "🎰 777 Jackpot! +" + prize + " Coins!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void shareRoomLink() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        String rId = currentRoom != null ? currentRoom.id : "001";
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my live voice room on QLUVX! Room: " + roomTitle + " (" + rId + ") • Channel: " + channelName);
        startActivity(Intent.createChooser(sendIntent, "Share Voice Room"));
    }

    private void setBroadcasterRole(boolean broadcaster) {
        this.isBroadcaster = broadcaster;
        if (rtcEngine != null && isEngineInitialized) {
            try {
                rtcEngine.setClientRole(broadcaster
                        ? Constants.CLIENT_ROLE_BROADCASTER
                        : Constants.CLIENT_ROLE_AUDIENCE);
                ChannelMediaOptions options = new ChannelMediaOptions();
                options.clientRoleType = broadcaster
                        ? Constants.CLIENT_ROLE_BROADCASTER
                        : Constants.CLIENT_ROLE_AUDIENCE;
                options.publishMicrophoneTrack = broadcaster && !isLocalMuted;
                rtcEngine.updateChannelMediaOptions(options);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to update client role", t);
            }
        }
    }

    private void onMicToggleClicked() {
        if (currentLocalSeatIndex < 0) {
            for (VoiceRoomModels.SpeakerSeat seat : seats) {
                if (!seat.isOccupied && !seat.isLocked) {
                    seat.assignUser(localUid, "You", UiStyleHelper.COLOR_AGORA_BLUE, false, "Seat " + (seat.seatIndex + 1));
                    currentLocalSeatIndex = seat.seatIndex;
                    setBroadcasterRole(true);
                    refreshAllSeatsAndControls();
                    return;
                }
            }
            return;
        }

        VoiceRoomModels.SpeakerSeat mySeat = seats.get(currentLocalSeatIndex);
        isLocalMuted = !mySeat.isMuted;
        mySeat.isMuted = isLocalMuted;
        if (isLocalMuted) {
            mySeat.speakingVolume = 0;
        } else if (!hasRequiredPermissions()) {
            requestAudioPermissionsAndConnect();
        } else if (!isEngineInitialized) {
            initializeAndJoinAgoraVoiceChannel();
        }

        if (rtcEngine != null && isEngineInitialized) {
            try {
                rtcEngine.muteLocalAudioStream(isLocalMuted);
                ChannelMediaOptions options = new ChannelMediaOptions();
                options.clientRoleType = isBroadcaster
                        ? Constants.CLIENT_ROLE_BROADCASTER
                        : Constants.CLIENT_ROLE_AUDIENCE;
                options.publishMicrophoneTrack = isBroadcaster && !isLocalMuted;
                rtcEngine.updateChannelMediaOptions(options);
            } catch (Throwable t) {
                Log.w(TAG, "Error muting local stream", t);
            }
        }
        refreshAllSeatsAndControls();
    }

    private void onAudioOutputSwitchClicked() {
        isSpeakerphoneOn = !isSpeakerphoneOn;
        if (rtcEngine != null && isEngineInitialized) {
            try {
                rtcEngine.setEnableSpeakerphone(isSpeakerphoneOn);
            } catch (Throwable t) {
                Log.w(TAG, "Error switching speakerphone", t);
            }
        }
        if (audioOutputBtn != null) {
            audioOutputBtn.setText(isSpeakerphoneOn ? "🔊 Spkr" : "🔈 Ear");
        }
        Toast.makeText(
                this,
                isSpeakerphoneOn ? "Audio Output: Speakerphone" : "Audio Output: Earpiece",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateSeatUI(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= seatViews.size() || seatIndex >= seats.size()) return;
        VoiceRoomModels.SpeakerSeat seat = seats.get(seatIndex);
        boolean isLocal = seat.isOccupied && seat.uid == localUid;
        seatViews.get(seatIndex).bindSeat(seat, isLocal);
    }

    private void refreshAllSeatsAndControls() {
        int occupied = 0;
        for (int i = 0; i < seats.size() && i < seatViews.size(); i++) {
            if (seats.get(i).isOccupied) occupied++;
            updateSeatUI(i);
        }
        if (seatCountChip != null) {
            int cap = currentRoom != null ? currentRoom.activeSeatCapacity : 12;
            seatCountChip.setText("🎙️ " + occupied + "/" + cap + " Seats");
        }
        if (micToggleBtn != null) {
            micToggleBtn.setText(isLocalMuted ? "🔇 Muted" : "🎙️ Mic");
            int bg = isLocalMuted ? Color.argb(45, 239, 68, 68) : UiStyleHelper.COLOR_AGORA_BLUE;
            int stroke = isLocalMuted ? Color.argb(130, 239, 68, 68) : Color.argb(130, 14, 165, 233);
            micToggleBtn.setBackground(UiStyleHelper.rippleButton(this, bg, 12f, stroke, 1f));
        }
        if (audioOutputBtn != null) {
            audioOutputBtn.setText(isSpeakerphoneOn ? "🔊 Spkr" : "🔈 Ear");
        }
        updateVoiceChangerButtonUI();
        updateAudioMixingButtonUI();
    }

    private void updateConnectionStateUI(int state, String label, boolean connected) {
        if (connectionStateChip != null) {
            connectionStateChip.setText("● " + label);
            connectionStateChip.setTextColor(connected
                    ? UiStyleHelper.COLOR_LIVE_GREEN
                    : UiStyleHelper.COLOR_WARNING_AMBER);
        }
    }

    private void updateRttIndicator(int rttMs) {
        this.currentRttMs = rttMs;
        if (rttLatencyChip != null) {
            rttLatencyChip.setText("⚡ RTT: " + rttMs + " ms");
            if (rttMs < 80) {
                rttLatencyChip.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
            } else if (rttMs < 180) {
                rttLatencyChip.setTextColor(UiStyleHelper.COLOR_WARNING_AMBER);
            } else {
                rttLatencyChip.setTextColor(UiStyleHelper.COLOR_DANGER_RED);
            }
        }
    }

    private void updateActiveSpeakerBanner() {
        if (activeSpeakerBanner == null) return;
        StringBuilder sb = new StringBuilder();
        for (VoiceRoomModels.SpeakerSeat seat : seats) {
            if (seat.isOccupied && !seat.isMuted && seat.speakingVolume > 8) {
                if (sb.length() > 0) sb.append("  •  ");
                String name = (seat.uid == localUid) ? "You" : seat.userName;
                sb.append(name).append(" (Vol ").append(seat.speakingVolume).append(")");
            }
        }
        if (sb.length() > 0) {
            activeSpeakerBanner.setText("🔊 Active Speaker: " + sb.toString()
                    + (isAudioMixingActive ? "  •  🎵 Mixing ON" : ""));
            activeSpeakerBanner.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
        } else {
            activeSpeakerBanner.setText("🔊 Agora Audio • Preset: " + activeVoiceEffectLabel
                    + (isAudioMixingActive ? " • 🎵 Mixing ON" : " • Tap Mixing or Changer"));
            activeSpeakerBanner.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        }
    }

    private void installSafeBackgroundThreadGuard() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String tName = thread != null ? thread.getName() : "";
            if (Looper.myLooper() != Looper.getMainLooper()
                    && (throwable instanceof SecurityException
                    || tName.toLowerCase().contains("agora")
                    || tName.toLowerCase().contains("audio"))) {
                Log.w(TAG, "Guarded background audio thread exception: " + throwable.getMessage());
                return;
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private void leaveRoomAndFinish() {
        leaveAndDestroyAgoraEngine();
        finish();
    }

    private synchronized void leaveAndDestroyAgoraEngine() {
        isJoinedChannel = false;
        isAudioMixingActive = false;
        stopLocalAssetMixingPlayer();
        if (rtcEngine != null) {
            try {
                rtcEngine.stopAudioMixing();
                rtcEngine.leaveChannel();
                RtcEngine.destroy();
            } catch (Throwable ignored) {
            }
            rtcEngine = null;
            isEngineInitialized = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(stageWaveRunnable);
        leaveAndDestroyAgoraEngine();
    }
}
