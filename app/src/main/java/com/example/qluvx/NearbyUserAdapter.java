package com.example.qluvx;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NearbyUserAdapter extends RecyclerView.Adapter<NearbyUserAdapter.ViewHolder> {

    private final Context context;
    private final List<HomeFragment.NearbyUser> users;
    private final Set<String> followedUsers = new HashSet<>();

    public NearbyUserAdapter(Context context, List<HomeFragment.NearbyUser> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeFragment.NearbyUser user = users.get(position);
        holder.tvNameAge.setText(user.name + ", " + user.age);
        holder.tvDistance.setText(user.distance);
        holder.tvRoleTag.setText(user.role);

        int portraitIdx = user.portraitIndex >= 0 ? user.portraitIndex : position;
        if (holder.ivUserPhoto != null) {
            WallaPortraitStudio.loadGuyPhoto(context, holder.ivUserPhoto, portraitIdx);
        }

        if (holder.tvVerifiedBadge != null) {
            holder.tvVerifiedBadge.setVisibility(user.isVerified ? View.VISIBLE : View.GONE);
        }

        // Minimalist role caption without clunky colored background boxes
        holder.tvRoleTag.setBackground(null);
        holder.tvRoleTag.setTextColor(Color.parseColor("#C7C7CC"));

        holder.viewOnline.setVisibility(user.isOnline ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v -> showMiniProfileBottomSheet(user, portraitIdx));
    }

    /**
     * Rich BottomSheetDialog (Mini Profile) when tapping any Nearby Grid card:
     * - Large DP surrounded by Imperial Golden Dragon frame
     * - Name, age, verified tick, role badge, and distance
     * - Working "Chat" button -> Directly opens 1-on-1 full-screen chat for that user
     * - Working "Follow" button -> Toggles between "+ Follow" and "✓ Following"
     * - Working "Send Gift" button -> Opens virtual gift tray with wallet coin balance
     */
    private void showMiniProfileBottomSheet(HomeFragment.NearbyUser user, int portraitIdx) {
        final BottomSheetDialog bottomSheet = new BottomSheetDialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#09090E"));
        int pad = dp(20);
        root.setPadding(pad, dp(14), pad, dp(24));

        // Top Drag Handle Pill
        View dragHandle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setCornerRadius(dp(4));
        handleBg.setColor(Color.parseColor("#33FFFFFF"));
        dragHandle.setBackground(handleBg);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(40), dp(4));
        handleLp.bottomMargin = dp(14);
        root.addView(dragHandle, handleLp);

        // Large DP with Golden Dragon Frame
        FrameLayout dpFrameContainer = new FrameLayout(context);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(dp(124), dp(124));
        dpFrameContainer.setLayoutParams(frameLp);

        CardView circularDpCard = new CardView(context);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(dp(86), dp(86), Gravity.CENTER);
        circularDpCard.setLayoutParams(cardLp);
        circularDpCard.setRadius(dp(43));
        circularDpCard.setCardElevation(0f);
        circularDpCard.setCardBackgroundColor(Color.parseColor("#13131D"));

        ImageView ivBigDp = new ImageView(context);
        ivBigDp.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivBigDp.setScaleType(ImageView.ScaleType.CENTER_CROP);
        WallaPortraitStudio.loadGuyPhoto(context, ivBigDp, portraitIdx);
        circularDpCard.addView(ivBigDp);
        dpFrameContainer.addView(circularDpCard);

        ImageView ivDragonFrame = new ImageView(context);
        ivDragonFrame.setLayoutParams(new FrameLayout.LayoutParams(dp(124), dp(124), Gravity.CENTER));
        ivDragonFrame.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivDragonFrame.setImageDrawable(WallaPortraitStudio.getImperialDragonFrameDrawable(context));
        dpFrameContainer.addView(ivDragonFrame);

        if (user.isOnline) {
            View onlineDot = new View(context);
            FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(14), dp(14), Gravity.BOTTOM | Gravity.END);
            dotLp.rightMargin = dp(16);
            dotLp.bottomMargin = dp(14);
            onlineDot.setLayoutParams(dotLp);
            onlineDot.setBackgroundResource(R.drawable.bg_online_dot_glow);
            dpFrameContainer.addView(onlineDot);
        }

        root.addView(dpFrameContainer);

        // Name, Age & Verified Badge
        TextView tvTitle = new TextView(context);
        tvTitle.setText(user.name + ", " + user.age + (user.isVerified ? "  ✓" : ""));
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, dp(10), 0, dp(6));
        root.addView(tvTitle);

        // Minimalist Role + Distance + VIP Pill Row
        LinearLayout badgesRow = new LinearLayout(context);
        badgesRow.setOrientation(LinearLayout.HORIZONTAL);
        badgesRow.setGravity(Gravity.CENTER);

        TextView tvRolePill = new TextView(context);
        tvRolePill.setText(user.role);
        tvRolePill.setTextColor(Color.WHITE);
        tvRolePill.setTextSize(11f);
        tvRolePill.setTypeface(Typeface.DEFAULT_BOLD);
        tvRolePill.setBackgroundResource(R.drawable.bg_walla_cyan_pill);
        tvRolePill.setPadding(dp(12), dp(4), dp(12), dp(4));
        badgesRow.addView(tvRolePill);

        TextView tvDistPill = new TextView(context);
        tvDistPill.setText(user.distance + " away");
        tvDistPill.setTextColor(Color.parseColor("#8E8E93"));
        tvDistPill.setTextSize(11f);
        tvDistPill.setTypeface(Typeface.DEFAULT_BOLD);
        tvDistPill.setBackgroundResource(R.drawable.bg_walla_cyan_pill);
        tvDistPill.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams distLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        distLp.leftMargin = dp(8);
        tvDistPill.setLayoutParams(distLp);
        badgesRow.addView(tvDistPill);

        TextView tvVipBadge = new TextView(context);
        tvVipBadge.setText("👑 VIP");
        tvVipBadge.setTextColor(Color.parseColor("#E6B800"));
        tvVipBadge.setTextSize(11f);
        tvVipBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvVipBadge.setBackgroundResource(R.drawable.bg_walla_gold_pill);
        tvVipBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams vipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vipLp.leftMargin = dp(8);
        tvVipBadge.setLayoutParams(vipLp);
        badgesRow.addView(tvVipBadge);

        root.addView(badgesRow);

        // Bio / Stats Line
        TextView tvBio = new TextView(context);
        tvBio.setText(user.bioStats != null ? user.bioStats : "180 cm • 75 kg • Verified Member");
        tvBio.setTextColor(Color.parseColor("#8E8E93"));
        tvBio.setTextSize(12f);
        tvBio.setGravity(Gravity.CENTER);
        tvBio.setPadding(0, dp(10), 0, dp(16));
        root.addView(tvBio);

        // 3 Primary Interactive Buttons: Follow, Chat, Send Gift
        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);
        actionRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 1. Working "+ Follow" / "✓ Following" Toggle Button
        final Button btnFollow = new Button(context);
        btnFollow.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));
        btnFollow.setAllCaps(false);
        btnFollow.setTextSize(12f);
        btnFollow.setTypeface(Typeface.DEFAULT_BOLD);
        updateFollowButtonUI(btnFollow, followedUsers.contains(user.name));

        btnFollow.setOnClickListener(v -> {
            boolean nowFollowing;
            if (followedUsers.contains(user.name)) {
                followedUsers.remove(user.name);
                nowFollowing = false;
                Toast.makeText(context, "Unfollowed " + user.name, Toast.LENGTH_SHORT).show();
            } else {
                followedUsers.add(user.name);
                nowFollowing = true;
                Toast.makeText(context, "Following " + user.name, Toast.LENGTH_SHORT).show();
            }
            updateFollowButtonUI(btnFollow, nowFollowing);
        });

        View spacer1 = new View(context);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(1)));

        // 2. Working "Chat" Button -> Directly opens 1-on-1 chat screen for that user
        Button btnChat = new Button(context);
        btnChat.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));
        btnChat.setText("Message");
        btnChat.setAllCaps(false);
        btnChat.setTextSize(12f);
        btnChat.setTypeface(Typeface.DEFAULT_BOLD);
        btnChat.setTextColor(Color.WHITE);
        btnChat.setBackgroundResource(R.drawable.bg_walla_cyan_pill);
        btnChat.setOnClickListener(v -> {
            bottomSheet.dismiss();
            WallaChatOverlay.openFullScreenChat(
                    context,
                    user.name + ", " + user.age,
                    portraitIdx,
                    user.role,
                    user.distance,
                    false,
                    null
            );
        });

        View spacer2 = new View(context);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(1)));

        // 3. Working "Send Gift" Button -> Opens Virtual Gift Tray
        Button btnSendGift = new Button(context);
        btnSendGift.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));
        btnSendGift.setText("Gift");
        btnSendGift.setAllCaps(false);
        btnSendGift.setTextSize(12f);
        btnSendGift.setTypeface(Typeface.DEFAULT_BOLD);
        btnSendGift.setTextColor(Color.parseColor("#E6B800"));
        btnSendGift.setBackgroundResource(R.drawable.bg_walla_gold_pill);
        btnSendGift.setOnClickListener(v -> showVirtualGiftTray(user));

        actionRow.addView(btnFollow);
        actionRow.addView(spacer1);
        actionRow.addView(btnChat);
        actionRow.addView(spacer2);
        actionRow.addView(btnSendGift);
        root.addView(actionRow);

        // 4. Full-Width "📹 1-on-1 Private HD Video Call" Button -> Launches PrivateVideoCallActivity
        Button btnVideoCall = new Button(context);
        LinearLayout.LayoutParams vcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        vcLp.topMargin = dp(10);
        btnVideoCall.setLayoutParams(vcLp);
        btnVideoCall.setText("📹 Start 1-on-1 Private HD Video Call");
        btnVideoCall.setAllCaps(false);
        btnVideoCall.setTextSize(13f);
        btnVideoCall.setTypeface(Typeface.DEFAULT_BOLD);
        btnVideoCall.setTextColor(Color.WHITE);
        btnVideoCall.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        btnVideoCall.setOnClickListener(v -> {
            bottomSheet.dismiss();
            android.content.Intent callIntent = new android.content.Intent(context, PrivateVideoCallActivity.class);
            callIntent.putExtra(PrivateVideoCallActivity.EXTRA_PARTNER_NAME, user.name + ", " + user.age);
            callIntent.putExtra(PrivateVideoCallActivity.EXTRA_PARTNER_PORTRAIT_IDX, portraitIdx);
            context.startActivity(callIntent);
        });
        root.addView(btnVideoCall);

        bottomSheet.setContentView(root);
        bottomSheet.show();
    }

    private void updateFollowButtonUI(Button btnFollow, boolean isFollowing) {
        if (isFollowing) {
            btnFollow.setText("✓ Following");
            btnFollow.setTextColor(Color.parseColor("#0A0A14"));
            btnFollow.setBackgroundResource(R.drawable.bg_walla_gold_pill);
        } else {
            btnFollow.setText("+ Follow");
            btnFollow.setTextColor(Color.WHITE);
            btnFollow.setBackgroundResource(R.drawable.bg_walla_pink_pill);
        }
    }

    private void showVirtualGiftTray(HomeFragment.NearbyUser recipient) {
        SessionManager sm = new SessionManager(context);
        String[] gifts = new String[]{
                "🌹 Neon Velvet Rose (10 Coins)",
                "🏎️ Cyber Sports Car (100 Coins)",
                "✈️ Supersonic Private Jet (500 Coins)",
                "👑 Imperial Dragon Crown (1,000 Coins)"
        };
        int[] costs = {10, 100, 500, 1000};

        new AlertDialog.Builder(context)
                .setTitle("🎁 Send Gift to " + recipient.name + " (Wallet: " + sm.getFormattedCoins() + " 🪙)")
                .setItems(gifts, (dialog, which) -> {
                    int cost = costs[which];
                    int balance = sm.getUserCoins();
                    if (balance >= cost) {
                        sm.setUserCoins(balance - cost);
                    }
                    Toast.makeText(
                            context,
                            "🎉 Sent " + gifts[which] + " to " + recipient.name + "! Remaining: " + sm.getFormattedCoins() + " 🪙",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dp(int val) {
        return Math.round(val * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto;
        TextView tvNameAge, tvDistance, tvRoleTag, tvVerifiedBadge;
        View viewOnline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            tvNameAge = itemView.findViewById(R.id.tvNameAge);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvRoleTag = itemView.findViewById(R.id.tvRoleTag);
            tvVerifiedBadge = itemView.findViewById(R.id.tvVerifiedBadge);
            viewOnline = itemView.findViewById(R.id.viewOnlineIndicator);
        }
    }
}
