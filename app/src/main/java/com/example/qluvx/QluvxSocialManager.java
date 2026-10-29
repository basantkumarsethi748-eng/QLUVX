package com.example.qluvx;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Persistent repository and state manager for QLUVX:
 * - Phase 2: Authentication & Profile (User ID, profile photo avatar, bio, followers, following, account management)
 * - Menu 2: Discover (Users, nearby users, countries, categories, filters)
 * - Menu 5: Chat (Private messages, room chat bridge, emojis, stickers, images, voice messages)
 * - Menu 9: VIP / SVIP (Memberships, badges, profile frames, benefits)
 * - Menu 11: User administration & platform analytics
 * - Menu 12: Settings (Privacy, notifications, language, permissions, support, account deletion)
 */
public class QluvxSocialManager {

    public static class UserProfile {
        public String userId;
        public String displayName;
        public String emailOrPhone;
        public String authMethod;
        public String bio;
        public String country;
        public String distanceKm;
        public String avatarEmoji;
        public int avatarColor;
        public String vipTier;
        public String profileFrame;
        public int followersCount;
        public int followingCount;
        public boolean isOnline;
        public boolean isFollowing;
        public boolean isBanned;
        public String systemRole;
        public int avatarResId;
        public int age;
        public int heightCm;
        public int weightKg;
        public String zodiacSign;
        public String relationshipStatus;
        public boolean isVerified;
        public int voiceIntroSec;
        public boolean privateAlbumUnlocked;
        public boolean flameTapped;

        public UserProfile(
                String userId,
                String displayName,
                String country,
                String distanceKm,
                String avatarEmoji,
                int avatarColor,
                String vipTier,
                String profileFrame,
                String bio,
                int followersCount,
                int followingCount,
                boolean isOnline,
                boolean isFollowing,
                String systemRole
        ) {
            this.userId = userId;
            this.displayName = displayName;
            this.emailOrPhone = userId.toLowerCase(Locale.US) + "@qluvx.app";
            this.authMethod = "Firebase Auth • Verified";
            this.country = country;
            this.distanceKm = distanceKm;
            this.avatarEmoji = avatarEmoji;
            this.avatarColor = avatarColor;
            this.vipTier = vipTier;
            this.profileFrame = profileFrame;
            this.bio = bio;
            this.followersCount = followersCount;
            this.followingCount = followingCount;
            this.isOnline = isOnline;
            this.isFollowing = isFollowing;
            this.isBanned = false;
            this.systemRole = systemRole;
            int hash = Math.abs(userId.hashCode());
            this.avatarResId = VoiceRoomModels.getAvatarResId(hash);
            this.age = 21 + (hash % 9);
            this.heightCm = 170 + (hash % 16);
            this.weightKg = 62 + (hash % 18);
            String[] zodiacs = new String[]{"Leo ♌", "Aries ♈", "Scorpio ♏", "Libra ♎", "Gemini ♊", "Aquarius ♒"};
            this.zodiacSign = zodiacs[hash % zodiacs.length];
            this.relationshipStatus = (hash % 2 == 0) ? "Single • Looking for Friends/Voice Chat" : "Open to Chat & Live Party";
            this.isVerified = true;
            this.voiceIntroSec = 6 + (Math.abs(displayName.hashCode()) % 10);
            this.privateAlbumUnlocked = false;
            this.flameTapped = false;
        }
    }

    public static class VisitorEntry {
        public final String userId;
        public final String displayName;
        public final int avatarResId;
        public final String distanceKm;
        public final String timeAgo;
        public final String detailText;

        public VisitorEntry(String userId, String displayName, int avatarResId, String distanceKm, String timeAgo, String detailText) {
            this.userId = userId;
            this.displayName = displayName;
            this.avatarResId = avatarResId;
            this.distanceKm = distanceKm;
            this.timeAgo = timeAgo;
            this.detailText = detailText;
        }
    }

    public static class PrivateAlbumRequest {
        public final String userId;
        public final String userName;
        public final int avatarResId;
        public final String timeAgo;
        public String status; // "PENDING", "APPROVED", "REVOKED"

        public PrivateAlbumRequest(String userId, String userName, int avatarResId, String timeAgo, String status) {
            this.userId = userId;
            this.userName = userName;
            this.avatarResId = avatarResId;
            this.timeAgo = timeAgo;
            this.status = status;
        }
    }

    public static class FamilyGroup {
        public final String id;
        public final String name;
        public final String badge;
        public int memberCount;
        public final String topic;
        public boolean isJoined;

        public FamilyGroup(String id, String name, String badge, int memberCount, String topic, boolean isJoined) {
            this.id = id;
            this.name = name;
            this.badge = badge;
            this.memberCount = memberCount;
            this.topic = topic;
            this.isJoined = isJoined;
        }
    }

    public static class InteractionNotice {
        public final String icon;
        public final String userName;
        public final String actionText;
        public final String timeAgo;

        public InteractionNotice(String icon, String userName, String actionText, String timeAgo) {
            this.icon = icon;
            this.userName = userName;
            this.actionText = actionText;
            this.timeAgo = timeAgo;
        }
    }

    public static class MomentPost {
        public final String id;
        public final String authorUserId;
        public final String authorName;
        public final int authorAvatarResId;
        public final boolean authorVerified;
        public final String locationAndDistance;
        public final String timeAgo;
        public final String caption;
        public final int photoResId;
        public final int voiceDurationSec;
        public final String linkedRoomChannel;
        public int likesCount;
        public boolean isLiked;
        public final List<String> comments = new ArrayList<>();

        public MomentPost(
                String id,
                String authorUserId,
                String authorName,
                int authorAvatarResId,
                boolean authorVerified,
                String locationAndDistance,
                String timeAgo,
                String caption,
                int photoResId,
                int voiceDurationSec,
                String linkedRoomChannel,
                int likesCount
        ) {
            this.id = id;
            this.authorUserId = authorUserId;
            this.authorName = authorName;
            this.authorAvatarResId = authorAvatarResId;
            this.authorVerified = authorVerified;
            this.locationAndDistance = locationAndDistance;
            this.timeAgo = timeAgo;
            this.caption = caption;
            this.photoResId = photoResId;
            this.voiceDurationSec = voiceDurationSec;
            this.linkedRoomChannel = linkedRoomChannel;
            this.likesCount = likesCount;
            this.isLiked = false;
        }
    }

    public static class DirectMessage {
        public final String senderName;
        public final boolean fromMe;
        public final String messageType; // "TEXT", "EMOJI", "STICKER", "IMAGE", "VOICE"
        public final String content;
        public final String mediaMeta;
        public final String timestamp;

        public DirectMessage(String senderName, boolean fromMe, String messageType, String content, String mediaMeta) {
            this.senderName = senderName;
            this.fromMe = fromMe;
            this.messageType = messageType;
            this.content = content;
            this.mediaMeta = mediaMeta;
            this.timestamp = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        }
    }

    public static class ChatConversation {
        public final String peerUserId;
        public final String peerName;
        public final String peerAvatar;
        public final int peerColor;
        public final String peerStatus;
        public final List<DirectMessage> messages = new ArrayList<>();

        public ChatConversation(String peerUserId, String peerName, String peerAvatar, int peerColor, String peerStatus) {
            this.peerUserId = peerUserId;
            this.peerName = peerName;
            this.peerAvatar = peerAvatar;
            this.peerColor = peerColor;
            this.peerStatus = peerStatus;
        }
    }

    public static class VipTierOption {
        public final String id;
        public final String title;
        public final String badgeIcon;
        public final String frameName;
        public final int monthlyCoins;
        public final int accentColor;
        public final String[] benefits;

        public VipTierOption(String id, String title, String badgeIcon, String frameName,
                             int monthlyCoins, int accentColor, String[] benefits) {
            this.id = id;
            this.title = title;
            this.badgeIcon = badgeIcon;
            this.frameName = frameName;
            this.monthlyCoins = monthlyCoins;
            this.accentColor = accentColor;
            this.benefits = benefits;
        }
    }

    private static final String PREF_SOCIAL = "qluvx_social_prefs";
    private static QluvxSocialManager instance;

    private final SharedPreferences prefs;
    private UserProfile currentUser;
    private boolean isAuthenticated = true;
    private final List<UserProfile> communityUsers = new ArrayList<>();
    private final List<ChatConversation> conversations = new ArrayList<>();
    private final List<VipTierOption> vipCatalog = new ArrayList<>();
    private final List<MomentPost> momentsFeed = new ArrayList<>();
    private final List<VisitorEntry> whoViewedMeList = new ArrayList<>();
    private final List<VisitorEntry> myFootprintsList = new ArrayList<>();
    private final List<PrivateAlbumRequest> privateAlbumRequests = new ArrayList<>();
    private final List<FamilyGroup> familyGroups = new ArrayList<>();
    private final List<InteractionNotice> interactionNotices = new ArrayList<>();
    private final List<String> supportTickets = new ArrayList<>();
    private final List<String> blockedUsers = new ArrayList<>();

    private String currentRoamCity = "Mumbai, India (Local GPS)";
    private boolean invisibleFootprintMode = false;

    // Phase 3 Complete Settings State
    // 1. Privacy
    private boolean settingPrivateAccount = false;
    private boolean settingShowOnlineStatus = true;
    private boolean settingAllowStrangerDm = true;
    private String settingDmPermission = "Everyone"; // "Everyone", "Following Only", "No One"

    // 2. Notifications
    private boolean settingPushNotifications = true;
    private boolean settingRoomInviteNotifications = true;
    private boolean settingMessageNotifications = true;
    private boolean settingGiftAlerts = true;
    private boolean settingLiveRoomNotifications = true;

    // 3. Language
    private String settingLanguage = "English"; // "English" or "Hindi (हिन्दी)"

    public static synchronized QluvxSocialManager getInstance(Context context) {
        if (instance == null) {
            instance = new QluvxSocialManager(context.getApplicationContext());
        }
        return instance;
    }

    private QluvxSocialManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_SOCIAL, Context.MODE_PRIVATE);
        loadOrSeedProfile();
        seedCommunityUsers();
        seedConversations();
        seedVipCatalog();
        seedMomentsFeed();
        seedWallaBluedFeatures();
    }

    private void loadOrSeedProfile() {
        String uid = prefs.getString("user_id", "QLX-884920");
        String name = prefs.getString("display_name", "Aarav Sharma");
        String email = prefs.getString("email_phone", "aarav@qluvx.app");
        String method = prefs.getString("auth_method", "Firebase Auth • Verified");
        String bio = prefs.getString("bio", "Agora Voice & Live Room Creator • Music & Ludo Lover 🎙️");
        String country = prefs.getString("country", "India");
        String avatarEmoji = prefs.getString("avatar_emoji", "👑");
        String vipTier = prefs.getString("vip_tier", "VIP 1 Silver");
        String frame = prefs.getString("profile_frame", "Royal Sapphire Frame");
        int followers = prefs.getInt("followers", 1280);
        int following = prefs.getInt("following", 194);
        isAuthenticated = prefs.getBoolean("is_authenticated", true);

        // Settings persistence
        settingLanguage = prefs.getString("setting_lang", "English");
        settingPrivateAccount = prefs.getBoolean("setting_private", false);
        settingShowOnlineStatus = prefs.getBoolean("setting_online", true);
        settingAllowStrangerDm = prefs.getBoolean("setting_dm", true);
        settingDmPermission = prefs.getString("setting_dm_mode", settingAllowStrangerDm ? "Everyone" : "Following Only");

        settingPushNotifications = prefs.getBoolean("setting_push", true);
        settingRoomInviteNotifications = prefs.getBoolean("setting_room_invites", true);
        settingMessageNotifications = prefs.getBoolean("setting_msg_notif", true);
        settingGiftAlerts = prefs.getBoolean("setting_gift_alerts", true);
        settingLiveRoomNotifications = prefs.getBoolean("setting_live_notif", true);

        blockedUsers.clear();
        Set<String> savedBlocked = prefs.getStringSet("blocked_users_set", null);
        if (savedBlocked != null) {
            blockedUsers.addAll(savedBlocked);
        } else {
            blockedUsers.add("SpamBot_992 (QLX-99012)");
        }

        currentUser = new UserProfile(
                uid,
                name,
                country,
                "0.2 km",
                avatarEmoji,
                Color.rgb(37, 99, 235),
                vipTier,
                frame,
                bio,
                followers,
                following,
                true,
                false,
                "Super Admin"
        );
        currentUser.emailOrPhone = email;
        currentUser.authMethod = method;

        VoiceRoomModels.RoomRepository.getInstance().localUserName = currentUser.displayName;
    }

    public boolean isHindi() {
        return settingLanguage != null && settingLanguage.toLowerCase(Locale.US).contains("hindi");
    }

    public String tr(String englishText, String hindiText) {
        return isHindi() ? hindiText : englishText;
    }

    public void saveProfileChanges(String newName, String newBio, String newCountry, String newAvatarEmoji) {
        if (newName != null && !newName.trim().isEmpty()) {
            currentUser.displayName = newName.trim();
            VoiceRoomModels.RoomRepository.getInstance().localUserName = currentUser.displayName;
        }
        if (newBio != null) {
            currentUser.bio = newBio.trim();
        }
        if (newCountry != null && !newCountry.trim().isEmpty()) {
            currentUser.country = newCountry.trim();
        }
        if (newAvatarEmoji != null && !newAvatarEmoji.trim().isEmpty()) {
            currentUser.avatarEmoji = newAvatarEmoji.trim();
        }
        prefs.edit()
                .putString("display_name", currentUser.displayName)
                .putString("bio", currentUser.bio)
                .putString("country", currentUser.country)
                .putString("avatar_emoji", currentUser.avatarEmoji)
                .apply();
    }

    public void signInUser(String displayName, String emailOrPhone, String authMethod) {
        isAuthenticated = true;
        if (displayName != null && !displayName.trim().isEmpty()) {
            currentUser.displayName = displayName.trim();
            VoiceRoomModels.RoomRepository.getInstance().localUserName = currentUser.displayName;
        }
        if (emailOrPhone != null && !emailOrPhone.trim().isEmpty()) {
            currentUser.emailOrPhone = emailOrPhone.trim();
        }
        currentUser.authMethod = authMethod;
        prefs.edit()
                .putBoolean("is_authenticated", true)
                .putString("display_name", currentUser.displayName)
                .putString("email_phone", currentUser.emailOrPhone)
                .putString("auth_method", currentUser.authMethod)
                .apply();
    }

    public void signOutUser() {
        isAuthenticated = false;
        prefs.edit().putBoolean("is_authenticated", false).apply();
    }

    public void deleteOrResetAccount(Context context) {
        String preservedLang = settingLanguage;
        prefs.edit().clear().putString("setting_lang", preservedLang).apply();
        isAuthenticated = false;
        currentUser = new UserProfile(
                "QLX-" + (100000 + (int) (System.currentTimeMillis() % 899999)),
                "Guest User",
                "India",
                "1.0 km",
                "🎙️",
                Color.rgb(37, 99, 235),
                "Standard Member",
                "Classic Slate Frame",
                "New QLUVX Member",
                0,
                0,
                true,
                false,
                "User"
        );
        VoiceRoomModels.RoomRepository.getInstance().localUserName = currentUser.displayName;
        GiftEconomyManager.getInstance(context).logAudit("Account deletion & personal data wipe executed by user.");
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public UserProfile getCurrentUser() {
        return currentUser;
    }

    private void seedCommunityUsers() {
        communityUsers.clear();
        String[][] raw = new String[][]{
                {"QLX-10201", "Zoya Khan", "India", "0.4 km", "🎤", "SVIP Royal Emperor", "Golden Crown Frame", "Indie Singer • Hosting Live Acoustic Rooms daily at 9 PM", "8420", "310", "true", "true", "Host", "23"},
                {"QLX-10202", "Kabir Verma", "India", "0.8 km", "🎸", "VIP 3 Diamond", "Emerald Wave Frame", "Voice Room Co-Host & Ludo Champion 🎲", "3190", "215", "true", "true", "Moderator", "25"},
                {"QLX-10203", "Meera Nair", "UAE", "1.2 km", "🌸", "VIP 2 Gold", "Starlight Frame", "Dubai • Coffee chats, podcasts & 8 Ball Pool", "5120", "180", "true", "false", "Host", "22"},
                {"QLX-10204", "Liam Carter", "USA", "1.6 km", "🎧", "VIP 1 Silver", "Sapphire Frame", "Audio Engineer • Testing Agora 48kHz Low-Latency Streams", "1890", "95", "true", "false", "User", "26"},
                {"QLX-10205", "Putri Ayu", "Indonesia", "1.9 km", "✨", "VIP 3 Diamond", "Ruby Glow Frame", "Live Video PK Host & Acoustic Covers", "9640", "412", "true", "true", "Host", "24"},
                {"QLX-10206", "Lucas Silva", "Brazil", "2.3 km", "⚽", "VIP 2 Gold", "Brazil Verde Frame", "Gaming Squad Voice Comms & Carrom Master", "2740", "140", "true", "false", "User", "27"},
                {"QLX-10207", "Sophia Sterling", "UK", "2.7 km", "🎻", "SVIP Royal Emperor", "Imperial Crown Frame", "Classical Crossover & Late Night Lounge Host", "11200", "260", "true", "false", "Host", "24"},
                {"QLX-10208", "Rohan Kapoor", "India", "0.6 km", "🔥", "VIP 3 Diamond", "Neon Pulse Frame", "Late Night Chill & Music Mixing • Tap to Voice Match!", "4380", "190", "true", "true", "Host", "23"},
                {"QLX-10209", "Aisha Al-Mansoor", "UAE", "1.5 km", "🌙", "SVIP Royal Emperor", "Gold Crescent Frame", "Poetry, Acoustic Sessions & VIP Lounge Anchor", "7850", "230", "true", "false", "Host", "25"},
                {"QLX-10210", "Ryan Vance", "USA", "3.1 km", "🎹", "VIP 2 Gold", "Cyber Wave Frame", "R&B Producer • Hosting Open Mic Voice Battles", "3620", "175", "true", "false", "User", "28"},
                {"QLX-10211", "Ananya Sen", "India", "0.9 km", "💃", "VIP 2 Gold", "Rose Quartz Frame", "Bollywood Unplugged & Evening Friends Hangout", "6290", "305", "true", "true", "Host", "22"},
                {"QLX-10212", "Mateo Costa", "Brazil", "3.8 km", "🌊", "VIP 1 Silver", "Oceanic Frame", "Travel Stories, Guitar Loops & Global Language Exchange", "2150", "120", "false", "false", "User", "26"}
        };

        int[] palette = new int[]{
                Color.rgb(14, 165, 233), Color.rgb(16, 185, 129), Color.rgb(99, 102, 241),
                Color.rgb(245, 158, 11), Color.rgb(236, 72, 153), Color.rgb(59, 130, 246)
        };

        for (int i = 0; i < raw.length; i++) {
            String[] r = raw[i];
            UserProfile u = new UserProfile(
                    r[0], r[1], r[2], r[3], r[4],
                    palette[i % palette.length],
                    r[5], r[6], r[7],
                    Integer.parseInt(r[8]),
                    Integer.parseInt(r[9]),
                    Boolean.parseBoolean(r[10]),
                    Boolean.parseBoolean(r[11]),
                    r[12]
            );
            u.avatarResId = VoiceRoomModels.getAvatarResId(i);
            u.age = Integer.parseInt(r[13]);
            u.isVerified = true;
            u.voiceIntroSec = 7 + (i % 8);
            communityUsers.add(u);
        }
    }

    private void seedMomentsFeed() {
        momentsFeed.clear();
        MomentPost m1 = new MomentPost(
                "MOM-101", "QLX-10201", "Zoya Khan", R.drawable.img_avatar_0, true,
                "📍 0.4 km • Mumbai, India", "12 mins ago",
                "Live in the 12-Seat Acoustic Lounge right now! Testing the new KTV reverb & background mixing 🎙️🎶 Come grab Seat 2 or 3!",
                R.drawable.bg_channel_0, 11, BuildConfig.AGORA_DEFAULT_CHANNEL, 248
        );
        m1.comments.add("Kabir Verma: Sound quality is crystal clear today 🔥");
        m1.comments.add("Rohan Kapoor: Joining in 2 mins!");
        momentsFeed.add(m1);

        MomentPost m2 = new MomentPost(
                "MOM-102", "QLX-10208", "Rohan Kapoor", R.drawable.img_avatar_7, true,
                "📍 0.6 km • Delhi, India", "38 mins ago",
                "Late night coffee & chill voice chat ☕✨ Who's up for a quick 1v1 Voice Match or Ludo game?",
                R.drawable.bg_channel_1, 8, "agora_voice_music_02", 184
        );
        m2.comments.add("Ananya Sen: Count me in for 4-player Ludo 🎲");
        momentsFeed.add(m2);

        MomentPost m3 = new MomentPost(
                "MOM-103", "QLX-10205", "Putri Ayu", R.drawable.img_avatar_4, true,
                "📍 1.9 km • Jakarta, Indonesia", "1 hr ago",
                "Thank you everyone for the Rocket gifts during tonight's PK Battle! 🚀👑 Hosting another session soon.",
                R.drawable.bg_channel_2, 14, "agora_voice_casual_03", 412
        );
        m3.comments.add("Sophia Sterling: Congrats on the PK win! 🎉");
        momentsFeed.add(m3);

        MomentPost m4 = new MomentPost(
                "MOM-104", "QLX-10203", "Meera Nair", R.drawable.img_avatar_2, true,
                "📍 1.2 km • Dubai, UAE", "2 hrs ago",
                "Sunset vibes & open mic conversations 🌅 Tap 'Join Room' below to hop on the stage!",
                R.drawable.bg_channel_3, 9, "agora_voice_gaming_04", 159
        );
        momentsFeed.add(m4);
    }

    public List<MomentPost> getMomentsFeed() {
        return new ArrayList<>(momentsFeed);
    }

    public void createMomentPost(String caption, int photoResId, int voiceSec, String linkedChannel) {
        MomentPost post = new MomentPost(
                "MOM-" + (105 + momentsFeed.size()),
                currentUser.userId,
                currentUser.displayName,
                R.drawable.img_avatar_1,
                true,
                "📍 0.1 km • " + currentUser.country,
                "Just now",
                caption,
                photoResId,
                voiceSec,
                linkedChannel,
                1
        );
        post.isLiked = true;
        momentsFeed.add(0, post);
    }

    public List<UserProfile> getAllCommunityUsers() {
        return new ArrayList<>(communityUsers);
    }

    public List<UserProfile> filterDiscoverUsers(String query, String countryFilter, boolean nearbyOnly) {
        List<UserProfile> result = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        for (UserProfile u : communityUsers) {
            if (u.isBanned) continue;
            boolean countryMatch = (countryFilter == null || countryFilter.equalsIgnoreCase("All")
                    || u.country.equalsIgnoreCase(countryFilter));
            boolean nearbyMatch = !nearbyOnly || u.distanceKm.startsWith("0.") || u.distanceKm.startsWith("1.") || u.distanceKm.startsWith("2.");
            boolean queryMatch = q.isEmpty()
                    || u.displayName.toLowerCase(Locale.US).contains(q)
                    || u.userId.toLowerCase(Locale.US).contains(q)
                    || u.bio.toLowerCase(Locale.US).contains(q)
                    || u.country.toLowerCase(Locale.US).contains(q);
            if (countryMatch && nearbyMatch && queryMatch) {
                result.add(u);
            }
        }
        return result;
    }

    public void toggleFollowUser(UserProfile target) {
        if (target == null) return;
        target.isFollowing = !target.isFollowing;
        if (target.isFollowing) {
            target.followersCount++;
            currentUser.followingCount++;
        } else {
            target.followersCount = Math.max(0, target.followersCount - 1);
            currentUser.followingCount = Math.max(0, currentUser.followingCount - 1);
        }
        prefs.edit().putInt("following", currentUser.followingCount).apply();
    }

    private void seedConversations() {
        conversations.clear();

        ChatConversation c1 = new ChatConversation(
                "QLX-10201", "Zoya Khan", "🎤", Color.rgb(14, 165, 233), "Online • In Voice Room RM-101"
        );
        c1.messages.add(new DirectMessage("Zoya Khan", false, "TEXT", "Hey! Are you joining the 12-seat voice stage tonight?", ""));
        c1.messages.add(new DirectMessage(currentUser.displayName, true, "TEXT", "Yes! Testing the Agora audio volume indicator right now.", ""));
        c1.messages.add(new DirectMessage("Zoya Khan", false, "VOICE", "Voice Note (0:05) • 48kHz Audio", "0:05"));
        conversations.add(c1);

        ChatConversation c2 = new ChatConversation(
                "QLX-10202", "Kabir Verma", "🎸", Color.rgb(16, 185, 129), "Online • Playing Ludo 4P"
        );
        c2.messages.add(new DirectMessage("Kabir Verma", false, "TEXT", "Bro, join Game Center for a 4-player Ludo match!", ""));
        c2.messages.add(new DirectMessage("Kabir Verma", false, "STICKER", "🎲🏆 Ludo Royal Champion Sticker", "Sticker"));
        conversations.add(c2);

        ChatConversation c3 = new ChatConversation(
                "QLX-10203", "Meera Nair", "🌸", Color.rgb(99, 102, 241), "Online • Live Video PK Stage"
        );
        c3.messages.add(new DirectMessage("Meera Nair", false, "TEXT", "Thanks for the Crown combo gift in the PK battle!", ""));
        conversations.add(c3);
    }

    public List<ChatConversation> getConversations() {
        return conversations;
    }

    public ChatConversation getOrCreateConversation(UserProfile user) {
        for (ChatConversation conv : conversations) {
            if (conv.peerUserId.equalsIgnoreCase(user.userId)) {
                return conv;
            }
        }
        ChatConversation created = new ChatConversation(
                user.userId, user.displayName, user.avatarEmoji, user.avatarColor,
                user.isOnline ? "Online • " + user.country : "Offline • " + user.country
        );
        created.messages.add(new DirectMessage(user.displayName, false, "TEXT", "Hi! Nice to connect on QLUVX 👋", ""));
        conversations.add(0, created);
        return created;
    }

    private void seedVipCatalog() {
        vipCatalog.clear();
        vipCatalog.add(new VipTierOption(
                "vip_1",
                "VIP 1 • Silver Guardian",
                "🛡️",
                "Royal Sapphire Frame",
                300,
                Color.rgb(56, 189, 248),
                new String[]{
                        "Silver VIP Badge next to name in all 12-seat voice rooms",
                        "Royal Sapphire Avatar Frame on speaker seat",
                        "Priority speaker request queue"
                }
        ));
        vipCatalog.add(new VipTierOption(
                "vip_2",
                "VIP 2 • Golden Noble",
                "⚜️",
                "Golden Aura Frame",
                800,
                Color.rgb(245, 158, 11),
                new String[]{
                        "Golden Noble Badge + Entrance Banner in Voice & Video rooms",
                        "Exclusive VIP Stickers & Combo Gift Effects",
                        "Create PIN-protected private voice rooms up to 12 seats"
                }
        ));
        vipCatalog.add(new VipTierOption(
                "vip_3",
                "VIP 3 • Diamond Monarch",
                "💎",
                "Diamond Prism Frame",
                1800,
                Color.rgb(99, 102, 241),
                new String[]{
                        "Diamond Monarch Badge + Diamond Prism Seat Frame",
                        "10% Bonus Coins on Verified Wallet Recharges",
                        "Anti-kick protection in standard rooms & custom room theme"
                }
        ));
        vipCatalog.add(new VipTierOption(
                "svip_emperor",
                "SVIP • Royal Emperor",
                "👑",
                "Imperial Dragon & Crown Frame",
                3500,
                Color.rgb(16, 185, 129),
                new String[]{
                        "Full-screen Royal Emperor Entrance Effect in Voice & Video stages",
                        "Imperial Crown Frame + Top placement in Room Listener List",
                        "Dedicated SVIP Concierge & All Luxury Gifts Unlocked"
                }
        ));
    }

    public List<VipTierOption> getVipCatalog() {
        return vipCatalog;
    }

    public boolean activateVipTier(Context context, VipTierOption option) {
        if (option == null) return false;
        GiftEconomyManager economy = GiftEconomyManager.getInstance(context);
        if (economy.getCoinBalance() < option.monthlyCoins) {
            return false;
        }
        GiftEconomyManager.GiftItem vipPassItem = new GiftEconomyManager.GiftItem(
                option.id, option.title, option.badgeIcon, "VIP", option.monthlyCoins, true
        );
        GiftEconomyManager.WalletTransaction tx = economy.sendGiftCombo(
                vipPassItem, 1, currentUser.displayName, "QLUVX VIP Membership", null
        );
        if (tx == null) return false;

        currentUser.vipTier = option.title;
        currentUser.profileFrame = option.frameName;
        economy.setVipStatusLabel(option.badgeIcon + " " + option.title);
        prefs.edit()
                .putString("vip_tier", currentUser.vipTier)
                .putString("profile_frame", currentUser.profileFrame)
                .apply();
        return true;
    }

    // =========================================================================
    // PHASE 3 SETTINGS GETTERS & MUTATORS
    // =========================================================================
    public boolean isSettingPrivateAccount() { return settingPrivateAccount; }
    public void setSettingPrivateAccount(boolean val) {
        settingPrivateAccount = val;
        prefs.edit().putBoolean("setting_private", val).apply();
    }

    public boolean isSettingShowOnlineStatus() { return settingShowOnlineStatus; }
    public void setSettingShowOnlineStatus(boolean val) {
        settingShowOnlineStatus = val;
        prefs.edit().putBoolean("setting_online", val).apply();
    }

    public boolean isSettingAllowStrangerDm() { return settingAllowStrangerDm; }
    public void setSettingAllowStrangerDm(boolean val) {
        settingAllowStrangerDm = val;
        settingDmPermission = val ? "Everyone" : "Following Only";
        prefs.edit()
                .putBoolean("setting_dm", val)
                .putString("setting_dm_mode", settingDmPermission)
                .apply();
    }

    public String getSettingDmPermission() { return settingDmPermission; }
    public void setSettingDmPermission(String mode) {
        settingDmPermission = mode;
        settingAllowStrangerDm = "Everyone".equalsIgnoreCase(mode);
        prefs.edit()
                .putString("setting_dm_mode", settingDmPermission)
                .putBoolean("setting_dm", settingAllowStrangerDm)
                .apply();
    }

    public List<String> getBlockedUsers() {
        return new ArrayList<>(blockedUsers);
    }

    public void blockUser(String userLabel) {
        if (userLabel == null || userLabel.trim().isEmpty()) return;
        String clean = userLabel.trim();
        if (!blockedUsers.contains(clean)) {
            blockedUsers.add(clean);
            saveBlockedUsers();
        }
    }

    public void unblockUser(String userLabel) {
        blockedUsers.remove(userLabel);
        saveBlockedUsers();
    }

    private void saveBlockedUsers() {
        prefs.edit().putStringSet("blocked_users_set", new HashSet<>(blockedUsers)).apply();
    }

    public boolean isSettingPushNotifications() { return settingPushNotifications; }
    public void setSettingPushNotifications(boolean val) {
        settingPushNotifications = val;
        prefs.edit().putBoolean("setting_push", val).apply();
    }

    public boolean isSettingRoomInviteNotifications() { return settingRoomInviteNotifications; }
    public void setSettingRoomInviteNotifications(boolean val) {
        settingRoomInviteNotifications = val;
        prefs.edit().putBoolean("setting_room_invites", val).apply();
    }

    public boolean isSettingMessageNotifications() { return settingMessageNotifications; }
    public void setSettingMessageNotifications(boolean val) {
        settingMessageNotifications = val;
        prefs.edit().putBoolean("setting_msg_notif", val).apply();
    }

    public boolean isSettingGiftAlerts() { return settingGiftAlerts; }
    public void setSettingGiftAlerts(boolean val) {
        settingGiftAlerts = val;
        prefs.edit().putBoolean("setting_gift_alerts", val).apply();
    }

    public boolean isSettingLiveRoomNotifications() { return settingLiveRoomNotifications; }
    public void setSettingLiveRoomNotifications(boolean val) {
        settingLiveRoomNotifications = val;
        prefs.edit().putBoolean("setting_live_notif", val).apply();
    }

    public String getSettingLanguage() { return settingLanguage; }
    public void setSettingLanguage(String lang) {
        settingLanguage = lang;
        prefs.edit().putString("setting_lang", lang).apply();
    }

    public void addSupportTicket(String subject, String details) {
        addSupportTicket("Support", subject, details, "Open");
    }

    public void addSupportTicket(String category, String subject, String details, String syncStatus) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        supportTickets.add(0, "[" + time + "] [" + category + "] " + subject + " — " + details + " (" + syncStatus + ")");
    }

    public List<String> getSupportTickets() {
        if (supportTickets.isEmpty()) {
            supportTickets.add("[2026-10-23 09:30] [System] Agora Token Endpoint — Verified /api/rtc/token & fallback logic ready (Resolved)");
        }
        return new ArrayList<>(supportTickets);
    }

    private void seedWallaBluedFeatures() {
        currentRoamCity = prefs.getString("roam_city", "Mumbai, India (Local GPS)");
        invisibleFootprintMode = prefs.getBoolean("invisible_footprint", false);

        whoViewedMeList.clear();
        whoViewedMeList.add(new VisitorEntry("QLX-10201", "Zoya Khan, 23", R.drawable.img_avatar_0, "0.4 km", "2 mins ago", "Viewed your profile & listened to your Voice Intro"));
        whoViewedMeList.add(new VisitorEntry("QLX-10208", "Rohan Kapoor, 23", R.drawable.img_avatar_7, "0.6 km", "14 mins ago", "Sent you a 🔥 Flame Tap & viewed your Moments"));
        whoViewedMeList.add(new VisitorEntry("QLX-10202", "Kabir Verma, 25", R.drawable.img_avatar_1, "0.8 km", "35 mins ago", "Viewed your profile from 12-Seat Voice Room"));
        whoViewedMeList.add(new VisitorEntry("QLX-10205", "Putri Ayu, 24", R.drawable.img_avatar_4, "1.9 km", "1 hr ago", "Viewed your Public & Private Album preview"));
        whoViewedMeList.add(new VisitorEntry("QLX-10203", "Meera Nair, 22", R.drawable.img_avatar_2, "1.2 km", "2 hrs ago", "Visited via Nearby Radar Match"));

        myFootprintsList.clear();
        myFootprintsList.add(new VisitorEntry("QLX-10201", "Zoya Khan, 23", R.drawable.img_avatar_0, "0.4 km", "10 mins ago", "You viewed Zoya's profile"));
        myFootprintsList.add(new VisitorEntry("QLX-10207", "Sophia Sterling, 24", R.drawable.img_avatar_6, "2.7 km", "1 hr ago", "You listened to Sophia's Voice Bio"));

        privateAlbumRequests.clear();
        privateAlbumRequests.add(new PrivateAlbumRequest("QLX-10208", "Rohan Kapoor, 23", R.drawable.img_avatar_7, "5 mins ago", "PENDING"));
        privateAlbumRequests.add(new PrivateAlbumRequest("QLX-10201", "Zoya Khan, 23", R.drawable.img_avatar_0, "28 mins ago", "APPROVED"));
        privateAlbumRequests.add(new PrivateAlbumRequest("QLX-10204", "Liam Carter, 26", R.drawable.img_avatar_3, "2 hrs ago", "PENDING"));

        familyGroups.clear();
        familyGroups.add(new FamilyGroup("FAM-01", "👑 Royal Acoustic & KTV Family", "🏆 Lv.12", 428, "Daily 12-Seat Singing & PK Battles", true));
        familyGroups.add(new FamilyGroup("FAM-02", "⚡ Night Owls & Nearby Radar", "🔥 Lv.9", 315, "Late Night Voice Chat & Speed Match", true));
        familyGroups.add(new FamilyGroup("FAM-03", "🎲 Pro Ludo, Carrom & 8-Ball Clan", "🎮 Lv.10", 512, "Voice Comms + Tournaments", false));
        familyGroups.add(new FamilyGroup("FAM-04", "🌍 Global Roamers & Travel Hub", "✈️ Lv.8", 264, "Mumbai • Dubai • Bangkok • London • Tokyo", false));

        interactionNotices.clear();
        interactionNotices.add(new InteractionNotice("🔥", "Rohan Kapoor", "sent you a Flame Tap greeting!", "4 mins ago"));
        interactionNotices.add(new InteractionNotice("❤️", "Zoya Khan", "liked your Moment post & Voice Status", "18 mins ago"));
        interactionNotices.add(new InteractionNotice("🔐", "Liam Carter", "requested access to your Private Photo Album", "42 mins ago"));
        interactionNotices.add(new InteractionNotice("🎙️", "Kabir Verma", "invited you to take Seat 2 in Agora Lounge", "1 hr ago"));
    }

    public String getCurrentRoamCity() {
        return currentRoamCity;
    }

    public void setCurrentRoamCity(String city) {
        this.currentRoamCity = city;
        prefs.edit().putString("roam_city", city).apply();
    }

    public boolean isInvisibleFootprintMode() {
        return invisibleFootprintMode;
    }

    public void setInvisibleFootprintMode(boolean invisible) {
        this.invisibleFootprintMode = invisible;
        prefs.edit().putBoolean("invisible_footprint", invisible).apply();
    }

    public List<VisitorEntry> getWhoViewedMeList() {
        return new ArrayList<>(whoViewedMeList);
    }

    public List<VisitorEntry> getMyFootprintsList() {
        return new ArrayList<>(myFootprintsList);
    }

    public void recordMyFootprint(UserProfile visited) {
        if (visited == null || invisibleFootprintMode) return;
        myFootprintsList.add(0, new VisitorEntry(
                visited.userId,
                visited.displayName + ", " + visited.age,
                visited.avatarResId,
                visited.distanceKm,
                "Just now",
                "You viewed " + visited.displayName + "'s profile"
        ));
    }

    public List<PrivateAlbumRequest> getPrivateAlbumRequests() {
        return new ArrayList<>(privateAlbumRequests);
    }

    public List<FamilyGroup> getFamilyGroups() {
        return new ArrayList<>(familyGroups);
    }

    public List<InteractionNotice> getInteractionNotices() {
        return new ArrayList<>(interactionNotices);
    }

    public void addInteractionNotice(String icon, String userName, String actionText) {
        interactionNotices.add(0, new InteractionNotice(icon, userName, actionText, "Just now"));
    }
}
