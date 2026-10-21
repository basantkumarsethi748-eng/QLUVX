package com.example.qluvx;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VoiceRoomModels {

    public static final int TOTAL_ROOM_SEATS = 12; // 0 = Host, 1 = Co-Host, 2..11 = Speakers

    public enum RoomMode {
        VOICE_ROOM,
        LIVE_VIDEO
    }

    public static class WallpaperTheme {
        public final String id;
        public final String name;
        public final int topGradientColor;
        public final int bottomGradientColor;
        public final int accentColor;

        public WallpaperTheme(String id, String name, int topGradientColor, int bottomGradientColor, int accentColor) {
            this.id = id;
            this.name = name;
            this.topGradientColor = topGradientColor;
            this.bottomGradientColor = bottomGradientColor;
            this.accentColor = accentColor;
        }
    }

    public static final WallpaperTheme[] WALLPAPER_THEMES = new WallpaperTheme[]{
            new WallpaperTheme("obsidian_neon", "Obsidian Neon (Default)", Color.rgb(22, 10, 40), Color.rgb(8, 8, 14), Color.rgb(255, 55, 180)),
            new WallpaperTheme("cyber_matrix", "Cyberpunk Matrix", Color.rgb(6, 26, 38), Color.rgb(6, 10, 18), Color.rgb(0, 229, 255)),
            new WallpaperTheme("royal_gold", "Royal Velvet Gold", Color.rgb(36, 14, 24), Color.rgb(12, 8, 10), Color.rgb(255, 215, 64)),
            new WallpaperTheme("emerald_aurora", "Emerald Aurora", Color.rgb(6, 32, 24), Color.rgb(8, 12, 16), Color.rgb(0, 230, 118)),
            new WallpaperTheme("midnight_galaxy", "Midnight Galaxy", Color.rgb(16, 14, 48), Color.rgb(8, 8, 18), Color.rgb(124, 77, 255)),
            new WallpaperTheme("crimson_studio", "Crimson Studio", Color.rgb(40, 10, 20), Color.rgb(10, 6, 10), Color.rgb(255, 64, 96))
    };

    public static class SpeakerSeat {
        public final int seatIndex; // 0 = Host, 1 = Co-Host, 2..11 = Speaker seats
        public int uid;
        public String userName;
        public int avatarColor;
        public boolean isOccupied;
        public boolean isMuted;
        public boolean isVideoEnabled;
        public boolean isLocked;
        public int speakingVolume; // 0 - 255 from real Agora onAudioVolumeIndication
        public String roleTag;

        public SpeakerSeat(int seatIndex) {
            this.seatIndex = seatIndex;
            this.uid = 0;
            this.userName = "";
            this.avatarColor = Color.rgb(124, 77, 255);
            this.isOccupied = false;
            this.isMuted = false;
            this.isVideoEnabled = false;
            this.isLocked = false;
            this.speakingVolume = 0;
            this.roleTag = defaultRoleForIndex(seatIndex);
        }

        public static String defaultRoleForIndex(int idx) {
            if (idx == 0) return "HOST";
            if (idx == 1) return "CO-HOST";
            return "SEAT " + idx;
        }

        public void assignUser(int uid, String name, int color, boolean muted, String roleTag) {
            this.uid = uid;
            this.userName = name;
            this.avatarColor = color;
            this.isOccupied = true;
            this.isMuted = muted;
            this.speakingVolume = 0;
            this.roleTag = roleTag;
        }

        public void vacate() {
            this.uid = 0;
            this.userName = "";
            this.isOccupied = false;
            this.isMuted = false;
            this.isVideoEnabled = false;
            this.speakingVolume = 0;
            this.roleTag = defaultRoleForIndex(seatIndex);
        }
    }

    public static class HandRaiseRequest {
        public final int uid;
        public final String userName;
        public final int targetSeatIndex;
        public String status; // "RAISED", "INVITED", "ACCEPTED", "REJECTED"
        public final long timestampMs;

        public HandRaiseRequest(int uid, String userName, int targetSeatIndex, String status) {
            this.uid = uid;
            this.userName = userName;
            this.targetSeatIndex = targetSeatIndex;
            this.status = status;
            this.timestampMs = System.currentTimeMillis();
        }
    }

    public static class ChatMessage {
        public final int senderUid;
        public final String senderName;
        public final int senderColor;
        public final String roleBadge;
        public final String message;
        public final boolean isSystem;

        public ChatMessage(String senderName, int senderColor, String roleBadge, String message, boolean isSystem) {
            this(0, senderName, senderColor, roleBadge, message, isSystem);
        }

        public ChatMessage(int senderUid, String senderName, int senderColor, String roleBadge, String message, boolean isSystem) {
            this.senderUid = senderUid;
            this.senderName = senderName;
            this.senderColor = senderColor;
            this.roleBadge = roleBadge;
            this.message = message;
            this.isSystem = isSystem;
        }
    }

    public static class RoomInfo {
        public final String id;
        public String title;
        public String coverTagline;
        public String channelName;
        public String category;
        public RoomMode mode;
        public WallpaperTheme wallpaper;
        public int accentColor;
        public String welcomeNotice;
        public final List<SpeakerSeat> seats = new ArrayList<>();
        public final List<ChatMessage> messages = new ArrayList<>();
        public final List<HandRaiseRequest> handRaises = new ArrayList<>();
        public final Set<Integer> chatMutedUids = new HashSet<>();
        public final List<String> moderationLog = new ArrayList<>();

        public RoomInfo(String id, String title, String channelName, String category,
                        RoomMode mode, WallpaperTheme wallpaper, String welcomeNotice) {
            this.id = id;
            this.title = title;
            this.coverTagline = category.toUpperCase() + " • 12-SEAT STAGE";
            this.channelName = channelName;
            this.category = category;
            this.mode = mode;
            this.wallpaper = wallpaper != null ? wallpaper : WALLPAPER_THEMES[0];
            this.accentColor = this.wallpaper.accentColor;
            this.welcomeNotice = welcomeNotice;

            for (int i = 0; i < TOTAL_ROOM_SEATS; i++) {
                seats.add(new SpeakerSeat(i));
            }
        }

        public int getActiveSpeakerCount() {
            int count = 0;
            for (SpeakerSeat s : seats) {
                if (s.isOccupied) count++;
            }
            return count;
        }

        public void clearAllSeats() {
            for (SpeakerSeat s : seats) {
                s.vacate();
            }
        }
    }

    public static class RoomRepository {
        public int assignedLocalUid = 0;
        public String localUserName = "You";
        public int localAvatarColor = Color.rgb(255, 55, 180);
        private final List<RoomInfo> rooms = new ArrayList<>();

        public RoomRepository() {
            seedDefaultChannels();
        }

        private void seedDefaultChannels() {
            String defaultChannel = AgoraVoiceManager.getDefaultChannelName();

            RoomInfo r1 = new RoomInfo(
                    "room_default",
                    "QLUVX Main 12-Seat Stage (" + defaultChannel + ")",
                    defaultChannel,
                    "Music",
                    RoomMode.VOICE_ROOM,
                    WALLPAPER_THEMES[0],
                    "Real Agora RTC 12-Seat Voice & Video Room '" + defaultChannel + "'. Supports Host, Co-Host, Speakers, Listeners, Gifts & Games."
            );
            rooms.add(r1);

            RoomInfo r2 = new RoomInfo(
                    "room_video_live",
                    "QLUVX Live Video Broadcast Stage",
                    "qluvx_video_live",
                    "Video",
                    RoomMode.LIVE_VIDEO,
                    WALLPAPER_THEMES[1],
                    "Real Agora RTC Live Video Room 'qluvx_video_live'. Publish camera & mic and view remote participant video."
            );
            rooms.add(r2);

            RoomInfo r3 = new RoomInfo(
                    "room_chill",
                    "Late Night Lounge & Games",
                    "qluvx_chill_vibes",
                    "Chill",
                    RoomMode.VOICE_ROOM,
                    WALLPAPER_THEMES[4],
                    "Agora RTC 12-seat voice room with Ludo, Carrom, and 8 Ball Pool."
            );
            rooms.add(r3);

            RoomInfo r4 = new RoomInfo(
                    "room_gaming",
                    "Pro Gaming Squad Voice & Ludo",
                    "qluvx_gaming_squad",
                    "Gaming",
                    RoomMode.VOICE_ROOM,
                    WALLPAPER_THEMES[3],
                    "Low-latency Agora RTC channel 'qluvx_gaming_squad' for multiplayer games & voice."
            );
            rooms.add(r4);
        }

        public List<RoomInfo> getRooms(String categoryFilter) {
            if (categoryFilter == null || categoryFilter.equals("All")) {
                return new ArrayList<>(rooms);
            }
            List<RoomInfo> filtered = new ArrayList<>();
            for (RoomInfo room : rooms) {
                if (room.category.equalsIgnoreCase(categoryFilter)) {
                    filtered.add(room);
                }
            }
            return filtered;
        }

        public RoomInfo createOrFindRoom(String title, String channelName, String category, RoomMode mode) {
            String cleanChannel = AgoraVoiceManager.sanitizeChannelName(channelName);
            if (cleanChannel.isEmpty()) {
                cleanChannel = AgoraVoiceManager.getDefaultChannelName();
            }
            for (RoomInfo existing : rooms) {
                if (existing.channelName.equals(cleanChannel)) {
                    if (title != null && !title.trim().isEmpty()) {
                        existing.title = title.trim();
                    }
                    if (mode != null) {
                        existing.mode = mode;
                    }
                    return existing;
                }
            }
            String cleanTitle = (title == null || title.trim().isEmpty())
                    ? ("Room • " + cleanChannel)
                    : title.trim();
            WallpaperTheme theme = mode == RoomMode.LIVE_VIDEO ? WALLPAPER_THEMES[1] : WALLPAPER_THEMES[0];
            RoomInfo newRoom = new RoomInfo(
                    "room_" + (rooms.size() + 1),
                    cleanTitle,
                    cleanChannel,
                    category == null || category.trim().isEmpty() ? "Music" : category.trim(),
                    mode != null ? mode : RoomMode.VOICE_ROOM,
                    theme,
                    "Real Agora RTC Channel '" + cleanChannel + "' (" + (mode == RoomMode.LIVE_VIDEO ? "Live Video" : "12-Seat Voice") + ")."
            );
            rooms.add(0, newRoom);
            return newRoom;
        }
    }
}
