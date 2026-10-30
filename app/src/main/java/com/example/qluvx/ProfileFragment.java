package com.example.qluvx;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private TextView tvDisplayName, tvUserId, tvCoins, tvDiamonds, tvProfileBioLine, tvEquippedRideName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Context ctx = requireContext();
        sessionManager = new SessionManager(ctx);

        ImageView ivProfileCover = view.findViewById(R.id.ivProfileCover);
        ImageView ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        ImageView ivDragonFrameOverlay = view.findViewById(R.id.ivDragonFrameOverlay);
        ImageView ivSettings = view.findViewById(R.id.ivSettings);

        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvCoins = view.findViewById(R.id.tvCoins);
        tvDiamonds = view.findViewById(R.id.tvDiamonds);
        tvProfileBioLine = view.findViewById(R.id.tvProfileBioLine);
        tvEquippedRideName = view.findViewById(R.id.tvEquippedRideName);

        View cardBuyCoins = view.findViewById(R.id.cardBuyCoins);
        View cardDiamondPayout = view.findViewById(R.id.cardDiamondPayout);
        View cardEquippedRide = view.findViewById(R.id.cardEquippedRide);
        TextView btnPreviewStarship = view.findViewById(R.id.btnPreviewStarship);
        Button btnBuyCoins = view.findViewById(R.id.btnBuyCoins);
        Button btnWithdrawDiamonds = view.findViewById(R.id.btnWithdrawDiamonds);
        Button btnOpenProfileSettings = view.findViewById(R.id.btnOpenProfileSettings);

        // Render Cover Wallpaper, Portrait DP, and Imperial Gold Dragon Avatar Frame
        if (ivProfileCover != null) {
            ivProfileCover.setImageDrawable(WallaPortraitStudio.getProfileCoverDrawable(ctx));
        }
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setImageDrawable(WallaPortraitStudio.getGuyPortraitDrawable(ctx, 6));
        }
        if (ivDragonFrameOverlay != null) {
            ivDragonFrameOverlay.setImageDrawable(WallaPortraitStudio.getImperialDragonFrameDrawable(ctx));
        }

        refreshProfileHeader();

        // 1-Tap ID Copy ("ID: 1002938 📑")
        if (tvUserId != null) {
            tvUserId.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Walla ID", sessionManager.getUserId());
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(ctx, "✅ Copied ID: " + sessionManager.getUserId() + " to clipboard!", Toast.LENGTH_SHORT).show();
            });
        }

        // Settings Gear & Settings Button -> Open ProfileSettingsActivity
        View.OnClickListener openSettingsClick = v -> {
            Intent intent = new Intent(getActivity(), ProfileSettingsActivity.class);
            startActivity(intent);
        };
        if (ivSettings != null) ivSettings.setOnClickListener(openSettingsClick);
        if (btnOpenProfileSettings != null) btnOpenProfileSettings.setOnClickListener(openSettingsClick);

        // Recharge Coins Sheet
        View.OnClickListener buyCoinsClick = v -> showCoinRechargeDialog();
        if (cardBuyCoins != null) cardBuyCoins.setOnClickListener(buyCoinsClick);
        if (btnBuyCoins != null) btnBuyCoins.setOnClickListener(buyCoinsClick);

        // Diamond Cashout Sheet
        View.OnClickListener payoutClick = v -> showDiamondPayoutDialog();
        if (cardDiamondPayout != null) cardDiamondPayout.setOnClickListener(payoutClick);
        if (btnWithdrawDiamonds != null) btnWithdrawDiamonds.setOnClickListener(payoutClick);

        // Equipped Ride ("Interstellar Starship X 🚀") Preview / Selector
        View.OnClickListener rideClick = v -> showStarshipEntrancePreviewDialog();
        if (cardEquippedRide != null) cardEquippedRide.setOnClickListener(rideClick);
        if (btnPreviewStarship != null) btnPreviewStarship.setOnClickListener(rideClick);

        return view;
    }

    private void refreshProfileHeader() {
        if (sessionManager == null) return;
        if (tvDisplayName != null) {
            tvDisplayName.setText(sessionManager.getUserName());
        }
        if (tvUserId != null) {
            tvUserId.setText("ID: " + sessionManager.getUserId() + " 📑");
        }
        if (tvCoins != null) {
            tvCoins.setText(sessionManager.getFormattedCoins() + " 🪙");
        }
        if (tvDiamonds != null) {
            tvDiamonds.setText(sessionManager.getFormattedDiamonds() + " 💎");
        }
        if (tvProfileBioLine != null) {
            String stealthTag = sessionManager.isGhostMode() ? " • 🕵️ Stealth ON" : " • Bangalore";
            tvProfileBioLine.setText(
                    sessionManager.getUserRole() + " • "
                            + sessionManager.getUserHeightCm() + " cm • "
                            + sessionManager.getUserWeightKg() + " kg"
                            + stealthTag
            );
        }
        if (tvEquippedRideName != null) {
            tvEquippedRideName.setText(sessionManager.getEquippedRide());
        }
    }

    private void showCoinRechargeDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        String[] packages = new String[]{
                "🪙 +500 Gold Coins  •  ₹89 (Instant Credit)",
                "🪙 +1,500 Gold Coins  •  ₹249 (+10% Bonus)",
                "🪙 +5,000 Gold Coins  •  ₹799 (VIP Pack)",
                "👑 +15,000 Gold Coins + Imperial Dragon VIP  •  ₹1,999"
        };

        new AlertDialog.Builder(ctx)
                .setTitle("🪙 Walla Coin Recharge Store")
                .setItems(packages, (dialog, which) -> {
                    int[] amounts = {500, 1500, 5000, 15000};
                    int added = amounts[which];
                    sessionManager.setUserCoins(sessionManager.getUserCoins() + added);
                    refreshProfileHeader();
                    Toast.makeText(ctx, "🎉 Recharged +" + added + " Coins! New Balance: " + sessionManager.getFormattedCoins() + " 🪙", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showDiamondPayoutDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        int currentDiamonds = sessionManager.getUserDiamonds();
        double inrVal = currentDiamonds * 0.50;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("💎 Streamer Diamond Cashout");
        tvTitle.setTextColor(Color.parseColor("#00E5FF"));
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        TextView tvInfo = new TextView(ctx);
        tvInfo.setText(String.format(Locale.US,
                "Balance: %s 💎 (≈ ₹%.2f INR)\nRate: 1 💎 = ₹0.50 INR • Instant UPI / IMPS",
                sessionManager.getFormattedDiamonds(), inrVal));
        tvInfo.setTextColor(Color.parseColor("#C7C7E2"));
        tvInfo.setTextSize(12.5f);
        tvInfo.setPadding(0, dp(8), 0, dp(12));
        root.addView(tvInfo);

        final EditText etAmount = new EditText(ctx);
        etAmount.setHint("Diamonds to withdraw (e.g. 1000)");
        etAmount.setText("1000");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        etAmount.setTextColor(Color.WHITE);
        etAmount.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        etAmount.setPadding(dp(14), dp(10), dp(14), dp(10));
        root.addView(etAmount);

        final EditText etUpi = new EditText(ctx);
        LinearLayout.LayoutParams upiLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        upiLp.topMargin = dp(10);
        etUpi.setLayoutParams(upiLp);
        etUpi.setHint("Enter UPI ID (julian@upi)");
        etUpi.setText("julianvance@okaxis");
        etUpi.setTextColor(Color.WHITE);
        etUpi.setBackgroundResource(R.drawable.bg_walla_pill_dark);
        etUpi.setPadding(dp(14), dp(10), dp(14), dp(10));
        root.addView(etUpi);

        new AlertDialog.Builder(ctx)
                .setView(root)
                .setPositiveButton("Cashout Now", (d, w) -> {
                    int amt = 1000;
                    try {
                        amt = Integer.parseInt(etAmount.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}
                    amt = Math.min(amt, sessionManager.getUserDiamonds());
                    sessionManager.setUserDiamonds(sessionManager.getUserDiamonds() - amt);
                    refreshProfileHeader();
                    Toast.makeText(ctx, String.format(Locale.US, "✅ ₹%.2f INR transferred to %s!", amt * 0.50, etUpi.getText().toString()), Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStarshipEntrancePreviewDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.parseColor("#141424"));
        int pad = dp(22);
        box.setPadding(pad, pad, pad, pad);

        TextView tvIcon = new TextView(ctx);
        tvIcon.setText("🚀✨🐉");
        tvIcon.setTextSize(42f);
        tvIcon.setGravity(Gravity.CENTER);
        box.addView(tvIcon);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("Interstellar Starship X 🚀");
        tvTitle.setTextColor(Color.parseColor("#F1C40F"));
        tvTitle.setTextSize(19f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, dp(10), 0, dp(6));
        box.addView(tvTitle);

        TextView tvDesc = new TextView(ctx);
        tvDesc.setText("Equipped VIP Entrance Mount & Imperial Gold Dragon Frame.\nAnnounces your arrival across all Live PK & 8-Seat Voice Party rooms!");
        tvDesc.setTextColor(Color.parseColor("#C7C7E2"));
        tvDesc.setTextSize(12.5f);
        tvDesc.setGravity(Gravity.CENTER);
        box.addView(tvDesc);

        new AlertDialog.Builder(ctx)
                .setView(box)
                .setPositiveButton("Awesome ✨", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileHeader();
    }

    private int dp(int val) {
        if (getContext() == null) return val;
        return Math.round(val * getContext().getResources().getDisplayMetrics().density);
    }
}
