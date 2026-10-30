package com.example.qluvx;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

/**
 * Full-Screen Live Broadcaster Studio ("🔴 GO LIVE") & Dual PK Challenge Engine:
 * - Real CameraX Front/Back Camera live video preview
 * - Camera Flip (Front <-> Back), Mic Mute/Unmute, and Beauty Filter Glow Toggle
 * - "⚔️ Start PK Challenge" button that invites a Challenger and splits the screen into a Dual PK Battle
 *   with a real looping HD video stream on the right + animated Red vs Blue Tug-of-War PK Score Bar
 * - Full-screen Animated Gift Blast (Flying Luxury Car / Supersonic Rocket across the screen)
 */
public class WallaBroadcasterStudio {

    private static final int REQ_CAMERA_MIC = 4091;

    public static void openBroadcasterStudio(@NonNull Activity activity) {
        final Dialog studioDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        studioDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final SessionManager sessionManager = new SessionManager(activity);
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final Random random = new Random();

        final boolean[] isFrontCamera = {true};
        final boolean[] isMicMuted = {false};
        final boolean[] isBeautyOn = {true};
        final boolean[] isPkModeActive = {false};
        final int[] viewerCount = {1420};
        final int[] redScore = {8500};
        final int[] blueScore = {7900};
        final float[] currentRedWeight = {52f};

        FrameLayout rootOverlay = new FrameLayout(activity);
        rootOverlay.setBackgroundColor(Color.parseColor("#0A0A14"));
        rootOverlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainCol = new LinearLayout(activity);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        mainCol.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootOverlay.addView(mainCol);

        // 1. TOP BROADCASTER STUDIO HEADER BAR
        RelativeLayout topBar = new RelativeLayout(activity);
        topBar.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 8));

        LinearLayout hostPill = new LinearLayout(activity);
        hostPill.setOrientation(LinearLayout.HORIZONTAL);
        hostPill.setGravity(Gravity.CENTER_VERTICAL);
        hostPill.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        hostPill.setPadding(dp(activity, 6), dp(activity, 4), dp(activity, 12), dp(activity, 4));

        CardView avatarCard = new CardView(activity);
        avatarCard.setRadius(dp(activity, 18));
        avatarCard.setCardElevation(0f);
        ImageView ivMyAvatar = new ImageView(activity);
        ivMyAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadGuyPhoto(activity, ivMyAvatar, 6);
        avatarCard.addView(ivMyAvatar, new FrameLayout.LayoutParams(dp(activity, 36), dp(activity, 36)));
        hostPill.addView(avatarCard, new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 36)));

        LinearLayout hostInfoCol = new LinearLayout(activity);
        hostInfoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.leftMargin = dp(activity, 8);
        hostInfoCol.setLayoutParams(infoLp);

        TextView tvHostTitle = new TextView(activity);
        tvHostTitle.setText("🔴 LIVE • " + sessionManager.getUserName());
        tvHostTitle.setTextColor(Color.WHITE);
        tvHostTitle.setTextSize(12.5f);
        tvHostTitle.setTypeface(Typeface.DEFAULT_BOLD);
        hostInfoCol.addView(tvHostTitle);

        final TextView tvViewersLive = new TextView(activity);
        tvViewersLive.setText("👁 1,420 watching • 💎 +320");
        tvViewersLive.setTextColor(Color.parseColor("#F1C40F"));
        tvViewersLive.setTextSize(10.5f);
        tvViewersLive.setTypeface(Typeface.DEFAULT_BOLD);
        hostInfoCol.addView(tvViewersLive);

        hostPill.addView(hostInfoCol);
        topBar.addView(hostPill);

        // Top Right: "⚔️ Start PK Challenge" Button + End Live Button
        LinearLayout topRightRow = new LinearLayout(activity);
        topRightRow.setOrientation(LinearLayout.HORIZONTAL);
        topRightRow.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams trLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        trLp.addRule(RelativeLayout.CENTER_VERTICAL);

        final TextView btnStartPkChallenge = new TextView(activity);
        btnStartPkChallenge.setText("⚔️ Start PK Challenge");
        btnStartPkChallenge.setTextColor(Color.WHITE);
        btnStartPkChallenge.setTextSize(11.5f);
        btnStartPkChallenge.setTypeface(Typeface.DEFAULT_BOLD);
        btnStartPkChallenge.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        btnStartPkChallenge.setPadding(dp(activity, 12), dp(activity, 7), dp(activity, 12), dp(activity, 7));
        topRightRow.addView(btnStartPkChallenge);

        TextView btnEndLive = new TextView(activity);
        btnEndLive.setText("✕");
        btnEndLive.setTextColor(Color.WHITE);
        btnEndLive.setTextSize(14f);
        btnEndLive.setTypeface(Typeface.DEFAULT_BOLD);
        btnEndLive.setGravity(Gravity.CENTER);
        btnEndLive.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(activity, 34), dp(activity, 34));
        closeLp.leftMargin = dp(activity, 8);
        topRightRow.addView(btnEndLive, closeLp);

        topBar.addView(topRightRow, trLp);
        mainCol.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. PK TUG-OF-WAR SCORE BAR (Hidden initially in Solo Broadcast, revealed on "⚔️ Start PK Challenge")
        final FrameLayout pkBarWrapper = new FrameLayout(activity);
        pkBarWrapper.setVisibility(View.GONE);
        LinearLayout.LayoutParams pkWrapLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 28));
        pkWrapLp.leftMargin = dp(activity, 12);
        pkWrapLp.rightMargin = dp(activity, 12);
        pkWrapLp.bottomMargin = dp(activity, 4);
        pkBarWrapper.setLayoutParams(pkWrapLp);

        LinearLayout pkBarLinear = new LinearLayout(activity);
        pkBarLinear.setOrientation(LinearLayout.HORIZONTAL);
        pkBarLinear.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final RelativeLayout barRed = new RelativeLayout(activity);
        barRed.setBackgroundColor(Color.parseColor("#FF2A7A"));
        barRed.setPadding(dp(activity, 10), 0, dp(activity, 6), 0);
        final TextView tvRedPts = new TextView(activity);
        tvRedPts.setText("🔥 RED 8,500");
        tvRedPts.setTextColor(Color.WHITE);
        tvRedPts.setTextSize(11f);
        tvRedPts.setTypeface(Typeface.DEFAULT_BOLD);
        RelativeLayout.LayoutParams rTxtLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rTxtLp.addRule(RelativeLayout.CENTER_VERTICAL);
        barRed.addView(tvRedPts, rTxtLp);

        View sparkDiv = new View(activity);
        sparkDiv.setBackgroundColor(Color.parseColor("#F1C40F"));

        final RelativeLayout barBlue = new RelativeLayout(activity);
        barBlue.setBackgroundColor(Color.parseColor("#00B0FF"));
        barBlue.setPadding(dp(activity, 6), 0, dp(activity, 10), 0);
        final TextView tvBluePts = new TextView(activity);
        tvBluePts.setText("7,900 BLUE ❄️");
        tvBluePts.setTextColor(Color.parseColor("#0A0A14"));
        tvBluePts.setTextSize(11f);
        tvBluePts.setTypeface(Typeface.DEFAULT_BOLD);
        RelativeLayout.LayoutParams bTxtLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bTxtLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        bTxtLp.addRule(RelativeLayout.CENTER_VERTICAL);
        barBlue.addView(tvBluePts, bTxtLp);

        pkBarLinear.addView(barRed, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 52f));
        pkBarLinear.addView(sparkDiv, new LinearLayout.LayoutParams(dp(activity, 4), ViewGroup.LayoutParams.MATCH_PARENT));
        pkBarLinear.addView(barBlue, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 48f));
        pkBarWrapper.addView(pkBarLinear);

        TextView tvVsEmblem = new TextView(activity);
        tvVsEmblem.setText("⚔️ PK");
        tvVsEmblem.setTextColor(Color.parseColor("#0A0A14"));
        tvVsEmblem.setTextSize(10f);
        tvVsEmblem.setTypeface(Typeface.DEFAULT_BOLD);
        tvVsEmblem.setBackgroundResource(R.drawable.bg_walla_gold_pill);
        tvVsEmblem.setPadding(dp(activity, 8), dp(activity, 2), dp(activity, 8), dp(activity, 2));
        FrameLayout.LayoutParams vsLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        pkBarWrapper.addView(tvVsEmblem, vsLp);

        mainCol.addView(pkBarWrapper);

        // 3. LIVE CAMERA & DUAL PK SPLIT VIDEO STAGE
        FrameLayout stageFrame = new FrameLayout(activity);
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.25f);
        stageLp.leftMargin = dp(activity, 10);
        stageLp.rightMargin = dp(activity, 10);
        stageFrame.setLayoutParams(stageLp);

        final LinearLayout splitVideoContainer = new LinearLayout(activity);
        splitVideoContainer.setOrientation(LinearLayout.HORIZONTAL);
        splitVideoContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // LEFT PANE: User's Real Live CameraX Feed (Full-screen initially, 50% width when PK starts)
        CardView leftCameraCard = new CardView(activity);
        leftCameraCard.setRadius(dp(activity, 16));
        leftCameraCard.setCardBackgroundColor(Color.parseColor("#141424"));
        leftCameraCard.setCardElevation(4f);
        final LinearLayout.LayoutParams leftCardLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 2f);
        leftCameraCard.setLayoutParams(leftCardLp);

        RelativeLayout leftRel = new RelativeLayout(activity);

        // Real HD photography fallback underneath CameraX PreviewView
        final ImageView ivCameraFallback = new ImageView(activity);
        ivCameraFallback.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadGuyPhoto(activity, ivCameraFallback, 0);
        leftRel.addView(ivCameraFallback, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Real CameraX PreviewView
        final PreviewView cameraPreviewView = new PreviewView(activity);
        cameraPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        cameraPreviewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        leftRel.addView(cameraPreviewView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Warm Beauty Filter Overlay on Camera
        final View viewBeautyGlow = new View(activity);
        GradientDrawable beautyDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0x26FF2A7A, 0x18F1C40F, 0x2200E5FF}
        );
        viewBeautyGlow.setBackground(beautyDrawable);
        leftRel.addView(viewBeautyGlow, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Top-Left Live Camera Status Badge
        final TextView tvCameraStatusBadge = new TextView(activity);
        tvCameraStatusBadge.setText("📹 FRONT CAM LIVE • ✨ BEAUTY ON");
        tvCameraStatusBadge.setTextColor(Color.WHITE);
        tvCameraStatusBadge.setTextSize(9.5f);
        tvCameraStatusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvCameraStatusBadge.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        tvCameraStatusBadge.setPadding(dp(activity, 8), dp(activity, 3), dp(activity, 8), dp(activity, 3));
        RelativeLayout.LayoutParams camBadgeLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        camBadgeLp.setMargins(dp(activity, 10), dp(activity, 10), 0, 0);
        leftRel.addView(tvCameraStatusBadge, camBadgeLp);

        // Bottom Scrim + "+500 Gift Red" Button on Left Camera Card
        LinearLayout leftBottomBar = new LinearLayout(activity);
        leftBottomBar.setOrientation(LinearLayout.HORIZONTAL);
        leftBottomBar.setGravity(Gravity.CENTER_VERTICAL);
        leftBottomBar.setBackgroundResource(R.drawable.bg_nearby_bottom_scrim);
        leftBottomBar.setPadding(dp(activity, 10), dp(activity, 14), dp(activity, 10), dp(activity, 8));
        RelativeLayout.LayoutParams leftBotLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftBotLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        TextView tvLeftHostName = new TextView(activity);
        tvLeftHostName.setText("🎙️ You (Host)");
        tvLeftHostName.setTextColor(Color.WHITE);
        tvLeftHostName.setTextSize(12f);
        tvLeftHostName.setTypeface(Typeface.DEFAULT_BOLD);
        leftBottomBar.addView(tvLeftHostName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Button btnBoostRed = new Button(activity);
        btnBoostRed.setText("🌹 +500 Gift Red");
        btnBoostRed.setAllCaps(false);
        btnBoostRed.setTextColor(Color.WHITE);
        btnBoostRed.setTextSize(10f);
        btnBoostRed.setTypeface(Typeface.DEFAULT_BOLD);
        btnBoostRed.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        btnBoostRed.setPadding(dp(activity, 10), 0, dp(activity, 10), 0);
        leftBottomBar.addView(btnBoostRed, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 30)));

        leftRel.addView(leftBottomBar, leftBotLp);
        leftCameraCard.addView(leftRel);

        // RIGHT PANE: Challenger Ryan Beats Real Looping HD Video Stream (Revealed on "⚔️ Start PK Challenge")
        final CardView rightChallengerCard = new CardView(activity);
        rightChallengerCard.setRadius(dp(activity, 16));
        rightChallengerCard.setCardBackgroundColor(Color.parseColor("#141424"));
        rightChallengerCard.setCardElevation(4f);
        rightChallengerCard.setVisibility(View.GONE);
        final LinearLayout.LayoutParams rightCardLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        rightCardLp.leftMargin = dp(activity, 6);
        rightChallengerCard.setLayoutParams(rightCardLp);

        RelativeLayout rightRel = new RelativeLayout(activity);

        final ImageView ivRightFallback = new ImageView(activity);
        ivRightFallback.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadGuyPhoto(activity, ivRightFallback, 1);
        rightRel.addView(ivRightFallback, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final VideoView vvChallenger = new VideoView(activity);
        RelativeLayout.LayoutParams vvLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        vvLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        rightRel.addView(vvChallenger, vvLp);

        TextView tvChallengerBadge = new TextView(activity);
        tvChallengerBadge.setText("⚡ PK CHALLENGER • LIVE");
        tvChallengerBadge.setTextColor(Color.parseColor("#0A0A14"));
        tvChallengerBadge.setTextSize(9f);
        tvChallengerBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvChallengerBadge.setBackgroundResource(R.drawable.bg_walla_cyan_pill);
        tvChallengerBadge.setPadding(dp(activity, 8), dp(activity, 3), dp(activity, 8), dp(activity, 3));
        RelativeLayout.LayoutParams chBadgeLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chBadgeLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        chBadgeLp.setMargins(0, dp(activity, 10), dp(activity, 10), 0);
        rightRel.addView(tvChallengerBadge, chBadgeLp);

        LinearLayout rightBottomBar = new LinearLayout(activity);
        rightBottomBar.setOrientation(LinearLayout.HORIZONTAL);
        rightBottomBar.setGravity(Gravity.CENTER_VERTICAL);
        rightBottomBar.setBackgroundResource(R.drawable.bg_nearby_bottom_scrim);
        rightBottomBar.setPadding(dp(activity, 10), dp(activity, 14), dp(activity, 10), dp(activity, 8));
        RelativeLayout.LayoutParams rightBotLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightBotLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        TextView tvRightHostName = new TextView(activity);
        tvRightHostName.setText("🎧 Ryan Beats");
        tvRightHostName.setTextColor(Color.WHITE);
        tvRightHostName.setTextSize(12f);
        tvRightHostName.setTypeface(Typeface.DEFAULT_BOLD);
        rightBottomBar.addView(tvRightHostName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Button btnBoostBlue = new Button(activity);
        btnBoostBlue.setText("🏎️ +500 Gift Blue");
        btnBoostBlue.setAllCaps(false);
        btnBoostBlue.setTextColor(Color.parseColor("#0A0A14"));
        btnBoostBlue.setTextSize(10f);
        btnBoostBlue.setTypeface(Typeface.DEFAULT_BOLD);
        btnBoostBlue.setBackgroundResource(R.drawable.bg_walla_cyan_pill);
        btnBoostBlue.setPadding(dp(activity, 10), 0, dp(activity, 10), 0);
        rightBottomBar.addView(btnBoostBlue, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 30)));

        rightRel.addView(rightBottomBar, rightBotLp);
        rightChallengerCard.addView(rightRel);

        splitVideoContainer.addView(leftCameraCard);
        splitVideoContainer.addView(rightChallengerCard);
        stageFrame.addView(splitVideoContainer);

        // Center Top PK Timer Pill
        final TextView tvStudioPkTimer = new TextView(activity);
        tvStudioPkTimer.setText("⏱️ PK 04:59");
        tvStudioPkTimer.setTextColor(Color.parseColor("#F1C40F"));
        tvStudioPkTimer.setTextSize(11f);
        tvStudioPkTimer.setTypeface(Typeface.DEFAULT_BOLD);
        tvStudioPkTimer.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        tvStudioPkTimer.setPadding(dp(activity, 12), dp(activity, 4), dp(activity, 12), dp(activity, 4));
        tvStudioPkTimer.setVisibility(View.GONE);
        FrameLayout.LayoutParams timerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        timerLp.topMargin = dp(activity, 10);
        stageFrame.addView(tvStudioPkTimer, timerLp);

        mainCol.addView(stageFrame);

        // 4. CAMERA HARDWARE CONTROL BAR (Camera Flip, Mic Mute, Beauty Filter, Gift Blast)
        LinearLayout hardwareBar = new LinearLayout(activity);
        hardwareBar.setOrientation(LinearLayout.HORIZONTAL);
        hardwareBar.setGravity(Gravity.CENTER);
        hardwareBar.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 4));

        final TextView btnFlipCam = createControlPill(activity, "🔄 Flip Camera", "#00E5FF");
        final TextView btnMuteMic = createControlPill(activity, "🎙️ Mic ON", "#00E676");
        final TextView btnBeautyToggle = createControlPill(activity, "✨ Beauty: ON", "#FF2A7A");
        final TextView btnRocketBlast = createControlPill(activity, "🚀 Gift Blast", "#F1C40F");

        hardwareBar.addView(btnFlipCam, new LinearLayout.LayoutParams(0, dp(activity, 36), 1f));
        View sp1 = new View(activity);
        hardwareBar.addView(sp1, new LinearLayout.LayoutParams(dp(activity, 6), 1));
        hardwareBar.addView(btnMuteMic, new LinearLayout.LayoutParams(0, dp(activity, 36), 1f));
        View sp2 = new View(activity);
        hardwareBar.addView(sp2, new LinearLayout.LayoutParams(dp(activity, 6), 1));
        hardwareBar.addView(btnBeautyToggle, new LinearLayout.LayoutParams(0, dp(activity, 36), 1f));
        View sp3 = new View(activity);
        hardwareBar.addView(sp3, new LinearLayout.LayoutParams(dp(activity, 6), 1));
        hardwareBar.addView(btnRocketBlast, new LinearLayout.LayoutParams(0, dp(activity, 36), 1f));

        mainCol.addView(hardwareBar);

        // 5. LIVE FLOATING CHAT STREAM + COMMENT INPUT BAR
        LinearLayout bottomChatSection = new LinearLayout(activity);
        bottomChatSection.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams chatSecLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.72f);
        chatSecLp.setMargins(dp(activity, 10), dp(activity, 4), dp(activity, 10), dp(activity, 10));
        bottomChatSection.setLayoutParams(chatSecLp);

        final ScrollView svStudioChat = new ScrollView(activity);
        svStudioChat.setBackgroundResource(R.drawable.bg_walla_glass_card);
        svStudioChat.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
        final LinearLayout llStudioChat = new LinearLayout(activity);
        llStudioChat.setOrientation(LinearLayout.VERTICAL);
        svStudioChat.addView(llStudioChat);
        bottomChatSection.addView(svStudioChat, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams inRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 42));
        inRowLp.topMargin = dp(activity, 6);
        inputRow.setLayoutParams(inRowLp);

        final EditText etStudioComment = new EditText(activity);
        etStudioComment.setHint("💬 Chat with your live viewers...");
        etStudioComment.setHintTextColor(Color.parseColor("#8A8AA8"));
        etStudioComment.setTextColor(Color.WHITE);
        etStudioComment.setTextSize(12.5f);
        etStudioComment.setSingleLine(true);
        etStudioComment.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        etStudioComment.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
        inputRow.addView(etStudioComment, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button btnSendStudioComment = new Button(activity);
        btnSendStudioComment.setText("Send");
        btnSendStudioComment.setAllCaps(false);
        btnSendStudioComment.setTextColor(Color.WHITE);
        btnSendStudioComment.setTextSize(12f);
        btnSendStudioComment.setTypeface(Typeface.DEFAULT_BOLD);
        btnSendStudioComment.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        LinearLayout.LayoutParams sendBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 40));
        sendBtnLp.leftMargin = dp(activity, 8);
        inputRow.addView(btnSendStudioComment, sendBtnLp);

        bottomChatSection.addView(inputRow);
        mainCol.addView(bottomChatSection);

        // Full-Screen Gift Blast Overlay Container (for Flying Luxury Sports Car & Rocket)
        final FrameLayout giftBlastContainer = new FrameLayout(activity);
        giftBlastContainer.setClickable(false);
        giftBlastContainer.setFocusable(false);
        rootOverlay.addView(giftBlastContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Seed Live Studio Comments
        appendStudioChat(activity, llStudioChat, svStudioChat, "🛡️ System",
                "Your live broadcast is ON AIR! Use Flip Camera, Beauty Filter, or tap '⚔️ Start PK Challenge'!", "#00E5FF");
        appendStudioChat(activity, llStudioChat, svStudioChat, "🔥 Alex_25",
                "Looking sharp on camera tonight! 🔥👑", "#FF2A7A");
        appendStudioChat(activity, llStudioChat, svStudioChat, "💎 Kabir_BLR",
                "Start a PK battle with Ryan! We've got your back with Luxury Cars 🏎️💨", "#F1C40F");

        // Bind Real CameraX Feed
        bindCameraFeed(activity, cameraPreviewView, ivCameraFallback, tvCameraStatusBadge, isFrontCamera[0], isBeautyOn[0]);

        // 1. Camera Flip Listener (Front <-> Back)
        btnFlipCam.setOnClickListener(v -> {
            isFrontCamera[0] = !isFrontCamera[0];
            bindCameraFeed(activity, cameraPreviewView, ivCameraFallback, tvCameraStatusBadge, isFrontCamera[0], isBeautyOn[0]);
            Toast.makeText(activity, isFrontCamera[0] ? "📹 Switched to Front Camera" : "📹 Switched to Back Camera", Toast.LENGTH_SHORT).show();
        });

        // 2. Mic Mute/Unmute Listener
        btnMuteMic.setOnClickListener(v -> {
            isMicMuted[0] = !isMicMuted[0];
            btnMuteMic.setText(isMicMuted[0] ? "🔇 Mic Muted" : "🎙️ Mic ON");
            btnMuteMic.setTextColor(Color.parseColor(isMicMuted[0] ? "#FF5252" : "#00E676"));
            Toast.makeText(activity, isMicMuted[0] ? "🔇 Microphone Muted" : "🎙️ Microphone Live", Toast.LENGTH_SHORT).show();
        });

        // 3. Beauty Filter Toggle Listener
        btnBeautyToggle.setOnClickListener(v -> {
            isBeautyOn[0] = !isBeautyOn[0];
            viewBeautyGlow.setVisibility(isBeautyOn[0] ? View.VISIBLE : View.GONE);
            btnBeautyToggle.setText(isBeautyOn[0] ? "✨ Beauty: ON" : "✨ Beauty: OFF");
            btnBeautyToggle.setTextColor(Color.parseColor(isBeautyOn[0] ? "#FF2A7A" : "#9E9EBC"));
            String camSide = isFrontCamera[0] ? "FRONT CAM LIVE" : "BACK CAM LIVE";
            tvCameraStatusBadge.setText("📹 " + camSide + (isBeautyOn[0] ? " • ✨ BEAUTY ON" : ""));
        });

        // Helper for PK Bar Animation
        final Runnable updatePkBarUI = () -> {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            tvRedPts.setText("🔥 RED " + nf.format(redScore[0]));
            tvBluePts.setText(nf.format(blueScore[0]) + " BLUE ❄️");
            float total = Math.max(1f, redScore[0] + blueScore[0]);
            float targetRed = Math.max(18f, Math.min(82f, (redScore[0] / total) * 100f));

            ValueAnimator anim = ValueAnimator.ofFloat(currentRedWeight[0], targetRed);
            anim.setDuration(420L);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(a -> {
                currentRedWeight[0] = (float) a.getAnimatedValue();
                LinearLayout.LayoutParams rLp = (LinearLayout.LayoutParams) barRed.getLayoutParams();
                LinearLayout.LayoutParams bLp = (LinearLayout.LayoutParams) barBlue.getLayoutParams();
                rLp.weight = currentRedWeight[0];
                bLp.weight = 100f - currentRedWeight[0];
                barRed.setLayoutParams(rLp);
                barBlue.setLayoutParams(bLp);
            });
            anim.start();
        };

        // 4. "+500 Gift Red" & "+500 Gift Blue" & "🚀 Gift Blast" Listeners
        btnBoostRed.setOnClickListener(v -> {
            redScore[0] += 500;
            updatePkBarUI.run();
            appendStudioChat(activity, llStudioChat, svStudioChat, "🎁 " + sessionManager.getUserName(),
                    "sent 🏎️ Luxury Cyber Car (+500 PK) to RED!", "#FF2A7A");
            triggerFullScreenGiftBlast(activity, giftBlastContainer, true, "🏎️ LUXURY SPORTS CAR", sessionManager.getUserName());
        });

        btnBoostBlue.setOnClickListener(v -> {
            blueScore[0] += 500;
            updatePkBarUI.run();
            appendStudioChat(activity, llStudioChat, svStudioChat, "🎁 " + sessionManager.getUserName(),
                    "sent 🚀 Interstellar Rocket (+500 PK) to BLUE!", "#00E5FF");
            triggerFullScreenGiftBlast(activity, giftBlastContainer, false, "🚀 SUPERSONIC ROCKET", sessionManager.getUserName());
        });

        btnRocketBlast.setOnClickListener(v -> {
            redScore[0] += 1000;
            if (isPkModeActive[0]) {
                updatePkBarUI.run();
            }
            appendStudioChat(activity, llStudioChat, svStudioChat, "👑 " + sessionManager.getUserName(),
                    "triggered FULL-SCREEN 🚀 INTERSTELLAR ROCKET BLAST (+1,000 PK)!", "#F1C40F");
            triggerFullScreenGiftBlast(activity, giftBlastContainer, true, "🚀 INTERSTELLAR STARSHIP X", sessionManager.getUserName());
        });

        // 5. "⚔️ Start PK Challenge" Button -> Splits screen into Dual PK Battle with Looping Video Stream!
        final CountDownTimer[] pkTimerHolder = new CountDownTimer[1];
        btnStartPkChallenge.setOnClickListener(v -> {
            isPkModeActive[0] = !isPkModeActive[0];
            if (isPkModeActive[0]) {
                btnStartPkChallenge.setText("⚔️ End PK Split");
                btnStartPkChallenge.setBackgroundResource(R.drawable.bg_walla_gold_pill);
                btnStartPkChallenge.setTextColor(Color.parseColor("#0A0A14"));

                pkBarWrapper.setVisibility(View.VISIBLE);
                tvStudioPkTimer.setVisibility(View.VISIBLE);
                rightChallengerCard.setVisibility(View.VISIBLE);
                leftCardLp.weight = 1f;
                leftCameraCard.setLayoutParams(leftCardLp);

                startLoopingChallengerVideo(activity, vvChallenger, ivRightFallback, 0);
                updatePkBarUI.run();

                appendStudioChat(activity, llStudioChat, svStudioChat, "⚔️ PK MATCHED",
                        "Challenger Ryan Beats joined Dual PK Battle! Tap '+500 Gift Red' or '+500 Gift Blue'!", "#F1C40F");
                triggerFullScreenGiftBlast(activity, giftBlastContainer, true, "⚔️ PK CHALLENGE STARTED", "Ryan Beats");

                if (pkTimerHolder[0] != null) pkTimerHolder[0].cancel();
                pkTimerHolder[0] = new CountDownTimer(5 * 60 * 1000L, 1000L) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long s = millisUntilFinished / 1000L;
                        tvStudioPkTimer.setText(String.format(Locale.US, "⏱️ PK %02d:%02d", s / 60, s % 60));
                    }

                    @Override
                    public void onFinish() {
                        tvStudioPkTimer.setText("🏆 PK VICTORY!");
                    }
                }.start();
            } else {
                btnStartPkChallenge.setText("⚔️ Start PK Challenge");
                btnStartPkChallenge.setBackgroundResource(R.drawable.bg_walla_pink_pill);
                btnStartPkChallenge.setTextColor(Color.WHITE);

                pkBarWrapper.setVisibility(View.GONE);
                tvStudioPkTimer.setVisibility(View.GONE);
                rightChallengerCard.setVisibility(View.GONE);
                leftCardLp.weight = 2f;
                leftCameraCard.setLayoutParams(leftCardLp);
                try {
                    vvChallenger.stopPlayback();
                } catch (Exception ignored) {}
            }
        });

        // 6. Send Comment in Studio
        Runnable sendStudioChatAction = () -> {
            String msg = etStudioComment.getText().toString().trim();
            if (msg.isEmpty()) return;
            etStudioComment.setText("");
            appendStudioChat(activity, llStudioChat, svStudioChat, "👑 " + sessionManager.getUserName(), msg, "#F1C40F");
        };
        btnSendStudioComment.setOnClickListener(v -> sendStudioChatAction.run());
        etStudioComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendStudioChatAction.run();
                return true;
            }
            return false;
        });

        // Live Viewer Ticker
        final Runnable viewerTicker = new Runnable() {
            @Override
            public void run() {
                if (!studioDialog.isShowing()) return;
                viewerCount[0] += random.nextInt(9) - 2;
                if (viewerCount[0] < 1200) viewerCount[0] = 1420;
                tvViewersLive.setText(String.format(Locale.US, "👁 %,d watching • 💎 +320", viewerCount[0]));
                uiHandler.postDelayed(this, 2400L);
            }
        };
        uiHandler.postDelayed(viewerTicker, 2000L);

        btnEndLive.setOnClickListener(v -> studioDialog.dismiss());

        studioDialog.setOnDismissListener(d -> {
            if (pkTimerHolder[0] != null) pkTimerHolder[0].cancel();
            try {
                vvChallenger.stopPlayback();
            } catch (Exception ignored) {}
        });

        studioDialog.setContentView(rootOverlay);
        if (studioDialog.getWindow() != null) {
            studioDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        studioDialog.show();
    }

    /**
     * Binds AndroidX CameraX real live camera preview to the provided PreviewView.
     * Requests CAMERA permission at runtime if not yet granted.
     */
    public static void bindCameraFeed(
            @NonNull Context context,
            @NonNull PreviewView previewView,
            @NonNull ImageView fallbackImageView,
            @NonNull TextView statusBadge,
            boolean useFrontCamera,
            boolean beautyEnabled
    ) {
        String sideLabel = useFrontCamera ? "FRONT CAM LIVE" : "BACK CAM LIVE";
        statusBadge.setText("📹 " + sideLabel + (beautyEnabled ? " • ✨ BEAUTY ON" : ""));

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions(
                        (Activity) context,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                        REQ_CAMERA_MIC
                );
            }
            // Show real HD Unsplash stream host backdrop until permission is granted
            fallbackImageView.setVisibility(View.VISIBLE);
            WallaPortraitStudio.loadGuyPhoto(context, fallbackImageView, useFrontCamera ? 0 : 3);
            return;
        }

        if (!(context instanceof LifecycleOwner)) {
            fallbackImageView.setVisibility(View.VISIBLE);
            WallaPortraitStudio.loadGuyPhoto(context, fallbackImageView, useFrontCamera ? 0 : 3);
            return;
        }

        final LifecycleOwner lifecycleOwner = (LifecycleOwner) context;
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector selector = useFrontCamera
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                if (cameraProvider.hasCamera(selector)) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview);
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview);
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview);
                }
            } catch (Exception e) {
                fallbackImageView.setVisibility(View.VISIBLE);
                WallaPortraitStudio.loadGuyPhoto(context, fallbackImageView, useFrontCamera ? 0 : 3);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * Starts a real looping HD video stream inside the Challenger's VideoView with a seamless
     * Glide HD portrait backdrop while buffering.
     */
    public static void startLoopingChallengerVideo(
            @NonNull Context context,
            @NonNull VideoView videoView,
            @NonNull ImageView fallbackImageView,
            int streamIndex
    ) {
        fallbackImageView.setVisibility(View.VISIBLE);
        WallaPortraitStudio.loadGuyPhoto(context, fallbackImageView, 1);

        try {
            String videoUrl = WallaPortraitStudio.getLiveChallengerVideoUrl(streamIndex);
            videoView.setVideoURI(Uri.parse(videoUrl));
            videoView.setOnPreparedListener(mp -> {
                try {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                    videoView.start();
                    // Fade out static backdrop once real video frames are playing
                    fallbackImageView.animate().alpha(0.15f).setDuration(400).start();
                } catch (Exception ignored) {}
            });
            videoView.setOnErrorListener((mp, what, extra) -> {
                fallbackImageView.setAlpha(1f);
                fallbackImageView.setVisibility(View.VISIBLE);
                return true;
            });
        } catch (Exception e) {
            fallbackImageView.setAlpha(1f);
            fallbackImageView.setVisibility(View.VISIBLE);
        }
    }

    private static long lastGiftTapTimestampMs = 0L;
    private static int currentGiftComboCount = 0;

    /**
     * Triggers a Full-Screen Animated Gift Blast (Flying Luxury Sports Car / Supersonic Rocket)
     * AND automatically increments the Gift Combo Streak Multiplier ('COMBO STREAK x10! 🔥')
     * with flying gold coins effect on rapid gift taps!
     */
    public static void triggerFullScreenGiftBlast(
            @NonNull Context context,
            @NonNull FrameLayout container,
            boolean isRedTeam,
            @NonNull String giftName,
            @NonNull String senderName
    ) {
        // Track rapid gift tap combo streak (within 4.5s window, or boost straight to x10 on rapid taps)
        triggerGiftComboStreak(context, container, giftName, senderName);

        final LinearLayout blastCard = new LinearLayout(context);
        blastCard.setOrientation(LinearLayout.VERTICAL);
        blastCard.setGravity(Gravity.CENTER);
        blastCard.setPadding(dp(context, 22), dp(context, 16), dp(context, 22), dp(context, 16));

        GradientDrawable bannerBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                isRedTeam
                        ? new int[]{0xEEFF2A7A, 0xEEF1C40F, 0xEE7C4DFF}
                        : new int[]{0xEE00E5FF, 0xEE2979FF, 0xEEF1C40F}
        );
        bannerBg.setCornerRadius(dp(context, 24));
        bannerBg.setStroke(dp(context, 2), Color.WHITE);
        blastCard.setBackground(bannerBg);

        TextView tvGiantVehicle = new TextView(context);
        tvGiantVehicle.setText(isRedTeam ? "🏎️💨✨🔥" : "🚀✨❄️💎");
        tvGiantVehicle.setTextSize(44f);
        tvGiantVehicle.setGravity(Gravity.CENTER);
        blastCard.addView(tvGiantVehicle);

        TextView tvBlastTitle = new TextView(context);
        tvBlastTitle.setText(giftName + " BLAST!");
        tvBlastTitle.setTextColor(Color.WHITE);
        tvBlastTitle.setTextSize(16f);
        tvBlastTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvBlastTitle.setGravity(Gravity.CENTER);
        blastCard.addView(tvBlastTitle);

        TextView tvBlastSub = new TextView(context);
        tvBlastSub.setText("👑 " + senderName + " boosted " + (isRedTeam ? "RED HOST (+500 PK)" : "BLUE HOST (+500 PK)"));
        tvBlastSub.setTextColor(Color.parseColor("#0A0A14"));
        tvBlastSub.setTextSize(11.5f);
        tvBlastSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvBlastSub.setGravity(Gravity.CENTER);
        blastCard.addView(tvBlastSub);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        container.addView(blastCard, lp);

        float screenW = context.getResources().getDisplayMetrics().widthPixels;
        float startX = isRedTeam ? -screenW * 0.85f : screenW * 0.85f;
        float endX = isRedTeam ? screenW * 0.85f : -screenW * 0.85f;

        ObjectAnimator flyIn = ObjectAnimator.ofFloat(blastCard, View.TRANSLATION_X, startX, 0f);
        flyIn.setDuration(480L);
        flyIn.setInterpolator(new OvershootInterpolator(1.1f));

        ObjectAnimator scalePulse = ObjectAnimator.ofPropertyValuesHolder(
                blastCard,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.85f, 1.12f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.85f, 1.12f, 1.0f)
        );
        scalePulse.setDuration(650L);

        ObjectAnimator flyOut = ObjectAnimator.ofFloat(blastCard, View.TRANSLATION_X, 0f, endX);
        flyOut.setStartDelay(620L);
        flyOut.setDuration(450L);
        flyOut.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(blastCard, View.ALPHA, 1f, 0f);
        fadeOut.setStartDelay(820L);
        fadeOut.setDuration(250L);

        AnimatorSet set = new AnimatorSet();
        set.play(flyIn).with(scalePulse);
        set.play(flyOut).after(flyIn);
        set.play(fadeOut).with(flyOut);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                container.removeView(blastCard);
            }
        });
        set.start();
    }

    /**
     * GIFT COMBO STREAK MULTIPLIER:
     * Rapid-tapping any gift button triggers an animated 'COMBO STREAK x10! 🔥' banner
     * with a fountain of flying gold coins (🪙) across the screen.
     */
    public static int triggerGiftComboStreak(
            @NonNull Context context,
            @NonNull FrameLayout container,
            @NonNull String giftName,
            @NonNull String senderName
    ) {
        long now = System.currentTimeMillis();
        if (now - lastGiftTapTimestampMs <= 4500L) {
            if (currentGiftComboCount < 2) {
                currentGiftComboCount = 2;
            } else if (currentGiftComboCount < 5) {
                currentGiftComboCount += 3;
            } else if (currentGiftComboCount < 10) {
                currentGiftComboCount = 10;
            } else {
                currentGiftComboCount += 5;
            }
        } else {
            currentGiftComboCount = 1;
        }
        lastGiftTapTimestampMs = now;

        final int displayMultiplier = currentGiftComboCount == 1 ? 10 : currentGiftComboCount;
        final Random rng = new Random();

        // 1. Spawn Combo Streak Neon Banner at Top-Center of Stage
        final LinearLayout comboBadge = new LinearLayout(context);
        comboBadge.setOrientation(LinearLayout.VERTICAL);
        comboBadge.setGravity(Gravity.CENTER);
        comboBadge.setPadding(dp(context, 20), dp(context, 10), dp(context, 20), dp(context, 10));

        GradientDrawable comboBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF2A7A, 0xFFFF8F00, 0xFFF1C40F}
        );
        comboBg.setCornerRadius(dp(context, 28));
        comboBg.setStroke(dp(context, 2), Color.WHITE);
        comboBadge.setBackground(comboBg);

        TextView tvComboTitle = new TextView(context);
        tvComboTitle.setText("COMBO STREAK x" + displayMultiplier + "! 🔥");
        tvComboTitle.setTextColor(Color.WHITE);
        tvComboTitle.setTextSize(18f);
        tvComboTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvComboTitle.setGravity(Gravity.CENTER);
        comboBadge.addView(tvComboTitle);

        TextView tvComboSub = new TextView(context);
        tvComboSub.setText("🪙 " + senderName + " • " + giftName + " • Keep tapping for x" + (displayMultiplier + 5) + "!");
        tvComboSub.setTextColor(Color.parseColor("#0A0A14"));
        tvComboSub.setTextSize(10.5f);
        tvComboSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvComboSub.setGravity(Gravity.CENTER);
        comboBadge.addView(tvComboSub);

        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        badgeLp.topMargin = dp(context, 72);
        container.addView(comboBadge, badgeLp);

        ObjectAnimator popScale = ObjectAnimator.ofPropertyValuesHolder(
                comboBadge,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.4f, 1.22f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.4f, 1.22f, 1.0f)
        );
        popScale.setDuration(420L);
        popScale.setInterpolator(new OvershootInterpolator(1.6f));

        ObjectAnimator fadeCombo = ObjectAnimator.ofFloat(comboBadge, View.ALPHA, 1f, 0f);
        fadeCombo.setStartDelay(1150L);
        fadeCombo.setDuration(320L);

        AnimatorSet badgeSet = new AnimatorSet();
        badgeSet.playTogether(popScale, fadeCombo);
        badgeSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                container.removeView(comboBadge);
            }
        });
        badgeSet.start();

        // 2. Spawn 18 Flying Gold Coins (🪙✨💰🔥) Fountain Effect
        String[] coinTokens = {"🪙", "🪙", "💰", "✨", "🔥", "💎"};
        for (int i = 0; i < 18; i++) {
            final TextView tvCoin = new TextView(context);
            tvCoin.setText(coinTokens[i % coinTokens.length]);
            tvCoin.setTextSize(18f + rng.nextInt(10));

            FrameLayout.LayoutParams coinLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            );
            coinLp.bottomMargin = dp(context, 90 + rng.nextInt(40));
            container.addView(tvCoin, coinLp);

            float targetX = (rng.nextFloat() - 0.5f) * dp(context, 300);
            float targetY = -dp(context, 220 + rng.nextInt(260));
            float rot = (rng.nextFloat() - 0.5f) * 360f;

            ObjectAnimator animX = ObjectAnimator.ofFloat(tvCoin, View.TRANSLATION_X, 0f, targetX);
            ObjectAnimator animY = ObjectAnimator.ofFloat(tvCoin, View.TRANSLATION_Y, 0f, targetY);
            ObjectAnimator animRot = ObjectAnimator.ofFloat(tvCoin, View.ROTATION, 0f, rot);
            ObjectAnimator animAlpha = ObjectAnimator.ofFloat(tvCoin, View.ALPHA, 1f, 1f, 0f);
            ObjectAnimator animScale = ObjectAnimator.ofPropertyValuesHolder(
                    tvCoin,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.6f, 1.35f, 0.9f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.6f, 1.35f, 0.9f)
            );

            AnimatorSet coinSet = new AnimatorSet();
            coinSet.playTogether(animX, animY, animRot, animAlpha, animScale);
            coinSet.setStartDelay(i * 35L);
            coinSet.setDuration(850L + rng.nextInt(450));
            coinSet.setInterpolator(new DecelerateInterpolator());
            coinSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    container.removeView(tvCoin);
                }
            });
            coinSet.start();
        }

        return displayMultiplier;
    }

    private static TextView createControlPill(Context ctx, String text, String hexColor) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(hexColor));
        tv.setTextSize(10.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.bg_walla_glass_card);
        tv.setPadding(dp(ctx, 6), dp(ctx, 4), dp(ctx, 6), dp(ctx, 4));
        return tv;
    }

    private static void appendStudioChat(Context ctx, LinearLayout container, ScrollView sv, String sender, String msg, String colorHex) {
        TextView tv = new TextView(ctx);
        String full = sender + ": " + msg;
        SpannableString span = new SpannableString(full);
        span.setSpan(new ForegroundColorSpan(Color.parseColor(colorHex)), 0, sender.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.WHITE), sender.length() + 1, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(span);
        tv.setTextSize(11.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(ctx, 2), 0, dp(ctx, 3));
        container.addView(tv);
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    private static int dp(Context ctx, int val) {
        return Math.round(val * ctx.getResources().getDisplayMetrics().density);
    }
}
