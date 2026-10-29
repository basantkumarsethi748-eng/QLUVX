package com.example.qluvx;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Handles fetching and renewing short-lived Agora RTC tokens and executing backend API calls
 * (account deletion, support ticket submission, wallet verification) against the trusted backend
 * configured via BuildConfig.AGORA_TOKEN_SERVER_URL.
 * Clearly distinguishes server-verified responses from offline/local-pending states.
 */
public class TokenServiceClient {
    private static final String TAG = "TokenServiceClient";

    public interface TokenCallback {
        void onTokenResult(String token, boolean fromBackendServer);
    }

    public interface ApiStatusCallback {
        void onResult(boolean serverConfirmed, String statusMessage);
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static boolean isBackendUrlConfigured() {
        try {
            String url = BuildConfig.AGORA_TOKEN_SERVER_URL;
            if (url == null) return false;
            String trimmed = url.trim();
            return !trimmed.isEmpty()
                    && !trimmed.equals("YOUR_AGORA_TOKEN_SERVER_URL")
                    && !trimmed.contains("your-token-server.example.com")
                    && (trimmed.startsWith("http://") || trimmed.startsWith("https://"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String getBackendBaseUrl() {
        if (!isBackendUrlConfigured()) return "";
        String raw = BuildConfig.AGORA_TOKEN_SERVER_URL.trim();
        int idx = raw.indexOf("/api/");
        if (idx > 0) {
            return raw.substring(0, idx);
        }
        if (raw.endsWith("/")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    public static String getFallbackTempToken() {
        try {
            String raw = BuildConfig.AGORA_TEMP_TOKEN;
            if (raw != null && !raw.trim().isEmpty()
                    && !raw.trim().equals("YOUR_AGORA_TEMP_TOKEN")) {
                return raw.trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void requestRtcToken(
            final String channelName,
            final int uid,
            final boolean isBroadcaster,
            final TokenCallback callback
    ) {
        if (!isBackendUrlConfigured()) {
            final String fallback = getFallbackTempToken();
            mainHandler.post(() -> callback.onTokenResult(fallback, false));
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BuildConfig.AGORA_TOKEN_SERVER_URL.trim());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(4500);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("channelName", channelName);
                payload.put("uid", uid);
                payload.put("role", isBroadcaster ? "BROADCASTER" : "AUDIENCE");

                byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    final String fetchedToken = json.optString("token", null);
                    if (fetchedToken != null && !fetchedToken.isEmpty()) {
                        mainHandler.post(() -> callback.onTokenResult(fetchedToken, true));
                        return;
                    }
                }
            } catch (Throwable e) {
                Log.w(TAG, "Token server unreachable, falling back to configured temp token: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            final String fallback = getFallbackTempToken();
            mainHandler.post(() -> callback.onTokenResult(fallback, false));
        }, "QLUVX-TokenClient").start();
    }

    /**
     * Invokes POST /api/account/delete on the backend server when configured.
     * If the backend server URL is not configured or unreachable, reports local deletion + pending server sync.
     */
    public static void requestAccountDeletion(
            final String userId,
            final String emailOrPhone,
            final String reason,
            final ApiStatusCallback callback
    ) {
        if (!isBackendUrlConfigured()) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(
                            false,
                            "Local account & profile data wiped. Remote backend deletion is marked PENDING (configure AGORA_TOKEN_SERVER_URL to sync with server)."
                    );
                }
            });
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String endpoint = getBackendBaseUrl() + "/api/account/delete";
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(4500);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("userId", userId);
                payload.put("emailOrPhone", emailOrPhone);
                payload.put("reason", reason);
                payload.put("confirmDelete", true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResult(true, "Account deletion confirmed by QLUVX backend server and local data wiped.");
                        }
                    });
                    return;
                }
            } catch (Throwable e) {
                Log.w(TAG, "Account deletion endpoint unreachable: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(
                            false,
                            "Local account wiped. Backend server was unreachable, so remote deletion is queued as PENDING."
                    );
                }
            });
        }, "QLUVX-AccountDelete").start();
    }

    /**
     * Invokes POST /api/support/ticket on the backend server when configured.
     */
    public static void submitSupportTicket(
            final String userId,
            final String category,
            final String subject,
            final String details,
            final ApiStatusCallback callback
    ) {
        if (!isBackendUrlConfigured()) {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(false, "Saved to device support log (Pending remote server sync — backend URL not configured).");
                }
            });
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String endpoint = getBackendBaseUrl() + "/api/support/ticket";
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(4500);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("userId", userId);
                payload.put("category", category);
                payload.put("subject", subject);
                payload.put("details", details);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResult(true, "Ticket registered on QLUVX support server ✓");
                        }
                    });
                    return;
                }
            } catch (Throwable e) {
                Log.w(TAG, "Support endpoint unreachable: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(false, "Saved locally (Server unreachable — queued for sync).");
                }
            });
        }, "QLUVX-SupportTicket").start();
    }
}
