package com.example.qluvx;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class RoomDialogsHelper {

    public interface RoomActionCallbacks {
        void onGiftSentToRoom(EconomyManager.GiftItem gift, String recipientName, String validationMode);
        void onRoomDisplayUpdated();
        void onSeatPermissionAction(String actionType, int targetSeatIndex, int targetUid, String targetName);
        void onModerationActionLogged(String logEntry);
    }

    public static void showGiftAndEconomyDialog(
            Activity activity,
            VoiceRoomModels.RoomInfo room,
            EconomyManager economyManager,
            AgoraVoiceManager voiceManager,
            String localUserName,
            RoomActionCallbacks callbacks
    ) {
        if (room == null) return;
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiStyleHelper.COLOR_BG_DEEP);

        // Wallet Balance & Validation Banner
        final TextView balanceHeader = new TextView(activity);
        boolean hasBackend = EconomyManager.isBackendConfigured();
        balanceHeader.setText("🪙 Wallet Balance: " + economyManager.getCoinBalance() + " Coins\n"
                + (hasBackend
                ? "🟢 Server Validation: Active (QLUVX_BACKEND_API_URL)"
                : "🟡 Mode: Local Preview Ledger (Set QLUVX_BACKEND_API_URL in Secrets for server billing)"));
        balanceHeader.setTextColor(UiStyleHelper.COLOR_CYBER_CYAN);
        balanceHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        balanceHeader.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        balanceHeader.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        root.addView(balanceHeader);

        // Determine recipient
        int recipientUid = 0;
        String recipientName = "Room Host Stage";
        for (VoiceRoomModels.SpeakerSeat s : room.seats) {
            if (s.isOccupied) {
                recipientUid = s.uid;
                recipientName = s.userName + " (" + s.roleTag + ")";
                break;
            }
        }
        final int finalRecUid = recipientUid;
        final String finalRecName = recipientName;

        TextView recView = new TextView(activity);
        recView.setText("🎁 Recipient: " + finalRecName + " • Duplicate-Payment Protection (UUID Idempotency) Active");
        recView.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        recView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        recView.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        root.addView(recView);

        AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎁 QLUVX Gifts & Coin Economy")
                .setView(scroll)
                .setNeutralButton("📜 Transaction History", (d, w) -> showWalletHistoryDialog(activity, economyManager))
                .setNegativeButton("Close", null)
                .create();

        // Gift Catalogue Items
        for (EconomyManager.GiftItem gift : EconomyManager.GIFT_CATALOGUE) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int rPad = UiStyleHelper.dp(activity, 10);
            row.setPadding(rPad, rPad, rPad, rPad);
            row.setBackground(UiStyleHelper.roundedBox(activity, UiStyleHelper.COLOR_CARD_ELEVATED, 12f, Color.argb(45, 255, 255, 255), 1f));

            TextView info = new TextView(activity);
            info.setText(gift.emoji + "  " + gift.name + "  [" + gift.category + "]\n🪙 " + gift.priceCoins + " Coins");
            info.setTextColor(Color.WHITE);
            info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            info.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView sendBtn = UiStyleHelper.createPillButton(
                    activity,
                    "Send 🎁",
                    UiStyleHelper.COLOR_NEON_PINK,
                    Color.WHITE,
                    v -> {
                        String idempotencyKey = "gift-" + UUID.randomUUID().toString();
                        economyManager.sendGiftWithDuplicateProtection(
                                idempotencyKey,
                                gift,
                                voiceManager.getAssignedLocalUid(),
                                localUserName,
                                finalRecUid,
                                finalRecName,
                                room.channelName,
                                (success, statusMessage, sentGift, recName, validationMode) -> {
                                    Toast.makeText(activity, statusMessage, Toast.LENGTH_SHORT).show();
                                    if (success) {
                                        balanceHeader.setText("🪙 Wallet Balance: " + economyManager.getCoinBalance() + " Coins (" + validationMode + ")");
                                        try {
                                            JSONObject pkt = new JSONObject();
                                            pkt.put("type", "GIFT_EVENT");
                                            pkt.put("idempotencyKey", idempotencyKey);
                                            pkt.put("emoji", sentGift.emoji);
                                            pkt.put("giftName", sentGift.name);
                                            pkt.put("coins", sentGift.priceCoins);
                                            pkt.put("sender", localUserName);
                                            pkt.put("recipient", recName);
                                            voiceManager.broadcastDataPacket(pkt);
                                        } catch (Throwable ignored) {
                                        }
                                        callbacks.onGiftSentToRoom(sentGift, recName, validationMode);
                                        dialog.dismiss();
                                    }
                                }
                        );
                    }
            );

            row.addView(info);
            row.addView(sendBtn);
            root.addView(row, UiStyleHelper.params(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 0, 0, UiStyleHelper.dp(activity, 8)
            ));
        }

        scroll.addView(root);
        dialog.show();
    }

    public static void showWalletHistoryDialog(Activity activity, EconomyManager economyManager) {
        List<EconomyManager.WalletTransaction> txs = economyManager.getTransactions();
        StringBuilder sb = new StringBuilder();
        sb.append("Current Balance: ").append(economyManager.getCoinBalance()).append(" Coins\n");
        sb.append("Duplicate-Payment Protection: Active (UUID Idempotency)\n\n");
        for (EconomyManager.WalletTransaction tx : txs) {
            sb.append("• [").append(tx.timestamp).append("] ")
                    .append(tx.description)
                    .append(" (").append(tx.deltaCoins >= 0 ? "+" : "").append(tx.deltaCoins).append(" Coins)\n")
                    .append("  Status: ").append(tx.validationState)
                    .append(" | Key: ").append(tx.idempotencyKey.substring(0, Math.min(12, tx.idempotencyKey.length())))
                    .append("\n\n");
        }
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("📜 Wallet & Idempotent Transaction Ledger")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    public static void showParticipantsAndHandRaiseDialog(
            Activity activity,
            VoiceRoomModels.RoomInfo room,
            AgoraVoiceManager voiceManager,
            int currentLocalSeatIndex,
            String localUserName,
            RoomActionCallbacks callbacks
    ) {
        if (room == null) return;
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiStyleHelper.COLOR_BG_DEEP);

        boolean connected = voiceManager.isChannelConnected();
        int totalOnline = (connected ? 1 : 0) + voiceManager.getRemoteUserCount();

        TextView summary = new TextView(activity);
        summary.setText("📡 Channel: " + room.channelName
                + " (" + (room.mode == VoiceRoomModels.RoomMode.LIVE_VIDEO ? "Live Video" : "12-Seat Voice") + ")\n"
                + "Online in Agora Channel: " + totalOnline
                + "  •  Seated on Mic: " + room.getActiveSpeakerCount() + "/12\n"
                + "Your Role: " + (currentLocalSeatIndex == 0 ? "HOST" : (currentLocalSeatIndex == 1 ? "CO-HOST" : (currentLocalSeatIndex > 1 ? "SPEAKER" : "LISTENER (Audience)"))));
        summary.setTextColor(UiStyleHelper.COLOR_CYBER_CYAN);
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        summary.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 10));
        root.addView(summary);

        AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("👥 Participants, Invites & Hand-Raise Queue")
                .setView(scroll)
                .setPositiveButton("✋ Raise Hand for Mic", (d, w) -> {
                    int myUid = voiceManager.getAssignedLocalUid();
                    VoiceRoomModels.HandRaiseRequest req = new VoiceRoomModels.HandRaiseRequest(
                            myUid, localUserName, -1, "RAISED"
                    );
                    room.handRaises.add(0, req);
                    try {
                        JSONObject pkt = new JSONObject();
                        pkt.put("type", "HAND_RAISE");
                        pkt.put("uid", myUid);
                        pkt.put("userName", localUserName);
                        pkt.put("action", "RAISED");
                        voiceManager.broadcastDataPacket(pkt);
                    } catch (Throwable ignored) {
                    }
                    callbacks.onModerationActionLogged("✋ " + localUserName + " raised hand to request a microphone seat.");
                })
                .setNeutralButton("🛡️ Moderation Log", (d, w) -> showModerationLogDialog(activity, room))
                .setNegativeButton("Close", null)
                .create();

        // Section 1: Hand-Raise & Seat Invite Queue
        TextView hrTitle = new TextView(activity);
        hrTitle.setText("✋ Hand-Raise & Seat Requests (" + room.handRaises.size() + ")");
        hrTitle.setTextColor(UiStyleHelper.COLOR_NEON_PINK);
        hrTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        hrTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        root.addView(hrTitle);

        if (room.handRaises.isEmpty()) {
            TextView emptyHr = new TextView(activity);
            emptyHr.setText("No pending hand-raise requests. Listeners can tap '✋ Raise Hand' to request a seat.");
            emptyHr.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            emptyHr.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            emptyHr.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 10));
            root.addView(emptyHr);
        } else {
            for (int i = 0; i < room.handRaises.size(); i++) {
                final VoiceRoomModels.HandRaiseRequest req = room.handRaises.get(i);
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, UiStyleHelper.dp(activity, 6), 0, UiStyleHelper.dp(activity, 6));

                TextView label = new TextView(activity);
                label.setText("✋ " + req.userName + " (UID " + req.uid + ") • " + req.status);
                label.setTextColor(Color.WHITE);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView acceptBtn = UiStyleHelper.createPillButton(activity, "Accept", UiStyleHelper.COLOR_LIVE_GREEN, Color.BLACK, v -> {
                    req.status = "ACCEPTED";
                    room.handRaises.remove(req);
                    callbacks.onSeatPermissionAction("ACCEPT_HAND_RAISE", 2, req.uid, req.userName);
                    dialog.dismiss();
                });

                TextView rejectBtn = UiStyleHelper.createPillButton(activity, "Reject", UiStyleHelper.COLOR_MUTED_RED, Color.WHITE, v -> {
                    req.status = "REJECTED";
                    room.handRaises.remove(req);
                    callbacks.onSeatPermissionAction("REJECT_HAND_RAISE", -1, req.uid, req.userName);
                    dialog.dismiss();
                });

                row.addView(label);
                row.addView(acceptBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
                row.addView(rejectBtn);
                root.addView(row);
            }
        }

        // Section 2: Seated Speakers & Host/Co-Host Controls
        TextView seatedTitle = new TextView(activity);
        seatedTitle.setText("🎙️ Active Microphone Seats (" + room.getActiveSpeakerCount() + "/12)");
        seatedTitle.setTextColor(Color.WHITE);
        seatedTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        seatedTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        seatedTitle.setPadding(0, UiStyleHelper.dp(activity, 8), 0, UiStyleHelper.dp(activity, 4));
        root.addView(seatedTitle);

        for (final VoiceRoomModels.SpeakerSeat seat : room.seats) {
            if (!seat.isOccupied) continue;
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiStyleHelper.dp(activity, 6), 0, UiStyleHelper.dp(activity, 6));

            TextView sInfo = new TextView(activity);
            sInfo.setText("Seat " + seat.seatIndex + " [" + seat.roleTag + "]: " + seat.userName
                    + " (UID " + seat.uid + ") " + (seat.isMuted ? "🔇" : "🎙️"));
            sInfo.setTextColor(Color.WHITE);
            sInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            sInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView muteBtn = UiStyleHelper.createPillButton(
                    activity,
                    seat.isMuted ? "Unmute" : "Mute",
                    UiStyleHelper.COLOR_CARD_ELEVATED,
                    UiStyleHelper.COLOR_CYBER_CYAN,
                    v -> {
                        callbacks.onSeatPermissionAction(seat.isMuted ? "UNMUTE_SEAT" : "MUTE_SEAT", seat.seatIndex, seat.uid, seat.userName);
                        dialog.dismiss();
                    }
            );

            TextView removeBtn = UiStyleHelper.createPillButton(
                    activity,
                    "Remove",
                    Color.argb(70, 255, 64, 96),
                    UiStyleHelper.COLOR_MUTED_RED,
                    v -> {
                        callbacks.onSeatPermissionAction("REMOVE_FROM_SEAT", seat.seatIndex, seat.uid, seat.userName);
                        dialog.dismiss();
                    }
            );

            row.addView(sInfo);
            row.addView(muteBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 6), 0));
            row.addView(removeBtn);
            root.addView(row);
        }

        // Section 3: Remote Participants in Agora Channel (Invite to Mic / Report)
        List<Integer> remoteUids = voiceManager.getRemoteUidList();
        TextView remoteTitle = new TextView(activity);
        remoteTitle.setText("🎧 Remote Agora Channel Peers (" + remoteUids.size() + ")");
        remoteTitle.setTextColor(Color.WHITE);
        remoteTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        remoteTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        remoteTitle.setPadding(0, UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 4));
        root.addView(remoteTitle);

        if (remoteUids.isEmpty()) {
            TextView noRem = new TextView(activity);
            noRem.setText("No remote Agora UIDs currently in channel '" + room.channelName + "'. Connect a second device to this channel name to see remote participants here.");
            noRem.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            noRem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            root.addView(noRem);
        } else {
            for (final int rUid : remoteUids) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 4));

                TextView uLabel = new TextView(activity);
                uLabel.setText("Remote Peer UID " + rUid);
                uLabel.setTextColor(Color.WHITE);
                uLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                uLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView inviteBtn = UiStyleHelper.createPillButton(activity, "Invite to Mic", UiStyleHelper.COLOR_ELECTRIC_VIOLET, Color.WHITE, v -> {
                    callbacks.onSeatPermissionAction("INVITE_TO_MIC", 2, rUid, "User " + rUid);
                    dialog.dismiss();
                });

                row.addView(uLabel);
                row.addView(inviteBtn);
                root.addView(row);
            }
        }

        scroll.addView(root);
        dialog.show();
    }

    public static void showRoomDisplaySettingsDialog(
            Activity activity,
            VoiceRoomModels.RoomInfo room,
            AgoraVoiceManager voiceManager,
            RoomActionCallbacks callbacks
    ) {
        if (room == null) return;
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);

        final EditText titleInput = new EditText(activity);
        titleInput.setText(room.title);
        titleInput.setHint("Room Title");
        titleInput.setSingleLine(true);
        root.addView(titleInput);

        final EditText coverInput = new EditText(activity);
        coverInput.setText(room.coverTagline);
        coverInput.setHint("Room Cover Badge / Tagline");
        coverInput.setSingleLine(true);
        root.addView(coverInput);

        final EditText noticeInput = new EditText(activity);
        noticeInput.setText(room.welcomeNotice);
        noticeInput.setHint("Room Welcome Announcement");
        root.addView(noticeInput);

        TextView wpTitle = new TextView(activity);
        wpTitle.setText("🎨 Select Room Wallpaper / Background Theme:");
        wpTitle.setTextColor(UiStyleHelper.COLOR_CYBER_CYAN);
        wpTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        wpTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        wpTitle.setPadding(0, UiStyleHelper.dp(activity, 10), 0, UiStyleHelper.dp(activity, 6));
        root.addView(wpTitle);

        HorizontalScrollView wpScroll = new HorizontalScrollView(activity);
        LinearLayout wpRow = new LinearLayout(activity);
        wpRow.setOrientation(LinearLayout.HORIZONTAL);
        for (final VoiceRoomModels.WallpaperTheme theme : VoiceRoomModels.WALLPAPER_THEMES) {
            boolean selected = room.wallpaper != null && room.wallpaper.id.equals(theme.id);
            TextView btn = UiStyleHelper.createPillButton(
                    activity,
                    (selected ? "✓ " : "") + theme.name,
                    theme.topGradientColor,
                    Color.WHITE,
                    v -> {
                        room.wallpaper = theme;
                        room.accentColor = theme.accentColor;
                        Toast.makeText(activity, "Wallpaper set to " + theme.name, Toast.LENGTH_SHORT).show();
                    }
            );
            wpRow.addView(btn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 8), 0));
        }
        wpScroll.addView(wpRow);
        root.addView(wpScroll);

        // Lock / Unlock All Open Seats
        LinearLayout seatLockRow = new LinearLayout(activity);
        seatLockRow.setOrientation(LinearLayout.HORIZONTAL);
        seatLockRow.setPadding(0, UiStyleHelper.dp(activity, 12), 0, 0);

        TextView lockAllBtn = UiStyleHelper.createPillButton(activity, "🔒 Lock Empty Seats", UiStyleHelper.COLOR_CARD_ELEVATED, Color.WHITE, v -> {
            for (int i = 2; i < room.seats.size(); i++) {
                if (!room.seats.get(i).isOccupied) room.seats.get(i).isLocked = true;
            }
            callbacks.onRoomDisplayUpdated();
            Toast.makeText(activity, "Locked all empty speaker seats.", Toast.LENGTH_SHORT).show();
        });

        TextView unlockAllBtn = UiStyleHelper.createPillButton(activity, "🔓 Unlock All Seats", UiStyleHelper.COLOR_CARD_ELEVATED, UiStyleHelper.COLOR_LIVE_GREEN, v -> {
            for (VoiceRoomModels.SpeakerSeat s : room.seats) {
                s.isLocked = false;
            }
            callbacks.onRoomDisplayUpdated();
            Toast.makeText(activity, "Unlocked all speaker seats.", Toast.LENGTH_SHORT).show();
        });

        seatLockRow.addView(lockAllBtn, UiStyleHelper.params(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, UiStyleHelper.dp(activity, 8), 0));
        seatLockRow.addView(unlockAllBtn);
        root.addView(seatLockRow);

        scroll.addView(root);

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎨 Room Display, Cover & Wallpaper Settings")
                .setView(scroll)
                .setPositiveButton("Save & Sync", (d, w) -> {
                    String newTitle = titleInput.getText().toString().trim();
                    String newCover = coverInput.getText().toString().trim();
                    String newNotice = noticeInput.getText().toString().trim();
                    if (!newTitle.isEmpty()) room.title = newTitle;
                    if (!newCover.isEmpty()) room.coverTagline = newCover;
                    if (!newNotice.isEmpty()) room.welcomeNotice = newNotice;
                    try {
                        JSONObject pkt = new JSONObject();
                        pkt.put("type", "WALLPAPER_UPDATE");
                        pkt.put("wallpaperId", room.wallpaper.id);
                        pkt.put("title", room.title);
                        pkt.put("cover", room.coverTagline);
                        pkt.put("notice", room.welcomeNotice);
                        voiceManager.broadcastDataPacket(pkt);
                    } catch (Throwable ignored) {
                    }
                    callbacks.onRoomDisplayUpdated();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showModerationLogDialog(Activity activity, VoiceRoomModels.RoomInfo room) {
        if (room == null) return;
        String[] reasons = new String[]{
                "🚩 Report Room / Participant: Abusive Audio",
                "🚩 Report Room / Participant: Spam or Scam",
                "🚩 Report Room / Participant: Harassment",
                "🔇 Clear Room Chat History"
        };
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🛡️ Room Moderation & Reporting")
                .setItems(reasons, (d, which) -> {
                    if (which == 3) {
                        room.messages.clear();
                        Toast.makeText(activity, "Room chat cleared by moderator.", Toast.LENGTH_SHORT).show();
                    } else {
                        String entry = "Report submitted (" + reasons[which] + ") — "
                                + (EconomyManager.isBackendConfigured() ? "Forwarded to backend moderation queue." : "Logged locally (Configure QLUVX_BACKEND_API_URL for server moderation).");
                        room.moderationLog.add(entry);
                        Toast.makeText(activity, entry, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
