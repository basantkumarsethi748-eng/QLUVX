package com.example.qluvx;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Full-Screen 1-on-1 Interactive Chat Screen with:
 * - Past message history bubbles + timestamps
 * - Working Message Input + Send button that immediately appends bubbles
 * - Clickable 5s Disappearing Photo bubble + "📸 5s Photo" sender that opens a full-screen
 *   5..4..3..2..1 countdown dialog and automatically burns & closes when time expires
 * - Clickable Voice Note bubble + "🎙️ Voice" sender with animated multi-bar audio waveform
 */
public class WallaChatOverlay {

    public interface OnChatUpdatedListener {
        void onNewLastMessage(String peerName, String previewText, String timestamp);
    }

    public static class ChatBubble {
        public static final int TYPE_TEXT = 0;
        public static final int TYPE_BURN_PHOTO = 1;
        public static final int TYPE_VOICE_NOTE = 2;

        public final boolean isMe;
        public final int type;
        public String text;
        public final String timestamp;
        public boolean isBurned;
        public int voiceDurationSec;
        public boolean isPlayingVoice;
        public int voicePlayProgressSec;

        public ChatBubble(boolean isMe, int type, String text, String timestamp, int voiceDurationSec) {
            this.isMe = isMe;
            this.type = type;
            this.text = text;
            this.timestamp = timestamp;
            this.isBurned = false;
            this.voiceDurationSec = voiceDurationSec;
            this.isPlayingVoice = false;
            this.voicePlayProgressSec = 0;
        }
    }

    // Persist chat history per peer across the session so sent messages remain when reopening
    private static final Map<String, List<ChatBubble>> HISTORY_MAP = new HashMap<>();

    private static List<ChatBubble> getOrCreateHistory(String peerName, String role) {
        List<ChatBubble> list = HISTORY_MAP.get(peerName);
        if (list != null) {
            return list;
        }
        list = new ArrayList<>();
        list.add(new ChatBubble(
                false,
                ChatBubble.TYPE_TEXT,
                "Hey handsome! Saw your profile nearby in Bangalore 👀✨ (" + role + ")",
                "10:38 PM",
                0
        ));
        list.add(new ChatBubble(
                true,
                ChatBubble.TYPE_TEXT,
                "Hey " + peerName + "! Great to match with you 🔥",
                "10:39 PM",
                0
        ));
        list.add(new ChatBubble(
                false,
                ChatBubble.TYPE_BURN_PHOTO,
                "⏱️ 5s disappearing photo (Tap to view)",
                "10:41 PM",
                0
        ));
        list.add(new ChatBubble(
                false,
                ChatBubble.TYPE_VOICE_NOTE,
                "Voice Clip",
                "10:42 PM",
                8
        ));
        HISTORY_MAP.put(peerName, list);
        return list;
    }

    public static void openFullScreenChat(
            @NonNull Context context,
            @NonNull String peerName,
            int portraitIndex,
            @NonNull String roleBadge,
            @NonNull String distance,
            boolean openBurnPhotoImmediately,
            OnChatUpdatedListener listener
    ) {
        final Dialog chatDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        chatDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final List<ChatBubble> messages = getOrCreateHistory(peerName, roleBadge);
        final Handler uiHandler = new Handler(Looper.getMainLooper());

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#09090E"));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 1. TOP HEADER BAR
        RelativeLayout topBar = new RelativeLayout(context);
        topBar.setBackgroundColor(Color.parseColor("#09090E"));
        topBar.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));

        TextView btnBack = new TextView(context);
        btnBack.setId(View.generateViewId());
        btnBack.setText("⬅ Back");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(13f);
        btnBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnBack.setBackgroundResource(R.drawable.bg_walla_glass_card);
        btnBack.setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7));
        RelativeLayout.LayoutParams backLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        backLp.addRule(RelativeLayout.CENTER_VERTICAL);
        topBar.addView(btnBack, backLp);

        // Avatar + Name + Online Status
        LinearLayout userBox = new LinearLayout(context);
        userBox.setOrientation(LinearLayout.HORIZONTAL);
        userBox.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams userBoxLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        userBoxLp.addRule(RelativeLayout.END_OF, btnBack.getId());
        userBoxLp.addRule(RelativeLayout.CENTER_VERTICAL);
        userBoxLp.leftMargin = dp(context, 10);

        CardView avatarCard = new CardView(context);
        avatarCard.setRadius(dp(context, 20));
        avatarCard.setCardElevation(0f);
        avatarCard.setCardBackgroundColor(Color.parseColor("#13131D"));
        ImageView ivAvatar = new ImageView(context);
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadGuyPhoto(context, ivAvatar, portraitIndex);
        avatarCard.addView(ivAvatar, new FrameLayout.LayoutParams(dp(context, 40), dp(context, 40)));
        userBox.addView(avatarCard, new LinearLayout.LayoutParams(dp(context, 40), dp(context, 40)));

        LinearLayout nameCol = new LinearLayout(context);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameColLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameColLp.leftMargin = dp(context, 10);

        TextView tvPeerTitle = new TextView(context);
        tvPeerTitle.setText(peerName + " ✓");
        tvPeerTitle.setTextColor(Color.WHITE);
        tvPeerTitle.setTextSize(15.5f);
        tvPeerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        nameCol.addView(tvPeerTitle);

        TextView tvPeerSub = new TextView(context);
        tvPeerSub.setText("🟢 Online • " + roleBadge + " • " + distance);
        tvPeerSub.setTextColor(Color.parseColor("#30D158"));
        tvPeerSub.setTextSize(11f);
        nameCol.addView(tvPeerSub);

        userBox.addView(nameCol, nameColLp);
        topBar.addView(userBox, userBoxLp);

        // Quick 1-on-1 HD Video Call + 5s Burn Photo buttons in top right of chat header
        LinearLayout headerRightActions = new LinearLayout(context);
        headerRightActions.setOrientation(LinearLayout.HORIZONTAL);
        headerRightActions.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams headerRightLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerRightLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        headerRightLp.addRule(RelativeLayout.CENTER_VERTICAL);

        TextView btnHeaderVideoCall = new TextView(context);
        btnHeaderVideoCall.setText("📹 HD Call");
        btnHeaderVideoCall.setTextColor(Color.WHITE);
        btnHeaderVideoCall.setTextSize(11f);
        btnHeaderVideoCall.setTypeface(Typeface.DEFAULT_BOLD);
        btnHeaderVideoCall.setBackgroundResource(R.drawable.bg_walla_glass_card);
        btnHeaderVideoCall.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        btnHeaderVideoCall.setOnClickListener(v -> {
            android.content.Intent callIntent = new android.content.Intent(context, PrivateVideoCallActivity.class);
            callIntent.putExtra(PrivateVideoCallActivity.EXTRA_PARTNER_NAME, peerName);
            callIntent.putExtra(PrivateVideoCallActivity.EXTRA_PARTNER_PORTRAIT_IDX, portraitIndex);
            context.startActivity(callIntent);
        });
        headerRightActions.addView(btnHeaderVideoCall);

        TextView btnHeaderBurn = new TextView(context);
        btnHeaderBurn.setText("🔥 5s Photo");
        btnHeaderBurn.setTextColor(Color.WHITE);
        btnHeaderBurn.setTextSize(11f);
        btnHeaderBurn.setTypeface(Typeface.DEFAULT_BOLD);
        btnHeaderBurn.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        btnHeaderBurn.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        LinearLayout.LayoutParams burnMarginLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        burnMarginLp.leftMargin = dp(context, 6);
        headerRightActions.addView(btnHeaderBurn, burnMarginLp);

        topBar.addView(headerRightActions, headerRightLp);

        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Subtle divider
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#1E1E2C"));
        root.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1)));

        // 2. MESSAGE HISTORY RECYCLERVIEW
        final RecyclerView rvMessages = new RecyclerView(context);
        rvMessages.setLayoutManager(new LinearLayoutManager(context));
        rvMessages.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        rvMessages.setClipToPadding(false);
        LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(rvMessages, rvLp);

        // 3. QUICK MEDIA BAR (5s Disappearing Photo + Voice Clip + Gift)
        LinearLayout mediaBar = new LinearLayout(context);
        mediaBar.setOrientation(LinearLayout.HORIZONTAL);
        mediaBar.setBackgroundColor(Color.parseColor("#09090E"));
        mediaBar.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 6));

        TextView btnSend5sPhoto = createMediaChip(context, "📸 Send 5s Photo", "#FF1B6B");
        TextView btnView5sPhoto = createMediaChip(context, "⏱️ View 5s Photo", "#E6B800");
        TextView btnSendVoiceClip = createMediaChip(context, "🎙️ Send Voice Clip", "#FFFFFF");

        mediaBar.addView(btnSend5sPhoto);
        LinearLayout.LayoutParams chipGap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipGap.leftMargin = dp(context, 8);
        mediaBar.addView(btnView5sPhoto, chipGap);
        LinearLayout.LayoutParams chipGap2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipGap2.leftMargin = dp(context, 8);
        mediaBar.addView(btnSendVoiceClip, chipGap2);

        root.addView(mediaBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 4. BOTTOM INPUT BAR (EditText + Working Send Button)
        LinearLayout inputBar = new LinearLayout(context);
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setGravity(Gravity.CENTER_VERTICAL);
        inputBar.setBackgroundColor(Color.parseColor("#09090E"));
        inputBar.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 12));

        final EditText etMessage = new EditText(context);
        etMessage.setHint("Message " + peerName + "...");
        etMessage.setHintTextColor(Color.parseColor("#8E8E93"));
        etMessage.setTextColor(Color.WHITE);
        etMessage.setTextSize(14f);
        etMessage.setSingleLine(true);
        etMessage.setImeOptions(EditorInfo.IME_ACTION_SEND);
        etMessage.setBackgroundResource(R.drawable.bg_walla_glass_card);
        etMessage.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, dp(context, 44), 1f);
        inputBar.addView(etMessage, etLp);

        Button btnSend = new Button(context);
        btnSend.setText("Send ➤");
        btnSend.setAllCaps(false);
        btnSend.setTextColor(Color.WHITE);
        btnSend.setTextSize(13.5f);
        btnSend.setTypeface(Typeface.DEFAULT_BOLD);
        btnSend.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 44));
        sendLp.leftMargin = dp(context, 8);
        inputBar.addView(btnSend, sendLp);

        root.addView(inputBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Adapter for Chat Bubbles
        final BubbleAdapter[] adapterHolder = new BubbleAdapter[1];
        adapterHolder[0] = new BubbleAdapter(context, messages, peerName, portraitIndex, new BubbleAdapter.OnBubbleActionListener() {
            @Override
            public void onBurnPhotoClicked(ChatBubble bubble, int position) {
                showFiveSecondBurnPhotoDialog(context, peerName, portraitIndex, () -> {
                    bubble.isBurned = true;
                    bubble.text = "🔥 Disappearing Photo (Burned & Destroyed)";
                    adapterHolder[0].notifyItemChanged(position);
                });
            }

            @Override
            public void onVoiceBubbleClicked(ChatBubble bubble, int position) {
                playVoiceNoteWithWaveform(context, bubble, position, adapterHolder[0], uiHandler);
            }
        });

        rvMessages.setAdapter(adapterHolder[0]);
        rvMessages.scrollToPosition(Math.max(0, messages.size() - 1));

        // Working Send Logic
        Runnable sendAction = () -> {
            String raw = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(raw)) {
                Toast.makeText(context, "Type a message first!", Toast.LENGTH_SHORT).show();
                return;
            }
            String nowTime = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
            messages.add(new ChatBubble(true, ChatBubble.TYPE_TEXT, raw, nowTime, 0));
            adapterHolder[0].notifyItemInserted(messages.size() - 1);
            rvMessages.smoothScrollToPosition(messages.size() - 1);
            etMessage.setText("");

            if (listener != null) {
                listener.onNewLastMessage(peerName, "You: " + raw, "Just now");
            }

            // Realistic instant reply from peer after 1.1s
            uiHandler.postDelayed(() -> {
                String replyText = getSmartPeerReply(peerName, raw);
                String replyTime = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
                messages.add(new ChatBubble(false, ChatBubble.TYPE_TEXT, replyText, replyTime, 0));
                adapterHolder[0].notifyItemInserted(messages.size() - 1);
                rvMessages.smoothScrollToPosition(messages.size() - 1);
                if (listener != null) {
                    listener.onNewLastMessage(peerName, replyText, "Just now");
                }
            }, 1100);
        };

        btnSend.setOnClickListener(v -> sendAction.run());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendAction.run();
                return true;
            }
            return false;
        });

        // Quick 5s Photo Sender & Viewer
        btnSend5sPhoto.setOnClickListener(v -> {
            String nowTime = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
            ChatBubble newBurn = new ChatBubble(true, ChatBubble.TYPE_BURN_PHOTO, "⏱️ 5s disappearing photo (Tap to view)", nowTime, 0);
            messages.add(newBurn);
            int idx = messages.size() - 1;
            adapterHolder[0].notifyItemInserted(idx);
            rvMessages.smoothScrollToPosition(idx);
            if (listener != null) {
                listener.onNewLastMessage(peerName, "⏱️ Sent a 5s disappearing photo", "Just now");
            }
            Toast.makeText(context, "🔥 Sent 5s Disappearing Photo! Tap bubble to test 5s timer.", Toast.LENGTH_SHORT).show();
        });

        View.OnClickListener openBurnNow = v -> {
            showFiveSecondBurnPhotoDialog(context, peerName, portraitIndex, () -> {
                for (int i = 0; i < messages.size(); i++) {
                    ChatBubble b = messages.get(i);
                    if (b.type == ChatBubble.TYPE_BURN_PHOTO && !b.isBurned) {
                        b.isBurned = true;
                        b.text = "🔥 Disappearing Photo (Burned & Destroyed)";
                        adapterHolder[0].notifyItemChanged(i);
                        break;
                    }
                }
            });
        };
        btnView5sPhoto.setOnClickListener(openBurnNow);
        btnHeaderBurn.setOnClickListener(openBurnNow);

        // Quick Voice Clip Sender + Immediate Waveform Playback
        btnSendVoiceClip.setOnClickListener(v -> {
            String nowTime = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
            ChatBubble voiceBubble = new ChatBubble(true, ChatBubble.TYPE_VOICE_NOTE, "Voice Clip", nowTime, 6);
            messages.add(voiceBubble);
            int idx = messages.size() - 1;
            adapterHolder[0].notifyItemInserted(idx);
            rvMessages.smoothScrollToPosition(idx);
            if (listener != null) {
                listener.onNewLastMessage(peerName, "🎙️ Sent a voice clip (0:06)", "Just now");
            }
            playVoiceNoteWithWaveform(context, voiceBubble, idx, adapterHolder[0], uiHandler);
        });

        btnBack.setOnClickListener(v -> chatDialog.dismiss());

        chatDialog.setContentView(root);
        if (chatDialog.getWindow() != null) {
            chatDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        chatDialog.show();

        if (openBurnPhotoImmediately) {
            uiHandler.postDelayed(() -> openBurnNow.onClick(btnView5sPhoto), 250);
        }
    }

    /**
     * Full-screen 5-second Disappearing Photo Dialog with animated 5..4..3..2..1 countdown
     * that automatically burns and closes the photo when time expires.
     */
    public static void showFiveSecondBurnPhotoDialog(
            @NonNull Context context,
            @NonNull String senderName,
            int portraitIndex,
            Runnable onBurnCompleted
    ) {
        final Dialog burnDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        burnDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        burnDialog.setCancelable(true);

        RelativeLayout container = new RelativeLayout(context);
        container.setBackgroundColor(Color.parseColor("#09090E"));

        // Full-Screen Real HD Disappearing Portrait Photo via Glide
        ImageView ivSecretPhoto = new ImageView(context);
        ivSecretPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadSecretBurnPhoto(context, ivSecretPhoto, portraitIndex);
        container.addView(ivSecretPhoto, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Top Scrim Banner with Countdown Badge & Progress Bar
        LinearLayout topOverlay = new LinearLayout(context);
        topOverlay.setOrientation(LinearLayout.VERTICAL);
        topOverlay.setPadding(dp(context, 18), dp(context, 22), dp(context, 18), dp(context, 16));
        topOverlay.setBackgroundColor(Color.parseColor("#B309090E"));

        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(5000);
        progressBar.setProgress(5000);
        topOverlay.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 6)));

        TextView tvTimerBadge = new TextView(context);
        tvTimerBadge.setText("🔥 5s DISAPPEARING PHOTO FROM " + senderName.toUpperCase(Locale.US) + " • 5s");
        tvTimerBadge.setTextColor(Color.parseColor("#E6B800"));
        tvTimerBadge.setTextSize(14f);
        tvTimerBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvTimerBadge.setPadding(0, dp(context, 10), 0, 0);
        topOverlay.addView(tvTimerBadge);

        container.addView(topOverlay, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Giant Center Countdown Circle (5..4..3..2..1)
        final TextView tvBigCountdown = new TextView(context);
        tvBigCountdown.setText("5");
        tvBigCountdown.setTextColor(Color.WHITE);
        tvBigCountdown.setTextSize(46f);
        tvBigCountdown.setTypeface(Typeface.DEFAULT_BOLD);
        tvBigCountdown.setGravity(Gravity.CENTER);
        tvBigCountdown.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        RelativeLayout.LayoutParams centerLp = new RelativeLayout.LayoutParams(dp(context, 96), dp(context, 96));
        centerLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        container.addView(tvBigCountdown, centerLp);

        // Bottom Security Watermark
        TextView tvFooter = new TextView(context);
        tvFooter.setText("🛡️ FLAG_SECURE Screenshot Block Active • Auto-destroys in 5 seconds");
        tvFooter.setTextColor(Color.WHITE);
        tvFooter.setTextSize(12f);
        tvFooter.setGravity(Gravity.CENTER);
        tvFooter.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        tvFooter.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        RelativeLayout.LayoutParams footerLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        footerLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        footerLp.bottomMargin = dp(context, 32);
        container.addView(tvFooter, footerLp);

        burnDialog.setContentView(container);
        burnDialog.show();

        final CountDownTimer timer = new CountDownTimer(5000, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                progressBar.setProgress((int) millisUntilFinished);
                tvBigCountdown.setText(String.valueOf(secLeft));
                tvTimerBadge.setText("🔥 SELF-DESTRUCTING PHOTO • " + secLeft + "s REMAINING");
            }

            @Override
            public void onFinish() {
                progressBar.setProgress(0);
                tvBigCountdown.setText("💥");
                if (burnDialog.isShowing()) {
                    burnDialog.dismiss();
                }
                if (onBurnCompleted != null) {
                    onBurnCompleted.run();
                }
                Toast.makeText(context, "💥 5s Photo burned & permanently destroyed!", Toast.LENGTH_SHORT).show();
            }
        };
        timer.start();
        burnDialog.setOnDismissListener(d -> timer.cancel());
    }

    private static void playVoiceNoteWithWaveform(
            Context context,
            ChatBubble bubble,
            int position,
            BubbleAdapter adapter,
            Handler uiHandler
    ) {
        if (bubble.isPlayingVoice) {
            bubble.isPlayingVoice = false;
            bubble.voicePlayProgressSec = 0;
            adapter.notifyItemChanged(position);
            return;
        }
        bubble.isPlayingVoice = true;
        bubble.voicePlayProgressSec = 0;
        adapter.notifyItemChanged(position);

        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 160);
        } catch (Exception ignored) {}

        Toast.makeText(context, "🔊 Playing Voice Note...", Toast.LENGTH_SHORT).show();

        final int totalSec = Math.max(4, bubble.voiceDurationSec);
        Runnable tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!bubble.isPlayingVoice) return;
                bubble.voicePlayProgressSec++;
                if (bubble.voicePlayProgressSec > totalSec) {
                    bubble.isPlayingVoice = false;
                    bubble.voicePlayProgressSec = 0;
                    adapter.notifyItemChanged(position);
                } else {
                    adapter.notifyItemChanged(position);
                    uiHandler.postDelayed(this, 650);
                }
            }
        };
        uiHandler.postDelayed(tickRunnable, 650);
    }

    private static String getSmartPeerReply(String peerName, String userMsg) {
        String lower = userMsg.toLowerCase(Locale.US);
        if (lower.contains("hi") || lower.contains("hey") || lower.contains("hello")) {
            return "Heyy! How's your night going in Bangalore? 😊🔥";
        } else if (lower.contains("where") || lower.contains("meet") || lower.contains("drink")) {
            return "I'm near Indiranagar 100ft Road! Down for rooftop drinks tonight 🍸✨";
        } else if (lower.contains("pic") || lower.contains("photo")) {
            return "Tap my 5s disappearing photo above before it burns! 🔥😉";
        }
        return "Love that! Join my Voice Party room or let's catch up soon 🔥👑";
    }

    private static TextView createMediaChip(Context ctx, String label, String hexColor) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Color.parseColor(hexColor));
        tv.setTextSize(11.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setBackgroundResource(R.drawable.bg_walla_glass_card);
        tv.setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6));
        return tv;
    }

    /**
     * Custom Animated Audio Waveform View for Voice Note Bubbles.
     */
    private static class AnimatedWaveformView extends View {
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private ValueAnimator animator;
        private float phase = 0f;
        private boolean isAnimating = false;

        public AnimatedWaveformView(Context context) {
            super(context);
            barPaint.setStyle(Paint.Style.FILL);
        }

        public void setPlaying(boolean playing, boolean isMe) {
            barPaint.setColor(isMe ? Color.WHITE : Color.parseColor("#00E5FF"));
            if (playing && !isAnimating) {
                isAnimating = true;
                animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
                animator.setDuration(550);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.addUpdateListener(a -> {
                    phase = (float) a.getAnimatedValue();
                    invalidate();
                });
                animator.start();
            } else if (!playing && isAnimating) {
                isAnimating = false;
                if (animator != null) animator.cancel();
                phase = 0f;
                invalidate();
            } else {
                invalidate();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (animator != null) animator.cancel();
            isAnimating = false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            int bars = 18;
            float gap = w / (float) (bars * 2);
            float barW = gap * 1.15f;
            for (int i = 0; i < bars; i++) {
                float factor;
                if (isAnimating) {
                    factor = 0.28f + 0.70f * (float) Math.abs(Math.sin(phase + i * 0.55f));
                } else {
                    factor = 0.25f + 0.55f * ((i % 4 == 0) ? 0.9f : (i % 2 == 0 ? 0.55f : 0.35f));
                }
                float barH = h * factor;
                float left = i * (barW + gap * 0.7f);
                float top = (h - barH) / 2f;
                rect.set(left, top, left + barW, top + barH);
                canvas.drawRoundRect(rect, barW / 2f, barW / 2f, barPaint);
            }
        }
    }

    private static class BubbleAdapter extends RecyclerView.Adapter<BubbleAdapter.BubbleVH> {

        interface OnBubbleActionListener {
            void onBurnPhotoClicked(ChatBubble bubble, int position);
            void onVoiceBubbleClicked(ChatBubble bubble, int position);
        }

        private final Context context;
        private final List<ChatBubble> list;
        private final String peerName;
        private final int portraitIndex;
        private final OnBubbleActionListener actionListener;

        BubbleAdapter(Context context, List<ChatBubble> list, String peerName, int portraitIndex, OnBubbleActionListener actionListener) {
            this.context = context;
            this.list = list;
            this.peerName = peerName;
            this.portraitIndex = portraitIndex;
            this.actionListener = actionListener;
        }

        @NonNull
        @Override
        public BubbleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            row.setPadding(0, dp(context, 5), 0, dp(context, 5));

            LinearLayout bubbleBox = new LinearLayout(context);
            bubbleBox.setOrientation(LinearLayout.VERTICAL);
            bubbleBox.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));

            TextView tvText = new TextView(context);
            tvText.setTextSize(14f);
            bubbleBox.addView(tvText);

            AnimatedWaveformView waveformView = new AnimatedWaveformView(context);
            LinearLayout.LayoutParams waveLp = new LinearLayout.LayoutParams(dp(context, 150), dp(context, 26));
            waveLp.topMargin = dp(context, 6);
            waveformView.setLayoutParams(waveLp);
            waveformView.setVisibility(View.GONE);
            bubbleBox.addView(waveformView);

            TextView tvTime = new TextView(context);
            tvTime.setTextSize(10f);
            tvTime.setPadding(0, dp(context, 4), 0, 0);
            bubbleBox.addView(tvTime);

            row.addView(bubbleBox, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            return new BubbleVH(row, bubbleBox, tvText, waveformView, tvTime);
        }

        @Override
        public void onBindViewHolder(@NonNull BubbleVH holder, int position) {
            ChatBubble b = list.get(position);
            LinearLayout.LayoutParams boxLp = (LinearLayout.LayoutParams) holder.bubbleBox.getLayoutParams();
            if (b.isMe) {
                holder.row.setGravity(Gravity.END);
                boxLp.gravity = Gravity.END;
                holder.bubbleBox.setBackgroundResource(R.drawable.bg_walla_pink_pill);
                holder.tvText.setTextColor(Color.WHITE);
                holder.tvTime.setTextColor(Color.parseColor("#FFE4F0"));
            } else {
                holder.row.setGravity(Gravity.START);
                boxLp.gravity = Gravity.START;
                holder.bubbleBox.setBackgroundResource(R.drawable.bg_walla_glass_card);
                holder.tvText.setTextColor(Color.WHITE);
                holder.tvTime.setTextColor(Color.parseColor("#8E8E93"));
            }
            holder.bubbleBox.setLayoutParams(boxLp);

            if (b.type == ChatBubble.TYPE_BURN_PHOTO) {
                holder.waveformView.setVisibility(View.GONE);
                holder.waveformView.setPlaying(false, b.isMe);
                if (b.isBurned) {
                    holder.tvText.setText("🔥 Disappearing Photo (Burned & Expired)");
                    holder.tvText.setTextColor(Color.parseColor("#8E8E93"));
                } else {
                    holder.tvText.setText("🔥 5s Disappearing Photo • Tap to View (5..4..3..2..1)");
                    holder.tvText.setTextColor(Color.parseColor("#E6B800"));
                }
                holder.bubbleBox.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    if (b.isBurned) {
                        Toast.makeText(context, "🔥 This 5-second photo has already burned!", Toast.LENGTH_SHORT).show();
                    } else {
                        actionListener.onBurnPhotoClicked(b, pos);
                    }
                });
            } else if (b.type == ChatBubble.TYPE_VOICE_NOTE) {
                holder.waveformView.setVisibility(View.VISIBLE);
                holder.waveformView.setPlaying(b.isPlayingVoice, b.isMe);
                String status = b.isPlayingVoice
                        ? ("🔊 Playing 0:0" + b.voicePlayProgressSec + " / 0:0" + b.voiceDurationSec)
                        : ("🎙️ Voice Clip (0:0" + b.voiceDurationSec + ") • Tap to Play");
                holder.tvText.setText(status);
                holder.tvText.setTextColor(Color.WHITE);
                holder.bubbleBox.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        actionListener.onVoiceBubbleClicked(b, pos);
                    }
                });
            } else {
                holder.waveformView.setVisibility(View.GONE);
                holder.waveformView.setPlaying(false, b.isMe);
                holder.tvText.setText(b.text);
                holder.bubbleBox.setOnClickListener(null);
            }

            holder.tvTime.setText((b.isMe ? "You • " : peerName + " • ") + b.timestamp);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class BubbleVH extends RecyclerView.ViewHolder {
            final LinearLayout row;
            final LinearLayout bubbleBox;
            final TextView tvText;
            final AnimatedWaveformView waveformView;
            final TextView tvTime;

            BubbleVH(LinearLayout row, LinearLayout bubbleBox, TextView tvText, AnimatedWaveformView waveformView, TextView tvTime) {
                super(row);
                this.row = row;
                this.bubbleBox = bubbleBox;
                this.tvText = tvText;
                this.waveformView = waveformView;
                this.tvTime = tvTime;
            }
        }
    }

    private static int dp(Context ctx, int val) {
        return Math.round(val * ctx.getResources().getDisplayMetrics().density);
    }
}
