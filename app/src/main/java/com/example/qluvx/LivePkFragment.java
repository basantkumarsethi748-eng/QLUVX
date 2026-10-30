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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class LivePkFragment extends Fragment {

    private int redScore = 18450;
    private int blueScore = 14900;
    private boolean isFollowingHost = false;

    private TextView tvScoreRed;
    private TextView tvScoreBlue;
    private TextView tvPkTimer;
    private TextView tvVipEntranceBanner;
    private RelativeLayout barRedSegment;
    private RelativeLayout barBlueSegment;
    private LinearLayout llLiveChatMessages;
    private ScrollView svLiveChat;
    private FrameLayout heartParticleContainer;

    private SessionManager sessionManager;
    private CountDownTimer pkCountdownTimer;
    private ObjectAnimator giftPulseAnimator;
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
        barRedSegment = view.findViewById(R.id.barRedSegment);
        barBlueSegment = view.findViewById(R.id.barBlueSegment);
        llLiveChatMessages = view.findViewById(R.id.llLiveChatMessages);
        svLiveChat = view.findViewById(R.id.svLiveChat);
        heartParticleContainer = view.findViewById(R.id.heartParticleContainer);

        ImageView ivBroadcasterAvatar = view.findViewById(R.id.ivBroadcasterAvatar);
        ImageView ivGifter1 = view.findViewById(R.id.ivGifter1);
        ImageView ivGifter2 = view.findViewById(R.id.ivGifter2);
        ImageView ivGifter3 = view.findViewById(R.id.ivGifter3);
        ImageView ivHostRedStream = view.findViewById(R.id.ivHostRedStream);
        ImageView ivHostBlueStream = view.findViewById(R.id.ivHostBlueStream);

        TextView btnFollowHost = view.findViewById(R.id.btnFollowHost);
        TextView btnCloseBroadcast = view.findViewById(R.id.btnCloseBroadcast);
        Button btnGiftRed = view.findViewById(R.id.btnGiftRed);
        Button btnGiftBlue = view.findViewById(R.id.btnGiftBlue);
        TextView btnTapHeart = view.findViewById(R.id.btnTapHeart);
        TextView btnOpenGiftTray = view.findViewById(R.id.btnOpenGiftTray);
        EditText etLiveChatInput = view.findViewById(R.id.etLiveChatInput);
        Button btnSendLiveChat = view.findViewById(R.id.btnSendLiveChat);

        // Bind High-Res Streamer Feeds & Avatars from WallaPortraitStudio
        if (ivBroadcasterAvatar != null) {
            ivBroadcasterAvatar.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, 0));
        }
        if (ivGifter1 != null) {
            ivGifter1.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, 6));
        }
        if (ivGifter2 != null) {
            ivGifter2.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, 3));
        }
        if (ivGifter3 != null) {
            ivGifter3.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, 9));
        }
        if (ivHostRedStream != null) {
            ivHostRedStream.setImageDrawable(WallaPortraitStudio.getLiveStreamerFeedDrawable(ctx, true));
        }
        if (ivHostBlueStream != null) {
            ivHostBlueStream.setImageDrawable(WallaPortraitStudio.getLiveStreamerFeedDrawable(ctx, false));
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
            btnTapHeart.setOnClickListener(v -> {
                for (int i = 0; i < 3; i++) {
                    spawnFloatingHeartParticle();
                }
            });
        }

        // Red vs Blue PK Vote / Gift Boost Buttons
        if (btnGiftRed != null) {
            btnGiftRed.setOnClickListener(v -> sendPkGift(true, "🌹 Rose Bouquet", 500, 50));
        }
        if (btnGiftBlue != null) {
            btnGiftBlue.setOnClickListener(v -> sendPkGift(false, "🏎️ Neon Sports Car", 500, 50));
        }

        // Seed Initial Live Chat Stream
        seedInitialChatMessages();

        // Live Chat Input
        if (btnSendLiveChat != null && etLiveChatInput != null) {
            btnSendLiveChat.setOnClickListener(v -> {
                String text = etLiveChatInput.getText().toString().trim();
                if (text.isEmpty()) return;
                etLiveChatInput.setText("");
                appendLiveChatMessage("👑 " + sessionManager.getUserName(), text, "#F1C40F");
                spawnFloatingHeartParticle();
            });
        }

        // Start 5-Minute Live PK Countdown Timer
        startPkCountdownTimer();
        updatePkTugOfWarBar();

        return view;
    }

    private void sendPkGift(boolean toRedHost, String giftTitle, int pkPoints, int coinCost) {
        int currentCoins = sessionManager.getUserCoins();
        if (currentCoins >= coinCost) {
            sessionManager.setUserCoins(currentCoins - coinCost);
        }

        if (toRedHost) {
            redScore += pkPoints;
            appendLiveChatMessage("🎁 " + sessionManager.getUserName(),
                    "sent " + giftTitle + " (+" + pkPoints + " PK) to Alex!", "#FF2A7A");
            if (tvVipEntranceBanner != null) {
                tvVipEntranceBanner.setText("🔥 " + sessionManager.getUserName() + " boosted RED with " + giftTitle + "!");
            }
        } else {
            blueScore += pkPoints;
            appendLiveChatMessage("🎁 " + sessionManager.getUserName(),
                    "sent " + giftTitle + " (+" + pkPoints + " PK) to Ryan!", "#00E5FF");
            if (tvVipEntranceBanner != null) {
                tvVipEntranceBanner.setText("❄️ " + sessionManager.getUserName() + " boosted BLUE with " + giftTitle + "!");
            }
        }

        updatePkTugOfWarBar();
        spawnFloatingHeartParticle();
        spawnFloatingHeartParticle();
    }

    private void updatePkTugOfWarBar() {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        if (tvScoreRed != null) {
            tvScoreRed.setText("🔥 RED " + nf.format(redScore));
        }
        if (tvScoreBlue != null) {
            tvScoreBlue.setText(nf.format(blueScore) + " BLUE ❄️");
        }

        if (barRedSegment != null && barBlueSegment != null) {
            float total = Math.max(1f, redScore + blueScore);
            float redWeight = Math.max(20f, Math.min(80f, (redScore / total) * 100f));
            float blueWeight = 100f - redWeight;

            LinearLayout.LayoutParams redLp = (LinearLayout.LayoutParams) barRedSegment.getLayoutParams();
            LinearLayout.LayoutParams blueLp = (LinearLayout.LayoutParams) barBlueSegment.getLayoutParams();
            redLp.weight = redWeight;
            blueLp.weight = blueWeight;
            barRedSegment.setLayoutParams(redLp);
            barBlueSegment.setLayoutParams(blueLp);
        }
    }

    private void showLuxuryGiftBottomDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        String[] gifts = new String[]{
                "🌹 Neon Rose (10 🪙)  •  +150 PK Score",
                "🏎️ Cyber Sports Car (100 🪙)  •  +1,000 PK Score",
                "✈️ Supersonic Private Jet (500 🪙)  •  +5,000 PK Score",
                "👑 Imperial Dragon Crown (1,000 🪙)  •  +12,000 PK Score"
        };

        new AlertDialog.Builder(ctx)
                .setTitle("🎁 Send Luxury Gift (Balance: " + sessionManager.getFormattedCoins() + " 🪙)")
                .setItems(gifts, (dialog, which) -> {
                    int[] costs = {10, 100, 500, 1000};
                    int[] points = {150, 1000, 5000, 12000};
                    String[] names = {"🌹 Neon Rose", "🏎️ Cyber Sports Car", "✈️ Supersonic Jet", "👑 Imperial Dragon Crown"};
                    sendPkGift(true, names[which], points[which], costs[which]);
                    Toast.makeText(ctx, "Sent " + names[which] + "! Wallet: " + sessionManager.getFormattedCoins() + " 🪙", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void seedInitialChatMessages() {
        if (llLiveChatMessages == null) return;
        llLiveChatMessages.removeAllViews();
        appendLiveChatMessage("🛡️ System", "Welcome to Walla HD Live PK Battle! Tap ❤️ or send gifts to boost your host.", "#00E5FF");
        appendLiveChatMessage("👑 Kabir (Lv.42)", "Alex's studio lighting looks insane tonight 🔥🔥", "#F1C40F");
        appendLiveChatMessage("💎 Zayn_BLR", "Sent 🌹 Neon Rose x5 to Alex Royal!", "#FF2A7A");
        appendLiveChatMessage("⚡ Liam_23", "Team Blue let's catch up! Ryan drop the beat 🎧", "#00E5FF");
        appendLiveChatMessage("🔥 Vikram", "Who's joining the 8-seat voice party after this PK?", "#00E676");
    }

    private void appendLiveChatMessage(String sender, String message, String senderHexColor) {
        Context ctx = getContext();
        if (ctx == null || llLiveChatMessages == null) return;

        TextView tvMsg = new TextView(ctx);
        String full = sender + ": " + message;
        SpannableString span = new SpannableString(full);
        span.setSpan(new ForegroundColorSpan(Color.parseColor(senderHexColor)),
                0, sender.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.WHITE),
                sender.length() + 1, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvMsg.setText(span);
        tvMsg.setTextSize(11.5f);
        tvMsg.setTypeface(Typeface.DEFAULT_BOLD);
        tvMsg.setPadding(0, dp(2), 0, dp(3));
        llLiveChatMessages.addView(tvMsg);

        if (svLiveChat != null) {
            svLiveChat.post(() -> svLiveChat.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void spawnFloatingHeartParticle() {
        Context ctx = getContext();
        if (ctx == null || heartParticleContainer == null) return;

        String[] hearts = {"❤️", "💖", "💗", "🔥", "💎", "✨"};
        final TextView tvHeart = new TextView(ctx);
        tvHeart.setText(hearts[random.nextInt(hearts.length)]);
        tvHeart.setTextSize(20f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END
        );
        lp.rightMargin = dp(18 + random.nextInt(36));
        lp.bottomMargin = dp(90);
        tvHeart.setLayoutParams(lp);
        heartParticleContainer.addView(tvHeart);

        float driftX = (random.nextFloat() - 0.5f) * dp(70);
        ObjectAnimator moveUp = ObjectAnimator.ofFloat(tvHeart, View.TRANSLATION_Y, 0f, -dp(260));
        ObjectAnimator moveSide = ObjectAnimator.ofFloat(tvHeart, View.TRANSLATION_X, 0f, driftX);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tvHeart, View.ALPHA, 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvHeart, View.SCALE_X, 0.7f, 1.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvHeart, View.SCALE_Y, 0.7f, 1.3f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveUp, moveSide, fadeOut, scaleX, scaleY);
        set.setDuration(1400 + random.nextInt(500));
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
                    tvPkTimer.setText("🏆 RED VICTORY!");
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
        super.onDestroyView();
    }
}
