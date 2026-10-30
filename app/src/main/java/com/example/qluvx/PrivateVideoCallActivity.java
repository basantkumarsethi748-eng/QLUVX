package com.example.qluvx;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import java.util.Locale;

/**
 * 1-ON-1 PRIVATE HD VIDEO CALLING (PrivateVideoCallActivity.java):
 * - Full-screen remote partner looping HD video surface + Glide HD portrait backdrop
 * - Draggable floating Picture-in-Picture (PiP) local CameraX preview card
 * - Real-time call timer (00:01, 00:02...), Camera Flip button, Mic mute toggle,
 *   and rapid-tap Gift Combo Streak Multiplier button ('COMBO STREAK x10! 🔥')
 */
public class PrivateVideoCallActivity extends AppCompatActivity {

    public static final String EXTRA_PARTNER_NAME = "extra_partner_name";
    public static final String EXTRA_PARTNER_PORTRAIT_IDX = "extra_partner_portrait_idx";

    private SessionManager sessionManager;
    private PreviewView previewLocalPipCamera;
    private ImageView ivLocalPipFallback;
    private TextView tvPipCameraBadge;
    private VideoView vvRemotePartnerVideo;
    private ImageView ivRemotePartnerBackdrop;
    private TextView tvCallTimer;
    private TextView tvComboHintBanner;
    private FrameLayout videoCallComboContainer;

    private boolean isFrontCamera = true;
    private boolean isMicMuted = false;
    private int elapsedCallSeconds = 1;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private final Runnable callTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            elapsedCallSeconds++;
            int mins = elapsedCallSeconds / 60;
            int secs = elapsedCallSeconds % 60;
            if (tvCallTimer != null) {
                tvCallTimer.setText(String.format(Locale.US, "🔴 %02d:%02d", mins, secs));
            }
            timerHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isScreenshotShieldEnabled() && getWindow() != null) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        setContentView(R.layout.activity_private_video_call);

        String partnerName = getIntent().getStringExtra(EXTRA_PARTNER_NAME);
        if (partnerName == null || partnerName.trim().isEmpty()) {
            partnerName = "Alex Royal, 25";
        }
        int partnerPortraitIdx = getIntent().getIntExtra(EXTRA_PARTNER_PORTRAIT_IDX, 0);

        ivRemotePartnerBackdrop = findViewById(R.id.ivRemotePartnerBackdrop);
        vvRemotePartnerVideo = findViewById(R.id.vvRemotePartnerVideo);
        ImageView ivPartnerHeaderAvatar = findViewById(R.id.ivPartnerHeaderAvatar);
        TextView tvCallPartnerName = findViewById(R.id.tvCallPartnerName);
        tvCallTimer = findViewById(R.id.tvCallTimer);
        tvComboHintBanner = findViewById(R.id.tvComboHintBanner);
        videoCallComboContainer = findViewById(R.id.videoCallComboContainer);

        CardView cardFloatingPipLocal = findViewById(R.id.cardFloatingPipLocal);
        previewLocalPipCamera = findViewById(R.id.previewLocalPipCamera);
        ivLocalPipFallback = findViewById(R.id.ivLocalPipFallback);
        tvPipCameraBadge = findViewById(R.id.tvPipCameraBadge);

        Button btnCallMuteMic = findViewById(R.id.btnCallMuteMic);
        Button btnCallFlipCamera = findViewById(R.id.btnCallFlipCamera);
        Button btnCallGiftCombo = findViewById(R.id.btnCallGiftCombo);
        Button btnEndVideoCall = findViewById(R.id.btnEndVideoCall);

        if (tvCallPartnerName != null) {
            tvCallPartnerName.setText(partnerName + " ✓");
        }
        if (ivPartnerHeaderAvatar != null) {
            WallaPortraitStudio.loadGuyPhoto(this, ivPartnerHeaderAvatar, partnerPortraitIdx);
        }
        if (ivRemotePartnerBackdrop != null) {
            WallaPortraitStudio.loadGuyPhoto(this, ivRemotePartnerBackdrop, partnerPortraitIdx);
        }

        // 1. Start Full-Screen Remote Partner HD Video Stream
        if (vvRemotePartnerVideo != null && ivRemotePartnerBackdrop != null) {
            WallaBroadcasterStudio.startLoopingChallengerVideo(
                    this,
                    vvRemotePartnerVideo,
                    ivRemotePartnerBackdrop,
                    partnerPortraitIdx
            );
        }

        // 2. Bind Local Picture-in-Picture (PiP) CameraX Preview
        refreshPipCamera();

        // Make Floating PiP Card Draggable around the screen
        if (cardFloatingPipLocal != null) {
            enableDraggablePip(cardFloatingPipLocal);
        }

        // 3. Mic Mute / Unmute Toggle
        if (btnCallMuteMic != null) {
            btnCallMuteMic.setOnClickListener(v -> {
                isMicMuted = !isMicMuted;
                btnCallMuteMic.setText(isMicMuted ? "🔇 Muted" : "🎙️ Mic ON");
                btnCallMuteMic.setTextColor(Color.parseColor(isMicMuted ? "#FF5252" : "#00E676"));
                Toast.makeText(this, isMicMuted ? "🔇 Microphone Muted" : "🎙️ Microphone Unmuted", Toast.LENGTH_SHORT).show();
            });
        }

        // 4. Camera Flip Button (Front <-> Back)
        if (btnCallFlipCamera != null) {
            btnCallFlipCamera.setOnClickListener(v -> {
                isFrontCamera = !isFrontCamera;
                refreshPipCamera();
                Toast.makeText(this, isFrontCamera ? "📹 Switched PiP to Front Camera" : "📹 Switched PiP to Back Camera", Toast.LENGTH_SHORT).show();
            });
        }

        // 5. Rapid-Tap Gift Combo Streak Multiplier Button ('COMBO STREAK x10! 🔥' + Flying Gold Coins)
        final String finalPartnerName = partnerName;
        if (btnCallGiftCombo != null) {
            btnCallGiftCombo.setOnClickListener(v -> {
                int coins = sessionManager.getUserCoins();
                if (coins >= 10) {
                    sessionManager.setUserCoins(coins - 10);
                }
                if (videoCallComboContainer != null) {
                    int mult = WallaBroadcasterStudio.triggerGiftComboStreak(
                            this,
                            videoCallComboContainer,
                            "🌹 Golden Rose Gift",
                            sessionManager.getUserName()
                    );
                    if (tvComboHintBanner != null) {
                        tvComboHintBanner.setText("🔥 COMBO STREAK x" + mult + "! Sent Golden Rose to " + finalPartnerName + "!");
                    }
                }
            });
        }

        // 6. End Call Button
        if (btnEndVideoCall != null) {
            btnEndVideoCall.setOnClickListener(v -> {
                Toast.makeText(this, "📞 Private HD Video Call Ended (" + tvCallTimer.getText() + ")", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        // Start Real-Time Call Timer
        timerHandler.postDelayed(callTimerRunnable, 1000L);
    }

    private void refreshPipCamera() {
        if (previewLocalPipCamera == null || ivLocalPipFallback == null || tvPipCameraBadge == null) return;
        WallaBroadcasterStudio.bindCameraFeed(
                this,
                previewLocalPipCamera,
                ivLocalPipFallback,
                tvPipCameraBadge,
                isFrontCamera,
                true
        );
        tvPipCameraBadge.setText(isFrontCamera ? "📹 You (Front)" : "📹 You (Back)");
    }

    private void enableDraggablePip(final View pipCard) {
        final float[] dX = new float[1];
        final float[] dY = new float[1];
        pipCard.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX[0] = view.getX() - event.getRawX();
                    dY[0] = view.getY() - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    view.animate()
                            .x(event.getRawX() + dX[0])
                            .y(event.getRawY() + dY[0])
                            .setDuration(0)
                            .start();
                    return true;
                default:
                    return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacksAndMessages(null);
        if (vvRemotePartnerVideo != null) {
            try {
                vvRemotePartnerVideo.stopPlayback();
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
