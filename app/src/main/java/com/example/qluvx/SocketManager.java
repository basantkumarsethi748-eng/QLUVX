package com.example.qluvx;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Persistent Socket.io Manager for QLUVX Real-Time Events:
 * - Live stream comments & VIP entrance banners
 * - Live PK Battle Red vs Blue Tug-of-War score updates
 * - 1-on-1 private messaging & 5-second countdown disappearing photos
 */
public class SocketManager {

    private static final String TAG = "SocketManager";
    private static volatile SocketManager instance;

    public interface SocketEventListener {
        default void onLiveCommentReceived(String roomId, String senderName, String commentText) {}
        default void onPkScoreUpdated(String battleId, int redScore, int blueScore, String lastGiftName) {}
        default void onPrivateMessageReceived(String senderId, String senderName, String message, boolean isDisappearingPhoto, int ttlSeconds) {}
        default void onVipEntranceReceived(String roomId, String vipName, int wealthLevel, String rideName) {}
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<SocketEventListener> listeners = new CopyOnWriteArrayList<>();
    private Socket socket;
    private String currentUserId = "QL-884920";

    private SocketManager() {
    }

    public static SocketManager getInstance() {
        if (instance == null) {
            synchronized (SocketManager.class) {
                if (instance == null) {
                    instance = new SocketManager();
                }
            }
        }
        return instance;
    }

    public synchronized void connect(String userId, String token) {
        if (userId != null && !userId.isEmpty()) {
            this.currentUserId = userId;
        }
        if (socket != null && socket.connected()) {
            return;
        }
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 2000;
            options.timeout = 8000;
            if (token != null && !token.isEmpty()) {
                options.auth = Collections.singletonMap("token", token);
            }

            String serverUrl = ApiClient.getResolvedBaseUrl();
            socket = IO.socket(serverUrl, options);
            registerSocketEvents();
            socket.connect();
        } catch (URISyntaxException e) {
            Log.w(TAG, "Socket.io URI syntax warning: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "Socket.io init warning: " + e.getMessage());
        }
    }

    private void registerSocketEvents() {
        if (socket == null) return;

        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Socket.io connected for user: " + currentUserId);
            try {
                JSONObject presence = new JSONObject();
                presence.put("userId", currentUserId);
                presence.put("status", "online");
                socket.emit("user_online", presence);
            } catch (JSONException ignored) {
            }
        });

        socket.on("live_comment", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                JSONObject data = (JSONObject) args[0];
                final String roomId = data.optString("roomId", "qluvx_room");
                final String senderName = data.optString("senderName", "VIP Guest");
                final String comment = data.optString("comment", "");
                mainHandler.post(() -> {
                    for (SocketEventListener l : listeners) {
                        l.onLiveCommentReceived(roomId, senderName, comment);
                    }
                });
            }
        });

        socket.on("pk_score_update", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                JSONObject data = (JSONObject) args[0];
                final String battleId = data.optString("battleId", "pk_main");
                final int redScore = data.optInt("redScore", 14500);
                final int blueScore = data.optInt("blueScore", 11200);
                final String giftName = data.optString("giftName", "Rose");
                mainHandler.post(() -> {
                    for (SocketEventListener l : listeners) {
                        l.onPkScoreUpdated(battleId, redScore, blueScore, giftName);
                    }
                });
            }
        });

        socket.on("private_message", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                JSONObject data = (JSONObject) args[0];
                final String senderId = data.optString("senderId", "");
                final String senderName = data.optString("senderName", "Match");
                final String message = data.optString("message", "");
                final boolean isBurn = data.optBoolean("isDisappearingPhoto", false);
                final int ttl = data.optInt("ttlSeconds", 5);
                mainHandler.post(() -> {
                    for (SocketEventListener l : listeners) {
                        l.onPrivateMessageReceived(senderId, senderName, message, isBurn, ttl);
                    }
                });
            }
        });

        socket.on("vip_entrance", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                JSONObject data = (JSONObject) args[0];
                final String roomId = data.optString("roomId", "qluvx_room");
                final String vipName = data.optString("vipName", "Julian Vance");
                final int level = data.optInt("wealthLevel", 50);
                final String ride = data.optString("rideName", "Interstellar Starship X");
                mainHandler.post(() -> {
                    for (SocketEventListener l : listeners) {
                        l.onVipEntranceReceived(roomId, vipName, level, ride);
                    }
                });
            }
        });
    }

    public void addListener(SocketEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SocketEventListener listener) {
        listeners.remove(listener);
    }

    public void joinRoom(String roomId, String userId, String userName) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("roomId", roomId);
            payload.put("userId", userId);
            payload.put("userName", userName);
            payload.put("ride", "Interstellar Starship X");
            if (socket != null && socket.connected()) {
                socket.emit("join_room", payload);
            }
        } catch (JSONException ignored) {
        }
    }

    public void leaveRoom(String roomId, String userId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("roomId", roomId);
            payload.put("userId", userId);
            if (socket != null && socket.connected()) {
                socket.emit("leave_room", payload);
            }
        } catch (JSONException ignored) {
        }
    }

    public void sendLiveComment(String roomId, String userId, String userName, String comment) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("roomId", roomId);
            payload.put("userId", userId);
            payload.put("senderName", userName);
            payload.put("comment", comment);
            if (socket != null && socket.connected()) {
                socket.emit("live_comment", payload);
            }
        } catch (JSONException ignored) {
        }
    }

    public void sendPkGiftVote(String battleId, String team, String giftName, int points) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("battleId", battleId);
            payload.put("team", team);
            payload.put("giftName", giftName);
            payload.put("points", points);
            payload.put("senderId", currentUserId);
            if (socket != null && socket.connected()) {
                socket.emit("send_pk_gift", payload);
            }
        } catch (JSONException ignored) {
        }
    }

    public void sendPrivateMessage(
            String senderId,
            String receiverId,
            String message,
            boolean isDisappearingPhoto,
            int ttlSeconds
    ) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("senderId", senderId);
            payload.put("receiverId", receiverId);
            payload.put("message", message);
            payload.put("isDisappearingPhoto", isDisappearingPhoto);
            payload.put("ttlSeconds", ttlSeconds);
            if (socket != null && socket.connected()) {
                socket.emit("private_message", payload);
            }
        } catch (JSONException ignored) {
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }
}
