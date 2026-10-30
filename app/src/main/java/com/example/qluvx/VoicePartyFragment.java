package com.example.qluvx;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Deluxe 8-Seat Voice Party Room (Walla / Blued Audio Club) with all 7 Core Systems:
 * 1. 8-Seat Circular Stage (Center Host Throne Seat 0 + 7 Guest Circular Seats 1-7) & 4 Luxury Wallpapers
 *    (Cyber Galaxy, Royal Gold Palace, Velvet Rose, Midnight Neon).
 * 2. Host Seat Management Modal (Take Mic, Mute Mic, Lock Seat, Kick to Audience, Ban from Room).
 * 3. In-Room Audio Soundboard (Applause 👏, Cheering 🎉, DJ Air Horn 🎺, Laughter 😂) with ToneGenerator SFX.
 * 4. In-Room Mini-Game (Lucky Spin Wheel Arena with 20, 50, 100, 500 coin betting chips & 10x/50x payouts).
 * 5. Virtual Gift Action Drawer ("Send to All Mics", Rose, Supercar, Starship) & Top 3 Gifters Leaderboard.
 * 6. Sliding Golden Marquee Entrance Banner ("👑 [Lv.50] Julian Vance entered in Interstellar Starship X 🚀").
 * 7. Mini Profile Popup BottomSheet (Avatar Frame, Wealth Level Badge, Follow, Send Gift, 1-on-1 Chat).
 */
public class VoicePartyFragment extends Fragment {

    private boolean isMicMuted = false;
    private int currentThemeIndex = 0; // 0: Cyber Galaxy, 1: Royal Gold Palace, 2: Velvet Rose, 3: Midnight Neon
    private boolean sendToAllMics = true;
    private int selectedBetChip = 100; // Default betting chip (20, 50, 100, 500 coins)

    private static final String[] THEME_NAMES = {
            "🌌 Cyber Galaxy",
            "👑 Royal Gold Palace",
            "🌹 Velvet Rose",
            "⚡ Midnight Neon"
    };
    private static final String[] THEME_ACCENTS = {
            "#00E5FF",
            "#F1C40F",
            "#FF2A7A",
            "#00E676"
    };

    private SessionManager sessionManager;
    private final List<ObjectAnimator> seatAnimators = new ArrayList<>();
    private final Set<String> followedSpeakers = new HashSet<>();
    private final Random random = new Random();

    // Top 3 Gifters totals (updated live when gifts are sent)
    private int gifterAlexCoins = 48500;
    private int gifterRyanCoins = 31200;
    private int gifterZaynCoins = 19800;

    private ImageView ivRoomWallpaper;
    private TextView tvRoomTitle;
    private TextView btnWallpaperTheme;
    private TextView tvTopGiftersLeaderboard;
    private View cardGoldenEntranceBanner;
    private TextView tvGoldenEntranceMarquee;
    private TextView tvSfxLiveTicker;
    private TextView tvVoiceRoomLog;
    private TextView tvHostMicBadge;
    private View viewHostPulseOuter;
    private View viewHostPulseInner;
    private LinearLayout llEightSeatsGrid;

    /**
     * 7 Guest Circular Seats (Seats 1 to 7) + Seat 0 (Host Throne) = 8 Total Circular Seats.
     * Columns per seat row:
     * [0] seatNum (int)
     * [1] speakerName (String)
     * [2] portraitIdx (int, -1 if empty)
     * [3] isSpeaking (boolean)
     * [4] ringColorHex (String)
     * [5] isMuted (boolean)
     * [6] isLocked (boolean)
     * [7] wealthLevel (int)
     * [8] roleTag (String)
     */
    private final Object[][] guestSeatsState = new Object[][]{
            {1, "Alex", 0, true, "#FF2A7A", false, false, 46, "Top"},
            {2, "Ryan", 1, true, "#00E5FF", false, false, 42, "Versatile"},
            {3, "Kabir", 2, true, "#F1C40F", false, false, 38, "Top"},
            {4, "Empty Seat", -1, false, "#3D00E5FF", false, false, 1, "Versatile"},
            {5, "Liam", 4, false, "#9E9EBC", true, false, 35, "Bottom"},
            {6, "Zayn VIP", 3, true, "#F1C40F", false, false, 50, "Top"},
            {7, "Locked Seat", -1, false, "#FF5252", false, true, 1, "Versatile"}
    };

    private int entranceIndex = 0;
    private final String[] vipEntrances = new String[]{
            "👑 [Lv.50] Julian Vance entered in Interstellar Starship X 🚀",
            "🐉 [Lv.48] Alex Royal entered on Golden Imperial Dragon 🔥",
            "🏎️ [Lv.45] Zayn VIP drifted in Cyber Neon Bugatti Supercar ⚡"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_party, container, false);

        Context ctx = requireContext();
        sessionManager = new SessionManager(ctx);

        ivRoomWallpaper = view.findViewById(R.id.ivRoomWallpaper);
        tvRoomTitle = view.findViewById(R.id.tvRoomTitle);
        btnWallpaperTheme = view.findViewById(R.id.btnWallpaperTheme);
        tvTopGiftersLeaderboard = view.findViewById(R.id.tvTopGiftersLeaderboard);
        cardGoldenEntranceBanner = view.findViewById(R.id.cardGoldenEntranceBanner);
        tvGoldenEntranceMarquee = view.findViewById(R.id.tvGoldenEntranceMarquee);
        tvSfxLiveTicker = view.findViewById(R.id.tvSfxLiveTicker);
        tvVoiceRoomLog = view.findViewById(R.id.tvVoiceRoomLog);
        tvHostMicBadge = view.findViewById(R.id.tvHostMicBadge);
        viewHostPulseOuter = view.findViewById(R.id.viewHostPulseOuter);
        viewHostPulseInner = view.findViewById(R.id.viewHostPulseInner);

        ImageView ivHostSeatAvatar = view.findViewById(R.id.ivHostSeatAvatar);
        ImageView ivHostDragonFrame = view.findViewById(R.id.ivHostDragonFrame);
        TextView tvHostSeatName = view.findViewById(R.id.tvHostSeatName);
        View hostCenterStage = view.findViewById(R.id.hostCenterStage);
        View llTopGiftersStrip = view.findViewById(R.id.llTopGiftersStrip);
        llEightSeatsGrid = view.findViewById(R.id.llEightSeatsGrid);

        Button btnMicToggle = view.findViewById(R.id.ivMicToggle);
        Button btnSoundboard = view.findViewById(R.id.ivSoundboard);
        Button btnGift = view.findViewById(R.id.ivGift);
        Button btnLuckySpin = view.findViewById(R.id.btnLuckySpin);
        Button btnBottomWheel = view.findViewById(R.id.btnBottomWheel);

        // 1. Apply Initial Room Wallpaper Theme & 1-Tap Switcher (plus picker on Room Title tap)
        applyRoomWallpaper();

        if (btnWallpaperTheme != null) {
            btnWallpaperTheme.setOnClickListener(v -> {
                currentThemeIndex = (currentThemeIndex + 1) % THEME_NAMES.length;
                applyRoomWallpaper();
                Toast.makeText(ctx, "🎨 Switched Room Theme: " + THEME_NAMES[currentThemeIndex], Toast.LENGTH_SHORT).show();
            });
            btnWallpaperTheme.setOnLongClickListener(v -> {
                showThemeSelectorDialog();
                return true;
            });
        }
        if (tvRoomTitle != null) {
            tvRoomTitle.setOnClickListener(v -> showThemeSelectorDialog());
        }

        // 2. Top 3 Gifters Leaderboard Strip
        updateTopGiftersStrip();
        if (llTopGiftersStrip != null) {
            llTopGiftersStrip.setOnClickListener(v -> showTopGiftersLeaderboardModal());
        }

        // 3. Sliding Golden Marquee Entrance Banner
        if (tvGoldenEntranceMarquee != null) {
            tvGoldenEntranceMarquee.setSelected(true);
        }
        triggerGoldenEntranceSlideAnimation(vipEntrances[0]);
        if (cardGoldenEntranceBanner != null) {
            cardGoldenEntranceBanner.setOnClickListener(v -> {
                entranceIndex = (entranceIndex + 1) % vipEntrances.length;
                String msg = vipEntrances[entranceIndex];
                triggerGoldenEntranceSlideAnimation(msg);
                prependRoomLog(msg);
                playSfxTone(ToneGenerator.TONE_PROP_BEEP2, 180);
            });
        }

        // 4. Center Host Throne (Seat 0) with Golden Crown Badge, Imperial Dragon Frame & Pulsing Soundwaves
        if (ivHostSeatAvatar != null) {
            WallaPortraitStudio.loadGuyPhoto(ctx, ivHostSeatAvatar, 6);
        }
        if (ivHostDragonFrame != null) {
            ivHostDragonFrame.setImageDrawable(WallaPortraitStudio.getImperialDragonFrameDrawable(ctx));
        }
        if (tvHostSeatName != null) {
            tvHostSeatName.setText("👑 " + sessionManager.getUserName() + " • Lv.50 Host");
        }
        if (hostCenterStage != null) {
            hostCenterStage.setOnClickListener(v ->
                    showMiniProfileBottomSheet(
                            sessionManager.getUserName() + " (Host)",
                            6,
                            50,
                            sessionManager.getUserRole(),
                            "0.1km",
                            "Seat 0 • Host Throne"
                    )
            );
        }
        startNeonPulse(viewHostPulseOuter, 950L, 1.11f);
        startNeonPulse(viewHostPulseInner, 740L, 1.07f);

        // 5. Render 7 Guest Circular Seats (Seats 1–7) around/below Host Throne (8 Seats Total)
        populateGuestCircularSeats(llEightSeatsGrid);

        // 6. Bottom Bar: Host Mic Toggle
        btnMicToggle.setOnClickListener(v -> {
            isMicMuted = !isMicMuted;
            btnMicToggle.setText(isMicMuted ? "🔇 Muted" : "🎙️ Mic ON");
            btnMicToggle.setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(isMicMuted ? "#4A4A68" : "#00E676")
            ));
            btnMicToggle.setTextColor(isMicMuted ? Color.WHITE : Color.parseColor("#0A0A14"));
            if (tvHostMicBadge != null) {
                tvHostMicBadge.setText(isMicMuted ? "🔇 MUTED" : "🎙️ ON AIR");
            }
            if (tvSfxLiveTicker != null) {
                tvSfxLiveTicker.setText(isMicMuted
                        ? "🔇 Host mic is muted • Tap again to resume live spatial voice!"
                        : "🎙️ Host Throne (Seat 0) is LIVE with pulsing neon soundwaves!");
            }
        });

        // 7. Bottom Bar: In-Room Audio Soundboard (Applause 👏, Cheering 🎉, DJ Air Horn 🎺, Laughter 😂)
        btnSoundboard.setOnClickListener(v -> showSfxSoundboardBottomSheet());

        // 8. In-Room Mini-Game: Lucky Spin Wheel with Betting Chips (20, 50, 100, 500 coins)
        View.OnClickListener wheelListener = v -> showLuckySpinWheelDialog();
        if (btnLuckySpin != null) btnLuckySpin.setOnClickListener(wheelListener);
        if (btnBottomWheel != null) btnBottomWheel.setOnClickListener(wheelListener);

        // 9. Virtual Gift Action Drawer ("Send to All Mics" + Top 3 Gifters + Rose / Supercar / Starship)
        btnGift.setOnClickListener(v -> showVoicePartyGiftDrawer(null));

        return view;
    }

    private void applyRoomWallpaper() {
        Context ctx = getContext();
        if (ctx == null || ivRoomWallpaper == null) return;
        ivRoomWallpaper.setImageDrawable(WallaPortraitStudio.getVoiceRoomWallpaperByIndex(ctx, currentThemeIndex));
        String themeName = THEME_NAMES[currentThemeIndex];
        String accentHex = THEME_ACCENTS[currentThemeIndex];
        if (tvRoomTitle != null) {
            tvRoomTitle.setText(themeName + " Audio Club");
            tvRoomTitle.setTextColor(Color.parseColor(accentHex));
        }
        if (btnWallpaperTheme != null) {
            btnWallpaperTheme.setText(themeName);
            btnWallpaperTheme.setTextColor(Color.parseColor(accentHex));
        }
    }

    private void showThemeSelectorDialog() {
        Context ctx = getContext();
        if (ctx == null) return;
        new AlertDialog.Builder(ctx)
                .setTitle("🎨 Select Luxury Room Wallpaper Theme")
                .setItems(THEME_NAMES, (d, which) -> {
                    currentThemeIndex = which;
                    applyRoomWallpaper();
                    prependRoomLog("🎨 Room theme switched to " + THEME_NAMES[which]);
                })
                .show();
    }

    private void triggerGoldenEntranceSlideAnimation(String bannerText) {
        if (cardGoldenEntranceBanner == null || tvGoldenEntranceMarquee == null) return;
        tvGoldenEntranceMarquee.setText(bannerText + "   •   ✨ Tap banner to replay luxury entrance!");
        cardGoldenEntranceBanner.setTranslationX(520f);
        cardGoldenEntranceBanner.setAlpha(0.2f);
        cardGoldenEntranceBanner.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(620L)
                .setInterpolator(new OvershootInterpolator(0.85f))
                .start();
    }

    private void updateTopGiftersStrip() {
        if (tvTopGiftersLeaderboard == null) return;
        tvTopGiftersLeaderboard.setText(String.format(
                Locale.US,
                "🥇 Alex %.1fk 🪙  •  🥈 Ryan %.1fk 🪙  •  🥉 Zayn %.1fk 🪙",
                gifterAlexCoins / 1000f,
                gifterRyanCoins / 1000f,
                gifterZaynCoins / 1000f
        ));
    }

    /**
     * Populates the 7 Guest Circular Seats (Seats 1–7) below Center Host Throne (Seat 0):
     * Row 0 has Seats 1, 2, 3, 4; Row 1 has centered Seats 5, 6, 7.
     */
    private void populateGuestCircularSeats(LinearLayout gridContainer) {
        Context ctx = getContext();
        if (ctx == null || gridContainer == null) return;

        for (ObjectAnimator anim : seatAnimators) {
            if (anim != null) anim.cancel();
        }
        seatAnimators.clear();
        startNeonPulse(viewHostPulseOuter, 950L, 1.11f);
        startNeonPulse(viewHostPulseInner, 740L, 1.07f);

        gridContainer.removeAllViews();

        int[][] rowIndices = new int[][]{
                {0, 1, 2, 3}, // Row 1: Seats 1, 2, 3, 4
                {4, 5, 6}     // Row 2: Seats 5, 6, 7 (Circular Arc below Row 1)
        };

        for (int r = 0; r < rowIndices.length; r++) {
            int[] indices = rowIndices[r];
            LinearLayout rowLayout = new LinearLayout(ctx);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            rowLayout.setWeightSum(4f);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (r == 1) {
                rowLp.topMargin = dp(6);
            }
            rowLayout.setLayoutParams(rowLp);

            for (int idx : indices) {
                Object[] seat = guestSeatsState[idx];
                final int seatNum = (Integer) seat[0];
                final String seatName = (String) seat[1];
                final int portraitIdx = (Integer) seat[2];
                final boolean isSpeaking = (Boolean) seat[3];
                final String ringColor = (String) seat[4];
                final boolean isMuted = (Boolean) seat[5];
                final boolean isLocked = (Boolean) seat[6];
                final int wealthLevel = (Integer) seat[7];
                final String roleTag = (String) seat[8];

                LinearLayout seatBox = new LinearLayout(ctx);
                seatBox.setOrientation(LinearLayout.VERTICAL);
                seatBox.setGravity(Gravity.CENTER_HORIZONTAL);
                seatBox.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                FrameLayout avatarFrame = new FrameLayout(ctx);
                avatarFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));

                // Outer Pulsing Speaking Soundwave Ring
                final View outerWaveRing = new View(ctx);
                FrameLayout.LayoutParams outerLp = new FrameLayout.LayoutParams(dp(62), dp(62), Gravity.CENTER);
                outerWaveRing.setLayoutParams(outerLp);
                GradientDrawable outerShape = new GradientDrawable();
                outerShape.setShape(GradientDrawable.OVAL);
                boolean activeWave = isSpeaking && !isMuted && !isLocked && portraitIdx >= 0;
                outerShape.setColor(activeWave ? Color.parseColor("#2A00E5FF") : Color.TRANSPARENT);
                outerShape.setStroke(dp(2), Color.parseColor(isLocked ? "#FF5252" : ringColor));
                outerWaveRing.setBackground(outerShape);
                avatarFrame.addView(outerWaveRing);

                // Inner Luxury Avatar Frame Ring
                final View neonRing = new View(ctx);
                FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(dp(55), dp(55), Gravity.CENTER);
                neonRing.setLayoutParams(ringLp);
                GradientDrawable ringShape = new GradientDrawable();
                ringShape.setShape(GradientDrawable.OVAL);
                ringShape.setColor(activeWave ? Color.parseColor("#22FF2A7A") : Color.parseColor("#141424"));
                ringShape.setStroke(dp(2), Color.parseColor(isLocked ? "#FF5252" : ringColor));
                neonRing.setBackground(ringShape);
                avatarFrame.addView(neonRing);

                if (activeWave) {
                    startNeonPulse(outerWaveRing, 700L + idx * 65L, 1.16f);
                    startNeonPulse(neonRing, 620L + idx * 55L, 1.09f);
                }

                // Circular Seat Avatar, Lock Icon 🔒, or Empty "+" Mic Icon
                final CardView circleCard = new CardView(ctx);
                FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER);
                circleCard.setLayoutParams(cardLp);
                circleCard.setRadius(dp(23));
                circleCard.setCardElevation(0f);
                circleCard.setCardBackgroundColor(Color.parseColor("#181830"));

                if (isLocked) {
                    TextView tvLock = new TextView(ctx);
                    tvLock.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    tvLock.setGravity(Gravity.CENTER);
                    tvLock.setText("🔒");
                    tvLock.setTextSize(17f);
                    circleCard.addView(tvLock);
                } else if (portraitIdx >= 0) {
                    ImageView ivSeat = new ImageView(ctx);
                    ivSeat.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    ivSeat.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    WallaPortraitStudio.loadGuyPhoto(ctx, ivSeat, portraitIdx);
                    circleCard.addView(ivSeat);
                } else {
                    TextView tvPlus = new TextView(ctx);
                    tvPlus.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    tvPlus.setGravity(Gravity.CENTER);
                    tvPlus.setText("🎙️");
                    tvPlus.setTextColor(Color.parseColor("#00E5FF"));
                    tvPlus.setTextSize(16f);
                    circleCard.addView(tvPlus);
                }
                avatarFrame.addView(circleCard);

                // Top-Right VIP Frame Crown or Muted Mic Badge
                if (portraitIdx >= 0 && !isLocked) {
                    TextView tvCornerBadge = new TextView(ctx);
                    FrameLayout.LayoutParams cornerLp = new FrameLayout.LayoutParams(
                            dp(18), dp(18), Gravity.TOP | Gravity.END
                    );
                    tvCornerBadge.setLayoutParams(cornerLp);
                    tvCornerBadge.setGravity(Gravity.CENTER);
                    tvCornerBadge.setTextSize(8.5f);
                    tvCornerBadge.setText(isMuted ? "🔇" : "👑");
                    tvCornerBadge.setBackgroundResource(isMuted ? R.drawable.bg_walla_pink_pill : R.drawable.bg_walla_gold_pill);
                    avatarFrame.addView(tvCornerBadge);
                }

                // Bottom Status Pill on Seat (Speaking / Muted / Locked / Take Mic)
                final TextView tvStatus = new TextView(ctx);
                FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                );
                tvStatus.setLayoutParams(statusLp);
                String statusText;
                if (isLocked) {
                    statusText = "🔒 Locked";
                } else if (portraitIdx < 0) {
                    statusText = "➕ Take Mic";
                } else if (isMuted) {
                    statusText = "🔇 Muted";
                } else {
                    statusText = "🎙️ Lv." + wealthLevel;
                }
                tvStatus.setText(statusText);
                tvStatus.setTextSize(7.5f);
                tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setPadding(dp(5), dp(1), dp(5), dp(1));
                tvStatus.setBackgroundResource(activeWave ? R.drawable.bg_walla_pink_pill : R.drawable.bg_walla_pill_dark);
                avatarFrame.addView(tvStatus);

                seatBox.addView(avatarFrame);

                final TextView tvLabel = new TextView(ctx);
                tvLabel.setText("#" + seatNum + " " + seatName);
                tvLabel.setTextColor(portraitIdx >= 0 && !isLocked ? Color.WHITE : Color.parseColor("#9E9EBC"));
                tvLabel.setTextSize(10f);
                tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
                tvLabel.setSingleLine(true);
                tvLabel.setPadding(0, dp(2), 0, 0);
                seatBox.addView(tvLabel);

                // CLICK LISTENER:
                // - Empty unlocked seat -> immediately takes the mic with pulsing speaking soundwaves!
                // - Occupied or Locked seat -> opens Host Seat Management Modal (Mute, Lock, Kick, Ban, or Mini Profile)
                seatBox.setOnClickListener(v -> {
                    int curPortrait = (Integer) guestSeatsState[idx][2];
                    boolean curLocked = (Boolean) guestSeatsState[idx][6];
                    if (curPortrait < 0 && !curLocked) {
                        occupyEmptySeatWithUser(idx, seatNum);
                    } else {
                        showHostSeatManagementBottomSheet(idx);
                    }
                });

                // LONG CLICK LISTENER: Directly opens Mini Profile Popup if seat is occupied
                seatBox.setOnLongClickListener(v -> {
                    if (portraitIdx >= 0 && !isLocked) {
                        showMiniProfileBottomSheet(
                                seatName,
                                portraitIdx,
                                wealthLevel,
                                roleTag,
                                (seatNum * 180) + "m",
                                "Seat #" + seatNum + " Speaker"
                        );
                        return true;
                    }
                    return false;
                });

                rowLayout.addView(seatBox);
            }
            gridContainer.addView(rowLayout);
        }
    }

    private void occupyEmptySeatWithUser(int idx, int seatNum) {
        Context ctx = getContext();
        if (ctx == null) return;
        String firstName = sessionManager.getUserName().split(" ")[0];
        guestSeatsState[idx][1] = "You (" + firstName + ")";
        guestSeatsState[idx][2] = 6;
        guestSeatsState[idx][3] = true;
        guestSeatsState[idx][4] = "#00E676";
        guestSeatsState[idx][5] = false;
        guestSeatsState[idx][6] = false;
        guestSeatsState[idx][7] = 50;
        guestSeatsState[idx][8] = sessionManager.getUserRole();

        populateGuestCircularSeats(llEightSeatsGrid);

        if (tvSfxLiveTicker != null) {
            tvSfxLiveTicker.setText("🎙️ " + sessionManager.getUserName() + " took Seat #" + seatNum + " on live mic!");
        }
        prependRoomLog("🎙️ " + sessionManager.getUserName() + " jumped on Mic at Seat #" + seatNum + "!");
        Toast.makeText(ctx, "🎙️ You are now LIVE on Mic at Seat #" + seatNum + "!", Toast.LENGTH_SHORT).show();
    }

    /**
     * SYSTEM 2: HOST SEAT MANAGEMENT MODAL (BottomSheetDialog)
     * Allows Host to Mute Mic, Lock Seat, Kick from Mic (move to audience), Ban from Room, or View Mini Profile.
     */
    private void showHostSeatManagementBottomSheet(final int idx) {
        Context ctx = getContext();
        if (ctx == null) return;

        final int seatNum = (Integer) guestSeatsState[idx][0];
        final String seatName = (String) guestSeatsState[idx][1];
        final int portraitIdx = (Integer) guestSeatsState[idx][2];
        final boolean isMuted = (Boolean) guestSeatsState[idx][5];
        final boolean isLocked = (Boolean) guestSeatsState[idx][6];
        final int wealthLevel = (Integer) guestSeatsState[idx][7];
        final String roleTag = (String) guestSeatsState[idx][8];

        final BottomSheetDialog sheet = new BottomSheetDialog(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        // Header Row with Avatar & Seat Badge
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("👑 Host Seat #" + seatNum + " Management • " + seatName);
        tvTitle.setTextColor(Color.parseColor("#F1C40F"));
        tvTitle.setTextSize(16.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        TextView tvSub = new TextView(ctx);
        tvSub.setText(isLocked
                ? "Status: 🔒 Seat Locked by Host • Tap Unlock below to allow speakers"
                : "Status: " + (isMuted ? "🔇 Mic Muted by Host" : "🎙️ Live on Spatial Mic") + " • Wealth Lv." + wealthLevel);
        tvSub.setTextColor(Color.parseColor("#00E5FF"));
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, dp(4), 0, dp(12));
        root.addView(tvSub);

        // Button 1: View Speaker Mini Profile (if occupied) or Take Mic (if unlocked empty)
        if (portraitIdx >= 0 && !isLocked) {
            Button btnProfile = createSheetButton(ctx, "👤 View " + seatName + "'s Mini Profile (Lv." + wealthLevel + ")", "#1E1E36", "#FFFFFF");
            btnProfile.setOnClickListener(v -> {
                sheet.dismiss();
                showMiniProfileBottomSheet(seatName, portraitIdx, wealthLevel, roleTag, (seatNum * 180) + "m", "Seat #" + seatNum + " Speaker");
            });
            root.addView(btnProfile);
        } else if (!isLocked) {
            Button btnTakeMic = createSheetButton(ctx, "🎙️ Take Mic on Seat #" + seatNum, "#00E676", "#0A0A14");
            btnTakeMic.setOnClickListener(v -> {
                sheet.dismiss();
                occupyEmptySeatWithUser(idx, seatNum);
            });
            root.addView(btnTakeMic);
        }

        // Button 2: Mute / Unmute Mic
        Button btnMute = createSheetButton(
                ctx,
                isMuted ? "🎙️ Unmute Seat #" + seatNum + " Mic" : "🔇 Mute Seat #" + seatNum + " Mic",
                "#252542",
                "#00E5FF"
        );
        btnMute.setOnClickListener(v -> {
            boolean newMute = !isMuted;
            guestSeatsState[idx][5] = newMute;
            guestSeatsState[idx][3] = !newMute;
            populateGuestCircularSeats(llEightSeatsGrid);
            String msg = newMute
                    ? "🔇 Host muted " + seatName + " on Seat #" + seatNum
                    : "🎙️ Host unmuted " + seatName + " on Seat #" + seatNum;
            prependRoomLog(msg);
            if (tvSfxLiveTicker != null) tvSfxLiveTicker.setText(msg);
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });
        root.addView(btnMute);

        // Button 3: Lock / Unlock Seat
        Button btnLock = createSheetButton(
                ctx,
                isLocked ? "🔓 Unlock Seat #" + seatNum + " (Open for Guests)" : "🔒 Lock Seat #" + seatNum + " (Host Only)",
                "#252542",
                "#F1C40F"
        );
        btnLock.setOnClickListener(v -> {
            boolean newLock = !isLocked;
            guestSeatsState[idx][6] = newLock;
            if (newLock) {
                guestSeatsState[idx][1] = "Locked Seat";
                guestSeatsState[idx][2] = -1;
                guestSeatsState[idx][3] = false;
                guestSeatsState[idx][4] = "#FF5252";
            } else {
                guestSeatsState[idx][1] = "Empty Seat";
                guestSeatsState[idx][2] = -1;
                guestSeatsState[idx][3] = false;
                guestSeatsState[idx][4] = "#3D00E5FF";
            }
            populateGuestCircularSeats(llEightSeatsGrid);
            String msg = newLock
                    ? "🔒 Host locked Seat #" + seatNum
                    : "🔓 Host unlocked Seat #" + seatNum + " — tap to take mic!";
            prependRoomLog(msg);
            if (tvSfxLiveTicker != null) tvSfxLiveTicker.setText(msg);
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });
        root.addView(btnLock);

        // Button 4: Kick from Mic (Move to Audience)
        if (portraitIdx >= 0 && !isLocked) {
            Button btnKick = createSheetButton(ctx, "⬇️ Kick " + seatName + " from Mic (Move to Audience)", "#2A1B2E", "#FF80AB");
            btnKick.setOnClickListener(v -> {
                guestSeatsState[idx][1] = "Empty Seat";
                guestSeatsState[idx][2] = -1;
                guestSeatsState[idx][3] = false;
                guestSeatsState[idx][4] = "#3D00E5FF";
                guestSeatsState[idx][5] = false;
                guestSeatsState[idx][6] = false;
                populateGuestCircularSeats(llEightSeatsGrid);
                String msg = "⬇️ Host moved " + seatName + " from Seat #" + seatNum + " to Audience";
                prependRoomLog(msg);
                if (tvSfxLiveTicker != null) tvSfxLiveTicker.setText(msg);
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            });
            root.addView(btnKick);

            // Button 5: Ban from Room
            Button btnBan = createSheetButton(ctx, "🚫 Ban " + seatName + " from Voice Room", "#3B1219", "#FF5252");
            btnBan.setOnClickListener(v -> {
                guestSeatsState[idx][1] = "Empty Seat";
                guestSeatsState[idx][2] = -1;
                guestSeatsState[idx][3] = false;
                guestSeatsState[idx][4] = "#3D00E5FF";
                guestSeatsState[idx][5] = false;
                guestSeatsState[idx][6] = false;
                populateGuestCircularSeats(llEightSeatsGrid);
                String msg = "🚫 Host banned " + seatName + " from the Voice Party Room";
                prependRoomLog(msg);
                if (tvSfxLiveTicker != null) tvSfxLiveTicker.setText(msg);
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            });
            root.addView(btnBan);
        }

        sheet.setContentView(root);
        sheet.show();
    }

    /**
     * SYSTEM 3: IN-ROOM AUDIO SOUNDBOARD (Applause 👏, Cheering 🎉, DJ Air Horn 🎺, Laughter 😂)
     */
    private void showSfxSoundboardBottomSheet() {
        Context ctx = getContext();
        if (ctx == null) return;

        final BottomSheetDialog sheet = new BottomSheetDialog(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("🔊 Live Voice Party SFX Soundboard");
        tvTitle.setTextColor(Color.parseColor("#00E5FF"));
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        TextView tvSub = new TextView(ctx);
        tvSub.setText("Tap any pad below to broadcast spatial sound effects to all 8 seats:");
        tvSub.setTextColor(Color.parseColor("#B0B0D0"));
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, dp(4), 0, dp(12));
        root.addView(tvSub);

        final String[][] sfxItems = new String[][]{
                {"👏 Applause", "👏 Stadium Applause & Standing Ovation!", "#00E676"},
                {"🎉 Cheering", "🎉 VIP Crowd Cheering & Confetti Roar!", "#F1C40F"},
                {"🎺 DJ Air Horn", "🎺 Club DJ Triple Air Horn Blast (x3)!", "#FF2A7A"},
                {"😂 Laughter", "😂 Studio Audience Burst of Laughter!", "#00E5FF"}
        };
        final int[] tones = {
                ToneGenerator.TONE_PROP_ACK,
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,
                ToneGenerator.TONE_PROP_BEEP2
        };

        for (int i = 0; i < sfxItems.length; i++) {
            final String label = sfxItems[i][0];
            final String broadcastDesc = sfxItems[i][1];
            final String colorHex = sfxItems[i][2];
            final int toneType = tones[i];

            Button btnSfx = createSheetButton(ctx, label + "   •   Broadcast to Room", "#1F1F38", colorHex);
            btnSfx.setOnClickListener(v -> {
                playSfxTone(toneType, 240);
                if (tvSfxLiveTicker != null) {
                    tvSfxLiveTicker.setText("🔊 SFX LIVE: " + broadcastDesc);
                }
                prependRoomLog("🔊 " + sessionManager.getUserName() + " played SFX: " + broadcastDesc);
                Toast.makeText(ctx, "🔊 Playing: " + label, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            });
            root.addView(btnSfx);
        }

        sheet.setContentView(root);
        sheet.show();
    }

    /**
     * SYSTEM 4: IN-ROOM MINI-GAME (LUCKY SPIN WHEEL WITH BETTING CHIPS 20, 50, 100, 500 & 10x/50x PAYOUTS)
     */
    private void showLuckySpinWheelDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("🎰 Royal Lucky Spin Wheel Arena");
        tvTitle.setTextColor(Color.parseColor("#F1C40F"));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        final TextView tvSub = new TextView(ctx);
        tvSub.setText("Wallet Balance: " + sessionManager.getFormattedCoins() + " 🪙  •  Select Betting Chip:");
        tvSub.setTextColor(Color.parseColor("#00E5FF"));
        tvSub.setTextSize(12.5f);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvSub.setPadding(0, dp(4), 0, dp(8));
        root.addView(tvSub);

        // Betting Chips Row: 20, 50, 100, 500 Coins
        LinearLayout chipsRow = new LinearLayout(ctx);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setGravity(Gravity.CENTER);
        chipsRow.setPadding(0, 0, 0, dp(10));
        final int[] chipValues = {20, 50, 100, 500};
        final Button[] chipButtons = new Button[chipValues.length];

        for (int i = 0; i < chipValues.length; i++) {
            final int chipVal = chipValues[i];
            Button chipBtn = new Button(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1f);
            if (i < chipValues.length - 1) lp.rightMargin = dp(6);
            chipBtn.setLayoutParams(lp);
            chipBtn.setText(chipVal + " 🪙");
            chipBtn.setAllCaps(false);
            chipBtn.setTextSize(11.5f);
            chipBtn.setTypeface(Typeface.DEFAULT_BOLD);
            chipBtn.setPadding(0, 0, 0, 0);
            chipButtons[i] = chipBtn;
            chipBtn.setOnClickListener(v -> {
                selectedBetChip = chipVal;
                styleBetChipButtons(chipButtons, chipValues, selectedBetChip);
            });
            chipsRow.addView(chipBtn);
        }
        styleBetChipButtons(chipButtons, chipValues, selectedBetChip);
        root.addView(chipsRow);

        // Top Gold Pointer Indicator
        TextView tvPointer = new TextView(ctx);
        tvPointer.setText("▼");
        tvPointer.setTextColor(Color.parseColor("#F1C40F"));
        tvPointer.setTextSize(18f);
        tvPointer.setGravity(Gravity.CENTER);
        root.addView(tvPointer);

        // Custom Drawn Colorful Lucky Wheel View with Multiplier Labels (including 10x and 50x Jackpots)
        final View wheelView = new View(ctx) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF oval = new RectF();
            private final int[] colors = {
                    0xFFFF2A7A, 0xFF00E5FF, 0xFFF1C40F, 0xFF7C4DFF,
                    0xFF00E676, 0xFFFF5252, 0xFF2979FF, 0xFFFFAB00
            };
            private final String[] labels = {
                    "10x", "2x", "50x", "3x",
                    "10x", "5x", "50x", "2x"
            };

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth();
                float h = getHeight();
                float r = Math.min(w, h) * 0.46f;
                float cx = w * 0.5f;
                float cy = h * 0.5f;
                oval.set(cx - r, cy - r, cx + r, cy + r);

                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(dp(12));
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                textPaint.setTextAlign(Paint.Align.CENTER);

                for (int i = 0; i < 8; i++) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(colors[i]);
                    float startAngle = i * 45f;
                    canvas.drawArc(oval, startAngle, 45f, true, paint);

                    double rad = Math.toRadians(startAngle + 22.5);
                    float tx = cx + (float) (Math.cos(rad) * r * 0.65f);
                    float ty = cy + (float) (Math.sin(rad) * r * 0.65f) + dp(4);
                    canvas.drawText(labels[i], tx, ty, textPaint);
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
        wheelView.setLayoutParams(new LinearLayout.LayoutParams(dp(186), dp(186)));
        root.addView(wheelView);

        final TextView tvResult = new TextView(ctx);
        tvResult.setText("Pick a chip (20 / 50 / 100 / 500) & SPIN for 10x or 50x Jackpot!");
        tvResult.setTextColor(Color.WHITE);
        tvResult.setTextSize(12.5f);
        tvResult.setTypeface(Typeface.DEFAULT_BOLD);
        tvResult.setGravity(Gravity.CENTER);
        tvResult.setPadding(0, dp(10), 0, dp(12));
        root.addView(tvResult);

        final Button btnSpinNow = new Button(ctx);
        btnSpinNow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        btnSpinNow.setText("🎰 SPIN WITH " + selectedBetChip + " 🪙 CHIP");
        btnSpinNow.setAllCaps(false);
        btnSpinNow.setTextSize(14.5f);
        btnSpinNow.setTypeface(Typeface.DEFAULT_BOLD);
        btnSpinNow.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        btnSpinNow.setTextColor(Color.WHITE);
        root.addView(btnSpinNow);

        for (int i = 0; i < chipButtons.length; i++) {
            final int chipVal = chipValues[i];
            chipButtons[i].setOnClickListener(v -> {
                selectedBetChip = chipVal;
                styleBetChipButtons(chipButtons, chipValues, selectedBetChip);
                btnSpinNow.setText("🎰 SPIN WITH " + selectedBetChip + " 🪙 CHIP");
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(root)
                .setNegativeButton("Close", null)
                .create();

        btnSpinNow.setOnClickListener(v -> {
            int currentCoins = sessionManager.getUserCoins();
            if (currentCoins < selectedBetChip) {
                sessionManager.setUserCoins(currentCoins + 1000);
                Toast.makeText(ctx, "🎁 Added +1,000 Free Bonus Coins so you can keep spinning!", Toast.LENGTH_SHORT).show();
            }
            sessionManager.setUserCoins(sessionManager.getUserCoins() - selectedBetChip);
            tvSub.setText("Wallet Balance: " + sessionManager.getFormattedCoins() + " 🪙  •  Bet: " + selectedBetChip + " 🪙");

            btnSpinNow.setEnabled(false);
            btnSpinNow.setText("🎰 Spinning Wheel...");
            playSfxTone(ToneGenerator.TONE_PROP_BEEP, 150);

            float currentRot = wheelView.getRotation();
            float targetAngle = currentRot + 1440f + random.nextInt(720);
            ObjectAnimator spinAnim = ObjectAnimator.ofFloat(wheelView, View.ROTATION, currentRot, targetAngle);
            spinAnim.setDuration(2100L);
            spinAnim.setInterpolator(new DecelerateInterpolator());
            spinAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    int[] multipliers = {2, 3, 5, 10, 10, 50};
                    int mult = multipliers[random.nextInt(multipliers.length)];
                    int payout = selectedBetChip * mult;
                    sessionManager.setUserCoins(sessionManager.getUserCoins() + payout);
                    playSfxTone(ToneGenerator.TONE_PROP_BEEP2, 240);

                    tvSub.setText("Wallet Balance: " + sessionManager.getFormattedCoins() + " 🪙  •  Select Betting Chip:");
                    tvResult.setText("🎉 " + mult + "x MEGA WIN! Bet " + selectedBetChip + " 🪙 ➔ Won +" + payout + " Coins!");
                    tvResult.setTextColor(Color.parseColor("#00E676"));
                    btnSpinNow.setEnabled(true);
                    btnSpinNow.setText("🎰 SPIN AGAIN (" + selectedBetChip + " 🪙)");

                    String logMsg = "🎰 " + sessionManager.getUserName() + " hit " + mult + "x on Lucky Wheel & won +" + payout + " Coins!";
                    prependRoomLog(logMsg);
                    if (tvSfxLiveTicker != null) tvSfxLiveTicker.setText(logMsg);
                    Toast.makeText(ctx, "🎉 " + mult + "x JACKPOT! +" + payout + " Coins credited!", Toast.LENGTH_SHORT).show();
                }
            });
            spinAnim.start();
        });

        dialog.show();
    }

    private void styleBetChipButtons(Button[] buttons, int[] values, int selectedValue) {
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = values[i] == selectedValue;
            buttons[i].setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(selected ? "#F1C40F" : "#252542")
            ));
            buttons[i].setTextColor(selected ? Color.parseColor("#0A0A14") : Color.WHITE);
        }
    }

    /**
     * SYSTEM 5: VIRTUAL GIFT TRAY DRAWER ("Send to All Mics" toggle, Rose, Supercar, Starship & Top 3 Gifters)
     */
    private void showVoicePartyGiftDrawer(@Nullable String specificRecipient) {
        Context ctx = getContext();
        if (ctx == null) return;

        final BottomSheetDialog sheet = new BottomSheetDialog(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("🎁 Luxury Voice Club Gift Drawer");
        tvTitle.setTextColor(Color.parseColor("#F1C40F"));
        tvTitle.setTextSize(17.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        TextView tvLeaderboard = new TextView(ctx);
        tvLeaderboard.setText("🏆 Top 3 Gifters: 🥇 Alex (" + (gifterAlexCoins / 1000f) + "k) • 🥈 Ryan (" + (gifterRyanCoins / 1000f) + "k) • 🥉 Zayn (" + (gifterZaynCoins / 1000f) + "k)");
        tvLeaderboard.setTextColor(Color.parseColor("#00E5FF"));
        tvLeaderboard.setTextSize(11.5f);
        tvLeaderboard.setTypeface(Typeface.DEFAULT_BOLD);
        tvLeaderboard.setPadding(0, dp(4), 0, dp(10));
        root.addView(tvLeaderboard);

        if (specificRecipient != null) {
            sendToAllMics = false;
        }

        // Toggle Button for "Send to All Mics (8 Seats)" vs "Single Speaker"
        final Button btnAllMicsToggle = createSheetButton(
                ctx,
                sendToAllMics
                        ? "✅ Send to All Mics: ON (Broadcasts to All 8 Seats)"
                        : "🎯 Target: " + (specificRecipient != null ? specificRecipient : "Host Seat 0 Only") + " (Tap to switch to All Mics)",
                sendToAllMics ? "#1B3B2B" : "#252542",
                sendToAllMics ? "#00E676" : "#F1C40F"
        );
        btnAllMicsToggle.setOnClickListener(v -> {
            sendToAllMics = !sendToAllMics;
            btnAllMicsToggle.setText(sendToAllMics
                    ? "✅ Send to All Mics: ON (Broadcasts to All 8 Seats)"
                    : "🎯 Target: Single Mic Seat Only (Tap for All Mics)");
            btnAllMicsToggle.setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(sendToAllMics ? "#1B3B2B" : "#252542")
            ));
            btnAllMicsToggle.setTextColor(Color.parseColor(sendToAllMics ? "#00E676" : "#F1C40F"));
        });
        root.addView(btnAllMicsToggle);

        final String[][] gifts = new String[][]{
                {"🌹", "Velvet Neon Rose", "10"},
                {"🏎️", "Cyber Bugatti Supercar", "200"},
                {"🚀", "Interstellar Starship X", "1000"},
                {"👑", "Imperial Golden Crown", "500"}
        };

        for (String[] g : gifts) {
            final String emoji = g[0];
            final String giftName = g[1];
            final int cost = Integer.parseInt(g[2]);

            Button btnGiftItem = createSheetButton(
                    ctx,
                    emoji + "  " + giftName + "   •   " + cost + " 🪙",
                    "#1F1F38",
                    "#FFFFFF"
            );
            btnGiftItem.setOnClickListener(v -> {
                String targetLabel = sendToAllMics
                        ? "All 8 Mic Seats"
                        : (specificRecipient != null ? specificRecipient : "Host " + sessionManager.getUserName());
                int totalCost = cost;
                if (sessionManager.getUserCoins() >= totalCost) {
                    sessionManager.setUserCoins(sessionManager.getUserCoins() - totalCost);
                }
                gifterAlexCoins += totalCost;
                updateTopGiftersStrip();

                String logMsg = "🎁 " + sessionManager.getUserName() + " sent " + emoji + " " + giftName + " to " + targetLabel + "!";
                prependRoomLog(logMsg);
                if (tvSfxLiveTicker != null) {
                    tvSfxLiveTicker.setText(logMsg);
                }
                if (getView() instanceof FrameLayout) {
                    WallaBroadcasterStudio.triggerFullScreenGiftBlast(
                            ctx,
                            (FrameLayout) getView(),
                            true,
                            emoji + " " + giftName + " (" + targetLabel + ")",
                            sessionManager.getUserName()
                    );
                }
                sheet.dismiss();
            });
            root.addView(btnGiftItem);
        }

        sheet.setContentView(root);
        sheet.show();
    }

    private void showTopGiftersLeaderboardModal() {
        Context ctx = getContext();
        if (ctx == null) return;
        String[] ranks = new String[]{
                "🥇 #1 Alex (Seat 1) — " + String.format(Locale.US, "%,d", gifterAlexCoins) + " 🪙 • 👑 Royal Duke",
                "🥈 #2 Ryan (Seat 2) — " + String.format(Locale.US, "%,d", gifterRyanCoins) + " 🪙 • 💎 VIP Lord",
                "🥉 #3 Zayn VIP (Seat 6) — " + String.format(Locale.US, "%,d", gifterZaynCoins) + " 🪙 • 🚀 Starship Patron"
        };
        new AlertDialog.Builder(ctx)
                .setTitle("🏆 Voice Room Top 3 Gifters Leaderboard")
                .setItems(ranks, (d, which) -> {
                    int[] pIdx = {0, 1, 3};
                    String[] names = {"Alex", "Ryan", "Zayn VIP"};
                    showMiniProfileBottomSheet(names[which], pIdx[which], 50 - which * 4, "Top", "320m", "Top #" + (which + 1) + " Room Gifter");
                })
                .setPositiveButton("🎁 Send Gift to Climb Rank", (d, w) -> showVoicePartyGiftDrawer(null))
                .show();
    }

    /**
     * SYSTEM 6: MINI PROFILE POPUP (BottomSheetDialog)
     * Shows Avatar Frame, Wealth Level badge, Follow button, Send Gift button, and 1-on-1 Chat button.
     */
    private void showMiniProfileBottomSheet(
            final String speakerName,
            final int portraitIdx,
            final int wealthLevel,
            final String roleTag,
            final String distance,
            final String seatSubtitle
    ) {
        Context ctx = getContext();
        if (ctx == null) return;

        final BottomSheetDialog sheet = new BottomSheetDialog(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        // Avatar + Imperial Dragon Frame
        FrameLayout avatarWrap = new FrameLayout(ctx);
        avatarWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(96), dp(96)));

        CardView cardAvatar = new CardView(ctx);
        cardAvatar.setLayoutParams(new FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER));
        cardAvatar.setRadius(dp(37));
        cardAvatar.setCardBackgroundColor(Color.parseColor("#1E1E36"));
        ImageView ivAvatar = new ImageView(ctx);
        ivAvatar.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadGuyPhoto(ctx, ivAvatar, Math.max(0, portraitIdx));
        cardAvatar.addView(ivAvatar);
        avatarWrap.addView(cardAvatar);

        ImageView ivFrame = new ImageView(ctx);
        ivFrame.setLayoutParams(new FrameLayout.LayoutParams(dp(94), dp(94), Gravity.CENTER));
        ivFrame.setImageDrawable(WallaPortraitStudio.getImperialDragonFrameDrawable(ctx));
        avatarWrap.addView(ivFrame);

        root.addView(avatarWrap);

        // Name & Verified Badge
        TextView tvName = new TextView(ctx);
        tvName.setText(speakerName + " ✓");
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(18f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setPadding(0, dp(8), 0, dp(2));
        root.addView(tvName);

        // Wealth Level Badge + Role + Seat Info
        TextView tvBadges = new TextView(ctx);
        tvBadges.setText("👑 Wealth Lv." + wealthLevel + "  •  " + roleTag + "  •  📍 " + distance + "  •  " + seatSubtitle);
        tvBadges.setTextColor(Color.parseColor("#F1C40F"));
        tvBadges.setTextSize(12f);
        tvBadges.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadges.setPadding(0, 0, 0, dp(14));
        root.addView(tvBadges);

        // Action Buttons Row: Follow, Send Gift, 1-on-1 Chat
        final boolean isFollowing = followedSpeakers.contains(speakerName);
        final Button btnFollow = createSheetButton(
                ctx,
                isFollowing ? "✓ Following " + speakerName : "➕ Follow " + speakerName,
                isFollowing ? "#1B3B2B" : "#FF2A7A",
                "#FFFFFF"
        );
        btnFollow.setOnClickListener(v -> {
            if (followedSpeakers.contains(speakerName)) {
                followedSpeakers.remove(speakerName);
                btnFollow.setText("➕ Follow " + speakerName);
                btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF2A7A")));
            } else {
                followedSpeakers.add(speakerName);
                btnFollow.setText("✓ Following " + speakerName);
                btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1B3B2B")));
                prependRoomLog("💖 " + sessionManager.getUserName() + " followed " + speakerName + "!");
                Toast.makeText(ctx, "💖 You are now following " + speakerName + "!", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnFollow);

        Button btnGiftSpeaker = createSheetButton(ctx, "🎁 Send Luxury Gift to " + speakerName, "#F1C40F", "#0A0A14");
        btnGiftSpeaker.setOnClickListener(v -> {
            sheet.dismiss();
            showVoicePartyGiftDrawer(speakerName);
        });
        root.addView(btnGiftSpeaker);

        Button btnChatSpeaker = createSheetButton(ctx, "💬 Open 1-on-1 Private Chat with " + speakerName, "#00E5FF", "#0A0A14");
        btnChatSpeaker.setOnClickListener(v -> {
            sheet.dismiss();
            WallaChatOverlay.openFullScreenChat(ctx, speakerName + ", 25", Math.max(0, portraitIdx), roleTag, distance, false, null);
        });
        root.addView(btnChatSpeaker);

        sheet.setContentView(root);
        sheet.show();
    }

    private Button createSheetButton(Context ctx, String text, String bgHex, String textHex) {
        Button btn = new Button(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        lp.bottomMargin = dp(8);
        btn.setLayoutParams(lp);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(bgHex)));
        btn.setTextColor(Color.parseColor(textHex));
        return btn;
    }

    private void prependRoomLog(String line) {
        if (tvVoiceRoomLog == null) return;
        CharSequence existing = tvVoiceRoomLog.getText();
        tvVoiceRoomLog.setText(line + "\n" + (existing != null ? existing.toString() : ""));
    }

    private void playSfxTone(int toneType, int durationMs) {
        try {
            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
            tg.startTone(toneType, durationMs);
            if (ivRoomWallpaper != null) {
                ivRoomWallpaper.postDelayed(tg::release, durationMs + 60L);
            }
        } catch (Exception ignored) {
        }
    }

    private void startNeonPulse(View target, long duration, float maxScale) {
        if (target == null) return;
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(
                target,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.92f, maxScale),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.92f, maxScale),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.35f)
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
