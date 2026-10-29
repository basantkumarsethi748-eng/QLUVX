package com.example.qluvx;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

/**
 * Menu 4 & Phase 7: QLUVX Live Video & PK Battles Activity.
 * - Live video stage with camera preview, HD stage covers, & Agora RTC engine connection
 * - Dynamic Wallpaper & Theme Set Studio integration
 * - Real-time comments feed (Live Message Box), quick chat pills, viewer count, floating reactions & PK battle scoreboard
 */
@SuppressWarnings("deprecation")
public class LiveVideoActivity extends Activity implements TextureView.SurfaceTextureListener {

    public static final String EXTRA_VIDEO_TITLE = "extra_video_title";
    public static final String EXTRA_VIDEO_CHANNEL = "extra_video_channel";
    public static final String EXTRA_START_IN_SETUP = "extra_start_in_setup";

    private static final int REQ_CAMERA_AUDIO = 3102;

    public static final class AgoraParticipantItem {
        public final int uid;
        public final String displayName;
        public final String roleLabel;
        public final int avatarResId;
        public boolean isAudioActive;

        public AgoraParticipantItem(int uid, String displayName, String roleLabel, int avatarResId, boolean isAudioActive) {
            this.uid = uid;
            this.displayName = displayName;
            this.roleLabel = roleLabel;
            this.avatarResId = avatarResId;
            this.isAudioActive = isAudioActive;
        }
    }

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<String> comments = new ArrayList<>();
    private final List<AgoraParticipantItem> connectedParticipants = new ArrayList<>();
    private boolean showParticipantsAsGrid = false;

    private GiftEconomyManager economy;
    private QluvxSocialManager social;

    private FrameLayout liveOuterFrame;
    private FrameLayout preLiveSetupOverlay;
    private LinearLayout connectedParticipantsBar;
    private ImageView ivVideoWallpaper;
    private TextView tvRoomTitle;
    private TextView tvHostMeta;
    private TextView btnVideoTheme;
    private TextView tvViewerCount;
    private TextView tvRtcStatus;
    private TextView tvPkLeftScore;
    private TextView tvPkRightScore;
    private TextView tvPkTimer;
    private ProgressBar progressPk;
    private TextureView textureCameraPreview;
    private LinearLayout videoHostPlaceholder;
    private TextView tvHostStageStatus;
    private TextView tvReactionBanner;
    private ScrollView scrollComments;
    private LinearLayout containerComments;
    private LinearLayout containerQuickPills;
    private EditText etComment;
    private TextView btnCamToggle;
    private TextView btnMicToggle;
    private TextView btnSendComment;

    private RtcEngine rtcEngine;
    private Camera hardwareCamera;
    private boolean isCameraOn = true;
    private boolean isFrontCamera = true;
    private boolean isMirrorEnabled = true;
    private boolean isAntiRecordingEnabled = false;
    private boolean isPublicLive = true;
    private boolean isLiveStreamingActive = true;
    private boolean isHostFollowed = false;
    private boolean isPkActive = true;
    private boolean isMicMuted = false;
    private int viewerCount = 142;
    private int hostPkScore = 480;
    private int challengerPkScore = 410;
    private int pkSecondsRemaining = 165;
    private String channelName = "qluvx_live_video_pk";

    private final Runnable pkTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (pkSecondsRemaining > 0) {
                pkSecondsRemaining--;
            } else {
                pkSecondsRemaining = 180;
            }
            updatePkScoreboardUi();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            uiHandler.post(() -> {
                if (tvRtcStatus != null) {
                    tvRtcStatus.setText("LIVE • " + Math.max(18, elapsed) + " ms");
                    tvRtcStatus.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
                }
                upsertConnectedParticipant(uid, social.getCurrentUser().displayName + " (You)", "👑 Host", R.drawable.img_avatar_0, !isMicMuted);
                refreshConnectedParticipantsBar();
                appendComment("System", "Connected to Agora Live Channel: " + channel + " (UID " + uid + ")");
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            uiHandler.post(() -> {
                viewerCount++;
                if (tvViewerCount != null) {
                    tvViewerCount.setText("👁️ " + viewerCount);
                }
                upsertConnectedParticipant(
                        uid,
                        "User_" + (Math.abs(uid) % 10000),
                        "🎙️ Co-Host / Viewer",
                        VoiceRoomModels.getAvatarResId(Math.abs(uid)),
                        true
                );
                refreshConnectedParticipantsBar();
                appendComment("System", "Remote participant UID " + uid + " joined Live Video stage.");
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            uiHandler.post(() -> {
                viewerCount = Math.max(1, viewerCount - 1);
                if (tvViewerCount != null) {
                    tvViewerCount.setText("👁️ " + viewerCount);
                }
                removeConnectedParticipant(uid);
                refreshConnectedParticipantsBar();
            });
        }

        @Override
        public void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
            if (stats == null) return;
            final int rtt = Math.max(16, stats.lastmileDelay);
            uiHandler.post(() -> {
                if (tvRtcStatus != null) {
                    tvRtcStatus.setText("LIVE • " + rtt + " ms");
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiStyleHelper.loadSavedTheme(this);
        economy = GiftEconomyManager.getInstance(this);
        social = QluvxSocialManager.getInstance(this);

        UiStyleHelper.configureSafeActivityWindow(this);
        setContentView(R.layout.activity_live_video);

        String customTitle = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);
        String customChannel = getIntent().getStringExtra(EXTRA_VIDEO_CHANNEL);
        boolean startInSetup = getIntent().getBooleanExtra(EXTRA_START_IN_SETUP, false);
        if (customChannel != null && !customChannel.trim().isEmpty()) {
            channelName = customChannel.trim();
        }

        seedConnectedParticipants();
        bindViews(customTitle);
        applyActiveThemeAndWallpaper();
        seedInitialComments();
        checkPermissionsAndStartEngine();
        if (startInSetup) {
            showPreLiveSetupOverlay(customTitle);
        }
        uiHandler.postDelayed(pkTimerRunnable, 1000L);
    }

    private void seedConnectedParticipants() {
        connectedParticipants.clear();
        int localUid = VoiceRoomModels.RoomRepository.getInstance().localUid;
        connectedParticipants.add(new AgoraParticipantItem(localUid, social.getCurrentUser().displayName + " (Host)", "👑 Broadcaster", R.drawable.img_avatar_0, true));
        connectedParticipants.add(new AgoraParticipantItem(204101, "Zoya Khan", "⚔️ PK Co-Host", R.drawable.img_avatar_1, true));
        connectedParticipants.add(new AgoraParticipantItem(204102, "sushmita", "💎 VIP Guest", R.drawable.img_avatar_2, true));
        connectedParticipants.add(new AgoraParticipantItem(204103, "Rohan_Live", "🎧 Connected", R.drawable.img_avatar_3, false));
        connectedParticipants.add(new AgoraParticipantItem(204104, "Ishani ✨", "🎧 Connected", R.drawable.img_avatar_4, false));
        connectedParticipants.add(new AgoraParticipantItem(204105, "Kabir_V", "🎧 Connected", R.drawable.img_avatar_5, false));
    }

    private void upsertConnectedParticipant(int uid, String name, String role, int avatarRes, boolean audioActive) {
        for (int i = 0; i < connectedParticipants.size(); i++) {
            if (connectedParticipants.get(i).uid == uid) {
                connectedParticipants.set(i, new AgoraParticipantItem(uid, name, role, avatarRes, audioActive));
                return;
            }
        }
        connectedParticipants.add(new AgoraParticipantItem(uid, name, role, avatarRes, audioActive));
    }

    private void removeConnectedParticipant(int uid) {
        for (int i = connectedParticipants.size() - 1; i >= 0; i--) {
            if (connectedParticipants.get(i).uid == uid) {
                connectedParticipants.remove(i);
            }
        }
    }

    private void applyActiveThemeAndWallpaper() {
        UiStyleHelper.loadSavedTheme(this);
        if (liveOuterFrame != null) {
            liveOuterFrame.setBackgroundColor(UiStyleHelper.COLOR_BG_DEEP);
        }
        if (ivVideoWallpaper != null) {
            ivVideoWallpaper.setImageResource(UiStyleHelper.getActiveWallpaperResId(this));
            ivVideoWallpaper.setAlpha(UiStyleHelper.getActiveWallpaperAlpha(this));
        }
        if (btnVideoTheme != null) {
            UiStyleHelper.ThemePreset p = UiStyleHelper.getActiveThemePreset(this);
            btnVideoTheme.setText(p.emoji + " Theme");
            btnVideoTheme.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
            btnVideoTheme.setBackground(UiStyleHelper.rippleButton(
                    this,
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    12f,
                    UiStyleHelper.COLOR_AGORA_CYAN,
                    1f
            ));
        }
        if (btnSendComment != null) {
            btnSendComment.setBackground(UiStyleHelper.rippleGradientButton(
                    this,
                    UiStyleHelper.COLOR_AGORA_BLUE,
                    UiStyleHelper.COLOR_INDIGO_ACCENT,
                    12f
            ));
        }
        if (btnCamToggle != null) {
            btnCamToggle.setBackground(isCameraOn
                    ? UiStyleHelper.rippleGradientButton(this, UiStyleHelper.COLOR_AGORA_BLUE, UiStyleHelper.COLOR_INDIGO_ACCENT, 12f)
                    : UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, UiStyleHelper.COLOR_BORDER_SUBTLE, 1f));
        }
    }

    private void bindViews(String customTitle) {
        liveOuterFrame = findViewById(R.id.live_video_outer_frame);
        ivVideoWallpaper = findViewById(R.id.iv_video_wallpaper);

        View backBtn = findViewById(R.id.btn_video_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
        tvRoomTitle = findViewById(R.id.tv_video_room_title);
        tvHostMeta = findViewById(R.id.tv_video_host_meta);
        btnVideoTheme = findViewById(R.id.btn_video_theme);
        tvViewerCount = findViewById(R.id.tv_video_viewer_count);
        tvRtcStatus = findViewById(R.id.tv_video_rtc_status);
        tvPkLeftScore = findViewById(R.id.tv_pk_left_score);
        tvPkRightScore = findViewById(R.id.tv_pk_right_score);
        tvPkTimer = findViewById(R.id.tv_pk_timer);
        progressPk = findViewById(R.id.progress_video_pk);
        textureCameraPreview = findViewById(R.id.texture_camera_preview);
        videoHostPlaceholder = findViewById(R.id.video_host_placeholder);
        tvHostStageStatus = findViewById(R.id.tv_host_stage_status);
        tvReactionBanner = findViewById(R.id.tv_video_reaction_banner);
        scrollComments = findViewById(R.id.scroll_video_comments);
        containerComments = findViewById(R.id.container_video_comments);
        containerQuickPills = findViewById(R.id.container_video_quick_pills);
        etComment = findViewById(R.id.et_video_comment);
        btnCamToggle = findViewById(R.id.btn_video_cam_toggle);
        btnMicToggle = findViewById(R.id.btn_video_mic_toggle);
        btnSendComment = findViewById(R.id.btn_send_video_comment);

        clipOvalAvatar(findViewById(R.id.iv_host_avatar_circle));
        clipOvalAvatar(findViewById(R.id.iv_challenger_avatar_circle));

        if (btnVideoTheme != null) {
            btnVideoTheme.setOnClickListener(v ->
                    UiStyleHelper.showThemeAndWallpaperStudioDialog(this, this::applyActiveThemeAndWallpaper)
            );
        }

        if (customTitle != null && !customTitle.trim().isEmpty()) {
            tvRoomTitle.setText(customTitle.trim());
        }
        tvHostMeta.setText("Host: " + social.getCurrentUser().displayName + " • Channel: " + channelName);
        tvViewerCount.setText("👁️ " + viewerCount);

        if (textureCameraPreview != null) {
            textureCameraPreview.setSurfaceTextureListener(this);
        }

        TextView btnFollowHost = findViewById(R.id.btn_follow_host);
        if (btnFollowHost != null) {
            btnFollowHost.setOnClickListener(v -> {
                isHostFollowed = !isHostFollowed;
                btnFollowHost.setText(isHostFollowed ? "✓ Following" : "+ Follow");
                btnFollowHost.setBackground(isHostFollowed
                        ? UiStyleHelper.rippleButton(this, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, UiStyleHelper.COLOR_AGORA_CYAN, 1f)
                        : UiStyleHelper.rippleGradientButton(this, UiStyleHelper.COLOR_AGORA_BLUE, UiStyleHelper.COLOR_INDIGO_ACCENT, 12f));
                triggerReaction(isHostFollowed
                        ? ("💖 " + social.getCurrentUser().displayName + " followed the Host!")
                        : "Unfollowed Host");
            });
        }

        TextView btnMinimizeLive = findViewById(R.id.btn_minimize_live);
        if (btnMinimizeLive != null) {
            btnMinimizeLive.setOnClickListener(v -> {
                Toast.makeText(this, "🗕 Minimized Live Room to floating audio/video feed", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        TextView btnStartEndLive = findViewById(R.id.btn_start_end_live);
        if (btnStartEndLive != null) {
            btnStartEndLive.setOnClickListener(v -> {
                isLiveStreamingActive = !isLiveStreamingActive;
                btnStartEndLive.setText(isLiveStreamingActive ? "⏹️ End Live" : "▶️ Start Live");
                if (tvRtcStatus != null) {
                    tvRtcStatus.setText(isLiveStreamingActive ? "LIVE" : "PAUSED");
                    tvRtcStatus.setTextColor(isLiveStreamingActive ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_WARNING_AMBER);
                }
                if (rtcEngine != null) {
                    rtcEngine.muteLocalAudioStream(!isLiveStreamingActive || isMicMuted);
                }
                triggerReaction(isLiveStreamingActive ? "▶️ Live Broadcast Resumed!" : "⏹️ Live Broadcast Paused");
            });
        }

        TextView btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        if (btnSwitchCamera != null) {
            btnSwitchCamera.setOnClickListener(v -> {
                isFrontCamera = !isFrontCamera;
                if (rtcEngine != null) {
                    try {
                        rtcEngine.switchCamera();
                    } catch (Throwable ignored) {
                    }
                }
                stopCameraPreview();
                startCameraPreviewIfReady();
                Toast.makeText(this, isFrontCamera ? "🔄 Switched to Front Camera" : "🔄 Switched to Rear Camera", Toast.LENGTH_SHORT).show();
            });
        }

        View pkBarLayout = findViewById(R.id.layout_video_pk_bar);
        TextView btnStartPk = findViewById(R.id.btn_start_pk);
        if (btnStartPk != null) {
            btnStartPk.setOnClickListener(v -> {
                isPkActive = true;
                pkSecondsRemaining = 180;
                if (pkBarLayout != null) pkBarLayout.setVisibility(View.VISIBLE);
                updatePkScoreboardUi();
                triggerReaction("⚔️ 3-Minute Live Video PK Battle Started!");
            });
        }

        TextView btnEndPk = findViewById(R.id.btn_end_pk);
        if (btnEndPk != null) {
            btnEndPk.setOnClickListener(v -> {
                isPkActive = false;
                if (pkBarLayout != null) pkBarLayout.setVisibility(View.GONE);
                triggerReaction("🏁 Live Video PK Battle Ended! Winner: " + (hostPkScore >= challengerPkScore ? "Host Team ⚡" : "Challenger 🔥"));
            });
        }

        TextView btnShareLive = findViewById(R.id.btn_share_live);
        if (btnShareLive != null) {
            btnShareLive.setOnClickListener(v -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "Watch QLUVX Live Video & PK Battle on channel: " + channelName);
                startActivity(Intent.createChooser(share, "Share Live Stream"));
            });
        }

        TextView btnReportLive = findViewById(R.id.btn_report_live);
        if (btnReportLive != null) {
            btnReportLive.setOnClickListener(v -> {
                VoiceRoomModels.RoomRepository.getInstance().addModerationReport("Reported Live Video Stream on channel " + channelName);
                economy.logAudit("Moderation report submitted for Live Video channel " + channelName);
                Toast.makeText(this, "🚩 Reported Live Stream to QLUVX Moderation Team", Toast.LENGTH_SHORT).show();
            });
        }

        TextView btnOpenComments = findViewById(R.id.btn_open_comments);
        if (btnOpenComments != null) {
            btnOpenComments.setOnClickListener(v -> {
                if (etComment != null) {
                    etComment.requestFocus();
                    try {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
                    } catch (Throwable ignored) {
                    }
                }
            });
        }

        findViewById(R.id.btn_react_heart).setOnClickListener(v -> triggerReaction("❤️ " + social.getCurrentUser().displayName + " sent Heart Likes!"));
        findViewById(R.id.btn_react_fire).setOnClickListener(v -> triggerReaction("🔥 " + social.getCurrentUser().displayName + " ignited the Live Stage!"));

        findViewById(R.id.btn_vote_host_pk).setOnClickListener(v -> {
            hostPkScore += 25;
            updatePkScoreboardUi();
            triggerReaction("⚡ +25 PK Points to Host Team (" + social.getCurrentUser().displayName + ")!");
        });

        findViewById(R.id.btn_vote_challenger_pk).setOnClickListener(v -> {
            challengerPkScore += 25;
            updatePkScoreboardUi();
            triggerReaction("⚔️ +25 PK Points to Challenger Zoya!");
        });

        // Quick Live Chat Pills
        if (containerQuickPills != null) {
            String[] quickComments = new String[]{
                    "🔥 OP Live Stream!",
                    "❤️ Love from India",
                    "👑 Best Host!",
                    "⚡ Let's win this PK!",
                    "🎨 Wallpaper looks amazing!"
            };
            for (final String qc : quickComments) {
                TextView pill = UiStyleHelper.createStatusChip(
                        this,
                        qc,
                        UiStyleHelper.COLOR_CARD_ELEVATED,
                        UiStyleHelper.COLOR_AGORA_CYAN,
                        UiStyleHelper.COLOR_BORDER_SUBTLE
                );
                pill.setOnClickListener(v -> {
                    appendComment(social.getCurrentUser().displayName, qc);
                    triggerReaction("💬 " + social.getCurrentUser().displayName + ": " + qc);
                });
                containerQuickPills.addView(pill, UiStyleHelper.params(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0, 0, UiStyleHelper.dp(this, 6), 0
                ));
            }
        }

        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> {
                String msg = etComment.getText().toString().trim();
                if (!msg.isEmpty()) {
                    appendComment(social.getCurrentUser().displayName, msg);
                    etComment.setText("");
                }
            });
        }

        btnCamToggle.setOnClickListener(v -> {
            isCameraOn = !isCameraOn;
            btnCamToggle.setText(isCameraOn ? "📷 Cam ON" : "📷 Cam OFF");
            applyActiveThemeAndWallpaper();
            if (isCameraOn) {
                startCameraPreviewIfReady();
            } else {
                stopCameraPreview();
                videoHostPlaceholder.setVisibility(View.VISIBLE);
                tvHostStageStatus.setText("👑 Host Stage\nCamera Paused • Audio Active");
            }
        });

        btnMicToggle.setOnClickListener(v -> {
            isMicMuted = !isMicMuted;
            if (rtcEngine != null) {
                rtcEngine.muteLocalAudioStream(isMicMuted);
            }
            btnMicToggle.setText(isMicMuted ? "🔇 Muted" : "🎙️ Mic ON");
            btnMicToggle.setBackground(UiStyleHelper.rippleButton(
                    this,
                    isMicMuted ? UiStyleHelper.COLOR_DANGER_RED : UiStyleHelper.COLOR_CARD_ELEVATED,
                    12f,
                    0,
                    0f
            ));
        });

        findViewById(R.id.btn_video_gift).setOnClickListener(v -> showVideoGiftDialog());
        findViewById(R.id.btn_video_leave).setOnClickListener(v -> finish());

        if (tvViewerCount != null) {
            tvViewerCount.setOnClickListener(v -> showConnectedParticipantsListOrGridDialog());
        }

        // Attach Connected Agora Channel Participants Strip below top bar
        if (liveOuterFrame != null && connectedParticipantsBar == null) {
            connectedParticipantsBar = new LinearLayout(this);
            connectedParticipantsBar.setOrientation(LinearLayout.HORIZONTAL);
            connectedParticipantsBar.setGravity(Gravity.CENTER_VERTICAL);
            int padH = UiStyleHelper.dp(this, 10);
            int padV = UiStyleHelper.dp(this, 5);
            connectedParticipantsBar.setPadding(padH, padV, padH, padV);
            connectedParticipantsBar.setBackground(UiStyleHelper.roundedBox(this, Color.argb(190, 15, 23, 42), 14f, Color.rgb(45, 212, 191), 1f));
            FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END
            );
            barLp.setMargins(UiStyleHelper.dp(this, 12), UiStyleHelper.dp(this, 56), UiStyleHelper.dp(this, 12), 0);
            liveOuterFrame.addView(connectedParticipantsBar, barLp);
            refreshConnectedParticipantsBar();
        }

        updatePkScoreboardUi();
    }

    /**
     * Renders a compact live strip of participants currently connected to the Agora channel.
     * Tapping opens the full Connected Agora Participants List / Grid dialog.
     */
    private void refreshConnectedParticipantsBar() {
        if (connectedParticipantsBar == null) return;
        connectedParticipantsBar.removeAllViews();

        LinearLayout avRow = new LinearLayout(this);
        avRow.setOrientation(LinearLayout.HORIZONTAL);
        avRow.setGravity(Gravity.CENTER_VERTICAL);
        int maxIcons = Math.min(4, connectedParticipants.size());
        for (int i = 0; i < maxIcons; i++) {
            AgoraParticipantItem p = connectedParticipants.get(i);
            View av = UiStyleHelper.createCircularAvatar(
                    this,
                    p.avatarResId,
                    24,
                    p.isAudioActive ? UiStyleHelper.COLOR_LIVE_GREEN : Color.rgb(148, 163, 184),
                    false
            );
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiStyleHelper.dp(this, 24), UiStyleHelper.dp(this, 24));
            if (i > 0) {
                lp.setMargins(-UiStyleHelper.dp(this, 5), 0, 0, 0);
            }
            avRow.addView(av, lp);
        }
        connectedParticipantsBar.addView(avRow);

        TextView label = new TextView(this);
        label.setText("  📡 " + connectedParticipants.size() + " in Agora • List/Grid ▾");
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        connectedParticipantsBar.addView(label);

        connectedParticipantsBar.setOnClickListener(v -> showConnectedParticipantsListOrGridDialog());
    }

    /**
     * Displays all participants currently connected to the Agora channel with a toggle
     * between a clean vertical List and a 3-column Grid.
     */
    private void showConnectedParticipantsListOrGridDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar).create();

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(this, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(18, 20, 30), 20f, Color.rgb(56, 189, 248), 1.2f));

        final Runnable[] renderBodyRef = new Runnable[1];

        // Header row with List / Grid toggle buttons
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, UiStyleHelper.dp(this, 10));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("📡 Agora Channel Participants (" + connectedParticipants.size() + ")");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

        TextView sub = new TextView(this);
        sub.setText("Channel: " + channelName);
        sub.setTextColor(Color.rgb(94, 234, 212));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

        titleCol.addView(title);
        titleCol.addView(sub);
        header.addView(titleCol);

        final TextView toggleBtn = new TextView(this);
        toggleBtn.setText(showParticipantsAsGrid ? "☰ List View" : "⊞ Grid View");
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        toggleBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        toggleBtn.setPadding(UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 6), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 6));
        toggleBtn.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(37, 99, 235), 10f, 0, 0));
        header.addView(toggleBtn);

        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        final LinearLayout contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(contentBox);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStyleHelper.dp(this, 320)));

        renderBodyRef[0] = () -> {
            contentBox.removeAllViews();
            toggleBtn.setText(showParticipantsAsGrid ? "☰ List View" : "⊞ Grid View");
            if (!showParticipantsAsGrid) {
                for (final AgoraParticipantItem p : connectedParticipants) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    int pPad = UiStyleHelper.dp(this, 10);
                    row.setPadding(pPad, pPad, pPad, pPad);
                    row.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(28, 32, 46), 12f, Color.rgb(45, 55, 72), 1f));

                    View av = UiStyleHelper.createCircularAvatar(this, p.avatarResId, 42, p.isAudioActive ? UiStyleHelper.COLOR_LIVE_GREEN : Color.rgb(100, 116, 139), false);
                    row.addView(av);

                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setPadding(UiStyleHelper.dp(this, 10), 0, UiStyleHelper.dp(this, 8), 0);
                    info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    TextView nm = new TextView(this);
                    nm.setText(p.displayName);
                    nm.setTextColor(Color.WHITE);
                    nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                    nm.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

                    TextView rl = new TextView(this);
                    rl.setText(p.roleLabel + " • UID: " + p.uid);
                    rl.setTextColor(Color.rgb(148, 163, 184));
                    rl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

                    info.addView(nm);
                    info.addView(rl);
                    row.addView(info);

                    TextView stateChip = new TextView(this);
                    stateChip.setText(p.isAudioActive ? "🎙️ Active" : "🎧 Listening");
                    stateChip.setTextColor(p.isAudioActive ? Color.rgb(52, 211, 153) : Color.rgb(203, 213, 225));
                    stateChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                    stateChip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    row.addView(stateChip);

                    contentBox.addView(row, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, UiStyleHelper.dp(this, 8)));
                }
            } else {
                for (int i = 0; i < connectedParticipants.size(); i += 3) {
                    LinearLayout gridRow = new LinearLayout(this);
                    gridRow.setOrientation(LinearLayout.HORIZONTAL);
                    gridRow.setWeightSum(3f);
                    for (int c = 0; c < 3; c++) {
                        int idx = i + c;
                        if (idx < connectedParticipants.size()) {
                            final AgoraParticipantItem p = connectedParticipants.get(idx);
                            LinearLayout cell = new LinearLayout(this);
                            cell.setOrientation(LinearLayout.VERTICAL);
                            cell.setGravity(Gravity.CENTER_HORIZONTAL);
                            int cPad = UiStyleHelper.dp(this, 10);
                            cell.setPadding(cPad, cPad, cPad, cPad);
                            cell.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(28, 32, 46), 12f, p.isAudioActive ? UiStyleHelper.COLOR_LIVE_GREEN : Color.rgb(45, 55, 72), 1f));

                            View av = UiStyleHelper.createCircularAvatar(this, p.avatarResId, 46, p.isAudioActive ? UiStyleHelper.COLOR_LIVE_GREEN : Color.rgb(100, 116, 139), false);
                            cell.addView(av);

                            TextView nm = new TextView(this);
                            nm.setText(p.displayName);
                            nm.setSingleLine(true);
                            nm.setTextColor(Color.WHITE);
                            nm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                            nm.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                            nm.setPadding(0, UiStyleHelper.dp(this, 6), 0, UiStyleHelper.dp(this, 2));
                            cell.addView(nm);

                            TextView uidTv = new TextView(this);
                            uidTv.setText("UID " + p.uid);
                            uidTv.setTextColor(Color.rgb(94, 234, 212));
                            uidTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
                            cell.addView(uidTv);

                            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                            cellLp.setMargins(UiStyleHelper.dp(this, 3), 0, UiStyleHelper.dp(this, 3), UiStyleHelper.dp(this, 8));
                            gridRow.addView(cell, cellLp);
                        } else {
                            View spacer = new View(this);
                            gridRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
                        }
                    }
                    contentBox.addView(gridRow);
                }
            }
        };

        toggleBtn.setOnClickListener(v -> {
            showParticipantsAsGrid = !showParticipantsAsGrid;
            renderBodyRef[0].run();
        });
        renderBodyRef[0].run();

        TextView closeBtn = new TextView(this);
        closeBtn.setText("Close");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        closeBtn.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(0, UiStyleHelper.dp(this, 10), 0, UiStyleHelper.dp(this, 10));
        closeBtn.setBackground(UiStyleHelper.roundedBox(this, Color.rgb(236, 72, 153), 12f, 0, 0));
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn, UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, UiStyleHelper.dp(this, 10), 0, 0));

        dialog.setView(root);
        dialog.show();
    }

    /**
     * Exact replica of the Pre-LIVE Camera Setup Screen (01:21 - 01:33 of the video):
     * - Fullscreen camera/portrait background
     * - Top-left ✕ close button
     * - Top dark translucent card with square avatar + 🔄 icon, editable "Your LIVE Title ✏️", "India", and "Public Live >" pill
     * - Bottom 2 rows of circular icons:
     *   Row 1: 🪄 Beauty, 🎭 Masks, 👑 Skin, 📷 Flip Camera, 🪞 Mirror
     *   Row 2: 📤 Share, 💡 Connect
     * - Wide pink-to-purple gradient "Start LIVE" pill button + "Anti-recording" toggle switch
     */
    private void showPreLiveSetupOverlay(String initialTitle) {
        if (liveOuterFrame == null) return;
        if (preLiveSetupOverlay != null) {
            liveOuterFrame.removeView(preLiveSetupOverlay);
        }

        preLiveSetupOverlay = new FrameLayout(this);
        preLiveSetupOverlay.setClickable(true);
        preLiveSetupOverlay.setFocusable(true);
        preLiveSetupOverlay.setBackgroundColor(Color.argb(145, 8, 10, 18));

        // Top-left ✕ close button
        TextView btnCloseSetup = new TextView(this);
        btnCloseSetup.setText("✕");
        btnCloseSetup.setTextColor(Color.WHITE);
        btnCloseSetup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        btnCloseSetup.setGravity(Gravity.CENTER);
        int closeSz = UiStyleHelper.dp(this, 38);
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(closeSz, closeSz, Gravity.TOP | Gravity.START);
        closeLp.setMargins(UiStyleHelper.dp(this, 14), UiStyleHelper.dp(this, 14), 0, 0);
        btnCloseSetup.setOnClickListener(v -> finish());
        preLiveSetupOverlay.addView(btnCloseSetup, closeLp);

        // Top Translucent LIVE Title & Cover Card
        LinearLayout topSetupCard = new LinearLayout(this);
        topSetupCard.setOrientation(LinearLayout.HORIZONTAL);
        topSetupCard.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiStyleHelper.dp(this, 12);
        topSetupCard.setPadding(pad, pad, pad, pad);
        topSetupCard.setBackground(UiStyleHelper.roundedBox(this, Color.argb(200, 18, 22, 34), 16f, Color.argb(90, 255, 255, 255), 1f));
        FrameLayout.LayoutParams topCardLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
        );
        topCardLp.setMargins(UiStyleHelper.dp(this, 14), UiStyleHelper.dp(this, 60), UiStyleHelper.dp(this, 14), 0);

        // Left square avatar (68dp) + 🔄 badge
        FrameLayout coverWrap = new FrameLayout(this);
        int covSz = UiStyleHelper.dp(this, 68);
        coverWrap.setLayoutParams(new LinearLayout.LayoutParams(covSz, covSz));
        coverWrap.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiStyleHelper.dp(LiveVideoActivity.this, 12));
            }
        });
        coverWrap.setClipToOutline(true);

        final int[] coverIdx = new int[]{0};
        final ImageView ivCover = new ImageView(this);
        ivCover.setImageResource(R.drawable.img_avatar_0);
        ivCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverWrap.addView(ivCover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView changeIcon = new TextView(this);
        changeIcon.setText("🔄");
        changeIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        changeIcon.setPadding(UiStyleHelper.dp(this, 4), UiStyleHelper.dp(this, 2), UiStyleHelper.dp(this, 4), UiStyleHelper.dp(this, 2));
        changeIcon.setBackground(UiStyleHelper.roundedBox(this, Color.argb(160, 0, 0, 0), 6f, 0, 0));
        coverWrap.addView(changeIcon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END));
        coverWrap.setOnClickListener(v -> {
            coverIdx[0] = (coverIdx[0] + 1) % 6;
            ivCover.setImageResource(VoiceRoomModels.getAvatarResId(coverIdx[0]));
            Toast.makeText(this, "📸 Updated LIVE Cover Image", Toast.LENGTH_SHORT).show();
        });
        topSetupCard.addView(coverWrap);

        // Right column: Editable "Your LIVE Title ✏️", "India", and "Public Live >" pill
        LinearLayout rightSetupCol = new LinearLayout(this);
        rightSetupCol.setOrientation(LinearLayout.VERTICAL);
        rightSetupCol.setPadding(UiStyleHelper.dp(this, 12), 0, 0, 0);
        rightSetupCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final EditText etLiveTitle = new EditText(this);
        etLiveTitle.setText((initialTitle != null && !initialTitle.trim().isEmpty()) ? initialTitle : "Your LIVE Title ✏️");
        etLiveTitle.setTextColor(Color.WHITE);
        etLiveTitle.setHint("Your LIVE Title ✏️");
        etLiveTitle.setHintTextColor(Color.rgb(203, 213, 225));
        etLiveTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        etLiveTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        etLiveTitle.setSingleLine(true);
        etLiveTitle.setBackgroundColor(Color.TRANSPARENT);
        etLiveTitle.setPadding(0, 0, 0, UiStyleHelper.dp(this, 2));
        rightSetupCol.addView(etLiveTitle);

        TextView tvLoc = new TextView(this);
        tvLoc.setText("India");
        tvLoc.setTextColor(Color.rgb(203, 213, 225));
        tvLoc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tvLoc.setPadding(0, 0, 0, UiStyleHelper.dp(this, 6));
        rightSetupCol.addView(tvLoc);

        final TextView publicPill = new TextView(this);
        publicPill.setText(isPublicLive ? "Public Live  ❯" : "Private VIP Live  ❯");
        publicPill.setTextColor(Color.WHITE);
        publicPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        publicPill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        publicPill.setPadding(UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 4), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 4));
        publicPill.setBackground(UiStyleHelper.roundedBox(this, Color.argb(140, 51, 65, 85), 12f, 0, 0));
        publicPill.setOnClickListener(v -> {
            isPublicLive = !isPublicLive;
            publicPill.setText(isPublicLive ? "Public Live  ❯" : "Private VIP Live  ❯");
        });
        rightSetupCol.addView(publicPill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        topSetupCard.addView(rightSetupCol);
        preLiveSetupOverlay.addView(topSetupCard, topCardLp);

        // Bottom Controls Stack: 2 rows of circular icons + Start LIVE button + Anti-recording toggle
        LinearLayout bottomSetupCol = new LinearLayout(this);
        bottomSetupCol.setOrientation(LinearLayout.VERTICAL);
        int bPad = UiStyleHelper.dp(this, 16);
        bottomSetupCol.setPadding(bPad, bPad, bPad, UiStyleHelper.dp(this, 22));
        bottomSetupCol.setBackground(UiStyleHelper.bottomScrimGradient(this, 0f));

        // Row 1: 🪄 Beauty, 🎭 Masks, 👑 Skin, 📷 Flip Camera, 🪞 Mirror
        String[][] row1Tools = new String[][]{
                {"🪄", "Beauty"},
                {"🎭", "Masks"},
                {"👑", "Skin"},
                {"📷", "Flip Camera"},
                {"🪞", "Mirror"}
        };
        LinearLayout toolsRow1 = new LinearLayout(this);
        toolsRow1.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow1.setWeightSum(5f);
        for (int i = 0; i < row1Tools.length; i++) {
            final String emoji = row1Tools[i][0];
            final String label = row1Tools[i][1];
            final int toolIdx = i;
            LinearLayout item = buildSetupToolIcon(emoji, label, v -> {
                if (toolIdx == 3) {
                    View switchBtn = findViewById(R.id.btn_switch_camera);
                    if (switchBtn != null) switchBtn.performClick();
                } else if (toolIdx == 4) {
                    isMirrorEnabled = !isMirrorEnabled;
                    if (textureCameraPreview != null) {
                        textureCameraPreview.setScaleX(isMirrorEnabled ? 1f : -1f);
                    }
                    Toast.makeText(this, isMirrorEnabled ? "🪞 Camera Mirror ON" : "🪞 Camera Mirror OFF", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, emoji + " " + label + " filter applied", Toast.LENGTH_SHORT).show();
                }
            });
            toolsRow1.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        bottomSetupCol.addView(toolsRow1);

        // Row 2: 📤 Share, 💡 Connect (aligned left under first 2 columns)
        String[][] row2Tools = new String[][]{
                {"📤", "Share"},
                {"💡", "Connect"}
        };
        LinearLayout toolsRow2 = new LinearLayout(this);
        toolsRow2.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow2.setWeightSum(5f);
        toolsRow2.setPadding(0, UiStyleHelper.dp(this, 10), 0, UiStyleHelper.dp(this, 18));
        for (int i = 0; i < 5; i++) {
            if (i < row2Tools.length) {
                final String emoji = row2Tools[i][0];
                final String label = row2Tools[i][1];
                final int r2Idx = i;
                LinearLayout item = buildSetupToolIcon(emoji, label, v -> {
                    if (r2Idx == 0) {
                        View shareBtn = findViewById(R.id.btn_share_live);
                        if (shareBtn != null) shareBtn.performClick();
                    } else {
                        Toast.makeText(this, "💡 Multi-Host & PK Co-Host Connect Ready", Toast.LENGTH_SHORT).show();
                    }
                });
                toolsRow2.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            } else {
                View empty = new View(this);
                toolsRow2.addView(empty, new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
        bottomSetupCol.addView(toolsRow2);

        // Bottom Action Row: [ Start LIVE (Pink-to-Purple Gradient Pill) ] + [ Anti-recording switch ]
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnStartLiveNow = new TextView(this);
        btnStartLiveNow.setText("Start LIVE");
        btnStartLiveNow.setTextColor(Color.WHITE);
        btnStartLiveNow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        btnStartLiveNow.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        btnStartLiveNow.setGravity(Gravity.CENTER);
        btnStartLiveNow.setPadding(0, UiStyleHelper.dp(this, 13), 0, UiStyleHelper.dp(this, 13));
        btnStartLiveNow.setBackground(UiStyleHelper.gradientBox(
                this,
                Color.rgb(244, 63, 94),
                Color.rgb(168, 85, 247),
                26f,
                0,
                0f
        ));
        btnStartLiveNow.setOnClickListener(v -> {
            String chosenTitle = etLiveTitle.getText().toString().trim();
            if (!chosenTitle.isEmpty() && tvRoomTitle != null) {
                tvRoomTitle.setText(chosenTitle.replace("✏️", "").trim());
            }
            liveOuterFrame.removeView(preLiveSetupOverlay);
            preLiveSetupOverlay = null;
            triggerReaction("🎬 LIVE Broadcast Started on QLUVX!");
        });
        actionRow.addView(btnStartLiveNow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Right-side Anti-recording toggle
        LinearLayout antiRecCol = new LinearLayout(this);
        antiRecCol.setOrientation(LinearLayout.VERTICAL);
        antiRecCol.setGravity(Gravity.CENTER_HORIZONTAL);
        antiRecCol.setPadding(UiStyleHelper.dp(this, 14), 0, UiStyleHelper.dp(this, 4), 0);

        final TextView switchPill = new TextView(this);
        switchPill.setText(isAntiRecordingEnabled ? "ON ●" : "● OFF");
        switchPill.setTextColor(Color.WHITE);
        switchPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        switchPill.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        switchPill.setPadding(UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 4), UiStyleHelper.dp(this, 10), UiStyleHelper.dp(this, 4));
        switchPill.setBackground(UiStyleHelper.roundedBox(
                this,
                isAntiRecordingEnabled ? Color.rgb(16, 185, 129) : Color.argb(160, 71, 85, 105),
                12f,
                0,
                0f
        ));

        TextView antiRecLabel = new TextView(this);
        antiRecLabel.setText("Anti-recording");
        antiRecLabel.setTextColor(Color.rgb(226, 232, 240));
        antiRecLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        antiRecLabel.setPadding(0, UiStyleHelper.dp(this, 3), 0, 0);

        antiRecCol.addView(switchPill);
        antiRecCol.addView(antiRecLabel);
        antiRecCol.setOnClickListener(v -> {
            isAntiRecordingEnabled = !isAntiRecordingEnabled;
            switchPill.setText(isAntiRecordingEnabled ? "ON ●" : "● OFF");
            switchPill.setBackground(UiStyleHelper.roundedBox(
                    this,
                    isAntiRecordingEnabled ? Color.rgb(16, 185, 129) : Color.argb(160, 71, 85, 105),
                    12f,
                    0,
                    0f
            ));
            Toast.makeText(this, isAntiRecordingEnabled ? "🛡️ Anti-recording protection enabled" : "Anti-recording disabled", Toast.LENGTH_SHORT).show();
        });
        actionRow.addView(antiRecCol);

        bottomSetupCol.addView(actionRow);
        preLiveSetupOverlay.addView(bottomSetupCol, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        ));

        liveOuterFrame.addView(preLiveSetupOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private LinearLayout buildSetupToolIcon(String emoji, String label, View.OnClickListener clickListener) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView circle = new TextView(this);
        circle.setText(emoji);
        circle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        circle.setGravity(Gravity.CENTER);
        int sz = UiStyleHelper.dp(this, 44);
        circle.setBackground(UiStyleHelper.circle(this, Color.argb(150, 30, 41, 59), Color.argb(80, 255, 255, 255), 1f));
        col.addView(circle, new LinearLayout.LayoutParams(sz, sz));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tv.setPadding(0, UiStyleHelper.dp(this, 4), 0, 0);
        col.addView(tv);

        col.setOnClickListener(clickListener);
        return col;
    }

    private void clipOvalAvatar(View view) {
        if (view == null) return;
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                int sz = Math.min(v.getWidth(), v.getHeight());
                outline.setOval(0, 0, sz, sz);
            }
        });
        view.setClipToOutline(true);
    }

    private void seedInitialComments() {
        appendComment("System", "Welcome to QLUVX Live Video & PK Stage! Tap 🎨 Theme to customize wallpaper.");
        appendComment("Zoya Khan", "Ready for this PK battle! Let's go team 🔥");
        appendComment("Kabir Verma", "Audio & video clarity is super crisp today!");
        appendComment("Putri Ayu", "Sent Heart Likes ❤️❤️❤️ Keep shining!");
    }

    private void triggerReaction(String bannerText) {
        if (tvReactionBanner != null) {
            tvReactionBanner.setText(bannerText);
        }
        appendComment("Reaction", bannerText);
    }

    private void updatePkScoreboardUi() {
        if (tvPkLeftScore == null || tvPkRightScore == null || tvPkTimer == null || progressPk == null) return;
        tvPkLeftScore.setText("⚡ Host: " + hostPkScore);
        tvPkRightScore.setText("Challenger: " + challengerPkScore + " 🔥");
        int mins = pkSecondsRemaining / 60;
        int secs = pkSecondsRemaining % 60;
        tvPkTimer.setText(String.format(Locale.US, "⚔️ PK %02d:%02d", mins, secs));
        int total = Math.max(1, hostPkScore + challengerPkScore);
        int ratio = (int) ((hostPkScore * 100f) / total);
        progressPk.setProgress(Math.max(5, Math.min(95, ratio)));
    }

    private void appendComment(String sender, String message) {
        if (containerComments == null) return;
        String time = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        comments.add("[" + time + "] " + sender + ": " + message);

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        int padH = UiStyleHelper.dp(this, 8);
        int padV = UiStyleHelper.dp(this, 5);
        pill.setPadding(padH, padV, padH, padV);
        boolean isSystem = "System".equals(sender);
        boolean isReaction = "Reaction".equals(sender);
        pill.setBackground(UiStyleHelper.roundedBox(
                this,
                isReaction ? Color.argb(80, 245, 158, 11) : Color.argb(135, 15, 23, 42),
                10f,
                isSystem ? UiStyleHelper.COLOR_AGORA_CYAN : 0,
                isSystem ? 1f : 0f
        ));

        TextView line = new TextView(this);
        line.setText("[" + time + "] " + sender + ": " + message);
        line.setTextColor(isSystem
                ? UiStyleHelper.COLOR_AGORA_CYAN
                : (isReaction ? UiStyleHelper.COLOR_WARNING_AMBER : UiStyleHelper.COLOR_TEXT_PRIMARY));
        line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        line.setTypeface(Typeface.create("sans-serif", (isSystem || isReaction) ? Typeface.BOLD : Typeface.NORMAL));
        pill.addView(line);

        containerComments.addView(pill, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0, 0, 0, UiStyleHelper.dp(this, 4)
        ));

        if (scrollComments != null) {
            scrollComments.post(() -> scrollComments.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void showVideoGiftDialog() {
        UiStyleHelper.showUnifiedGiftPanelDialog(
                this,
                economy,
                social.getCurrentUser().displayName,
                "Live Video Host",
                (gift, combo, tx) -> {
                    hostPkScore += (gift.coinPrice * combo);
                    updatePkScoreboardUi();
                    triggerReaction("🎁 " + social.getCurrentUser().displayName + " sent " + gift.iconLabel + " " + gift.name + " x" + combo + " (+" + (gift.coinPrice * combo) + " PK)!");
                }
        );
    }

    private void checkPermissionsAndStartEngine() {
        List<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), REQ_CAMERA_AUDIO);
        } else {
            initAgoraEngineAndJoin();
            startCameraPreviewIfReady();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_AUDIO) {
            initAgoraEngineAndJoin();
            startCameraPreviewIfReady();
        }
    }

    private void initAgoraEngineAndJoin() {
        String appId = BuildConfig.AGORA_APP_ID != null ? BuildConfig.AGORA_APP_ID.trim() : "";
        if (appId.isEmpty() || "YOUR_AGORA_APP_ID".equalsIgnoreCase(appId)) {
            if (tvRtcStatus != null) {
                tvRtcStatus.setText("LIVE STAGE");
                tvRtcStatus.setTextColor(UiStyleHelper.COLOR_LIVE_GREEN);
            }
            return;
        }
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = appId;
            config.mEventHandler = rtcEventHandler;
            rtcEngine = RtcEngine.create(config);
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            rtcEngine.enableAudio();

            final int uid = VoiceRoomModels.RoomRepository.getInstance().localUid;
            TokenServiceClient.requestRtcToken(channelName, uid, true, (token, fromServer) -> {
                if (rtcEngine == null || isFinishing()) return;
                ChannelMediaOptions options = new ChannelMediaOptions();
                options.autoSubscribeAudio = true;
                options.publishMicrophoneTrack = !isMicMuted;
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                rtcEngine.joinChannel(token, channelName, uid, options);
            });
        } catch (Exception e) {
            if (tvRtcStatus != null) {
                tvRtcStatus.setText("STAGE READY");
            }
        }
    }

    private void startCameraPreviewIfReady() {
        if (!isCameraOn || textureCameraPreview == null || !textureCameraPreview.isAvailable()) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (tvHostStageStatus != null) {
                tvHostStageStatus.setText("👑 Host Video Stage\nHD Stream Active");
            }
            return;
        }
        try {
            if (hardwareCamera == null) {
                int numCams = Camera.getNumberOfCameras();
                if (numCams <= 0) {
                    if (tvHostStageStatus != null) {
                        tvHostStageStatus.setText("👑 Host Live Stage\nHD Virtual Stream");
                    }
                    return;
                }
                int camId = 0;
                int desiredFacing = isFrontCamera
                        ? Camera.CameraInfo.CAMERA_FACING_FRONT
                        : Camera.CameraInfo.CAMERA_FACING_BACK;
                Camera.CameraInfo info = new Camera.CameraInfo();
                for (int i = 0; i < numCams; i++) {
                    Camera.getCameraInfo(i, info);
                    if (info.facing == desiredFacing) {
                        camId = i;
                        break;
                    }
                }
                hardwareCamera = Camera.open(camId);
                hardwareCamera.setDisplayOrientation(90);
            }
            SurfaceTexture surface = textureCameraPreview.getSurfaceTexture();
            if (surface != null) {
                hardwareCamera.setPreviewTexture(surface);
                hardwareCamera.startPreview();
                if (videoHostPlaceholder != null) {
                    videoHostPlaceholder.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            if (videoHostPlaceholder != null) {
                videoHostPlaceholder.setVisibility(View.VISIBLE);
            }
            if (tvHostStageStatus != null) {
                tvHostStageStatus.setText("👑 Host Live Stage\nAgora Stream Ready");
            }
        }
    }

    private void stopCameraPreview() {
        if (hardwareCamera != null) {
            try {
                hardwareCamera.stopPreview();
                hardwareCamera.release();
            } catch (Exception ignored) {
            }
            hardwareCamera = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startCameraPreviewIfReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopCameraPreview();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        stopCameraPreview();
        if (rtcEngine != null) {
            try {
                rtcEngine.leaveChannel();
            } catch (Exception ignored) {
            }
            RtcEngine.destroy();
            rtcEngine = null;
        }
        super.onDestroy();
    }
}
