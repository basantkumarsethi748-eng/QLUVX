package com.example.qluvx;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Production Firebase Cloud Messaging Push Notification Service for QLUVX:
 *
 * Handles background & foreground push alerts for the 4 core real-time events:
 * 1. "Alex sent you a 5-second disappearing photo ⏱️"
 * 2. "🔥 Julian Vance just went live! Tap to watch"
 * 3. "⚡ You have been challenged to a 5-minute PK battle!"
 * 4. "🎉 You received a Super Sports Car from Leo!"
 */
public class MyFirebaseMessagingService extends Service {

    private static final String TAG = "QLUVX_FCM_Service";
    public static final String CHANNEL_ID = "qluvx_high_priority_push_alerts";
    public static final String CHANNEL_NAME = "QLUVX Live, PK & Secret Chat Alerts";

    public static final String ALERT_TYPE_DISAPPEARING_PHOTO = "DISAPPEARING_PHOTO";
    public static final String ALERT_TYPE_LIVE_STREAM = "LIVE_STREAM_STARTED";
    public static final String ALERT_TYPE_PK_CHALLENGE = "PK_BATTLE_CHALLENGE";
    public static final String ALERT_TYPE_LUXURY_GIFT = "LUXURY_GIFT_RECEIVED";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");
            String alertType = intent.getStringExtra("alertType");

            if (body != null && !body.isEmpty()) {
                dispatchHighPriorityNotification(
                        this,
                        title != null ? title : "QLUVX Live Alert",
                        body,
                        alertType != null ? alertType : ALERT_TYPE_LIVE_STREAM
                );
            } else if ("com.google.firebase.MESSAGING_EVENT".equals(intent.getAction())) {
                String gcmTitle = intent.getStringExtra("gcm.notification.title");
                String gcmBody = intent.getStringExtra("gcm.notification.body");
                if (gcmBody != null) {
                    dispatchHighPriorityNotification(
                            this,
                            gcmTitle != null ? gcmTitle : "QLUVX Alert",
                            gcmBody,
                            ALERT_TYPE_LIVE_STREAM
                    );
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void ensureNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Real-time push alerts for disappearing photos, live streams, PK battles, and luxury gifts.");
                channel.enableLights(true);
                channel.setLightColor(Color.parseColor("#FF4081"));
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void dispatchHighPriorityNotification(@NonNull Context context,
                                                        @NonNull String title,
                                                        @NonNull String messageBody,
                                                        @NonNull String alertType) {
        ensureNotificationChannel(context);

        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        tapIntent.putExtra("fcm_alert_type", alertType);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (System.currentTimeMillis() & 0xFFFF),
                tapIntent,
                pendingFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(Color.parseColor("#FFD700"))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = (int) (System.currentTimeMillis() & 0x7FFFFFFF);
            manager.notify(notificationId, builder.build());
            Log.d(TAG, "Dispatched Push Notification [" + alertType + "]: " + messageBody);
        }
    }

    /**
     * Convenience helper to trigger any of the 4 official QLUVX FCM Push Notification templates.
     */
    public static void triggerPresetPushAlert(@NonNull Context context, @NonNull String alertType) {
        switch (alertType) {
            case ALERT_TYPE_DISAPPEARING_PHOTO:
                dispatchHighPriorityNotification(
                        context,
                        "🔒 Secret Disappearing Photo",
                        "Alex sent you a 5-second disappearing photo ⏱️",
                        ALERT_TYPE_DISAPPEARING_PHOTO
                );
                break;

            case ALERT_TYPE_LIVE_STREAM:
                dispatchHighPriorityNotification(
                        context,
                        "🔴 VIP Streamer Live Now",
                        "🔥 Julian Vance just went live! Tap to watch",
                        ALERT_TYPE_LIVE_STREAM
                );
                break;

            case ALERT_TYPE_PK_CHALLENGE:
                dispatchHighPriorityNotification(
                        context,
                        "⚔️ Live PK Battle Invitation",
                        "⚡ You have been challenged to a 5-minute PK battle!",
                        ALERT_TYPE_PK_CHALLENGE
                );
                break;

            case ALERT_TYPE_LUXURY_GIFT:
            default:
                dispatchHighPriorityNotification(
                        context,
                        "🎁 Luxury Gift Received",
                        "🎉 You received a Super Sports Car from Leo!",
                        ALERT_TYPE_LUXURY_GIFT
                );
                break;
        }
    }
}
