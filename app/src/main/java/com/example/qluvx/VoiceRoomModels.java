package com.example.qluvx;

import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoiceRoomModels {

    public static final int TOTAL_SEATS = 12;

    public static final int[] AVATAR_DRAWABLES = new int[]{
            R.drawable.img_avatar_0,
            R.drawable.img_avatar_1,
            R.drawable.img_avatar_2,
            R.drawable.img_avatar_3,
            R.drawable.img_avatar_4,
            R.drawable.img_avatar_5,
            R.drawable.img_avatar_6,
            R.drawable.img_avatar_7,
            R.drawable.img_avatar_8,
            R.drawable.img_avatar_9,
            R.drawable.img_avatar_10,
            R.drawable.img_avatar_11
    };

    public static final int[] CHANNEL_COVER_DRAWABLES = new int[]{
            R.drawable.img_channel_0,
            R.drawable.img_channel_1,
            R.drawable.img_channel_2,
            R.drawable.img_channel_3,
            R.drawable.img_channel_4
    };

    public static final int[] CHANNEL_BG_DRAWABLES = new int[]{
            R.drawable.bg_channel_0,
            R.drawable.bg_channel_1,
            R.drawable.bg_channel_2,
            R.drawable.bg_channel_3,
            R.drawable.bg_channel_4
    };

    public static int getAvatarResId(int indexOrUid) {
        int idx = Math.abs(indexOrUid) % AVATAR_DRAWABLES.length;
        return AVATAR_DRAWABLES[idx];
    }

    public static int getChannelCoverResId(int index) {
        int idx = Math.abs(index) % CHANNEL_COVER_DRAWABLES.length;
        return CHANNEL_COVER_DRAWABLES[idx];
    }

    public static int getChannelBgResId(int index) {
        int idx = Math.abs(index) % CHANNEL_BG_DRAWABLES.length;
        return CHANNEL_BG_DRAWABLES[idx];
    }

    public enum UserRole {
        HOST("Host"),
        CO_HOST("Co-Host"),
        SPEAKER("Speaker"),
        LISTENER("Listener"),
        MODERATOR("Moderator");

        public final String displayLabel;

        UserRole(String displayLabel) {
            this.displayLabel = displayLabel;
        }
    }

    public static class SpeakerSeat {
        public final int seatIndex; // 0 = Host (Seat 1), 1..3 = Initial Guest, 4..7 = Expandable, 8..11 = Full-Capacity
        public int uid;
        public String userName;
        public int avatarColor;
        public int avatarResId;
        public boolean isOccupied;
        public boolean isMuted;
        public boolean isLocked;
        public int speakingVolume; // 0 - 255 from Agora onAudioVolumeIndication
        public boolean isSpeakingVad;
        public String roleTag;

        public SpeakerSeat(int seatIndex) {
            this.seatIndex = seatIndex;
            this.uid = 0;
            this.userName = "";
            this.avatarColor = Color.rgb(37, 99, 235);
            this.avatarResId = getAvatarResId(seatIndex);
            this.isOccupied = false;
            this.isMuted = false;
            this.isLocked = false;
            this.speakingVolume = 0;
            this.isSpeakingVad = false;
            this.roleTag = seatIndex == 0 ? "Host" : ("Seat " + (seatIndex + 1));
        }

        public void assignUser(int uid, String name, int color, boolean muted, String roleTag) {
            this.uid = uid;
            this.userName = name;
            this.avatarColor = color;
            this.avatarResId = getAvatarResId(uid + seatIndex);
            this.isOccupied = true;
            this.isMuted = muted;
            this.speakingVolume = 0;
            this.isSpeakingVad = false;
            this.roleTag = roleTag;
        }

        public void vacate() {
            this.uid = 0;
            this.userName = "";
            this.isOccupied = false;
            this.isMuted = false;
            this.speakingVolume = 0;
            this.isSpeakingVad = false;
            this.roleTag = seatIndex == 0 ? "Host" : ("Seat " + (seatIndex + 1));
        }
    }

    public static class ChatMessage {
        public final String senderName;
        public final String roleLabel;
        public final String text;
        public final String timestamp;
        public final boolean isSystemOrAnnouncement;
        public boolean isPinned;

        public ChatMessage(String senderName, String roleLabel, String text, boolean isSystemOrAnnouncement) {
            this.senderName = senderName;
            this.roleLabel = roleLabel;
            this.text = text;
            this.timestamp = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
            this.isSystemOrAnnouncement = isSystemOrAnnouncement;
            this.isPinned = false;
        }
    }

    public static class PkBattleState {
        public boolean isActive = false;
        public String hostTeamName = "Team Host";
        public String challengerTeamName = "Team Challenger";
        public int hostScore = 0;
        public int challengerScore = 0;
        public int remainingSeconds = 180;
    }

    public static class RoomInfo {
        public final String id;
        public String title;
        public String channelName;
        public String category; // Recommended, Popular, New, Friends, Music, Casual Chat, Gaming, Private Rooms
        public String language;
        public String topic;
        public String announcement;
        public String hostName;
        public int accentColor;
        public int coverResId;
        public int backgroundResId;
        public int listenerCount;
        public String subtitle;
        public boolean isPrivateLocked;
        public String roomPassword;
        public int activeSeatCapacity; // Capacity progression: 1, 4, 8, or 12
        public final List<SpeakerSeat> seats = new ArrayList<>(TOTAL_SEATS);
        public final List<ChatMessage> chatMessages = new ArrayList<>();
        public final List<String> speakerRequests = new ArrayList<>();
        public final PkBattleState pkBattle = new PkBattleState();

        public RoomInfo(String id, String title, String channelName, String category,
                        String hostName, int accentColor, int listenerCount, String subtitle) {
            this.id = id;
            this.title = title;
            this.channelName = channelName;
            this.category = category;
            this.language = "English / Hindi";
            this.topic = category;
            this.announcement = "Welcome to " + title + "! Please follow community guidelines.";
            this.hostName = hostName;
            this.accentColor = accentColor;
            this.coverResId = R.drawable.img_channel_0;
            this.backgroundResId = R.drawable.bg_channel_0;
            this.listenerCount = listenerCount;
            this.subtitle = subtitle;
            this.isPrivateLocked = false;
            this.roomPassword = "";
            this.activeSeatCapacity = 12;

            for (int i = 0; i < TOTAL_SEATS; i++) {
                seats.add(new SpeakerSeat(i));
            }
        }

        public int getOccupiedSeatCount() {
            int count = 0;
            for (int i = 0; i < Math.min(activeSeatCapacity, seats.size()); i++) {
                if (seats.get(i).isOccupied) count++;
            }
            return count;
        }
    }

    public static class RoomRepository {
        private static RoomRepository instance;

        public int localUid = 1001 + (int) (System.currentTimeMillis() % 8000);
        public String localUserName = "You";
        public int localAvatarColor = Color.rgb(14, 165, 233);
        private final List<RoomInfo> rooms = new ArrayList<>();
        private final List<String> moderationReports = new ArrayList<>();

        public static synchronized RoomRepository getInstance() {
            if (instance == null) {
                instance = new RoomRepository();
            }
            return instance;
        }

        public RoomRepository() {
            seedRooms();
        }

        private void seedRooms() {
            RoomInfo r1 = new RoomInfo(
                    "001",
                    "Agora Voice Chatroom • 001",
                    BuildConfig.AGORA_DEFAULT_CHANNEL,
                    "Recommended",
                    "Aarav",
                    Color.rgb(37, 99, 235),
                    38,
                    "12-Seat Official Voice Chatroom • Audio Mixing & Voice Changer"
            );
            r1.coverResId = R.drawable.img_channel_0;
            r1.backgroundResId = R.drawable.bg_channel_0;
            r1.language = "Hindi / English";
            r1.topic = "Live Music & Open Mic";
            r1.announcement = "Seat 1: Host • Seats 2-4: Guests • Seats 5-12: Open Stage. Use Mixing & Voice Changer below!";
            r1.seats.get(0).assignUser(201, "Aarav (Host)", Color.rgb(37, 99, 235), false, "Host");
            r1.seats.get(1).assignUser(202, "Zoya", Color.rgb(14, 165, 233), false, "Co-Host");
            r1.seats.get(2).assignUser(203, "Kabir", Color.rgb(16, 185, 129), false, "Speaker");
            r1.seats.get(3).assignUser(204, "Meera", Color.rgb(99, 102, 241), true, "Speaker");
            r1.chatMessages.add(new ChatMessage("System", "ANNOUNCEMENT", r1.announcement, true));
            r1.chatMessages.add(new ChatMessage("Aarav (Host)", "Host", "Welcome everyone! Try the Voice Changer & Audio Mixing controls below.", false));
            rooms.add(r1);

            RoomInfo r2 = new RoomInfo(
                    "002",
                    "Acoustic Studio & KTV • 002",
                    "agora_voice_music_02",
                    "Music",
                    "Zoya",
                    Color.rgb(14, 165, 233),
                    64,
                    "Popular • High-Clarity Music Voice Channel + KTV Reverb"
            );
            r2.coverResId = R.drawable.img_channel_1;
            r2.backgroundResId = R.drawable.bg_channel_1;
            r2.language = "Hindi";
            r2.topic = "Unplugged Singing";
            r2.seats.get(0).assignUser(301, "Zoya (Host)", Color.rgb(14, 165, 233), false, "Host");
            r2.seats.get(1).assignUser(302, "Rohan", Color.rgb(16, 185, 129), false, "Speaker");
            rooms.add(r2);

            RoomInfo r3 = new RoomInfo(
                    "003",
                    "Evening Casual Chat & Friends • 003",
                    "agora_voice_casual_03",
                    "Casual Chat",
                    "DevPriya",
                    Color.rgb(16, 185, 129),
                    24,
                    "Friends & Community Hangout • 8 Seats Active"
            );
            r3.coverResId = R.drawable.img_channel_2;
            r3.backgroundResId = R.drawable.bg_channel_2;
            r3.activeSeatCapacity = 8;
            r3.seats.get(0).assignUser(401, "DevPriya", Color.rgb(16, 185, 129), false, "Host");
            rooms.add(r3);

            RoomInfo r4 = new RoomInfo(
                    "004",
                    "Pro Squad Tactical Voice • 004",
                    "agora_voice_gaming_04",
                    "Gaming",
                    "ShadowX",
                    Color.rgb(99, 102, 241),
                    52,
                    "Low-Latency Team Voice Channel"
            );
            r4.coverResId = R.drawable.img_channel_3;
            r4.backgroundResId = R.drawable.bg_channel_3;
            r4.seats.get(0).assignUser(501, "ShadowX", Color.rgb(99, 102, 241), false, "Host");
            r4.seats.get(1).assignUser(502, "Viper", Color.rgb(37, 99, 235), false, "Co-Host");
            rooms.add(r4);

            RoomInfo r5 = new RoomInfo(
                    "005",
                    "VIP Founders Private Lounge • 005",
                    "agora_voice_private_05",
                    "Private Rooms",
                    "Vikram",
                    Color.rgb(245, 158, 11),
                    12,
                    "Private Room • PIN Protected (PIN: 1234)"
            );
            r5.coverResId = R.drawable.img_channel_4;
            r5.backgroundResId = R.drawable.bg_channel_4;
            r5.isPrivateLocked = true;
            r5.roomPassword = "1234";
            r5.activeSeatCapacity = 4;
            r5.seats.get(0).assignUser(601, "Vikram", Color.rgb(245, 158, 11), false, "Host");
            rooms.add(r5);
        }

        public List<RoomInfo> getAllRooms() {
            return new ArrayList<>(rooms);
        }

        public List<RoomInfo> getFilteredRooms(String category, String searchQuery) {
            List<RoomInfo> list = new ArrayList<>();
            String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.US);
            for (RoomInfo r : rooms) {
                boolean catMatch = (category == null || category.equalsIgnoreCase("All")
                        || category.equalsIgnoreCase("Recommended")
                        || (category.equalsIgnoreCase("Popular") && r.listenerCount >= 30)
                        || (category.equalsIgnoreCase("New") && r.id.compareTo("003") >= 0)
                        || (category.equalsIgnoreCase("Friends") && !r.isPrivateLocked)
                        || r.category.equalsIgnoreCase(category));
                boolean searchMatch = q.isEmpty()
                        || r.title.toLowerCase(Locale.US).contains(q)
                        || r.channelName.toLowerCase(Locale.US).contains(q)
                        || r.hostName.toLowerCase(Locale.US).contains(q)
                        || r.id.toLowerCase(Locale.US).contains(q);
                if (catMatch && searchMatch) {
                    list.add(r);
                }
            }
            return list;
        }

        public RoomInfo findRoomByChannel(String channelName) {
            if (channelName == null) return rooms.isEmpty() ? null : rooms.get(0);
            for (RoomInfo r : rooms) {
                if (r.channelName.equalsIgnoreCase(channelName.trim())) {
                    return r;
                }
            }
            return null;
        }

        public RoomInfo createOrFindRoom(String title, String channelName) {
            return createCustomRoom(title, channelName, "New", "English / Hindi", 12, false, "");
        }

        public RoomInfo createCustomRoom(
                String title,
                String channelName,
                String category,
                String language,
                int seatCapacity,
                boolean isPrivate,
                String pinCode
        ) {
            String cleanChannel = channelName == null ? "" : channelName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            if (cleanChannel.isEmpty()) {
                cleanChannel = "agora_room_" + (rooms.size() + 1);
            }
            RoomInfo existing = findRoomByChannel(cleanChannel);
            if (existing != null) {
                return existing;
            }

            String cleanTitle = (title == null || title.trim().isEmpty())
                    ? ("Voice Room • " + cleanChannel)
                    : title.trim();
            int validCap = (seatCapacity == 1 || seatCapacity == 4 || seatCapacity == 8 || seatCapacity == 12)
                    ? seatCapacity : 12;

            int idx = rooms.size();
            RoomInfo newRoom = new RoomInfo(
                    String.format(Locale.US, "%03d", idx + 1),
                    cleanTitle,
                    cleanChannel,
                    (category == null || category.isEmpty()) ? "New" : category,
                    localUserName,
                    Color.rgb(37, 99, 235),
                    1,
                    validCap + "-Seat Agora Voice Room • " + (language == null ? "English" : language)
            );
            newRoom.coverResId = getChannelCoverResId(idx);
            newRoom.backgroundResId = getChannelBgResId(idx);
            newRoom.language = (language == null || language.isEmpty()) ? "English / Hindi" : language;
            newRoom.activeSeatCapacity = validCap;
            newRoom.isPrivateLocked = isPrivate;
            newRoom.roomPassword = pinCode == null ? "" : pinCode.trim();
            newRoom.seats.get(0).assignUser(localUid, localUserName + " (Host)", localAvatarColor, false, "Host");
            newRoom.chatMessages.add(new ChatMessage("System", "ANNOUNCEMENT", newRoom.announcement, true));
            rooms.add(0, newRoom);
            return newRoom;
        }

        public void addModerationReport(String reportEntry) {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            moderationReports.add(0, "[" + time + "] " + reportEntry);
        }

        public List<String> getModerationReports() {
            if (moderationReports.isEmpty()) {
                moderationReports.add("[10:15:00] Sample Report: Spam check passed on channel qluvx_live_1 (Resolved)");
            }
            return new ArrayList<>(moderationReports);
        }
    }
}
