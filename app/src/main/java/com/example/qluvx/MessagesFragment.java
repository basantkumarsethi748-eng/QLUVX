package com.example.qluvx;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class MessagesFragment extends Fragment {

    private CountDownTimer activeTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        TextView itemSystemAlert = view.findViewById(R.id.itemSystemAlert);
        LinearLayout llMutualMatchesRow = view.findViewById(R.id.llMutualMatchesRow);
        LinearLayout llChatListContainer = view.findViewById(R.id.llChatListContainer);

        if (itemSystemAlert != null) {
            itemSystemAlert.setOnClickListener(v ->
                    Toast.makeText(getContext(), "🛡️ FLAG_SECURE Screenshot Protection Active for all 5s Disappearing Photos!", Toast.LENGTH_SHORT).show()
            );
        }

        populateMutualMatchesRow(llMutualMatchesRow);
        populateWallaChatInbox(llChatListContainer);

        return view;
    }

    private void populateMutualMatchesRow(LinearLayout container) {
        Context ctx = getContext();
        if (ctx == null || container == null) return;
        container.removeAllViews();

        Object[][] matches = new Object[][]{
                {"Alex, 25", 0, true},
                {"Ryan, 28", 1, true},
                {"Kabir, 24", 2, true},
                {"Zayn, 26", 3, true},
                {"Liam, 23", 4, true},
                {"Aarav, 25", 6, true},
                {"Vikram, 28", 9, true}
        };

        for (Object[] m : matches) {
            String name = (String) m[0];
            int portraitIdx = (Integer) m[1];
            boolean online = (Boolean) m[2];

            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(68), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            box.setLayoutParams(lp);

            FrameLayout avatarWrap = new FrameLayout(ctx);
            avatarWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));

            CardView card = new CardView(ctx);
            card.setLayoutParams(new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER));
            card.setRadius(dp(26));
            card.setCardElevation(0f);

            ImageView iv = new ImageView(ctx);
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, portraitIdx));
            card.addView(iv);
            avatarWrap.addView(card);

            if (online) {
                View dot = new View(ctx);
                FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(12), dp(12), Gravity.BOTTOM | Gravity.END);
                dotLp.rightMargin = dp(2);
                dotLp.bottomMargin = dp(2);
                dot.setLayoutParams(dotLp);
                dot.setBackgroundResource(R.drawable.bg_online_dot_glow);
                avatarWrap.addView(dot);
            }

            box.addView(avatarWrap);

            TextView tvName = new TextView(ctx);
            tvName.setText(name);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(10.5f);
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            tvName.setSingleLine(true);
            tvName.setPadding(0, dp(3), 0, 0);
            box.addView(tvName);

            box.setOnClickListener(v -> showOneOnOneChatDialog(name, portraitIdx, "120m", true));
            container.addView(box);
        }
    }

    private void populateWallaChatInbox(LinearLayout container) {
        Context ctx = getContext();
        if (ctx == null || container == null) return;
        container.removeAllViews();

        // [nameAge, distance, role, portraitIdx, isOnline, previewTag, timeAgo, unreadCount, isDisappearingPhoto]
        Object[][] chats = new Object[][]{
                {"Alex, 25", "120m", "Top", 0, true, "⏱️ 5s disappearing photo • Tap to view", "Just now", 2, true},
                {"Ryan, 28", "450m", "Versatile", 1, true, "⏱️ 5s disappearing photo • View-once", "1m ago", 1, true},
                {"Kabir, 24", "680m", "Bottom", 2, true, "🎙️ Voice Note (0:14) • Hey, join our party room!", "4m ago", 1, false},
                {"Zayn, 26", "1.2km", "Top", 3, true, "⏱️ 5s disappearing photo • Screenshot protected", "9m ago", 1, true},
                {"Liam, 23", "1.5km", "Versatile", 4, true, "🔥 Sent you a Super Sports Car in Live PK!", "18m ago", 0, false},
                {"Aarav, 25", "2.1km", "Top", 6, true, "👋 Hey handsome, free for coffee in Indiranagar?", "32m ago", 0, false},
                {"Vikram, 28", "3.2km", "Top", 9, false, "🌹 Sent Neon Rose x10 • Let's match!", "1h ago", 0, false}
        };

        for (Object[] c : chats) {
            String nameAge = (String) c[0];
            String dist = (String) c[1];
            String role = (String) c[2];
            int portraitIdx = (Integer) c[3];
            boolean isOnline = (Boolean) c[4];
            String preview = (String) c[5];
            String timeAgo = (String) c[6];
            int unread = (Integer) c[7];
            boolean isDisappearing = (Boolean) c[8];

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_walla_glass_card);
            int pad = dp(12);
            row.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);

            // Avatar + Online Green Dot
            FrameLayout avatarWrap = new FrameLayout(ctx);
            avatarWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));

            CardView avatarCard = new CardView(ctx);
            avatarCard.setLayoutParams(new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER));
            avatarCard.setRadius(dp(26));
            avatarCard.setCardElevation(0f);

            ImageView ivAvatar = new ImageView(ctx);
            ivAvatar.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivAvatar.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, portraitIdx));
            avatarCard.addView(ivAvatar);
            avatarWrap.addView(avatarCard);

            if (isOnline) {
                View onlineDot = new View(ctx);
                FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(13), dp(13), Gravity.BOTTOM | Gravity.END);
                dotLp.rightMargin = dp(2);
                dotLp.bottomMargin = dp(2);
                onlineDot.setLayoutParams(dotLp);
                onlineDot.setBackgroundResource(R.drawable.bg_online_dot_glow);
                avatarWrap.addView(onlineDot);
            }
            row.addView(avatarWrap);

            // Center Text Info
            LinearLayout centerCol = new LinearLayout(ctx);
            centerCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            centerLp.setMarginStart(dp(12));
            centerCol.setLayoutParams(centerLp);

            LinearLayout titleLine = new LinearLayout(ctx);
            titleLine.setOrientation(LinearLayout.HORIZONTAL);
            titleLine.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(ctx);
            tvName.setText(nameAge);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(14.5f);
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            titleLine.addView(tvName);

            TextView tvTick = new TextView(ctx);
            LinearLayout.LayoutParams tickLp = new LinearLayout.LayoutParams(dp(14), dp(14));
            tickLp.setMarginStart(dp(5));
            tvTick.setLayoutParams(tickLp);
            tvTick.setBackgroundResource(R.drawable.bg_verified_tick);
            tvTick.setGravity(Gravity.CENTER);
            tvTick.setText("✓");
            tvTick.setTextColor(Color.parseColor("#0A0A14"));
            tvTick.setTextSize(8.5f);
            tvTick.setTypeface(Typeface.DEFAULT_BOLD);
            titleLine.addView(tvTick);

            TextView tvRole = new TextView(ctx);
            LinearLayout.LayoutParams roleLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            roleLp.setMarginStart(dp(6));
            tvRole.setLayoutParams(roleLp);
            tvRole.setText(role + " • " + dist);
            tvRole.setTextColor(Color.parseColor("#00E5FF"));
            tvRole.setTextSize(10.5f);
            tvRole.setTypeface(Typeface.DEFAULT_BOLD);
            titleLine.addView(tvRole);

            centerCol.addView(titleLine);

            // Preview Tag ("⏱️ 5s disappearing photo")
            final TextView tvPreview = new TextView(ctx);
            tvPreview.setText(preview);
            tvPreview.setTextSize(12f);
            tvPreview.setSingleLine(true);
            tvPreview.setPadding(0, dp(4), 0, 0);
            if (isDisappearing) {
                tvPreview.setTextColor(Color.parseColor("#FF2A7A"));
                tvPreview.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                tvPreview.setTextColor(Color.parseColor("#B8B8D8"));
            }
            centerCol.addView(tvPreview);
            row.addView(centerCol);

            // Right Column: Time & Unread Pill
            LinearLayout rightCol = new LinearLayout(ctx);
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            TextView tvTime = new TextView(ctx);
            tvTime.setText(timeAgo);
            tvTime.setTextColor(Color.parseColor("#8A8AA8"));
            tvTime.setTextSize(10.5f);
            rightCol.addView(tvTime);

            if (unread > 0) {
                TextView tvUnread = new TextView(ctx);
                LinearLayout.LayoutParams unreadLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(19));
                unreadLp.topMargin = dp(5);
                tvUnread.setLayoutParams(unreadLp);
                tvUnread.setBackgroundResource(R.drawable.bg_walla_pink_pill);
                tvUnread.setGravity(Gravity.CENTER);
                tvUnread.setPadding(dp(7), 0, dp(7), 0);
                tvUnread.setText(String.valueOf(unread));
                tvUnread.setTextColor(Color.WHITE);
                tvUnread.setTextSize(10f);
                tvUnread.setTypeface(Typeface.DEFAULT_BOLD);
                rightCol.addView(tvUnread);
            }

            row.addView(rightCol);

            row.setOnClickListener(v -> {
                if (isDisappearing) {
                    showFiveSecondDisappearingPhotoDialog(nameAge, portraitIdx, tvPreview);
                } else {
                    showOneOnOneChatDialog(nameAge, portraitIdx, dist, false);
                }
            });

            container.addView(row);
        }
    }

    private void showFiveSecondDisappearingPhotoDialog(String senderName, int portraitIdx, @Nullable TextView tvStatus) {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(20);
        box.setPadding(pad, pad, pad, pad);

        TextView tvTimer = new TextView(ctx);
        tvTimer.setText("⏱️ 5s Disappearing Photo • 5s left");
        tvTimer.setTextColor(Color.parseColor("#FF2A7A"));
        tvTimer.setTextSize(16f);
        tvTimer.setTypeface(Typeface.DEFAULT_BOLD);
        tvTimer.setPadding(0, 0, 0, dp(8));
        box.addView(tvTimer);

        ProgressBar progressBar = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        progressBar.setMax(5000);
        progressBar.setProgress(5000);
        box.addView(progressBar);

        CardView photoCard = new CardView(ctx);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(240), dp(300));
        cardLp.topMargin = dp(12);
        photoCard.setLayoutParams(cardLp);
        photoCard.setRadius(dp(16));

        ImageView ivSecretPhoto = new ImageView(ctx);
        ivSecretPhoto.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivSecretPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivSecretPhoto.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, portraitIdx));
        photoCard.addView(ivSecretPhoto);
        box.addView(photoCard);

        TextView tvNote = new TextView(ctx);
        tvNote.setText("🔒 FLAG_SECURE Anti-Screenshot Active • Sent by " + senderName);
        tvNote.setTextColor(Color.parseColor("#F1C40F"));
        tvNote.setTextSize(11.5f);
        tvNote.setTypeface(Typeface.DEFAULT_BOLD);
        tvNote.setPadding(0, dp(12), 0, 0);
        box.addView(tvNote);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(box)
                .setCancelable(true)
                .create();

        dialog.show();

        if (activeTimer != null) {
            activeTimer.cancel();
        }

        activeTimer = new CountDownTimer(5000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = (millisUntilFinished / 1000) + 1;
                tvTimer.setText("⏱️ 5s Disappearing Photo • " + sec + "s left...");
                progressBar.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (tvStatus != null) {
                    tvStatus.setText("🔥 5s Disappearing Photo Expired (Viewed)");
                    tvStatus.setTextColor(Color.parseColor("#8A8AA8"));
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "💥 5s Photo Self-Destructed!", Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }

    private void showOneOnOneChatDialog(String peerName, int portraitIdx, String distance, boolean canSend5sPhoto) {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView tvHeader = new TextView(ctx);
        tvHeader.setText("💬 Chat with " + peerName + " (" + distance + ")");
        tvHeader.setTextColor(Color.parseColor("#00E5FF"));
        tvHeader.setTextSize(16f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvHeader);

        final LinearLayout chatHistory = new LinearLayout(ctx);
        chatHistory.setOrientation(LinearLayout.VERTICAL);
        chatHistory.setPadding(0, dp(12), 0, dp(12));

        TextView msg1 = new TextView(ctx);
        msg1.setText(peerName + ": Hey! Saw you on the Bangalore Nearby grid 🔥");
        msg1.setTextColor(Color.WHITE);
        msg1.setTextSize(13f);
        chatHistory.addView(msg1);

        TextView msg2 = new TextView(ctx);
        msg2.setText(peerName + ": 🎙️ Sent a 0:14 Voice Note • Tap to play");
        msg2.setTextColor(Color.parseColor("#F1C40F"));
        msg2.setTextSize(12.5f);
        msg2.setPadding(0, dp(6), 0, 0);
        chatHistory.addView(msg2);

        root.addView(chatHistory);

        final EditText etReply = new EditText(ctx);
        etReply.setHint("Write a message to " + peerName + "...");
        etReply.setTextColor(Color.WHITE);
        etReply.setHintTextColor(Color.parseColor("#7A7A9A"));
        etReply.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        etReply.setPadding(dp(14), dp(10), dp(14), dp(10));
        root.addView(etReply);

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(10), 0, 0);

        Button btnSend5s = new Button(ctx);
        btnSend5s.setLayoutParams(new LinearLayout.LayoutParams(0, dp(42), 1f));
        btnSend5s.setText("⏱️ Send 5s Photo");
        btnSend5s.setAllCaps(false);
        btnSend5s.setBackgroundColor(Color.parseColor("#FF2A7A"));
        btnSend5s.setTextColor(Color.WHITE);

        View spacer = new View(ctx);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(1)));

        Button btnSendText = new Button(ctx);
        btnSendText.setLayoutParams(new LinearLayout.LayoutParams(0, dp(42), 1f));
        btnSendText.setText("Send Reply ➤");
        btnSendText.setAllCaps(false);
        btnSendText.setBackgroundColor(Color.parseColor("#00E5FF"));
        btnSendText.setTextColor(Color.parseColor("#0A0A14"));

        btnRow.addView(btnSend5s);
        btnRow.addView(spacer);
        btnRow.addView(btnSendText);
        root.addView(btnRow);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(root)
                .setNegativeButton("Close", null)
                .create();

        btnSend5s.setOnClickListener(v -> {
            dialog.dismiss();
            showFiveSecondDisappearingPhotoDialog(peerName, portraitIdx, null);
        });

        btnSendText.setOnClickListener(v -> {
            String txt = etReply.getText().toString().trim();
            if (txt.isEmpty()) txt = "Hey! Let's hang out tonight 🔥";
            TextView myMsg = new TextView(ctx);
            myMsg.setText("You: " + txt);
            myMsg.setTextColor(Color.parseColor("#00E676"));
            myMsg.setTextSize(13f);
            myMsg.setPadding(0, dp(6), 0, 0);
            chatHistory.addView(myMsg);
            etReply.setText("");
        });

        dialog.show();
    }

    private int dp(int val) {
        if (getContext() == null) return val;
        return Math.round(val * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (activeTimer != null) {
            activeTimer.cancel();
        }
        super.onDestroyView();
    }
}
