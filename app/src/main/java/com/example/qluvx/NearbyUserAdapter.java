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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NearbyUserAdapter extends RecyclerView.Adapter<NearbyUserAdapter.ViewHolder> {

    private final Context context;
    private final List<HomeFragment.NearbyUser> users;

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
            holder.ivUserPhoto.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(context, portraitIdx));
        }

        if (holder.tvVerifiedBadge != null) {
            holder.tvVerifiedBadge.setVisibility(user.isVerified ? View.VISIBLE : View.GONE);
        }

        // Style Role Chip ("Top", "Versatile", "Bottom")
        GradientDrawable roleBg = new GradientDrawable();
        roleBg.setCornerRadius(999f);
        if ("Top".equalsIgnoreCase(user.role)) {
            roleBg.setColor(Color.parseColor("#FF2A7A"));
            holder.tvRoleTag.setTextColor(Color.WHITE);
        } else if ("Bottom".equalsIgnoreCase(user.role)) {
            roleBg.setColor(Color.parseColor("#8E24AA"));
            holder.tvRoleTag.setTextColor(Color.WHITE);
        } else {
            roleBg.setColor(Color.parseColor("#00E5FF"));
            holder.tvRoleTag.setTextColor(Color.parseColor("#0A0A14"));
        }
        holder.tvRoleTag.setBackground(roleBg);

        holder.viewOnline.setVisibility(user.isOnline ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v -> showGuyProfileSheet(user, portraitIdx));
    }

    private void showGuyProfileSheet(HomeFragment.NearbyUser user, int portraitIdx) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        CardView photoCard = new CardView(context);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(240));
        photoCard.setLayoutParams(cardLp);
        photoCard.setRadius(dp(16));
        photoCard.setCardElevation(0f);

        ImageView ivBig = new ImageView(context);
        ivBig.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivBig.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivBig.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(context, portraitIdx));
        photoCard.addView(ivBig);
        root.addView(photoCard);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(user.name + ", " + user.age + (user.isVerified ? "  ✓" : "") + "  •  " + user.distance);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(19f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, dp(12), 0, dp(4));
        root.addView(tvTitle);

        TextView tvRoleAndStats = new TextView(context);
        tvRoleAndStats.setText("Role: " + user.role + "  •  " + (user.bioStats != null ? user.bioStats : "180 cm • 75 kg"));
        tvRoleAndStats.setTextColor(Color.parseColor("#00E5FF"));
        tvRoleAndStats.setTextSize(12.5f);
        tvRoleAndStats.setPadding(0, 0, 0, dp(14));
        root.addView(tvRoleAndStats);

        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);

        Button btnWave = new Button(context);
        btnWave.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));
        btnWave.setText("👋 Say Hi");
        btnWave.setAllCaps(false);
        btnWave.setTextColor(Color.WHITE);
        btnWave.setBackgroundColor(Color.parseColor("#FF2A7A"));

        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(10), dp(1)));

        Button btnChat = new Button(context);
        btnChat.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1f));
        btnChat.setText("⏱️ Send 5s Photo");
        btnChat.setAllCaps(false);
        btnChat.setTextColor(Color.parseColor("#0A0A14"));
        btnChat.setBackgroundColor(Color.parseColor("#F1C40F"));

        actionRow.addView(btnWave);
        actionRow.addView(spacer);
        actionRow.addView(btnChat);
        root.addView(actionRow);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();

        btnWave.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "💖 Sent a Mutual Match Wave to " + user.name + "!", Toast.LENGTH_SHORT).show();
        });

        btnChat.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(context, "⏱️ Sent 5s Disappearing Photo to " + user.name + "!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
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
