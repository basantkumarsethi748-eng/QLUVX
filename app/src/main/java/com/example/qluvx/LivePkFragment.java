package com.example.qluvx;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class LivePkFragment extends Fragment {

    private int redScore = 18450;
    private int blueScore = 14900;
    private float currentRedWeight = 55f;
    private boolean isFollowingHost = false;
    private boolean isFrontCamera = true;
    private boolean isMicMuted = false;
    private boolean isBeautyFilterEnabled = true;

    private TextView tvScoreRed;
    private TextView tvScoreBlue;
    private TextView tvPkTimer;
    private TextView tvVipEntranceBanner;
    private TextView tvLeftCameraBadge;
    private RelativeLayout barRedSegment;
    private RelativeLayout barBlueSegment;
    private LinearLayout llLiveChatMessages;
    private ScrollView svLiveChat;
    private FrameLayout heartParticleContainer;

    private PreviewView previewHostRedCamera;
    private ImageView ivHostRedStream;
    private View viewRedBeautyOverlay;
    private VideoView vvHostBlueStream;
    private ImageView ivHostBlueStream;

    private SessionManager sessionManager;
    private CountDownTimer pkCountdownTimer;
    private ObjectAnimator giftPulseAnimator;
    private ValueAnimator tugBarAnimator;
    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_pk, container, false);

        Context ctx = requireContext();
        sessionManager = new SessionManager(ctx);

        tvScoreRed = view.findViewById(R.id.tvScoreRed);
        tvScoreBlue = view.findViewById(R.id.tvScoreBlue);
        tvPkTimer = view.findViewById(R.id.tvPkTimer);
        tvVipEntranceBanner = view.findViewById(R.id.tvVipEntranceBanner);
        tvLeftCameraBadge = view.findViewById(R.id.tvLeftCameraBadge);
        barRedSegment = view.findViewById(R.id.barRedSegment);
        barBlueSegment = view.findViewById(R.id.barBlueSegment);
        llLiveChatMessages = view.findViewById(R.id.llLiveChatMessages);
        svLiveChat = view.findViewById(R.id.svLiveChat);
        heartParticleContainer = view.findViewById(R.id.heartParticleContainer);

        ImageView ivBroadcasterAvatar = view.findViewById(R.id.ivBroadcasterAvatar);
        ImageView ivGifter1 = view.findViewById(R.id.ivGifter1);
        ImageView ivGifter2 = view.findViewById(R.id.ivGifter2);
        ImageView ivGifter3 = view.findViewById(R.id.ivGifter3);

        previewHostRedCamera = view.findViewById(R.id.previewHostRedCamera);
        ivHostRedStream = view.findViewById(R.id.ivHostRedStream);
        viewRedBeautyOverlay = view.findViewById(R.id.viewRedBeautyOverlay);
        vvHostBlueStream = view.findViewById(R.id.vvHostBlueStream);
        ivHostBlueStream = view.findViewById(R.id.ivHostBlueStream);

        View gifter1Box = view.findViewById(R.id.gifter1Box);
        View gifter2Box = view.findViewById(R.id.gifter2Box);
        View gifter3Box = view.findViewById(R.id.gifter3Box);

        TextView btnFollowHost = view.findViewById(R.id.btnFollowHost);
        TextView btnCloseBroadcast = view.findViewById(R.id.btnCloseBroadcast);
        Button btnGiftRed = view.findViewById(R.id.btnGiftRed);
        Button btnGiftBlue = view.findViewById(R.id.btnGiftBlue);
        TextView btnTapHeart = view.findViewById(R.id.btnTapHeart);
        TextView btnOpenGiftTray = view.findViewById(R.id.btnOpenGiftTray);
        EditText etLiveChatInput = view.findViewById(R.id.etLiveChatInput);
        Button btnSendLiveChat = view.findViewById(R.id.btnSendLiveChat);

        TextView btnFlipCameraPk = view.findViewById(R.id.btnFlipCameraPk);
        TextView btnToggleMicPk = view.findViewById(R.id.btnToggleMicPk);
        TextView btnToggleBeautyPk = view.findViewById(R.id.btnToggleBeautyPk);
        TextView btnOpenBroadcasterStudio = view.findViewById(R.id.btnOpenBroadcasterStudio);

        // 1. Load Real High-Resolution Male Portraits via Glide (NO cartoon faces!)
        WallaPortraitStudio.loadGuyPhoto(ctx, ivBroadcasterAvatar, 0);
        if (ivBroadcasterAvatar != null) {
            ivBroadcasterAvatar.setOnClickListener(v ->
                    WallaChatOverlay.openFullScreenChat(ctx, "Alex Royal, 25", 0, "Top", "120m", false, null)
            );
        }
        WallaPortraitStudio.loadGuyPhoto(ctx, ivGifter1, 6);
        WallaPortraitStudio.loadGuyPhoto(ctx, ivGifter2, 3);
        WallaPortraitStudio.loadGuyPhoto(ctx, ivGifter3, 9);

        // 2. Start REAL LIVE CameraX Front Camera on Left Screen + Real Looping HD Video Stream on Right Screen
        refreshLiveCameraFeed();
        if (vvHostBlueStream != null && ivHostBlueStream != null) {
            WallaBroadcasterStudio.startLoopingChallengerVideo(ctx, vvHostBlueStream, ivHostBlueStream, 0);
        }

        // 3. Camera Flip Button (Front <-> Back Camera)
        if (btnFlipCameraPk != null) {
            btnFlipCameraPk.setOnClickListener(v -> {
                isFrontCamera = !isFrontCamera;
                refreshLiveCameraFeed();
                Toast.makeText(ctx, isFrontCamera ? "📹 Switched to Front Camera" : "📹 Switched to Back Camera", Toast.LENGTH_SHORT).show();
            });
        }

        // 4. Mic Mute / Unmute Button
        if (btnToggleMicPk != null) {
            btnToggleMicPk.setOnClickListener(v -> {
                isMicMuted = !isMicMuted;
                btnToggleMicPk.setText(isMicMuted ? "🔇 Muted" : "🎙️ Mic ON");
                btnToggleMicPk.setTextColor(Color.parseColor(isMicMuted ? "#FF5252" : "#00E676"));
                Toast.makeText(ctx, isMicMuted ? "🔇 Live Microphone Muted" : "🎙️ Live Microphone Unmuted", Toast.LENGTH_SHORT).show();
            });
        }

        // 5. Beauty Filter Toggle Button
        if (btnToggleBeautyPk != null) {
            btnToggleBeautyPk.setOnClickListener(v -> {
                isBeautyFilterEnabled = !isBeautyFilterEnabled;
                if (viewRedBeautyOverlay != null) {
                    viewRedBeautyOverlay.setVisibility(isBeautyFilterEnabled ? View.VISIBLE : View.GONE);
                }
                btnToggleBeautyPk.setText(isBeautyFilterEnabled ? "✨ Beauty ON" : "✨ Beauty OFF");
                btnToggleBeautyPk.setTextColor(Color.parseColor(isBeautyFilterEnabled ? "#FF2A7A" : "#9E9EBC"));
                if (tvLeftCameraBadge != null) {
                    String cam = isFrontCamera ? "FRONT CAM LIVE" : "BACK CAM LIVE";
                    tvLeftCameraBadge.setText("📹 " + cam + (isBeautyFilterEnabled ? " • ✨ BEAUTY" : ""));
                }
            });
        }

        // 6. "🔴 GO LIVE" Full-Screen Broadcaster Studio Button
        if (btnOpenBroadcasterStudio != null) {
            btnOpenBroadcasterStudio.setOnClickListener(v -> {
                if (getActivity() != null) {
                    WallaBroadcasterStudio.openBroadcasterStudio(getActivity());
                }
            });
        }

        // Top 3 Gifter Leaderboard Clicks
        if (gifter1Box != null) {
            gifter1Box.setOnClickListener(v ->
                    Toast.makeText(ctx, "👑 #1 Top Gifter: Aarav (14,200 🪙 gifted tonight)", Toast.LENGTH_SHORT).show()
            );
        }
        if (gifter2Box != null) {
            gifter2Box.setOnClickListener(v ->
                    Toast.makeText(ctx, "🥈 #2 Top Gifter: Zayn (8,600 🪙 gifted tonight)", Toast.LENGTH_SHORT).show()
            );
        }
        if (gifter3Box != null) {
            gifter3Box.setOnClickListener(v ->
                    Toast.makeText(ctx, "🥉 #3 Top Gifter: Vikram (5,100 🪙 gifted tonight)", Toast.LENGTH_SHORT).show()
            );
        }

        // Top-Left Pink "+ Follow" Button
        if (btnFollowHost != null) {
            btnFollowHost.setOnClickListener(v -> {
                isFollowingHost = !isFollowingHost;
                btnFollowHost.setText(isFollowingHost ? "✓ Following" : "+ Follow");
                btnFollowHost.setBackgroundResource(isFollowingHost ? R.drawable.bg_walla_cyan_pill : R.drawable.bg_walla_pink_pill);
                btnFollowHost.setTextColor(isFollowingHost ? Color.parseColor("#0A0A14") : Color.WHITE);
                if (isFollowingHost) {
                    appendLiveChatMessage("💖 " + sessionManager.getUserName(), "followed Host Alex Royal!", "#FF2A7A");
                    spawnHeartBurst(4);
                }
            });
        }

        // Top-Right Close Button (Returns to Nearby Tab)
        if (btnCloseBroadcast != null) {
            btnCloseBroadcast.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_nearby);
                }
            });
        }

        // Animate Floating Gift Button Pulse
        if (btnOpenGiftTray != null) {
            giftPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    btnOpenGiftTray,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.94f, 1.08f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.94f, 1.08f)
            );
            giftPulseAnimator.setDuration(750);
            giftPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            giftPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            giftPulseAnimator.start();

            btnOpenGiftTray.setOnClickListener(v -> showLuxuryGiftBottomDialog());
        }

        // Heart Particle Tap Button
        if (btnTapHeart != null) {
            btnTapHeart.setOnClickListener(v -> spawnHeartBurst(5));
        }

        // "+500 Gift Red" and "+500 Gift Blue" Buttons -> Slide PK Bar + Full-Screen Animated Gift Blast!
        if (btnGiftRed != null) {
            btnGiftRed.setOnClickListener(v -> sendPkGift(true, "🏎️ LUXURY SPORTS CAR", 500, 50));
        }
        if (btnGiftBlue != null) {
            btnGiftBlue.setOnClickListener(v -> sendPkGift(false, "🚀 SUPERSONIC ROCKET", 500, 50));
        }

        // Seed Initial Live Chat Stream
        seedInitialChatMessages();

        // Live Comment Bar: Typing and pressing send adds the comment to the live stream floating chat list
        if (btnSendLiveChat != null && etLiveChatInput != null) {
            Runnable sendCommentAction = () -> {
                String text = etLiveChatInput.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(ctx, "Type a live comment first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                etLiveChatInput.setText("");
                appendLiveChatMessage("👑 " + sessionManager.getUserName(), text, "#F1C40F");
                spawnHeartBurst(3);
            };

            btnSendLiveChat.setOnClickListener(v -> sendCommentAction.run());
            etLiveChatInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    sendCommentAction.run();
                    return true;
                }
                return false;
            });
        }

        startPkCountdownTimer();
        animatePkTugOfWarBar(false);

        return view;
    }

    public void refreshLiveCameraFeed() {
        Context ctx = getContext();
        if (ctx == null || previewHostRedCamera == null || ivHostRedStream == null || tvLeftCameraBadge == null) {
            return;
        }
        WallaBroadcasterStudio.bindCameraFeed(
                requireActivity(),
                previewHostRedCamera,
                ivHostRedStream,
                tvLeftCameraBadge,
                isFrontCamera,
                isBeautyFilterEnabled
        );
    }

    private void sendPkGift(boolean toRedHost, String giftTitle, int pkPoints, int coinCost) {
        Context ctx = getContext();
        int currentCoins = sessionManager.getUserCoins();
        if (currentCoins >= coinCost) {
            sessionManager.setUserCoins(currentCoins - coinCost);
        }

        if (toRedHost) {
            redScore += pkPoints;
            appendLiveChatMessage("🎁 " + sessionManager.getUserName(),
                    "sent " + giftTitle + " (+" + pkPoints + " PK) to RED Host Alex!", "#FF2A7A");
            if (tvVipEntranceBanner != null) {
                tvVipEntranceBanner.setText("🔥 " + sessionManager.getUserName() + " boosted RED (+" + pkPoints + ") with " + giftTitle + "!");
            }
        } else {
            blueScore += pkPoints;
            appendLiveChatMessage("🎁 " + sessionManager.getUserName(),
                    "sent " + giftTitle + " (+" + pkPoints + " PK) to BLUE Host Ryan!", "#00E5FF");
            if (tvVipEntranceBanner != null) {
                tvVipEntranceBanner.setText("❄️ " + sessionManager.getUserName() + " boosted BLUE (+" + pkPoints + ") with " + giftTitle + "!");
            }
        }

        animatePkTugOfWarBar(true);
        spawnHeartBurst(6);

        // Trigger Full-Screen Animated Gift Blast (Flying Luxury Car / Rocket across the screen)
        if (ctx != null && heartParticleContainer != null) {
            WallaBroadcasterStudio.triggerFullScreenGiftBlast(
                    ctx,
                    heartParticleContainer,
                    toRedHost,
                    giftTitle,
                    sessionManager.getUserName()
            );
        }
    }

    private void animatePkTugOfWarBar(boolean animateSlide) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        if (tvScoreRed != null) {
            tvScoreRed.setText(nf.format(redScore));
        }
        if (tvScoreBlue != null) {
            tvScoreBlue.setText(nf.format(blueScore));
        }

        if (barRedSegment == null || barBlueSegment == null) return;

        float total = Math.max(1f, redScore + blueScore);
        final float targetRedWeight = Math.max(18f, Math.min(82f, (redScore / total) * 100f));

        if (!animateSlide) {
            currentRedWeight = targetRedWeight;
            applyBarWeights(currentRedWeight);
            return;
        }

        if (tugBarAnimator != null) {
            tugBarAnimator.cancel();
        }

        tugBarAnimator = ValueAnimator.ofFloat(currentRedWeight, targetRedWeight);
        tugBarAnimator.setDuration(420L);
        tugBarAnimator.setInterpolator(new DecelerateInterpolator());
        tugBarAnimator.addUpdateListener(anim -> {
            currentRedWeight = (float) anim.getAnimatedValue();
            applyBarWeights(currentRedWeight);
        });
        tugBarAnimator.start();
    }

    private void applyBarWeights(float redWeight) {
        if (barRedSegment == null || barBlueSegment == null) return;
        float blueWeight = 100f - redWeight;
        LinearLayout.LayoutParams redLp = (LinearLayout.LayoutParams) barRedSegment.getLayoutParams();
        LinearLayout.LayoutParams blueLp = (LinearLayout.LayoutParams) barBlueSegment.getLayoutParams();
        redLp.weight = redWeight;
        blueLp.weight = blueWeight;
        barRedSegment.setLayoutParams(redLp);
        barBlueSegment.setLayoutParams(blueLp);
    }

    private void showLuxuryGiftBottomDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        String[] gifts = new String[]{
                "🌹 Electric Rose to Alex (10 🪙)  •  +500 PK",
                "🏎️ Luxury Sports Car to Ryan (100 🪙)  •  +1,000 PK",
                "🚀 Interstellar Rocket to Alex (500 🪙)  •  +2,500 PK",
                "👑 Imperial Dragon Crown to Ryan (1,000 🪙)  •  +5,000 PK"
        };

        new AlertDialog.Builder(ctx)
                .setTitle("🎁 Send Luxury PK Gift (Balance: " + sessionManager.getFormattedCoins() + " 🪙)")
                .setItems(gifts, (dialog, which) -> {
                    int[] costs = {10, 100, 500, 1000};
                    int[] points = {500, 1000, 2500, 5000};
                    boolean[] toRed = {true, false, true, false};
                    String[] names = {"🌹 ELECTRIC ROSE", "🏎️ LUXURY SPORTS CAR", "🚀 INTERSTELLAR ROCKET", "👑 IMPERIAL DRAGON CROWN"};
                    sendPkGift(toRed[which], names[which], points[which], costs[which]);
                })
                .show();
    }

    private void seedInitialChatMessages() {
        if (llLiveChatMessages == null) return;
        llLiveChatMessages.removeAllViews();
        appendLiveChatMessage("System", "Live Camera & HD PK Battle Active", "#8E8E93");
        appendLiveChatMessage("👑 Kabir", "Lighting and camera look super clean tonight", "#E6B800");
        appendLiveChatMessage("Zayn", "Sent 🏎️ Luxury Sports Car to Alex Royal", "#FF4585");
        appendLiveChatMessage("Liam", "Ryan drop the progressive house mix", "#8E8E93");
    }

    private void appendLiveChatMessage(String sender, String message, String senderHexColor) {
        Context ctx = getContext();
        if (ctx == null || llLiveChatMessages == null) return;

        TextView tvMsg = new TextView(ctx);
        String full = sender + "  " + message;
        SpannableString span = new SpannableString(full);
        span.setSpan(new ForegroundColorSpan(Color.parseColor(senderHexColor)),
                0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.WHITE),
                sender.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvMsg.setText(span);
        tvMsg.setTextSize(11f);
        tvMsg.setTypeface(Typeface.DEFAULT_BOLD);
        tvMsg.setBackgroundResource(R.drawable.bg_live_comment_pill);
        tvMsg.setPadding(dp(11), dp(5), dp(11), dp(5));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(5);
        tvMsg.setLayoutParams(lp);

        // Smooth fade-in for translucent floating comment pill
        tvMsg.setAlpha(0f);
        tvMsg.animate().alpha(0.92f).setDuration(260L).start();

        llLiveChatMessages.addView(tvMsg);

        if (svLiveChat != null) {
            svLiveChat.post(() -> svLiveChat.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void spawnHeartBurst(int count) {
        for (int i = 0; i < count; i++) {
            spawnFloatingHeartParticle();
        }
    }

    private void spawnFloatingHeartParticle() {
        Context ctx = getContext();
        if (ctx == null || heartParticleContainer == null) return;

        String[] hearts = {"❤️", "💖", "💗", "🔥", "💎", "✨", "🌹"};
        final TextView tvHeart = new TextView(ctx);
        tvHeart.setText(hearts[random.nextInt(hearts.length)]);
        tvHeart.setTextSize(20f + random.nextInt(6));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END
        );
        lp.rightMargin = dp(16 + random.nextInt(90));
        lp.bottomMargin = dp(86 + random.nextInt(25));
        tvHeart.setLayoutParams(lp);
        heartParticleContainer.addView(tvHeart);

        float driftX = (random.nextFloat() - 0.5f) * dp(110);
        ObjectAnimator moveUp = ObjectAnimator.ofFloat(tvHeart, View.TRANSLATION_Y, 0f, -dp(290 + random.nextInt(80)));
        ObjectAnimator moveSide = ObjectAnimator.ofFloat(tvHeart, View.TRANSLATION_X, 0f, driftX);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tvHeart, View.ALPHA, 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvHeart, View.SCALE_X, 0.7f, 1.35f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvHeart, View.SCALE_Y, 0.7f, 1.35f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveUp, moveSide, fadeOut, scaleX, scaleY);
        set.setDuration(1200 + random.nextInt(650));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (heartParticleContainer != null) {
                    heartParticleContainer.removeView(tvHeart);
                }
            }
        });
        set.start();
    }

    private void startPkCountdownTimer() {
        if (pkCountdownTimer != null) {
            pkCountdownTimer.cancel();
        }
        pkCountdownTimer = new CountDownTimer(4 * 60 * 1000L + 42 * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvPkTimer == null) return;
                long totalSecs = millisUntilFinished / 1000L;
                long mins = totalSecs / 60L;
                long secs = totalSecs % 60L;
                tvPkTimer.setText(String.format(Locale.US, "⏱️ PK %02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                if (tvPkTimer != null) {
                    tvPkTimer.setText(redScore >= blueScore ? "🏆 RED VICTORY!" : "🏆 BLUE VICTORY!");
                }
            }
        }.start();
    }

    private int dp(int val) {
        if (getContext() == null) return val;
        return Math.round(val * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (pkCountdownTimer != null) {
            pkCountdownTimer.cancel();
        }
        if (giftPulseAnimator != null) {
            giftPulseAnimator.cancel();
        }
        if (tugBarAnimator != null) {
            tugBarAnimator.cancel();
        }
        if (vvHostBlueStream != null) {
            try {
                vvHostBlueStream.stopPlayback();
            } catch (Exception ignored) {}
        }
        super.onDestroyView();
    }
}
