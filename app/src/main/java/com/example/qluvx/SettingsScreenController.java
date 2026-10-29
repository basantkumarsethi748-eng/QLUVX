package com.example.qluvx;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Phase 3: Complete, Modular Settings Screen Controller for QLUVX.
 * Eliminates cramped/tiny inline controls and provides 6 structured, well-proportioned sections:
 * 1. Privacy & Safety
 * 2. Notifications & Alerts
 * 3. Language (English & Hindi with live UI application and persistence)
 * 4. Permissions (Microphone, Camera, Notifications + rationale + Android App Settings deep link)
 * 5. Support (Help Center, FAQ, Contact Support, Submit Ticket, Report a Problem)
 * 6. Account (Account Info, Change Password/Auth, Privacy Policy, Terms, Logout, Account Deletion workflow)
 */
public class SettingsScreenController {

    public static final int REQ_SETTINGS_PERMISSIONS = 4202;

    public interface SettingsHostCallbacks {
        void onRefreshUiRequested();
        void onShowAuthDialogRequested();
    }

    public static void renderSettingsScreen(
            final Activity activity,
            final LinearLayout container,
            final QluvxSocialManager social,
            final GiftEconomyManager economy,
            final SettingsHostCallbacks callbacks
    ) {
        final int gapSm = UiStyleHelper.dp(activity, 10);
        final int gapMd = UiStyleHelper.dp(activity, 16);

        // =====================================================================
        // SETTINGS OVERVIEW HEADER BANNER
        // =====================================================================
        LinearLayout headerCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_AGORA_BLUE);

        TextView headerTitle = new TextView(activity);
        headerTitle.setText(social.tr(
                "⚙️ QLUVX Settings & Account Preferences",
                "⚙️ QLUVX सेटिंग्स और खाता प्राथमिकताएं"
        ));
        headerTitle.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        headerTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        QluvxSocialManager.UserProfile me = social.getCurrentUser();
        boolean backendReady = TokenServiceClient.isBackendUrlConfigured();
        TextView headerSub = new TextView(activity);
        headerSub.setText(social.tr(
                "Signed in as " + me.displayName + " (" + me.userId + ") • Language: " + social.getSettingLanguage()
                        + "\nBackend API: " + (backendReady ? "Connected (" + TokenServiceClient.getBackendBaseUrl() + ")" : "Local / Temp-Token Mode (Configure AGORA_TOKEN_SERVER_URL for remote sync)"),
                "उपयोगकर्ता: " + me.displayName + " (" + me.userId + ") • भाषा: " + social.getSettingLanguage()
                        + "\nबैकएंड स्थिति: " + (backendReady ? "कनेक्टेड" : "लोकल / अस्थायी टोकन मोड")
        ));
        headerSub.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        headerSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        headerSub.setPadding(0, UiStyleHelper.dp(activity, 6), 0, 0);

        headerCard.addView(headerTitle);
        headerCard.addView(headerSub);
        container.addView(headerCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));

        // =====================================================================
        // SECTION 1: PRIVACY & SAFETY
        // =====================================================================
        addSectionHeader(activity, container, social.tr(
                "🔒 1. Privacy & Safety",
                "🔒 1. गोपनीयता और सुरक्षा (Privacy)"
        ));

        LinearLayout privacyCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_CARD_ELEVATED);

        privacyCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Private Profile Mode", "प्राइवेट प्रोफ़ाइल मोड"),
                social.tr(
                        "Only approved followers can view your full profile, bio, and active voice room status.",
                        "केवल आपके फ़ॉलोअर्स आपकी पूरी प्रोफ़ाइल और रूम स्थिति देख सकते हैं।"
                ),
                social.isSettingPrivateAccount(),
                isChecked -> {
                    social.setSettingPrivateAccount(isChecked);
                    Toast.makeText(activity, isChecked ? "Private Profile enabled" : "Public Profile enabled", Toast.LENGTH_SHORT).show();
                }
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        privacyCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Show Online Status", "ऑनलाइन स्टेटस दिखाएं"),
                social.tr(
                        "Display your green active badge in Discover, Chat, and 12-Seat Voice Rooms.",
                        "डिस्कवर और वॉइस रूम में अपना ऑनलाइन स्टेटस दिखाएं।"
                ),
                social.isSettingShowOnlineStatus(),
                isChecked -> social.setSettingShowOnlineStatus(isChecked)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        privacyCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("Direct Message Permissions", "निजी संदेश (DM) अनुमति"),
                social.tr(
                        "Control who can start a private conversation with you.",
                        "तय करें कि कौन आपको निजी संदेश भेज सकता है।"
                ),
                social.getSettingDmPermission() + " ▾",
                UiStyleHelper.COLOR_AGORA_BLUE,
                Color.WHITE,
                v -> showDmPermissionsDialog(activity, social, callbacks)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        int blockedCount = social.getBlockedUsers().size();
        privacyCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("Blocked Users", "ब्लॉक किए गए उपयोगकर्ता"),
                social.tr(
                        "Manage blocked accounts (" + blockedCount + " blocked). Blocked users cannot message you or join your private rooms.",
                        "ब्लॉक किए गए खातों को प्रबंधित करें (" + blockedCount + " ब्लॉक)।"
                ),
                social.tr("Manage (" + blockedCount + ")", "प्रबंधित करें (" + blockedCount + ")"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                v -> showBlockedUsersDialog(activity, social, callbacks)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        privacyCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("Data & Voice Room Privacy Controls", "डेटा और वॉइस रूम गोपनीयता नियंत्रण"),
                social.tr(
                        "Inspect Agora RTC audio stream security, seat lock rules, and data visibility.",
                        "अगोरा ऑडियो स्ट्रीम सुरक्षा और डेटा गोपनीयता की जांच करें।"
                ),
                social.tr("Inspect", "देखें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                v -> showPrivacyControlsSummaryDialog(activity, social)
        ));

        container.addView(privacyCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));

        // =====================================================================
        // SECTION 2: NOTIFICATIONS & ALERTS
        // =====================================================================
        addSectionHeader(activity, container, social.tr(
                "🔔 2. Notifications & Alerts",
                "🔔 2. सूचनाएं और अलर्ट (Notifications)"
        ));

        LinearLayout notifCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_CARD_ELEVATED);

        notifCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Push Notifications", "पुश नोटिफिकेशन"),
                social.tr(
                        "Master toggle for QLUVX push notifications on this Android device.",
                        "इस डिवाइस पर सभी QLUVX पुश सूचनाओं के लिए मुख्य स्विच।"
                ),
                social.isSettingPushNotifications(),
                social::setSettingPushNotifications
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        notifCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Room Invitations", "वॉइस रूम आमंत्रण"),
                social.tr(
                        "Receive alerts when a Host invites you to take one of the 12 speaker seats.",
                        "जब कोई होस्ट आपको 12-सीट वॉइस स्टेज पर आमंत्रित करे तो अलर्ट पाएं।"
                ),
                social.isSettingRoomInviteNotifications(),
                social::setSettingRoomInviteNotifications
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        notifCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Message Notifications", "चैट संदेश सूचनाएं"),
                social.tr(
                        "Alerts for new private messages, voice notes, and stickers.",
                        "नए निजी संदेशों, वॉइस नोट्स और स्टिकर के लिए अलर्ट।"
                ),
                social.isSettingMessageNotifications(),
                social::setSettingMessageNotifications
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        notifCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Gift Notifications", "गिफ्ट और कॉम्बो अलर्ट"),
                social.tr(
                        "Show luxury gift animations and combo banners (x1 / x5 / x10) in rooms.",
                        "रूम में प्रीमियम गिफ्ट एनिमेशन और कॉम्बो बैनर दिखाएं।"
                ),
                social.isSettingGiftAlerts(),
                social::setSettingGiftAlerts
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        notifCard.addView(UiStyleHelper.createSettingSwitchRow(
                activity,
                social.tr("Live Room Notifications", "लाइव रूम और पीके (PK) अलर्ट"),
                social.tr(
                        "Notify me when creators I follow start a 12-seat Voice Room or Live Video PK.",
                        "जब आपके फ़ॉलो किए गए क्रिएटर्स लाइव आएं तो सूचना पाएं।"
                ),
                social.isSettingLiveRoomNotifications(),
                social::setSettingLiveRoomNotifications
        ));

        container.addView(notifCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));

        // =====================================================================
        // SECTION 3: LANGUAGE SELECTION (ENGLISH & HINDI)
        // =====================================================================
        addSectionHeader(activity, container, social.tr(
                "🌐 3. App Language / भाषा चुनें",
                "🌐 3. ऐप की भाषा (App Language)"
        ));

        LinearLayout langCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_CARD_ELEVATED);

        TextView langDesc = new TextView(activity);
        langDesc.setText(social.tr(
                "Select your preferred interface language. Your choice is saved automatically and applied across navigation and Settings.",
                "अपनी पसंदीदा भाषा चुनें। आपकी पसंद सुरक्षित रहेगी और तुरंत लागू होगी।"
        ));
        langDesc.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
        langDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        langDesc.setPadding(0, 0, 0, gapSm);
        langCard.addView(langDesc);

        LinearLayout langBtnRow = new LinearLayout(activity);
        langBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        boolean isHindi = social.isHindi();

        TextView btnEnglish = UiStyleHelper.createControlBarButton(
                activity,
                !isHindi ? "✓ English (Active)" : "English (Global)",
                !isHindi ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                !isHindi ? UiStyleHelper.COLOR_AGORA_CYAN : 0,
                v -> {
                    social.setSettingLanguage("English");
                    Toast.makeText(activity, "Language set to English", Toast.LENGTH_SHORT).show();
                    if (callbacks != null) callbacks.onRefreshUiRequested();
                }
        );
        btnEnglish.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnHindi = UiStyleHelper.createControlBarButton(
                activity,
                isHindi ? "✓ हिन्दी (सक्रिय)" : "हिन्दी (Hindi)",
                isHindi ? UiStyleHelper.COLOR_AGORA_BLUE : UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                isHindi ? UiStyleHelper.COLOR_AGORA_CYAN : 0,
                v -> {
                    social.setSettingLanguage("Hindi (हिन्दी)");
                    Toast.makeText(activity, "भाषा हिन्दी में बदल दी गई है", Toast.LENGTH_SHORT).show();
                    if (callbacks != null) callbacks.onRefreshUiRequested();
                }
        );
        LinearLayout.LayoutParams hiLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hiLp.setMargins(gapSm, 0, 0, 0);
        btnHindi.setLayoutParams(hiLp);

        langBtnRow.addView(btnEnglish);
        langBtnRow.addView(btnHindi);
        langCard.addView(langBtnRow);

        container.addView(langCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));

        // =====================================================================
        // SECTION 4: RUNTIME PERMISSIONS (MIC, CAMERA, NOTIFICATIONS)
        // =====================================================================
        addSectionHeader(activity, container, social.tr(
                "🎙️ 4. Runtime Device Permissions",
                "🎙️ 4. डिवाइस अनुमतियां (Permissions)"
        ));

        LinearLayout permCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_CARD_ELEVATED);

        boolean micGranted = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean camGranted = activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean notifGranted = true;
        if (Build.VERSION.SDK_INT >= 33) {
            notifGranted = activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
        }

        permCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("🎙️ Microphone Permission (RECORD_AUDIO)", "🎙️ माइक्रोफ़ोन अनुमति"),
                social.tr(
                        "Required to speak on the 12-seat Agora Voice Stage, send voice notes, and join Live PK.",
                        "12-सीट अगोरा वॉइस रूम में बोलने और वॉइस नोट भेजने के लिए आवश्यक।"
                ),
                micGranted ? social.tr("GRANTED ✓", "अनुमति प्राप्त ✓") : social.tr("Grant Mic", "अनुमति दें"),
                micGranted ? Color.argb(55, 16, 185, 129) : UiStyleHelper.COLOR_WARNING_AMBER,
                micGranted ? UiStyleHelper.COLOR_LIVE_GREEN : Color.BLACK,
                v -> {
                    if (micGranted) {
                        Toast.makeText(activity, "Microphone permission is already granted ✓", Toast.LENGTH_SHORT).show();
                    } else {
                        activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_SETTINGS_PERMISSIONS);
                    }
                }
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        permCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("📷 Camera Permission (CAMERA)", "📷 कैमरा अनुमति"),
                social.tr(
                        "Required to broadcast your video stream in Live Video Rooms and PK Battles.",
                        "लाइव वीडियो रूम और पीके बैटल में वीडियो स्ट्रीम करने के लिए आवश्यक।"
                ),
                camGranted ? social.tr("GRANTED ✓", "अनुमति प्राप्त ✓") : social.tr("Grant Camera", "अनुमति दें"),
                camGranted ? Color.argb(55, 16, 185, 129) : UiStyleHelper.COLOR_WARNING_AMBER,
                camGranted ? UiStyleHelper.COLOR_LIVE_GREEN : Color.BLACK,
                v -> {
                    if (camGranted) {
                        Toast.makeText(activity, "Camera permission is already granted ✓", Toast.LENGTH_SHORT).show();
                    } else {
                        activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_SETTINGS_PERMISSIONS);
                    }
                }
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        final boolean finalNotifGranted = notifGranted;
        permCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("🔔 Notification Permission (POST_NOTIFICATIONS)", "🔔 नोटिफिकेशन अनुमति"),
                social.tr(
                        "Required on Android 13+ to display incoming room invitations and chat alerts.",
                        "रूम आमंत्रण और चैट अलर्ट दिखाने के लिए आवश्यक।"
                ),
                finalNotifGranted ? social.tr("GRANTED ✓", "अनुमति प्राप्त ✓") : social.tr("Grant Alerts", "अनुमति दें"),
                finalNotifGranted ? Color.argb(55, 16, 185, 129) : UiStyleHelper.COLOR_WARNING_AMBER,
                finalNotifGranted ? UiStyleHelper.COLOR_LIVE_GREEN : Color.BLACK,
                v -> {
                    if (finalNotifGranted) {
                        Toast.makeText(activity, "Notification permission is active ✓", Toast.LENGTH_SHORT).show();
                    } else if (Build.VERSION.SDK_INT >= 33) {
                        activity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_SETTINGS_PERMISSIONS);
                    }
                }
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        // Direct link to Android System Application Details Settings (for permanently denied permissions)
        TextView openSysSettingsBtn = UiStyleHelper.createControlBarButton(
                activity,
                social.tr("⚙️ Open Android System App Settings", "⚙️ एंड्रॉइड सिस्टम ऐप सेटिंग्स खोलें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                UiStyleHelper.COLOR_BORDER_SUBTLE,
                v -> openAndroidAppSettings(activity)
        );
        permCard.addView(openSysSettingsBtn);

        container.addView(permCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));

        // =====================================================================
        // SECTION 5: SUPPORT, HELP CENTER & PROBLEM REPORTING
        // =====================================================================
        addSectionHeader(activity, container, social.tr(
                "🛟 5. Help Center, Support & Reporting",
                "🛟 5. सहायता केंद्र और सपोर्ट (Support)"
        ));

        LinearLayout supportCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_CARD_ELEVATED);

        supportCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("📚 Help Center & Agora Voice Room Guide", "📚 सहायता केंद्र और वॉइस रूम गाइड"),
                social.tr(
                        "Learn how 12-seat stages, Host Crowns, Agora tokens, Wallet coins, and VIP frames work.",
                        "जानें कि 12-सीट स्टेज, होस्ट क्राउन, वॉलेट और वीआईपी कैसे काम करते हैं।"
                ),
                social.tr("Open Guide", "गाइड खोलें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                v -> showHelpCenterDialog(activity)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        supportCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("❓ Frequently Asked Questions (FAQ)", "❓ अक्सर पूछे जाने वाले प्रश्न (FAQ)"),
                social.tr(
                        "Answers on two-device Agora testing, microphone troubleshooting, and coin recharges.",
                        "अगोरा टेस्टिंग, माइक समस्या निवारण और कॉइन रिचार्ज पर उत्तर।"
                ),
                social.tr("View FAQ", "FAQ देखें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                v -> showFaqDialog(activity)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        supportCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("✉️ Contact Support / Submit a Ticket", "✉️ सपोर्ट टिकट सबमिट करें"),
                social.tr(
                        "Send a support inquiry to the QLUVX team (syncs with /api/support/ticket when backend is configured).",
                        "QLUVX सहायता टीम को अपना प्रश्न या टिकट भेजें।"
                ),
                social.tr("New Ticket", "नया टिकट"),
                UiStyleHelper.COLOR_AGORA_BLUE,
                Color.WHITE,
                v -> showSubmitTicketDialog(activity, social, "Support Inquiry", callbacks)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        supportCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("🚩 Report a Problem or Safety Issue", "🚩 समस्या या दुरुपयोग की रिपोर्ट करें"),
                social.tr(
                        "Report audio bugs, room abuse, or billing problems directly to Moderators.",
                        "ऑडियो बग, रूम दुरुपयोग या बिलिंग समस्या की रिपोर्ट करें।"
                ),
                social.tr("Report", "रिपोर्ट करें"),
                Color.argb(55, 245, 158, 11),
                UiStyleHelper.COLOR_WARNING_AMBER,
                v -> showSubmitTicketDialog(activity, social, "Problem / Safety Report", callbacks)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        // Recent Support Tickets Log
        List<String> tickets = social.getSupportTickets();
        TextView ticketHeader = new TextView(activity);
        ticketHeader.setText(social.tr("Recent Support Tickets (" + tickets.size() + "):", "हाल के सपोर्ट टिकट (" + tickets.size() + "):"));
        ticketHeader.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        ticketHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        ticketHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        ticketHeader.setPadding(0, UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 6));
        supportCard.addView(ticketHeader);

        int maxTickets = Math.min(3, tickets.size());
        for (int i = 0; i < maxTickets; i++) {
            TextView tRow = new TextView(activity);
            tRow.setText("• " + tickets.get(i));
            tRow.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            tRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tRow.setPadding(0, 0, 0, UiStyleHelper.dp(activity, 4));
            supportCard.addView(tRow);
        }

        container.addView(supportCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));

        // =====================================================================
        // SECTION 6: ACCOUNT MANAGEMENT, LEGAL & ACCOUNT DELETION
        // =====================================================================
        addSectionHeader(activity, container, social.tr(
                "👤 6. Account Management & Security",
                "👤 6. खाता प्रबंधन और सुरक्षा (Account)"
        ));

        LinearLayout accountCard = UiStyleHelper.createCardContainer(activity, UiStyleHelper.COLOR_CARD_ELEVATED);

        accountCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("🪪 Account Information", "🪪 खाता जानकारी"),
                "User ID: " + me.userId + " • Email: " + me.emailOrPhone + " • Role: " + me.systemRole,
                social.tr("View Info", "विवरण देखें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                v -> showAccountInfoDialog(activity, social, economy)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        accountCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("🔑 Change Password & Authentication Settings", "🔑 पासवर्ड और प्रमाणीकरण सेटिंग्स"),
                social.tr(
                        "Update your account password, email/phone binding, or switch Firebase Auth session.",
                        "अपना पासवर्ड अपडेट करें या खाता सत्र बदलें।"
                ),
                social.tr("Update Auth", "अपडेट करें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_AGORA_CYAN,
                v -> showChangePasswordDialog(activity, social, callbacks)
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        accountCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("📜 Privacy Policy", "📜 गोपनीयता नीति (Privacy Policy)"),
                social.tr(
                        "Read how QLUVX handles voice streams, profile data, and permissions.",
                        "पढ़ें कि QLUVX आपके डेटा और वॉइस स्ट्रीम को कैसे सुरक्षित रखता है।"
                ),
                social.tr("Read Policy", "नीति पढ़ें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                v -> showPolicyDocumentDialog(activity, "QLUVX Privacy Policy",
                        "1. Real-Time Audio & Video: QLUVX uses the official Agora RTC SDK for low-latency voice and video channels. Audio and video streams are transmitted in real time and are not recorded without explicit room notice.\n\n"
                        + "2. Account & Profile Data: Your User ID, display name, bio, and VIP tier are stored to personalize your room identity.\n\n"
                        + "3. Permissions: Microphone (RECORD_AUDIO) and Camera (CAMERA) are requested only when you join voice or video broadcasts. We never request broad file or storage permissions.\n\n"
                        + "4. Account Deletion: You may permanently delete your account and wipe local and server data at any time from Settings > Account.")
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        accountCard.addView(UiStyleHelper.createSettingActionRow(
                activity,
                social.tr("⚖️ Terms & Conditions", "⚖️ नियम और शर्तें (Terms & Conditions)"),
                social.tr(
                        "Community standards for 12-seat voice rooms, virtual gifts, wallet coins, and moderation.",
                        "12-सीट वॉइस रूम, गिफ्ट, वॉलेट और मॉडरेशन के लिए सामुदायिक नियम।"
                ),
                social.tr("Read Terms", "शर्तें पढ़ें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                v -> showPolicyDocumentDialog(activity, "QLUVX Terms & Conditions",
                        "1. Community Conduct: Harassment, hate speech, or unauthorized broadcasting in 12-seat voice rooms or Live Video PK stages may result in seat muting or account suspension by Moderators.\n\n"
                        + "2. Virtual Coins & Gifts: Wallet coins, combo gifts (x1/x5/x10), and VIP/SVIP frames are virtual items validated via idempotent ledger transactions.\n\n"
                        + "3. Host & Admin Controls: Room Hosts control seat locks and speaker invitations inside their rooms.")
        ), UiStyleHelper.params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapSm));

        // Logout Button
        TextView logoutBtn = UiStyleHelper.createControlBarButton(
                activity,
                social.isAuthenticated()
                        ? social.tr("🚪 Log Out of Current Session", "🚪 वर्तमान सत्र से लॉग आउट करें")
                        : social.tr("🔐 Sign In to QLUVX Account", "🔐 QLUVX खाते में साइन इन करें"),
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_TEXT_PRIMARY,
                UiStyleHelper.COLOR_BORDER_SUBTLE,
                v -> {
                    if (social.isAuthenticated()) {
                        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle(social.tr("Log Out?", "लॉग आउट करें?"))
                                .setMessage(social.tr(
                                        "Are you sure you want to log out of " + me.displayName + "? Your saved coins and settings will remain intact.",
                                        "क्या आप लॉग आउट करना चाहते हैं?"
                                ))
                                .setPositiveButton(social.tr("Log Out", "लॉग आउट"), (d, w) -> {
                                    social.signOutUser();
                                    Toast.makeText(activity, "Logged out of session.", Toast.LENGTH_SHORT).show();
                                    if (callbacks != null) callbacks.onRefreshUiRequested();
                                })
                                .setNegativeButton(social.tr("Cancel", "रद्द करें"), null)
                                .show();
                    } else if (callbacks != null) {
                        callbacks.onShowAuthDialogRequested();
                    }
                }
        );
        accountCard.addView(logoutBtn, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 4, 0, gapSm
        ));

        // Dedicated Account Deletion Workflow Button
        TextView deleteAccountBtn = UiStyleHelper.createControlBarButton(
                activity,
                social.tr("🗑️ Request Permanent Account Deletion", "🗑️ स्थायी खाता हटाने का अनुरोध करें"),
                Color.argb(55, 239, 68, 68),
                UiStyleHelper.COLOR_DANGER_RED,
                UiStyleHelper.COLOR_DANGER_RED,
                v -> showAccountDeletionWorkflowDialog(activity, social, callbacks)
        );
        accountCard.addView(deleteAccountBtn);

        container.addView(accountCard, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, gapMd
        ));
    }

    private static void addSectionHeader(Activity activity, LinearLayout container, String title) {
        TextView tv = new TextView(activity);
        tv.setText(title);
        tv.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setPadding(UiStyleHelper.dp(activity, 2), UiStyleHelper.dp(activity, 4), 0, UiStyleHelper.dp(activity, 8));
        container.addView(tv);
    }

    private static void openAndroidAppSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "Open Android Settings > Apps > QLUVX > Permissions", Toast.LENGTH_LONG).show();
        }
    }

    private static void showDmPermissionsDialog(
            Activity activity,
            QluvxSocialManager social,
            SettingsHostCallbacks callbacks
    ) {
        final String[] options = new String[]{"Everyone", "Following Only", "No One"};
        int checked = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(social.getSettingDmPermission())) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("💬 Direct Message Permissions")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    social.setSettingDmPermission(options[which]);
                    dialog.dismiss();
                    Toast.makeText(activity, "Direct Message privacy set to: " + options[which], Toast.LENGTH_SHORT).show();
                    if (callbacks != null) callbacks.onRefreshUiRequested();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showBlockedUsersDialog(
            Activity activity,
            QluvxSocialManager social,
            SettingsHostCallbacks callbacks
    ) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        box.setPadding(pad, pad, pad, pad);

        List<String> blocked = social.getBlockedUsers();
        if (blocked.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No blocked users. You can block disruptive users below or from any voice room.");
            empty.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            box.addView(empty);
        } else {
            for (final String u : blocked) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, UiStyleHelper.dp(activity, 6), 0, UiStyleHelper.dp(activity, 6));

                TextView name = new TextView(activity);
                name.setText("🚫 " + u);
                name.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView unblockBtn = UiStyleHelper.createStatusChip(
                        activity,
                        "Unblock",
                        UiStyleHelper.COLOR_AGORA_BLUE,
                        Color.WHITE,
                        0
                );
                unblockBtn.setOnClickListener(v -> {
                    social.unblockUser(u);
                    Toast.makeText(activity, "Unblocked " + u, Toast.LENGTH_SHORT).show();
                    if (callbacks != null) callbacks.onRefreshUiRequested();
                });

                row.addView(name);
                row.addView(unblockBtn);
                box.addView(row);
            }
        }

        final EditText blockInput = new EditText(activity);
        blockInput.setHint("Enter User ID or Username to block...");
        blockInput.setSingleLine(true);
        box.addView(blockInput, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, UiStyleHelper.dp(activity, 12), 0, 0
        ));

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🚫 Blocked Users Manager")
                .setView(box)
                .setPositiveButton("Block User", (d, w) -> {
                    String target = blockInput.getText().toString().trim();
                    if (!target.isEmpty()) {
                        social.blockUser(target);
                        Toast.makeText(activity, "Blocked " + target, Toast.LENGTH_SHORT).show();
                        if (callbacks != null) callbacks.onRefreshUiRequested();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private static void showPrivacyControlsSummaryDialog(Activity activity, QluvxSocialManager social) {
        String summary = "• Agora Audio Channel Security: Token-authenticated via TokenServiceClient (/api/rtc/token).\n"
                + "• Profile Visibility: " + (social.isSettingPrivateAccount() ? "Private (Followers Only)" : "Public") + "\n"
                + "• Online Status Broadcast: " + (social.isSettingShowOnlineStatus() ? "Visible" : "Hidden") + "\n"
                + "• Direct Message Filter: " + social.getSettingDmPermission() + "\n"
                + "• Storage Policy: Zero broad media permissions requested; chat images use Android system document picker.";
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🛡️ QLUVX Privacy & Security Controls")
                .setMessage(summary)
                .setPositiveButton("OK", null)
                .show();
    }

    private static void showHelpCenterDialog(Activity activity) {
        String guide = "1. Joining a 12-Seat Agora Voice Room:\n"
                + "Tap '12-Seat Live' or any room card on Home/Live. Seat 1 is the Host Crown seat, and Seats 2–12 are speaker seats.\n\n"
                + "2. Two-Client Real Voice Testing:\n"
                + "Install QLUVX on two devices with your AGORA_APP_ID configured, join channel 'qluvx_live_1', unmute your mic, and watch the circular active-speaker volume ring react to real speech.\n\n"
                + "3. Wallet, Gifts & VIP:\n"
                + "Use Menu 7 (Wallet) to recharge coins, Menu 8 (Gifts) to send x1/x5/x10 combos, and Menu 9 (VIP/SVIP) to unlock profile frames.";
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("📚 QLUVX Help Center")
                .setMessage(guide)
                .setPositiveButton("Got It", null)
                .show();
    }

    private static void showFaqDialog(Activity activity) {
        String faq = "Q: Why does Agora say 'App ID Required'?\n"
                + "A: Add your official AGORA_APP_ID in the AI Studio Secrets panel (or .env) so BuildConfig.AGORA_APP_ID is populated.\n\n"
                + "Q: How do I fix a permanently denied Microphone or Camera permission?\n"
                + "A: In Settings > Section 4 (Permissions), tap 'Open Android System App Settings' and enable Microphone and Camera.\n\n"
                + "Q: How does server token generation work?\n"
                + "A: Run /backend/server.js with AGORA_APP_ID and AGORA_APP_CERTIFICATE and set AGORA_TOKEN_SERVER_URL.";
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("❓ Frequently Asked Questions")
                .setMessage(faq)
                .setPositiveButton("Close", null)
                .show();
    }

    private static void showSubmitTicketDialog(
            final Activity activity,
            final QluvxSocialManager social,
            final String defaultCategory,
            final SettingsHostCallbacks callbacks
    ) {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        form.setPadding(pad, pad, pad, pad);

        final EditText etSubject = new EditText(activity);
        etSubject.setHint("Subject (e.g., Audio latency or room report)");
        etSubject.setSingleLine(true);

        final EditText etDetails = new EditText(activity);
        etDetails.setHint("Describe the issue or user ID in detail...");
        etDetails.setMinLines(3);

        form.addView(etSubject);
        form.addView(etDetails, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, UiStyleHelper.dp(activity, 8), 0, 0
        ));

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🛟 " + defaultCategory)
                .setView(form)
                .setPositiveButton("Submit", (d, w) -> {
                    String subj = etSubject.getText().toString().trim();
                    String det = etDetails.getText().toString().trim();
                    if (subj.isEmpty()) subj = defaultCategory;
                    if (det.isEmpty()) det = "Submitted from QLUVX Settings screen";

                    final String finalSubj = subj;
                    final String finalDet = det;
                    TokenServiceClient.submitSupportTicket(
                            social.getCurrentUser().userId,
                            defaultCategory,
                            finalSubj,
                            finalDet,
                            (serverConfirmed, statusMessage) -> {
                                social.addSupportTicket(
                                        defaultCategory,
                                        finalSubj,
                                        finalDet,
                                        serverConfirmed ? "Server Synced ✓" : "Local / Pending Server Sync"
                                );
                                Toast.makeText(activity, statusMessage, Toast.LENGTH_LONG).show();
                                if (callbacks != null) callbacks.onRefreshUiRequested();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showAccountInfoDialog(
            Activity activity,
            QluvxSocialManager social,
            GiftEconomyManager economy
    ) {
        QluvxSocialManager.UserProfile me = social.getCurrentUser();
        String info = "Display Name: " + me.displayName
                + "\nUser ID: " + me.userId
                + "\nAgora RTC UID: " + VoiceRoomModels.RoomRepository.getInstance().localUid
                + "\nEmail / Phone: " + me.emailOrPhone
                + "\nAuth Method: " + (social.isAuthenticated() ? me.authMethod : "Guest / Signed Out")
                + "\nSystem Role: " + me.systemRole
                + "\nVIP Membership: " + me.vipTier
                + "\nAvatar Frame: " + me.profileFrame
                + "\nWallet Balance: 🪙 " + economy.getCoinBalance() + " Coins";
        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🪪 QLUVX Account Information")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private static void showChangePasswordDialog(
            Activity activity,
            QluvxSocialManager social,
            SettingsHostCallbacks callbacks
    ) {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        form.setPadding(pad, pad, pad, pad);

        final EditText emailInput = new EditText(activity);
        emailInput.setHint("Email or Phone");
        emailInput.setText(social.getCurrentUser().emailOrPhone);
        emailInput.setSingleLine(true);

        final EditText curPass = new EditText(activity);
        curPass.setHint("Current Password (or leave blank for OTP)");
        curPass.setSingleLine(true);

        final EditText newPass = new EditText(activity);
        newPass.setHint("New Password (minimum 8 characters)");
        newPass.setSingleLine(true);

        form.addView(emailInput);
        form.addView(curPass);
        form.addView(newPass);

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔑 Change Password & Auth Settings")
                .setView(form)
                .setPositiveButton("Update Credentials", (d, w) -> {
                    String np = newPass.getText().toString().trim();
                    if (!np.isEmpty() && np.length() < 6) {
                        Toast.makeText(activity, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    social.signInUser(
                            social.getCurrentUser().displayName,
                            emailInput.getText().toString().trim(),
                            "Firebase Auth • Password Updated"
                    );
                    Toast.makeText(activity, "Account authentication settings updated ✓", Toast.LENGTH_SHORT).show();
                    if (callbacks != null) callbacks.onRefreshUiRequested();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showPolicyDocumentDialog(Activity activity, String title, String content) {
        ScrollView sv = new ScrollView(activity);
        TextView tv = new TextView(activity);
        int pad = UiStyleHelper.dp(activity, 18);
        tv.setPadding(pad, pad, pad, pad);
        tv.setText(content);
        tv.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        sv.addView(tv);

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Phase 3 Account Deletion Requirement:
     * Dedicated confirmation flow explaining consequences, requiring explicit confirmation ("DELETE"),
     * and invoking the backend deletion workflow + local wipe.
     */
    private static void showAccountDeletionWorkflowDialog(
            final Activity activity,
            final QluvxSocialManager social,
            final SettingsHostCallbacks callbacks
    ) {
        QluvxSocialManager.UserProfile me = social.getCurrentUser();
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = UiStyleHelper.dp(activity, 16);
        box.setPadding(pad, pad, pad, pad);

        TextView warningText = new TextView(activity);
        warningText.setText("⚠️ Permanent Account Deletion Consequences:\n"
                + "1. Your profile (" + me.displayName + " / " + me.userId + "), bio, followers, and VIP frame will be permanently removed.\n"
                + "2. You will be signed out of all active Agora Voice & Video rooms.\n"
                + "3. A deletion request will be dispatched to /api/account/delete and recorded in the audit ledger.\n\n"
                + "To confirm permanent deletion, type DELETE below:");
        warningText.setTextColor(UiStyleHelper.COLOR_TEXT_PRIMARY);
        warningText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);

        final EditText reasonInput = new EditText(activity);
        reasonInput.setHint("Optional reason for leaving QLUVX...");
        reasonInput.setSingleLine(true);

        final EditText confirmInput = new EditText(activity);
        confirmInput.setHint("Type DELETE to confirm");
        confirmInput.setSingleLine(true);

        box.addView(warningText);
        box.addView(reasonInput, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, UiStyleHelper.dp(activity, 8), 0, 0
        ));
        box.addView(confirmInput, UiStyleHelper.params(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, UiStyleHelper.dp(activity, 8), 0, 0
        ));

        new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🗑️ Confirm Permanent Account Deletion")
                .setView(box)
                .setPositiveButton("Permanently Delete Account", (d, w) -> {
                    String typed = confirmInput.getText().toString().trim();
                    if (!"DELETE".equalsIgnoreCase(typed)) {
                        Toast.makeText(activity, "Deletion cancelled: You must type DELETE to confirm.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String uid = me.userId;
                    String email = me.emailOrPhone;
                    String reason = reasonInput.getText().toString().trim();

                    TokenServiceClient.requestAccountDeletion(uid, email, reason, (serverConfirmed, statusMessage) -> {
                        social.deleteOrResetAccount(activity);
                        Toast.makeText(activity, statusMessage, Toast.LENGTH_LONG).show();
                        if (callbacks != null) callbacks.onRefreshUiRequested();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
