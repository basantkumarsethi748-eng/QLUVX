package com.example.qluvx;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class AgoraVoiceManager {
    private static final String TAG = "AgoraVoiceManager";

    public enum ConnectionStatus {
        CONNECTED,      // Green dot: Confirmed by Agora SDK (CONNECTION_STATE_CONNECTED / onJoinChannelSuccess)
        CONNECTING,     // Amber dot: CONNECTING or RECONNECTING
        DISCONNECTED    // Red dot: DISCONNECTED or FAILED
    }

    public interface VoiceEventListener {
        void onConnectionStateChanged(ConnectionStatus status, String statusText, int realRttMs, String diagnosticDetail);
        void onLocalJoinedSuccess(String channel, int assignedUid, int elapsedMs);
        void onRemoteUserJoined(int uid);
        void onRemoteUserLeft(int uid, int reason);
        void onRemoteUserMuteChanged(int uid, boolean muted);
        void onRemoteVideoStateChanged(int uid, boolean videoActive);
        void onLocalVolumeUpdated(int volume255);
        void onRemoteVolumeUpdated(int uid, int volume255);
        void onDataStreamPacketReceived(int fromUid, String jsonPayload);
        void onRtcEventLog(String logMessage);
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VoiceEventListener listener;

    private RtcEngine rtcEngine;
    private boolean isAgoraEngineInitialized = false;
    private boolean isChannelConnected = false;
    private boolean isBroadcaster = false;
    private boolean isVideoRoomMode = false;
    private boolean isLocalVideoEnabled = false;
    private boolean isFrontCamera = true;
    private boolean isLocalMuted = true;
    private boolean isRemoteAudioMuted = false;
    private boolean isSpeakerphoneOn = true;
    private int micSignalGain = 100;
    private int dataStreamId = -1;

    private String currentChannelName = "";
    private int assignedLocalUid = 0;
    private String sessionOverrideToken = null;
    private boolean triedFallbackWithoutToken = false;

    private ConnectionStatus currentStatus = ConnectionStatus.DISCONNECTED;
    private String currentStatusText = "Disconnected";
    private String lastDiagnosticDetail = "Not connected to a channel.";
    private int lastRealRttMs = -1; // -1 = no real SDK latency reported yet
    private int lastTxKbps = 0;
    private int lastRxKbps = 0;
    private final Set<Integer> remoteUids = new LinkedHashSet<>();

    public AgoraVoiceManager(Context context, VoiceEventListener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    public static String cleanSecretValue(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }

    public static String getCleanAppId() {
        try {
            String raw = cleanSecretValue(BuildConfig.AGORA_APP_ID);
            if (raw.equals("YOUR_AGORA_APP_ID") || raw.equals("MY_NEW_API_KEY_DEFAULT_VALUE")) {
                return "";
            }
            return raw;
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean isAgoraAppIdValidFormat() {
        String appId = getCleanAppId();
        return appId.matches("^[0-9a-fA-F]{32}$");
    }

    public static boolean isAgoraAppIdConfigured() {
        return !getCleanAppId().isEmpty();
    }

    public static String getConfiguredAppIdPreview() {
        String appId = getCleanAppId();
        if (appId.isEmpty()) {
            return "Not configured (Set AGORA_APP_ID in Secrets)";
        }
        if (!isAgoraAppIdValidFormat()) {
            return "Invalid format (" + appId.length() + " chars, needs 32 hex chars)";
        }
        return appId.substring(0, 4) + "••••" + appId.substring(appId.length() - 4);
    }

    public static String getDefaultChannelName() {
        try {
            String raw = cleanSecretValue(BuildConfig.AGORA_DEFAULT_CHANNEL);
            if (!raw.isEmpty() && !raw.equals("YOUR_AGORA_DEFAULT_CHANNEL")) {
                return sanitizeChannelName(raw);
            }
        } catch (Throwable ignored) {
        }
        return "qluvx_live";
    }

    public static String getConfiguredTempToken() {
        try {
            String raw = cleanSecretValue(BuildConfig.AGORA_TEMP_TOKEN);
            if (raw.isEmpty() || raw.equals("YOUR_AGORA_TEMP_TOKEN") || raw.equals("null")) {
                return "";
            }
            return raw;
        } catch (Throwable t) {
            return "";
        }
    }

    public static String getConfiguredTokenServerUrl() {
        try {
            String raw = cleanSecretValue(BuildConfig.AGORA_TOKEN_SERVER_URL);
            if (raw.isEmpty() || raw.equals("YOUR_AGORA_TOKEN_SERVER_URL") || raw.equals("null")) {
                return "";
            }
            return raw;
        } catch (Throwable t) {
            return "";
        }
    }

    public static String getConfiguredBackendApiUrl() {
        try {
            String raw = cleanSecretValue(BuildConfig.QLUVX_BACKEND_API_URL);
            if (raw.isEmpty() || raw.equals("YOUR_QLUVX_BACKEND_API_URL") || raw.equals("null")) {
                return "";
            }
            return raw;
        } catch (Throwable t) {
            return "";
        }
    }

    public static String sanitizeChannelName(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned;
    }

    public boolean hasInternetConnection() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Throwable t) {
            return true;
        }
    }

    public boolean hasMicPermission() {
        return appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasCameraPermission() {
        return appContext.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void setSessionOverrideToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            this.sessionOverrideToken = null;
        } else {
            this.sessionOverrideToken = cleanSecretValue(token);
        }
    }

    public String getSessionOverrideToken() {
        return sessionOverrideToken;
    }

    private void updateStatus(ConnectionStatus status, String text, int realRttMs, String diagnostic) {
        this.currentStatus = status;
        this.currentStatusText = text;
        this.lastRealRttMs = realRttMs;
        if (diagnostic != null && !diagnostic.isEmpty()) {
            this.lastDiagnosticDetail = diagnostic;
        }
        mainHandler.post(() -> listener.onConnectionStateChanged(status, text, realRttMs, this.lastDiagnosticDetail));
    }

    public ConnectionStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getCurrentStatusText() {
        return currentStatusText;
    }

    public String getLastDiagnosticDetail() {
        return lastDiagnosticDetail;
    }

    public int getLastRealRttMs() {
        return lastRealRttMs;
    }

    public int getLastTxKbps() {
        return lastTxKbps;
    }

    public int getLastRxKbps() {
        return lastRxKbps;
    }

    public int getAssignedLocalUid() {
        return assignedLocalUid;
    }

    public int getRemoteUserCount() {
        return remoteUids.size();
    }

    public List<Integer> getRemoteUidList() {
        return new ArrayList<>(remoteUids);
    }

    public String getSdkVersionString() {
        try {
            return RtcEngine.getSdkVersion();
        } catch (Throwable t) {
            return "4.5.0";
        }
    }

    public static String describeConnectionReason(int reason) {
        switch (reason) {
            case Constants.CONNECTION_CHANGED_CONNECTING:
                return "Establishing connection to Agora edge server (0)";
            case Constants.CONNECTION_CHANGED_JOIN_SUCCESS:
                return "Successfully joined Agora channel (1)";
            case Constants.CONNECTION_CHANGED_INTERRUPTED:
                return "Network connection interrupted (2)";
            case Constants.CONNECTION_CHANGED_BANNED_BY_SERVER:
                return "Connection banned by server (3)";
            case Constants.CONNECTION_CHANGED_JOIN_FAILED:
                return "Failed to join channel after retries (4)";
            case Constants.CONNECTION_CHANGED_LEAVE_CHANNEL:
                return "Left channel cleanly (5)";
            case Constants.CONNECTION_CHANGED_INVALID_APP_ID:
                return "Invalid App ID (6): Verify AGORA_APP_ID is an active 32-hex App ID";
            case Constants.CONNECTION_CHANGED_INVALID_CHANNEL_NAME:
                return "Invalid channel name (7): Must be <= 64 ASCII chars";
            case Constants.CONNECTION_CHANGED_INVALID_TOKEN:
                return "Invalid Token (8): Token does not match App ID, Channel Name, or UID=0";
            case Constants.CONNECTION_CHANGED_TOKEN_EXPIRED:
                return "Token Expired (9): Generate a new RTC token for this channel";
            case Constants.CONNECTION_CHANGED_REJECTED_BY_SERVER:
                return "Rejected by Agora Server (10): Check if your Agora project requires an RTC Token (App Certificate enabled) or if AGORA_TEMP_TOKEN was generated for a different Channel Name / UID.";
            case Constants.CONNECTION_CHANGED_SETTING_PROXY_SERVER:
                return "Setting proxy server (11)";
            case Constants.CONNECTION_CHANGED_RENEW_TOKEN:
                return "Renewing RTC token (12)";
            case Constants.CONNECTION_CHANGED_CLIENT_IP_ADDRESS_CHANGED:
                return "Client IP address changed (13)";
            case Constants.CONNECTION_CHANGED_KEEP_ALIVE_TIMEOUT:
                return "Keep-alive timeout with Agora edge server (14)";
            default:
                return "Connection reason code " + reason;
        }
    }

    public static String describeAgoraError(int err) {
        switch (err) {
            case Constants.ERR_INVALID_APP_ID:
                return "ERR_INVALID_APP_ID (101): App ID is invalid.";
            case Constants.ERR_INVALID_CHANNEL_NAME:
                return "ERR_INVALID_CHANNEL_NAME (102): Channel name is invalid.";
            case Constants.ERR_NO_SERVER_RESOURCES:
                return "ERR_NO_SERVER_RESOURCES (103): No server resources available.";
            case Constants.ERR_TOKEN_EXPIRED:
                return "ERR_TOKEN_EXPIRED (109): The RTC token has expired.";
            case Constants.ERR_INVALID_TOKEN:
                return "ERR_INVALID_TOKEN (110): App Certificate is enabled in Agora Console, so a valid token matching this exact Channel Name and UID=0 is required.";
            case Constants.ERR_NOT_INITIALIZED:
                return "ERR_NOT_INITIALIZED (7): Agora RtcEngine is not initialized.";
            default:
                return "Agora error code " + err;
        }
    }

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            mainHandler.post(() -> {
                isChannelConnected = true;
                assignedLocalUid = uid;
                initDataStreamIfNeeded();
                String diag = "Joined channel '" + channel + "' as UID " + uid + " in " + elapsed + "ms.";
                updateStatus(ConnectionStatus.CONNECTED, "CONNECTED • UID " + uid, lastRealRttMs, diag);
                listener.onLocalJoinedSuccess(channel, uid, elapsed);
                listener.onRtcEventLog("Agora Confirmed: Joined '" + channel + "' (Assigned UID: " + uid + ").");
            });
        }

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            mainHandler.post(() -> {
                String reasonDesc = describeConnectionReason(reason);
                if (state == Constants.CONNECTION_STATE_CONNECTED) {
                    isChannelConnected = true;
                    updateStatus(
                            ConnectionStatus.CONNECTED,
                            "CONNECTED" + (assignedLocalUid != 0 ? (" • UID " + assignedLocalUid) : ""),
                            lastRealRttMs,
                            reasonDesc
                    );
                } else if (state == Constants.CONNECTION_STATE_CONNECTING) {
                    isChannelConnected = false;
                    updateStatus(ConnectionStatus.CONNECTING, "CONNECTING…", -1, reasonDesc);
                } else if (state == Constants.CONNECTION_STATE_RECONNECTING) {
                    isChannelConnected = false;
                    updateStatus(ConnectionStatus.CONNECTING, "RECONNECTING…", -1, reasonDesc);
                    listener.onRtcEventLog("Agora Reconnecting: " + reasonDesc);
                } else if (state == Constants.CONNECTION_STATE_FAILED) {
                    isChannelConnected = false;
                    lastRealRttMs = -1;

                    String activeToken = resolveStaticToken();
                    if ((reason == Constants.CONNECTION_CHANGED_REJECTED_BY_SERVER
                            || reason == Constants.CONNECTION_CHANGED_INVALID_TOKEN)
                            && activeToken != null && !triedFallbackWithoutToken && rtcEngine != null) {
                        triedFallbackWithoutToken = true;
                        listener.onRtcEventLog("Token rejected for channel '" + currentChannelName + "' (Reason " + reason + "). Retrying with App-ID-only mode…");
                        try {
                            rtcEngine.leaveChannel();
                            ChannelMediaOptions options = buildMediaOptions(isBroadcaster && hasMicPermission());
                            rtcEngine.joinChannel(null, currentChannelName, 0, options);
                            return;
                        } catch (Throwable ignored) {
                        }
                    }

                    String detail = reasonDesc + " [Channel: '" + currentChannelName + "', UID: 0]";
                    updateStatus(ConnectionStatus.DISCONNECTED, "FAILED (" + reason + ")", -1, detail);
                    listener.onRtcEventLog("Connection Failed (" + reason + "): " + reasonDesc);
                } else if (state == Constants.CONNECTION_STATE_DISCONNECTED) {
                    isChannelConnected = false;
                    lastRealRttMs = -1;
                    updateStatus(ConnectionStatus.DISCONNECTED, "DISCONNECTED", -1, reasonDesc);
                }
            });
        }

        @Override
        public void onConnectionLost() {
            mainHandler.post(() -> {
                isChannelConnected = false;
                lastRealRttMs = -1;
                String diag = "Connection lost to Agora server.";
                updateStatus(ConnectionStatus.DISCONNECTED, "CONNECTION LOST", -1, diag);
                listener.onRtcEventLog(diag);
            });
        }

        @Override
        public void onRtcStats(RtcStats stats) {
            if (stats == null || !isChannelConnected) return;
            mainHandler.post(() -> {
                if (currentStatus == ConnectionStatus.CONNECTED) {
                    lastTxKbps = stats.txAudioKBitRate + stats.txVideoKBitRate;
                    lastRxKbps = stats.rxAudioKBitRate + stats.rxVideoKBitRate;
                    int realDelay = stats.lastmileDelay > 0 ? stats.lastmileDelay : -1;
                    updateStatus(
                            ConnectionStatus.CONNECTED,
                            "CONNECTED" + (assignedLocalUid != 0 ? (" • UID " + assignedLocalUid) : ""),
                            realDelay,
                            lastDiagnosticDetail
                    );
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            mainHandler.post(() -> {
                remoteUids.add(uid);
                listener.onRemoteUserJoined(uid);
                listener.onRtcEventLog("Remote user UID " + uid + " joined the channel.");
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            mainHandler.post(() -> {
                remoteUids.remove(uid);
                listener.onRemoteUserLeft(uid, reason);
                listener.onRtcEventLog("Remote user UID " + uid + " left the channel.");
            });
        }

        @Override
        public void onUserMuteAudio(int uid, boolean muted) {
            mainHandler.post(() -> {
                listener.onRemoteUserMuteChanged(uid, muted);
                listener.onRtcEventLog("Remote user UID " + uid + (muted ? " muted mic." : " unmuted mic."));
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            mainHandler.post(() -> {
                boolean active = (state == Constants.REMOTE_VIDEO_STATE_STARTING
                        || state == Constants.REMOTE_VIDEO_STATE_DECODING);
                listener.onRemoteVideoStateChanged(uid, active);
            });
        }

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            if (speakers == null) return;
            mainHandler.post(() -> {
                boolean localFound = false;
                for (AudioVolumeInfo info : speakers) {
                    if (info.uid == 0) {
                        localFound = true;
                        listener.onLocalVolumeUpdated(isLocalMuted ? 0 : info.volume);
                    } else {
                        listener.onRemoteVolumeUpdated(info.uid, info.volume);
                    }
                }
                if (!localFound) {
                    listener.onLocalVolumeUpdated(0);
                }
            });
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            if (data == null) return;
            final String payload = new String(data, StandardCharsets.UTF_8);
            mainHandler.post(() -> listener.onDataStreamPacketReceived(uid, payload));
        }

        @Override
        public void onTokenPrivilegeWillExpire(String token) {
            mainHandler.post(() -> listener.onRtcEventLog("Agora RTC token will expire soon — attempting backend renewal…"));
            renewTokenFromBackendIfConfigured();
        }

        @Override
        public void onRequestToken() {
            mainHandler.post(() -> listener.onRtcEventLog("Agora RTC token expired — requesting new token…"));
            renewTokenFromBackendIfConfigured();
        }

        @Override
        public void onError(int err) {
            mainHandler.post(() -> {
                String errDesc = describeAgoraError(err);
                lastDiagnosticDetail = errDesc;
                listener.onRtcEventLog(errDesc);
            });
        }
    };

    private void initDataStreamIfNeeded() {
        if (rtcEngine == null || !isChannelConnected) return;
        try {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.ordered = true;
            cfg.syncWithAudio = false;
            dataStreamId = rtcEngine.createDataStream(cfg);
        } catch (Throwable ignored) {
        }
    }

    public synchronized boolean broadcastDataPacket(JSONObject json) {
        if (rtcEngine == null || !isChannelConnected || json == null) return false;
        try {
            if (dataStreamId < 0) {
                initDataStreamIfNeeded();
            }
            if (dataStreamId >= 0) {
                byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                int res = rtcEngine.sendStreamMessage(dataStreamId, bytes);
                return res == 0;
            }
        } catch (Throwable e) {
            Log.w(TAG, "Error sending Agora stream message", e);
        }
        return false;
    }

    private ChannelMediaOptions buildMediaOptions(boolean broadcastAudio) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        options.clientRoleType = broadcastAudio
                ? Constants.CLIENT_ROLE_BROADCASTER
                : Constants.CLIENT_ROLE_AUDIENCE;
        options.publishMicrophoneTrack = broadcastAudio && !isLocalMuted;
        options.publishCameraTrack = broadcastAudio && isVideoRoomMode && isLocalVideoEnabled && hasCameraPermission();
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = isVideoRoomMode;
        return options;
    }

    private String resolveStaticToken() {
        if (sessionOverrideToken != null && !sessionOverrideToken.isEmpty()) {
            return sessionOverrideToken;
        }
        String envToken = getConfiguredTempToken();
        if (!envToken.isEmpty()) {
            return envToken;
        }
        return null;
    }

    public synchronized void joinVoiceRoom(String rawChannelName, boolean wantBroadcasterRole) {
        joinRoom(rawChannelName, wantBroadcasterRole, false);
    }

    public synchronized void joinRoom(String rawChannelName, boolean wantBroadcasterRole, boolean videoMode) {
        remoteUids.clear();
        assignedLocalUid = 0;
        lastRealRttMs = -1;
        lastTxKbps = 0;
        lastRxKbps = 0;
        dataStreamId = -1;
        triedFallbackWithoutToken = false;
        this.isVideoRoomMode = videoMode;

        // 1. Validate Channel Name
        String channelName = sanitizeChannelName(rawChannelName);
        if (channelName.isEmpty()) {
            updateStatus(ConnectionStatus.DISCONNECTED, "INVALID CHANNEL NAME", -1,
                    "Channel name cannot be empty. Use 1-64 ASCII characters.");
            return;
        }
        this.currentChannelName = channelName;

        // 2. Verify Internet Connectivity
        if (!hasInternetConnection()) {
            String diag = "No active internet connection detected on device.";
            updateStatus(ConnectionStatus.DISCONNECTED, "NO INTERNET", -1, diag);
            listener.onRtcEventLog(diag);
            return;
        }

        // 3. Verify Agora App ID
        if (!isAgoraAppIdConfigured()) {
            String diag = "AGORA_APP_ID is missing. Add your 32-character hex Agora App ID in the AI Studio Secrets panel.";
            updateStatus(ConnectionStatus.DISCONNECTED, "MISSING APP ID", -1, diag);
            listener.onRtcEventLog(diag);
            return;
        }
        if (!isAgoraAppIdValidFormat()) {
            String diag = "AGORA_APP_ID format is invalid (" + getCleanAppId().length()
                    + " chars). Agora App IDs must be exactly 32 hexadecimal characters (0-9, a-f).";
            updateStatus(ConnectionStatus.DISCONNECTED, "INVALID APP ID", -1, diag);
            listener.onRtcEventLog(diag);
            return;
        }

        // 4. Verify Microphone & Camera Permissions before enabling Broadcaster publishing
        boolean micPermitted = hasMicPermission();
        if (wantBroadcasterRole && !micPermitted) {
            listener.onRtcEventLog("Microphone permission not yet granted — joining as Audience until RECORD_AUDIO is allowed.");
        }
        this.isBroadcaster = wantBroadcasterRole && micPermitted;
        this.isLocalMuted = !this.isBroadcaster;
        this.isLocalVideoEnabled = videoMode && this.isBroadcaster && hasCameraPermission();

        updateStatus(ConnectionStatus.CONNECTING, "CONNECTING • " + channelName, -1,
                "Initializing Agora RtcEngine and joining '" + channelName + "' (UID=0)…");

        // 5. If backend token server is configured, fetch token asynchronously then join
        String tokenServerUrl = getConfiguredTokenServerUrl();
        if (!tokenServerUrl.isEmpty() && (sessionOverrideToken == null || sessionOverrideToken.isEmpty())) {
            int roleNum = this.isBroadcaster ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
            fetchTokenFromBackendAndJoin(tokenServerUrl, channelName, 0, roleNum);
        } else {
            executeRtcJoin(channelName, resolveStaticToken());
        }
    }

    private synchronized void executeRtcJoin(String channelName, String token) {
        try {
            if (rtcEngine == null) {
                RtcEngineConfig config = new RtcEngineConfig();
                config.mContext = appContext;
                config.mAppId = getCleanAppId();
                config.mEventHandler = rtcEventHandler;
                config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
                rtcEngine = RtcEngine.create(config);
                rtcEngine.setAudioProfile(
                        Constants.AUDIO_PROFILE_MUSIC_STANDARD,
                        Constants.AUDIO_SCENARIO_CHATROOM
                );
                rtcEngine.enableAudio();
                rtcEngine.enableAudioVolumeIndication(200, 3, true);
                rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
                isAgoraEngineInitialized = true;
            } else {
                try {
                    rtcEngine.leaveChannel();
                } catch (Throwable ignored) {
                }
            }

            if (isVideoRoomMode) {
                rtcEngine.enableVideo();
                VideoEncoderConfiguration vec = new VideoEncoderConfiguration(
                        VideoEncoderConfiguration.VD_640x360,
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                );
                rtcEngine.setVideoEncoderConfiguration(vec);
                rtcEngine.enableLocalVideo(isLocalVideoEnabled);
            } else {
                rtcEngine.disableVideo();
            }

            rtcEngine.enableLocalAudio(isBroadcaster);
            rtcEngine.muteLocalAudioStream(!isBroadcaster || isLocalMuted);

            ChannelMediaOptions options = buildMediaOptions(isBroadcaster);

            int joinResult = rtcEngine.joinChannel(token, channelName, 0, options);
            if (joinResult < 0) {
                String diag = "rtcEngine.joinChannel returned error " + joinResult + ": " + describeAgoraError(Math.abs(joinResult));
                updateStatus(ConnectionStatus.DISCONNECTED, "JOIN ERROR (" + joinResult + ")", -1, diag);
                listener.onRtcEventLog(diag);
            } else {
                String tokenMode = (token == null || token.isEmpty()) ? "App-ID-only (no token)" : "RTC Token";
                listener.onRtcEventLog("Joining Agora channel '" + channelName + "' with UID=0 (" + tokenMode + ")…");
            }
        } catch (Throwable e) {
            Log.e(TAG, "Agora RtcEngine join failure", e);
            String diag = "Agora SDK Initialization Exception: " + e.getMessage();
            updateStatus(ConnectionStatus.DISCONNECTED, "SDK INIT ERROR", -1, diag);
            listener.onRtcEventLog(diag);
        }
    }

    public synchronized SurfaceView createAndBindLocalVideoView(Context activityContext) {
        if (rtcEngine == null || !isAgoraEngineInitialized) return null;
        try {
            SurfaceView surfaceView = new SurfaceView(activityContext);
            surfaceView.setZOrderMediaOverlay(true);
            rtcEngine.enableVideo();
            rtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
            if (isLocalVideoEnabled && hasCameraPermission()) {
                rtcEngine.startPreview();
            }
            return surfaceView;
        } catch (Throwable e) {
            Log.w(TAG, "Failed to setup local video SurfaceView", e);
            return null;
        }
    }

    public synchronized SurfaceView createAndBindRemoteVideoView(Context activityContext, int remoteUid) {
        if (rtcEngine == null || !isAgoraEngineInitialized) return null;
        try {
            SurfaceView surfaceView = new SurfaceView(activityContext);
            rtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid));
            return surfaceView;
        } catch (Throwable e) {
            Log.w(TAG, "Failed to setup remote video SurfaceView", e);
            return null;
        }
    }

    public synchronized boolean setLocalVideoPublishing(boolean enabled) {
        if (enabled && !hasCameraPermission()) {
            listener.onRtcEventLog("CAMERA permission required before publishing video.");
            return false;
        }
        this.isVideoRoomMode = true;
        this.isLocalVideoEnabled = enabled;
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                if (enabled) {
                    rtcEngine.enableVideo();
                    rtcEngine.enableLocalVideo(true);
                    rtcEngine.muteLocalVideoStream(false);
                    rtcEngine.startPreview();
                } else {
                    rtcEngine.muteLocalVideoStream(true);
                    rtcEngine.enableLocalVideo(false);
                    rtcEngine.stopPreview();
                }
                ChannelMediaOptions options = buildMediaOptions(isBroadcaster);
                rtcEngine.updateChannelMediaOptions(options);
                listener.onRtcEventLog(enabled ? "Local camera publishing enabled via Agora SDK." : "Local camera turned off.");
                return true;
            } catch (Throwable e) {
                Log.w(TAG, "Error toggling local video", e);
            }
        }
        return false;
    }

    public synchronized void switchCameraDirection() {
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.switchCamera();
                isFrontCamera = !isFrontCamera;
                listener.onRtcEventLog("Switched camera to " + (isFrontCamera ? "Front" : "Rear") + " lens.");
            } catch (Throwable e) {
                Log.w(TAG, "Error switching camera", e);
            }
        }
    }

    private void fetchTokenFromBackendAndJoin(String baseUrl, String channelName, int uid, int role) {
        new Thread(() -> {
            String fetchedToken = requestTokenFromServer(baseUrl, channelName, uid, role);
            mainHandler.post(() -> {
                if (fetchedToken != null && !fetchedToken.isEmpty()) {
                    listener.onRtcEventLog("Fetched production RTC token from backend server.");
                    executeRtcJoin(channelName, fetchedToken);
                } else {
                    String fallback = resolveStaticToken();
                    listener.onRtcEventLog("Backend token server did not return a token; falling back to configured token.");
                    executeRtcJoin(channelName, fallback);
                }
            });
        }, "AgoraTokenFetcher").start();
    }

    private void renewTokenFromBackendIfConfigured() {
        String baseUrl = getConfiguredTokenServerUrl();
        if (baseUrl.isEmpty() || currentChannelName.isEmpty() || rtcEngine == null) return;
        int role = isBroadcaster ? Constants.CLIENT_ROLE_BROADCASTER : Constants.CLIENT_ROLE_AUDIENCE;
        new Thread(() -> {
            String newToken = requestTokenFromServer(baseUrl, currentChannelName, assignedLocalUid, role);
            if (newToken != null && !newToken.isEmpty()) {
                mainHandler.post(() -> {
                    if (rtcEngine != null) {
                        rtcEngine.renewToken(newToken);
                        listener.onRtcEventLog("Successfully renewed Agora RTC token from backend.");
                    }
                });
            }
        }, "AgoraTokenRenewer").start();
    }

    private String requestTokenFromServer(String baseUrl, String channelName, int uid, int role) {
        HttpURLConnection conn = null;
        try {
            String sep = baseUrl.contains("?") ? "&" : "?";
            String fullUrl = baseUrl + sep
                    + "channelName=" + URLEncoder.encode(channelName, "UTF-8")
                    + "&uid=" + uid
                    + "&role=" + role;
            conn = (HttpURLConnection) new URL(fullUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                String body = sb.toString().trim();
                if (body.startsWith("{")) {
                    JSONObject json = new JSONObject(body);
                    if (json.has("rtcToken")) return json.optString("rtcToken", "");
                    if (json.has("token")) return json.optString("token", "");
                    if (json.has("key")) return json.optString("key", "");
                } else {
                    return cleanSecretValue(body);
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "Token server request failed", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public synchronized boolean switchRole(boolean broadcaster) {
        if (broadcaster && !hasMicPermission()) {
            listener.onRtcEventLog("Cannot switch to Broadcaster until RECORD_AUDIO permission is granted.");
            return false;
        }
        this.isBroadcaster = broadcaster;
        this.isLocalMuted = !broadcaster;
        if (!broadcaster) {
            this.isLocalVideoEnabled = false;
            listener.onLocalVolumeUpdated(0);
        }

        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.enableLocalAudio(broadcaster);
                if (!broadcaster && isVideoRoomMode) {
                    rtcEngine.enableLocalVideo(false);
                    rtcEngine.stopPreview();
                }
                rtcEngine.setClientRole(broadcaster
                        ? Constants.CLIENT_ROLE_BROADCASTER
                        : Constants.CLIENT_ROLE_AUDIENCE);
                rtcEngine.muteLocalAudioStream(!broadcaster || isLocalMuted);
                ChannelMediaOptions mediaOptions = buildMediaOptions(broadcaster);
                rtcEngine.updateChannelMediaOptions(mediaOptions);
                listener.onRtcEventLog("Agora Role switched to " + (broadcaster ? "BROADCASTER (Mic Active)" : "AUDIENCE (Listener)"));
                return true;
            } catch (Throwable e) {
                Log.w(TAG, "Error switching Agora role", e);
                listener.onRtcEventLog("Failed to switch role: " + e.getMessage());
            }
        }
        return false;
    }

    public synchronized void setLocalMuted(boolean muted) {
        this.isLocalMuted = muted;
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.muteLocalAudioStream(muted);
                ChannelMediaOptions mediaOptions = buildMediaOptions(isBroadcaster);
                rtcEngine.updateChannelMediaOptions(mediaOptions);
                listener.onRtcEventLog(muted ? "Local microphone muted via Agora SDK." : "Local microphone unmuted via Agora SDK.");
            } catch (Throwable e) {
                Log.w(TAG, "Error muting local audio", e);
            }
        }
        if (muted) {
            listener.onLocalVolumeUpdated(0);
        }
    }

    public synchronized void setRemoteUserMuted(int remoteUid, boolean muted) {
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.muteRemoteAudioStream(remoteUid, muted);
                listener.onRtcEventLog((muted ? "Muted" : "Unmuted") + " remote audio stream for UID " + remoteUid);
            } catch (Throwable e) {
                Log.w(TAG, "Error muting remote user", e);
            }
        }
    }

    public synchronized void setAllRemoteAudioMuted(boolean muted) {
        this.isRemoteAudioMuted = muted;
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.muteAllRemoteAudioStreams(muted);
                listener.onRtcEventLog(muted ? "Muted all remote room audio." : "Unmuted all remote room audio.");
            } catch (Throwable e) {
                Log.w(TAG, "Error muting all remote streams", e);
            }
        }
    }

    public synchronized void setSpeakerphoneEnabled(boolean enabled) {
        this.isSpeakerphoneOn = enabled;
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.setEnableSpeakerphone(enabled);
                listener.onRtcEventLog("Audio route set to " + (enabled ? "Speakerphone" : "Earpiece") + ".");
            } catch (Throwable e) {
                Log.w(TAG, "Error toggling speakerphone", e);
            }
        }
    }

    public synchronized void setMicSignalGain(int gainPercent) {
        this.micSignalGain = Math.max(0, Math.min(200, gainPercent));
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.adjustRecordingSignalVolume(this.micSignalGain);
                listener.onRtcEventLog("Agora recording signal volume adjusted to " + this.micSignalGain + "%.");
            } catch (Throwable e) {
                Log.w(TAG, "Error adjusting mic gain", e);
            }
        }
    }

    public boolean isChannelConnected() {
        return isChannelConnected && currentStatus == ConnectionStatus.CONNECTED;
    }

    public boolean isBroadcaster() {
        return isBroadcaster;
    }

    public boolean isVideoRoomMode() {
        return isVideoRoomMode;
    }

    public boolean isLocalVideoEnabled() {
        return isLocalVideoEnabled;
    }

    public boolean isLocalMuted() {
        return isLocalMuted;
    }

    public boolean isRemoteAudioMuted() {
        return isRemoteAudioMuted;
    }

    public boolean isSpeakerphoneOn() {
        return isSpeakerphoneOn;
    }

    public int getMicSignalGain() {
        return micSignalGain;
    }

    public synchronized void leaveVoiceRoom() {
        remoteUids.clear();
        if (rtcEngine != null && isAgoraEngineInitialized) {
            try {
                rtcEngine.stopPreview();
                rtcEngine.leaveChannel();
            } catch (Throwable ignored) {
            }
        }
        isChannelConnected = false;
        isLocalVideoEnabled = false;
        assignedLocalUid = 0;
        dataStreamId = -1;
        currentStatus = ConnectionStatus.DISCONNECTED;
        currentStatusText = "DISCONNECTED";
        lastRealRttMs = -1;
        lastTxKbps = 0;
        lastRxKbps = 0;
    }

    public synchronized void destroy() {
        leaveVoiceRoom();
        if (rtcEngine != null) {
            try {
                RtcEngine.destroy();
            } catch (Throwable ignored) {
            }
            rtcEngine = null;
            isAgoraEngineInitialized = false;
        }
    }
}
