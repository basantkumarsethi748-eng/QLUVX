package com.example.qluvx;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VoicePartyFragment extends Fragment {

    private boolean isMicMuted = false;
    private boolean isVelvetRoseTheme = false;
    private SessionManager sessionManager;
    private final List<ObjectAnimator> seatAnimators = new ArrayList<>();
    private final Random random = new Random();

    private ImageView ivRoomWallpaper;
    private TextView tvRoomTitle;
    private TextView btnWallpaperTheme;
    private TextView tvSfxLiveTicker;
    private TextView tvVoiceRoomLog;
    private TextView tvHostMicBadge;
    private View viewHostPulseOuter;
    private View viewHostPulseInner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_party, container, false);

        Context ctx = requireContext();
        sessionManager = new SessionManager(ctx);

        ivRoomWallpaper = view.findViewById(R.id.ivRoomWallpaper);
        tvRoomTitle = view.findViewById(R.id.tvRoomTitle);
        btnWallpaperTheme = view.findViewById(R.id.btnWallpaperTheme);
        tvSfxLiveTicker = view.findViewById(R.id.tvSfxLiveTicker);
        tvVoiceRoomLog = view.findViewById(R.id.tvVoiceRoomLog);
        tvHostMicBadge = view.findViewById(R.id.tvHostMicBadge);
        viewHostPulseOuter = view.findViewById(R.id.viewHostPulseOuter);
        viewHostPulseInner = view.findViewById(R.id.viewHostPulseInner);

        ImageView ivHostSeatAvatar = view.findViewById(R.id.ivHostSeatAvatar);
        TextView tvHostSeatName = view.findViewById(R.id.tvHostSeatName);
        LinearLayout llEightSeatsGrid = view.findViewById(R.id.llEightSeatsGrid);

        Button btnMicToggle = view.findViewById(R.id.ivMicToggle);
        Button btnSoundboard = view.findViewById(R.id.ivSoundboard);
        Button btnGift = view.findViewById(R.id.ivGift);
        Button btnLuckySpin = view.findViewById(R.id.btnLuckySpin);
        Button btnBottomWheel = view.findViewById(R.id.btnBottomWheel);

        // Apply Initial Cyber Galaxy Wallpaper
        applyRoomWallpaper();

        if (btnWallpaperTheme != null) {
            btnWallpaperTheme.setOnClickListener(v -> {
                isVelvetRoseTheme = !isVelvetRoseTheme;
                applyRoomWallpaper();
            });
        }

        // Bind Host Center Seat (with Golden Crown Badge & Pulsing Rings)
        if (ivHostSeatAvatar != null) {
            ivHostSeatAvatar.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, 6));
        }
        if (tvHostSeatName != null) {
            tvHostSeatName.setText(sessionManager.getUserName() + " ✓ • 👑 Host");
        }
        startNeonPulse(viewHostPulseOuter, 950L, 1.10f);
        startNeonPulse(viewHostPulseInner, 750L, 1.07f);

        // Build 8 Circular Mic Seats (2 Rows x 4 Columns) with Pulsing Animated Neon Rings
        populateEightCircularSeats(llEightSeatsGrid);

        // 1. Mic Toggle
        btnMicToggle.setOnClickListener(v -> {
            isMicMuted = !isMicMuted;
            btnMicToggle.setText(isMicMuted ? "🔇 Muted" : "🎙️ Mic ON");
            btnMicToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor(isMicMuted ? "#4A4A68" : "#00E676")
            ));
            btnMicToggle.setTextColor(isMicMuted ? Color.WHITE : Color.parseColor("#0A0A14"));
            if (tvHostMicBadge != null) {
                tvHostMicBadge.setText(isMicMuted ? "🔇 MUTED" : "🎙️ ON AIR");
            }
            if (tvSfxLiveTicker != null) {
                tvSfxLiveTicker.setText(isMicMuted
                        ? "🔇 Your host mic is muted • Alex & Ryan are speaking..."
                        : "🎙️ Your microphone is LIVE with spatial neon soundwave!");
            }
        });

        // 2. SFX Soundboard (Applause, DJ Horn, Cheer, Drumroll)
        btnSoundboard.setOnClickListener(v -> showSfxSoundboardDialog());

        // 3. Lucky Spin Wheel Game
        View.OnClickListener wheelListener = v -> showLuckySpinWheelDialog();
        if (btnLuckySpin != null) btnLuckySpin.setOnClickListener(wheelListener);
        if (btnBottomWheel != null) btnBottomWheel.setOnClickListener(wheelListener);

        // 4. Send Gift to Voice Room
        btnGift.setOnClickListener(v -> showVoicePartyGiftDialog());

        return view;
    }

    private void applyRoomWallpaper() {
        Context ctx = getContext();
        if (ctx == null || ivRoomWallpaper == null) return;
        ivRoomWallpaper.setImageDrawable(WallaPortraitStudio.getVoiceRoomWallpaper(ctx, isVelvetRoseTheme));
        if (tvRoomTitle != null) {
            tvRoomTitle.setText(isVelvetRoseTheme ? "🌹 Velvet Rose Royal Lounge" : "🌌 Cyber Galaxy Voice Party");
        }
        if (btnWallpaperTheme != null) {
            btnWallpaperTheme.setText(isVelvetRoseTheme ? "🌹 Velvet" : "🌌 Galaxy");
            btnWallpaperTheme.setTextColor(Color.parseColor(isVelvetRoseTheme ? "#FF2A7A" : "#00E5FF"));
        }
    }

    private void populateEightCircularSeats(LinearLayout gridContainer) {
        Context ctx = getContext();
        if (ctx == null || gridContainer == null) return;
        gridContainer.removeAllViews();

        // 8 Seats: [seatNum, name, portraitIdx, isSpeaking, ringColorHex, statusTag]
        Object[][] seats = new Object[][]{
                {1, "Alex", 0, true, "#FF2A7A", "🎙️ Live"},
                {2, "Ryan", 1, true, "#00E5FF", "🎙️ Live"},
                {3, "Kabir", 2, true, "#F1C40F", "🎵 DJ"},
                {4, "Zayn", 3, false, "#4A4A6A", "🔇 Muted"},
                {5, "Liam", 4, true, "#00E5FF", "🎙️ Live"},
                {6, "Vikram", 9, false, "#4A4A6A", "🎧 Listen"},
                {7, "Seat 7", -1, false, "#2EFFFFFF", "➕ Join"},
                {8, "VIP 8", 7, true, "#F1C40F", "👑 VIP"}
        };

        for (int row = 0; row < 2; row++) {
            LinearLayout rowLayout = new LinearLayout(ctx);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setWeightSum(4f);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (row == 1) {
                rowLp.topMargin = dp(8);
            }
            rowLayout.setLayoutParams(rowLp);

            for (int col = 0; col < 4; col++) {
                int idx = row * 4 + col;
                Object[] seat = seats[idx];
                int seatNum = (Integer) seat[0];
                String seatName = (String) seat[1];
                int portraitIdx = (Integer) seat[2];
                boolean isSpeaking = (Boolean) seat[3];
                String ringColor = (String) seat[4];
                String badgeText = (String) seat[5];

                LinearLayout seatBox = new LinearLayout(ctx);
                seatBox.setOrientation(LinearLayout.VERTICAL);
                seatBox.setGravity(Gravity.CENTER_HORIZONTAL);
                seatBox.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                FrameLayout avatarFrame = new FrameLayout(ctx);
                avatarFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));

                // Pulsing Neon Ring around Speaking Seats
                View neonRing = new View(ctx);
                FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(dp(62), dp(62), Gravity.CENTER);
                neonRing.setLayoutParams(ringLp);
                GradientDrawable ringShape = new GradientDrawable();
                ringShape.setShape(GradientDrawable.OVAL);
                ringShape.setColor(isSpeaking ? Color.parseColor("#2200E5FF") : Color.parseColor("#141424"));
                ringShape.setStroke(dp(2), Color.parseColor(ringColor));
                neonRing.setBackground(ringShape);
                avatarFrame.addView(neonRing);

                if (isSpeaking) {
                    startNeonPulse(neonRing, 780L + idx * 90L, 1.11f);
                }

                // Circular Seat Avatar or Join Icon
                CardView circleCard = new CardView(ctx);
                FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER);
                circleCard.setLayoutParams(cardLp);
                circleCard.setRadius(dp(25));
                circleCard.setCardElevation(0f);
                circleCard.setCardBackgroundColor(Color.parseColor("#181830"));

                if (portraitIdx >= 0) {
                    ImageView ivSeat = new ImageView(ctx);
                    ivSeat.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    ivSeat.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivSeat.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, portraitIdx));
                    circleCard.addView(ivSeat);
                } else {
                    TextView tvPlus = new TextView(ctx);
                    tvPlus.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    tvPlus.setGravity(Gravity.CENTER);
                    tvPlus.setText("➕");
                    tvPlus.setTextColor(Color.parseColor("#00E5FF"));
                    tvPlus.setTextSize(18f);
                    circleCard.addView(tvPlus);
                }
                avatarFrame.addView(circleCard);

                // Bottom Status Pill on Seat
                TextView tvStatus = new TextView(ctx);
                FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                );
                tvStatus.setLayoutParams(statusLp);
                tvStatus.setText(badgeText);
                tvStatus.setTextSize(8f);
                tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setPadding(dp(5), dp(1), dp(5), dp(1));
                tvStatus.setBackgroundResource(isSpeaking ? R.drawable.bg_walla_pink_pill : R.drawable.bg_walla_pill_dark);
                avatarFrame.addView(tvStatus);

                seatBox.addView(avatarFrame);

                TextView tvLabel = new TextView(ctx);
                tvLabel.setText(seatNum + ". " + seatName);
                tvLabel.setTextColor(isSpeaking ? Color.WHITE : Color.parseColor("#9E9EBC"));
                tvLabel.setTextSize(10.5f);
                tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
                tvLabel.setSingleLine(true);
                tvLabel.setPadding(0, dp(2), 0, 0);
                seatBox.addView(tvLabel);

                seatBox.setOnClickListener(v -> {
                    if (portraitIdx < 0) {
                        Toast.makeText(ctx, "🎙️ Joined Seat #" + seatNum + " on Live Mic!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ctx, "🎁 Sent Neon Cheer to " + seatName + " (Seat #" + seatNum + ")", Toast.LENGTH_SHORT).show();
                    }
                });

                rowLayout.addView(seatBox);
            }
            gridContainer.addView(rowLayout);
        }
    }

    private void showSfxSoundboardDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        String[] sfxEffects = new String[]{
                "👏 Stadium Applause & Cheers",
                "📢 Club DJ Airhorn Blast (x3)",
                "🥁 Royal Drumroll Suspense",
                "😂 Studio Crowd Laughter",
                "🔥 Cyber Bass Drop Siren"
        };

        new AlertDialog.Builder(ctx)
                .setTitle("🔊 Voice Party Live SFX Soundboard")
                .setItems(sfxEffects, (dialog, which) -> {
                    String chosen = sfxEffects[which];
                    if (tvSfxLiveTicker != null) {
                        tvSfxLiveTicker.setText("🔊 Playing Room SFX: " + chosen + "!");
                    }
                    Toast.makeText(ctx, "🔊 Broadcasted SFX: " + chosen, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showLuckySpinWheelDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("🎡 Walla Royal Lucky Spin Wheel");
        tvTitle.setTextColor(Color.parseColor("#F1C40F"));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        TextView tvSub = new TextView(ctx);
        tvSub.setText("Wallet Balance: " + sessionManager.getFormattedCoins() + " 🪙");
        tvSub.setTextColor(Color.parseColor("#00E5FF"));
        tvSub.setTextSize(12.5f);
        tvSub.setPadding(0, dp(4), 0, dp(14));
        root.addView(tvSub);

        // Custom Drawn Colorful Lucky Wheel View
        final View wheelView = new View(ctx) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF oval = new RectF();
            private final int[] colors = {
                    0xFFFF2A7A, 0xFF00E5FF, 0xFFF1C40F, 0xFF7C4DFF,
                    0xFF00E676, 0xFFFF5252, 0xFF2979FF, 0xFFFFAB00
            };

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth();
                float h = getHeight();
                float r = Math.min(w, h) * 0.45f;
                float cx = w * 0.5f;
                float cy = h * 0.5f;
                oval.set(cx - r, cy - r, cx + r, cy + r);

                for (int i = 0; i < 8; i++) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(colors[i]);
                    canvas.drawArc(oval, i * 45f, 45f, true, paint);
                }

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(4));
                paint.setColor(0xFFF1C40F);
                canvas.drawCircle(cx, cy, r, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF0A0A14);
                canvas.drawCircle(cx, cy, r * 0.22f, paint);
                paint.setColor(0xFFF1C40F);
                canvas.drawCircle(cx, cy, r * 0.14f, paint);
            }
        };
        wheelView.setLayoutParams(new LinearLayout.LayoutParams(dp(180), dp(180)));
        root.addView(wheelView);

        final TextView tvResult = new TextView(ctx);
        tvResult.setText("Tap SPIN NOW to win up to +1,000 Coins!");
        tvResult.setTextColor(Color.WHITE);
        tvResult.setTextSize(13f);
        tvResult.setTypeface(Typeface.DEFAULT_BOLD);
        tvResult.setGravity(Gravity.CENTER);
        tvResult.setPadding(0, dp(14), 0, dp(14));
        root.addView(tvResult);

        Button btnSpinNow = new Button(ctx);
        btnSpinNow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        btnSpinNow.setText("🎡 SPIN NOW (Free VIP Spin)");
        btnSpinNow.setAllCaps(false);
        btnSpinNow.setBackgroundColor(Color.parseColor("#FF2A7A"));
        btnSpinNow.setTextColor(Color.WHITE);
        root.addView(btnSpinNow);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(root)
                .setNegativeButton("Close", null)
                .create();

        btnSpinNow.setOnClickListener(v -> {
            btnSpinNow.setEnabled(false);
            float targetAngle = 1080f + random.nextInt(720);
            ObjectAnimator spinAnim = ObjectAnimator.ofFloat(wheelView, View.ROTATION, 0f, targetAngle);
            spinAnim.setDuration(2200L);
            spinAnim.setInterpolator(new DecelerateInterpolator());
            spinAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    int[] rewards = {250, 500, 1000};
                    int won = rewards[random.nextInt(rewards.length)];
                    sessionManager.setUserCoins(sessionManager.getUserCoins() + won);
                    tvSub.setText("Wallet Balance: " + sessionManager.getFormattedCoins() + " 🪙");
                    tvResult.setText("🎉 JACKPOT! You won +" + won + " Gold Coins 🪙!");
                    tvResult.setTextColor(Color.parseColor("#00E676"));
                    btnSpinNow.setEnabled(true);
                    btnSpinNow.setText("🎡 Spin Again");
                }
            });
            spinAnim.start();
        });

        dialog.show();
    }

    private void showVoicePartyGiftDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        String[] partyGifts = new String[]{
                "🌹 Neon Velvet Rose (10 🪙) -> All 8 Mic Seats",
                "🥂 Golden Champagne Tower (200 🪙) -> Host Julian",
                "🚀 Interstellar Starship X Drop (1,000 🪙) -> Whole Room"
        };

        new AlertDialog.Builder(ctx)
                .setTitle("🎁 Send Party Room Gift")
                .setItems(partyGifts, (dialog, which) -> {
                    int[] costs = {10, 200, 1000};
                    if (sessionManager.getUserCoins() >= costs[which]) {
                        sessionManager.setUserCoins(sessionManager.getUserCoins() - costs[which]);
                    }
                    if (tvVoiceRoomLog != null) {
                        tvVoiceRoomLog.setText("🎁 " + sessionManager.getUserName() + " sent " + partyGifts[which] + "!\n" + tvVoiceRoomLog.getText());
                    }
                    Toast.makeText(ctx, "🚀 Gift broadcasted to all 8 mic seats!", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void startNeonPulse(View target, long duration, float maxScale) {
        if (target == null) return;
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(
                target,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.93f, maxScale),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.93f, maxScale),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.40f)
        );
        anim.setDuration(duration);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.start();
        seatAnimators.add(anim);
    }

    private int dp(int val) {
        if (getContext() == null) return val;
        return Math.round(val * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        for (ObjectAnimator anim : seatAnimators) {
            if (anim != null) anim.cancel();
        }
        seatAnimators.clear();
        super.onDestroyView();
    }
}
