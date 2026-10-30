package com.example.qluvx;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MessagesFragment extends Fragment implements SocketManager.SocketEventListener {

    private SessionManager sessionManager;
    private CountDownTimer activeTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        sessionManager = new SessionManager(requireContext());
        SocketManager.getInstance().addListener(this);

        LinearLayout itemSystemAlert = view.findViewById(R.id.itemSystemAlert);
        LinearLayout itemChat1 = view.findViewById(R.id.itemChat1);
        LinearLayout itemChat2 = view.findViewById(R.id.itemChat2);
        TextView tvDisappearingStatus = view.findViewById(R.id.tvDisappearingStatus);

        if (itemSystemAlert != null) {
            itemSystemAlert.setOnClickListener(v ->
                    Toast.makeText(getContext(), "🛡️ System Alert: FLAG_SECURE Screenshot Protection & VIP Perks Active!", Toast.LENGTH_SHORT).show()
            );
        }

        itemChat1.setOnClickListener(v -> {
            showFiveSecondDisappearingPhotoDialog(tvDisappearingStatus);
            SocketManager.getInstance().sendPrivateMessage(
                    sessionManager.getUserId(),
                    "user_aarav_23",
                    "Opened 5s disappearing photo",
                    true,
                    5
            );
        });

        itemChat2.setOnClickListener(v -> {
            Toast.makeText(getContext(), "🎙️ Playing 0:14 Voice Note from Vikram...", Toast.LENGTH_SHORT).show();
            SocketManager.getInstance().sendPrivateMessage(
                    sessionManager.getUserId(),
                    "user_vikram_27",
                    "Hey Vikram! Joining your voice party room now 🎙️",
                    false,
                    0
            );
        });

        return view;
    }

    private void showFiveSecondDisappearingPhotoDialog(@Nullable TextView tvStatus) {
        if (getContext() == null) return;

        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.parseColor("#121228"));
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        TextView tvTimer = new TextView(getContext());
        tvTimer.setText("⏱️ Self-Destructing in 5s...");
        tvTimer.setTextColor(Color.parseColor("#FF4081"));
        tvTimer.setTextSize(17f);
        tvTimer.setTypeface(Typeface.DEFAULT_BOLD);
        tvTimer.setPadding(0, 0, 0, pad / 2);
        box.addView(tvTimer);

        ImageView ivSecretPhoto = new ImageView(getContext());
        int size = Math.round(220 * getResources().getDisplayMetrics().density);
        ivSecretPhoto.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        ivSecretPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivSecretPhoto.setImageResource(R.drawable.img_avatar_1);
        box.addView(ivSecretPhoto);

        TextView tvNote = new TextView(getContext());
        tvNote.setText("🔒 Screenshot Protected • View-Once Photo from Aarav");
        tvNote.setTextColor(Color.parseColor("#FFD700"));
        tvNote.setTextSize(12f);
        tvNote.setPadding(0, pad / 2, 0, 0);
        box.addView(tvNote);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(box)
                .setCancelable(true)
                .create();

        dialog.show();

        if (activeTimer != null) {
            activeTimer.cancel();
        }

        activeTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = (millisUntilFinished / 1000) + 1;
                tvTimer.setText("⏱️ Self-Destructing in " + sec + "s...");
            }

            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (tvStatus != null) {
                    tvStatus.setText("🔥 Disappearing Photo Expired (Viewed)");
                    tvStatus.setTextColor(Color.parseColor("#9999BB"));
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "💥 5-Second Photo Self-Destructed!", Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }

    @Override
    public void onPrivateMessageReceived(String senderId, String senderName, String message, boolean isDisappearingPhoto, int ttlSeconds) {
        if (!isAdded() || getContext() == null) return;
        if (isDisappearingPhoto) {
            Toast.makeText(getContext(), "💣 " + senderName + " sent a " + ttlSeconds + "s disappearing photo!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "💬 " + senderName + ": " + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (activeTimer != null) {
            activeTimer.cancel();
        }
        SocketManager.getInstance().removeListener(this);
        super.onDestroyView();
    }
}
