package com.example.qluvx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class MessagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        TextView itemSystemAlert = view.findViewById(R.id.itemSystemAlert);
        TextView tvUnreadTotalBadge = view.findViewById(R.id.tvUnreadTotalBadge);
        LinearLayout llMutualMatchesRow = view.findViewById(R.id.llMutualMatchesRow);
        LinearLayout llChatListContainer = view.findViewById(R.id.llChatListContainer);

        if (itemSystemAlert != null) {
            itemSystemAlert.setOnClickListener(v -> {
                if (getContext() != null) {
                    WallaChatOverlay.showFiveSecondBurnPhotoDialog(
                            getContext(),
                            "Alex, 25",
                            0,
                            () -> itemSystemAlert.setText("🔥 5s View-Once Photo Destroyed • FLAG_SECURE Active")
                    );
                }
            });
        }

        if (tvUnreadTotalBadge != null) {
            tvUnreadTotalBadge.setOnClickListener(v -> {
                tvUnreadTotalBadge.setText("✓ All Read");
                Toast.makeText(getContext(), "✅ Marked all messages as read", Toast.LENGTH_SHORT).show();
            });
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
                {"Alex, 25", 0, "Top", "120m", true},
                {"Ryan, 28", 1, "Versatile", "450m", true},
                {"Kabir, 24", 2, "Bottom", "680m", true},
                {"Zayn, 26", 3, "Top", "1.2km", true},
                {"Liam, 23", 4, "Versatile", "1.5km", true},
                {"Aarav, 25", 6, "Top", "2.1km", true},
                {"Vikram, 28", 9, "Top", "3.2km", true}
        };

        for (Object[] m : matches) {
            String name = (String) m[0];
            int portraitIdx = (Integer) m[1];
            String role = (String) m[2];
            String dist = (String) m[3];
            boolean online = (Boolean) m[4];

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
            WallaPortraitStudio.loadGuyPhoto(ctx, iv, portraitIdx);
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

            box.setOnClickListener(v ->
                    WallaChatOverlay.openFullScreenChat(ctx, name, portraitIdx, role, dist, false, null)
            );
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
                {"Kabir, 24", "680m", "Bottom", 2, true, "🎙️ Voice Note (0:08) • Hey, join our party room!", "4m ago", 1, false},
                {"Zayn, 26", "1.2km", "Top", 3, true, "⏱️ 5s disappearing photo • Screenshot protected", "9m ago", 1, true},
                {"Liam, 23", "1.5km", "Versatile", 4, true, "🔥 Sent you a Super Sports Car in Live PK!", "18m ago", 0, false},
                {"Aarav, 25", "2.1km", "Top", 6, true, "👋 Hey handsome, free for coffee in Indiranagar?", "32m ago", 0, false},
                {"Vikram, 28", "3.2km", "Top", 9, false, "🌹 Sent Neon Rose x10 • Let's match!", "1h ago", 0, false}
        };

        for (Object[] c : chats) {
            final String nameAge = (String) c[0];
            final String dist = (String) c[1];
            final String role = (String) c[2];
            final int portraitIdx = (Integer) c[3];
            final boolean isOnline = (Boolean) c[4];
            final String preview = (String) c[5];
            final String timeAgo = (String) c[6];
            final int unread = (Integer) c[7];
            final boolean isDisappearing = (Boolean) c[8];

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
            WallaPortraitStudio.loadGuyPhoto(ctx, ivAvatar, portraitIdx);
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

            // Action chips inside row so user can directly tap "5s disappearing photo" OR open full chat
            LinearLayout actionChipRow = new LinearLayout(ctx);
            actionChipRow.setOrientation(LinearLayout.HORIZONTAL);
            actionChipRow.setPadding(0, dp(5), 0, 0);

            if (isDisappearing) {
                final TextView btn5sChip = new TextView(ctx);
                btn5sChip.setText("⏱️ 5s disappearing photo");
                btn5sChip.setTextColor(Color.WHITE);
                btn5sChip.setTextSize(10.5f);
                btn5sChip.setTypeface(Typeface.DEFAULT_BOLD);
                btn5sChip.setBackgroundResource(R.drawable.bg_walla_pink_pill);
                btn5sChip.setPadding(dp(9), dp(3), dp(9), dp(3));
                btn5sChip.setOnClickListener(v -> {
                    WallaChatOverlay.showFiveSecondBurnPhotoDialog(ctx, nameAge, portraitIdx, () -> {
                        tvPreview.setText("🔥 5s Disappearing Photo (Burned & Destroyed)");
                        tvPreview.setTextColor(Color.parseColor("#8A8AA8"));
                        btn5sChip.setText("🔥 Burned");
                        btn5sChip.setBackgroundResource(R.drawable.bg_walla_pill_dark);
                    });
                });
                actionChipRow.addView(btn5sChip);
            }

            TextView btnOpenChatChip = new TextView(ctx);
            btnOpenChatChip.setText("💬 Open Chat");
            btnOpenChatChip.setTextColor(Color.parseColor("#00E5FF"));
            btnOpenChatChip.setTextSize(10.5f);
            btnOpenChatChip.setTypeface(Typeface.DEFAULT_BOLD);
            btnOpenChatChip.setBackgroundResource(R.drawable.bg_walla_pill_dark);
            btnOpenChatChip.setPadding(dp(9), dp(3), dp(9), dp(3));
            LinearLayout.LayoutParams openChatLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (isDisappearing) {
                openChatLp.leftMargin = dp(6);
            }
            btnOpenChatChip.setLayoutParams(openChatLp);
            actionChipRow.addView(btnOpenChatChip);

            centerCol.addView(actionChipRow);
            row.addView(centerCol);

            // Right Column: Time & Unread Pill
            LinearLayout rightCol = new LinearLayout(ctx);
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            final TextView tvTime = new TextView(ctx);
            tvTime.setText(timeAgo);
            tvTime.setTextColor(Color.parseColor("#8A8AA8"));
            tvTime.setTextSize(10.5f);
            rightCol.addView(tvTime);

            final TextView tvUnread = new TextView(ctx);
            if (unread > 0) {
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

            // Tapping the message item opens the full-screen interactive Chat Screen
            View.OnClickListener openChatListener = v -> {
                tvUnread.setVisibility(View.GONE);
                WallaChatOverlay.openFullScreenChat(
                        ctx,
                        nameAge,
                        portraitIdx,
                        role,
                        dist,
                        false,
                        (peer, previewText, timestamp) -> {
                            tvPreview.setText(previewText);
                            tvPreview.setTextColor(Color.parseColor("#00E676"));
                            tvTime.setText(timestamp);
                        }
                );
            };

            row.setOnClickListener(openChatListener);
            btnOpenChatChip.setOnClickListener(openChatListener);

            // Tapping directly on the "⏱️ 5s disappearing photo" text also opens the 5s countdown photo dialog
            if (isDisappearing) {
                tvPreview.setOnClickListener(v -> {
                    WallaChatOverlay.showFiveSecondBurnPhotoDialog(ctx, nameAge, portraitIdx, () -> {
                        tvPreview.setText("🔥 5s Disappearing Photo (Burned & Destroyed)");
                        tvPreview.setTextColor(Color.parseColor("#8A8AA8"));
                    });
                });
            }

            container.addView(row);
        }
    }

    private int dp(int val) {
        if (getContext() == null) return val;
        return Math.round(val * getContext().getResources().getDisplayMetrics().density);
    }
}
