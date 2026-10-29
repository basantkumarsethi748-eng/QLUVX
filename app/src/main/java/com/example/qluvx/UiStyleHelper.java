package com.example.qluvx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * QLUVX Global Design System, Button ("Boton") Styler & Wallpaper/Theme Set Studio.
 * Supports 6 complete Wallpaper & Theme Sets + independent Wallpaper & Opacity controls,
 * persisted in SharedPreferences and applied across Home, Message Box, Live Video & 12-Seat Voice Rooms.
 */
public class UiStyleHelper {

    private static final String PREF_THEME_STUDIO = "qluvx_theme_studio_prefs";
    private static final String KEY_THEME_INDEX = "active_theme_index";
    private static final String KEY_WALLPAPER_INDEX = "active_wallpaper_index";
    private static final String KEY_CHAT_WALLPAPER_INDEX = "active_chat_wallpaper_index";
    private static final String KEY_WALLPAPER_ALPHA_MODE = "wallpaper_alpha_mode"; // 0=Balanced, 1=Vivid, 2=Subtle
    private static final String KEY_BUTTON_STYLE_MODE = "button_style_mode"; // 0=Neon Glow, 1=Royal Gold Border, 2=Crystal Pill

    // Dynamic palette colors (Default: QLUVX Obsidian Black, Royal Purple & Neon Pink)
    public static int COLOR_BG_DEEP = Color.rgb(11, 9, 23);            // Deep obsidian black-purple
    public static int COLOR_BG_STAGE_TOP = Color.rgb(22, 13, 43);      // Header / Dock surface
    public static int COLOR_CARD_BG = Color.rgb(28, 16, 54);           // Purple-black card surface
    public static int COLOR_CARD_ELEVATED = Color.rgb(46, 26, 86);     // Elevated purple pill/card
    public static int COLOR_BORDER_SUBTLE = Color.rgb(139, 92, 246);   // Neon purple border
    public static int COLOR_AGORA_BLUE = Color.rgb(236, 72, 153);      // Neon pink primary start
    public static int COLOR_INDIGO_ACCENT = Color.rgb(139, 92, 246);   // Royal purple primary end
    public static int COLOR_AGORA_CYAN = Color.rgb(244, 114, 182);     // Neon pink highlight
    public static int COLOR_NEON_PINK = Color.rgb(236, 72, 153);       // Signature neon pink
    public static int COLOR_ROYAL_PURPLE = Color.rgb(124, 58, 237);    // Signature royal purple
    public static int COLOR_LIVE_GREEN = Color.rgb(16, 185, 129);      // Online / active emerald
    public static int COLOR_WARNING_AMBER = Color.rgb(245, 158, 11);   // Gold / VIP amber
    public static int COLOR_DANGER_RED = Color.rgb(244, 63, 94);       // Live / PK rose red
    public static int COLOR_TEXT_PRIMARY = Color.rgb(248, 250, 252);   // Crisp white
    public static int COLOR_TEXT_SECONDARY = Color.rgb(226, 232, 240); // Soft silver
    public static int COLOR_TEXT_MUTED = Color.rgb(167, 139, 250);     // Muted lavender

    public static class ThemePreset {
        public final int index;
        public final String emoji;
        public final String title;
        public final String subtitle;
        public final int defaultWallpaperResId;
        public final int bgDeep;
        public final int bgTop;
        public final int cardBg;
        public final int cardElevated;
        public final int borderSubtle;
        public final int primaryBtnStart;
        public final int primaryBtnEnd;
        public final int accentHighlight;

        public ThemePreset(
                int index,
                String emoji,
                String title,
                String subtitle,
                int defaultWallpaperResId,
                int bgDeep,
                int bgTop,
                int cardBg,
                int cardElevated,
                int borderSubtle,
                int primaryBtnStart,
                int primaryBtnEnd,
                int accentHighlight
        ) {
            this.index = index;
            this.emoji = emoji;
            this.title = title;
            this.subtitle = subtitle;
            this.defaultWallpaperResId = defaultWallpaperResId;
            this.bgDeep = bgDeep;
            this.bgTop = bgTop;
            this.cardBg = cardBg;
            this.cardElevated = cardElevated;
            this.borderSubtle = borderSubtle;
            this.primaryBtnStart = primaryBtnStart;
            this.primaryBtnEnd = primaryBtnEnd;
            this.accentHighlight = accentHighlight;
        }
    }

    public static final int[] WALLPAPER_DRAWABLES = new int[]{
            R.drawable.bg_channel_2,
            R.drawable.bg_channel_0,
            R.drawable.bg_channel_1,
            R.drawable.bg_channel_3,
            R.drawable.bg_channel_4,
            R.drawable.bg_channel_list
    };

    public static final String[] WALLPAPER_NAMES = new String[]{
            "💖 QLUVX Neon Pink & Purple",
            "🌌 Starlight Cyber Stage",
            "👑 Royal Velvet Lounge",
            "💎 Emerald Aurora Club",
            "🔥 Sunset PK Arena",
            "🌊 Crystal Galaxy VIP"
    };

    public static final ThemePreset[] THEME_PRESETS = new ThemePreset[]{
            new ThemePreset(
                    0, "💖", "QLUVX Neon Pink & Purple", "Obsidian Black, Royal Purple & Neon Pink",
                    R.drawable.bg_channel_2,
                    Color.rgb(11, 9, 23),
                    Color.rgb(22, 13, 43),
                    Color.rgb(28, 16, 54),
                    Color.rgb(46, 26, 86),
                    Color.rgb(139, 92, 246),
                    Color.rgb(236, 72, 153),
                    Color.rgb(124, 58, 237),
                    Color.rgb(244, 114, 182)
            ),
            new ThemePreset(
                    1, "👑", "Royal Gold Palace", "Luxury Amber Gold & Velvet + Palace Wallpaper",
                    R.drawable.bg_channel_1,
                    Color.rgb(22, 15, 10),
                    Color.rgb(38, 26, 16),
                    Color.rgb(44, 30, 18),
                    Color.rgb(66, 45, 26),
                    Color.rgb(161, 98, 7),
                    Color.rgb(217, 119, 6),
                    Color.rgb(180, 83, 9),
                    Color.rgb(251, 191, 36)
            ),
            new ThemePreset(
                    2, "🌌", "Cyber Galaxy Neon", "Electric Blue & Sky Cyan + Starlight Wallpaper",
                    R.drawable.bg_channel_0,
                    Color.rgb(10, 16, 31),
                    Color.rgb(19, 29, 49),
                    Color.rgb(21, 32, 54),
                    Color.rgb(34, 50, 80),
                    Color.rgb(62, 92, 145),
                    Color.rgb(37, 99, 235),
                    Color.rgb(124, 58, 237),
                    Color.rgb(56, 189, 248)
            ),
            new ThemePreset(
                    3, "💎", "Emerald Aurora VIP", "Lush Emerald & Teal + Aurora Club Wallpaper",
                    R.drawable.bg_channel_3,
                    Color.rgb(8, 24, 21),
                    Color.rgb(13, 38, 34),
                    Color.rgb(17, 46, 41),
                    Color.rgb(26, 68, 60),
                    Color.rgb(16, 185, 129),
                    Color.rgb(5, 150, 105),
                    Color.rgb(13, 148, 136),
                    Color.rgb(52, 211, 153)
            ),
            new ThemePreset(
                    4, "🔥", "Sunset Ruby Stage", "Fiery Coral Red & Gold + Sunset PK Wallpaper",
                    R.drawable.bg_channel_4,
                    Color.rgb(26, 11, 16),
                    Color.rgb(45, 18, 26),
                    Color.rgb(52, 22, 31),
                    Color.rgb(78, 33, 46),
                    Color.rgb(190, 18, 60),
                    Color.rgb(225, 29, 72),
                    Color.rgb(234, 88, 12),
                    Color.rgb(251, 113, 133)
            ),
            new ThemePreset(
                    5, "🌊", "Crystal Starlight", "Deep Purple & Aqua + Galaxy VIP Wallpaper",
                    R.drawable.bg_channel_list,
                    Color.rgb(15, 12, 34),
                    Color.rgb(26, 21, 56),
                    Color.rgb(31, 25, 66),
                    Color.rgb(48, 39, 98),
                    Color.rgb(109, 40, 217),
                    Color.rgb(99, 102, 241),
                    Color.rgb(14, 165, 233),
                    Color.rgb(167, 139, 250)
            )
    };

    private static int activeThemeIndex = 0;
    private static int activeWallpaperIndex = 0;
    private static int activeChatWallpaperIndex = 2;
    private static int wallpaperAlphaMode = 0; // 0=Balanced (0.42f), 1=Vivid (0.62f), 2=Subtle (0.25f)
    private static int buttonStyleMode = 0;    // 0=Neon Glow, 1=Royal Gold Border, 2=Crystal Pill
    private static boolean themeLoaded = false;

    public interface BooleanToggleListener {
        void onToggled(boolean isChecked);
    }

    public static void loadSavedTheme(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_THEME_STUDIO, Context.MODE_PRIVATE);
        activeThemeIndex = Math.max(0, Math.min(THEME_PRESETS.length - 1, prefs.getInt(KEY_THEME_INDEX, 0)));
        activeWallpaperIndex = Math.max(0, Math.min(WALLPAPER_DRAWABLES.length - 1, prefs.getInt(KEY_WALLPAPER_INDEX, activeThemeIndex)));
        activeChatWallpaperIndex = Math.max(0, Math.min(WALLPAPER_DRAWABLES.length - 1, prefs.getInt(KEY_CHAT_WALLPAPER_INDEX, 2)));
        wallpaperAlphaMode = Math.max(0, Math.min(2, prefs.getInt(KEY_WALLPAPER_ALPHA_MODE, 0)));
        buttonStyleMode = Math.max(0, Math.min(2, prefs.getInt(KEY_BUTTON_STYLE_MODE, 0)));
        themeLoaded = true;
        syncStaticColorsFromActivePreset();
    }

    private static void syncStaticColorsFromActivePreset() {
        ThemePreset p = THEME_PRESETS[Math.max(0, Math.min(THEME_PRESETS.length - 1, activeThemeIndex))];
        COLOR_BG_DEEP = p.bgDeep;
        COLOR_BG_STAGE_TOP = p.bgTop;
        COLOR_CARD_BG = p.cardBg;
        COLOR_CARD_ELEVATED = p.cardElevated;
        COLOR_BORDER_SUBTLE = p.borderSubtle;
        COLOR_AGORA_BLUE = p.primaryBtnStart;
        COLOR_INDIGO_ACCENT = p.primaryBtnEnd;
        COLOR_AGORA_CYAN = p.accentHighlight;
    }

    public static ThemePreset getActiveThemePreset(Context context) {
        if (!themeLoaded && context != null) {
            loadSavedTheme(context);
        }
        return THEME_PRESETS[Math.max(0, Math.min(THEME_PRESETS.length - 1, activeThemeIndex))];
    }

    public static int getActiveThemeIndex(Context context) {
        if (!themeLoaded && context != null) loadSavedTheme(context);
        return activeThemeIndex;
    }

    public static int getActiveWallpaperIndex(Context context) {
        if (!themeLoaded && context != null) loadSavedTheme(context);
        return activeWallpaperIndex;
    }

    public static int getActiveWallpaperResId(Context context) {
        if (!themeLoaded && context != null) loadSavedTheme(context);
        return WALLPAPER_DRAWABLES[Math.max(0, Math.min(WALLPAPER_DRAWABLES.length - 1, activeWallpaperIndex))];
    }

    public static String getActiveWallpaperName(Context context) {
        if (!themeLoaded && context != null) loadSavedTheme(context);
        return WALLPAPER_NAMES[Math.max(0, Math.min(WALLPAPER_NAMES.length - 1, activeWallpaperIndex))];
    }

    public static int getActiveChatWallpaperResId(Context context) {
        if (!themeLoaded && context != null) loadSavedTheme(context);
        return WALLPAPER_DRAWABLES[Math.max(0, Math.min(WALLPAPER_DRAWABLES.length - 1, activeChatWallpaperIndex))];
    }

    public static float getActiveWallpaperAlpha(Context context) {
        if (!themeLoaded && context != null) loadSavedTheme(context);
        if (wallpaperAlphaMode == 1) return 0.62f;
        if (wallpaperAlphaMode == 2) return 0.24f;
        return 0.42f;
    }

    public static void applyThemePreset(Context context, int presetIndex) {
        if (context == null) return;
        activeThemeIndex = Math.max(0, Math.min(THEME_PRESETS.length - 1, presetIndex));
        activeWallpaperIndex = activeThemeIndex % WALLPAPER_DRAWABLES.length;
        activeChatWallpaperIndex = (activeThemeIndex + 2) % WALLPAPER_DRAWABLES.length;
        syncStaticColorsFromActivePreset();
        context.getSharedPreferences(PREF_THEME_STUDIO, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_INDEX, activeThemeIndex)
                .putInt(KEY_WALLPAPER_INDEX, activeWallpaperIndex)
                .putInt(KEY_CHAT_WALLPAPER_INDEX, activeChatWallpaperIndex)
                .apply();
    }

    public static void applyWallpaperOnly(Context context, int wallpaperIdx) {
        if (context == null) return;
        activeWallpaperIndex = Math.max(0, Math.min(WALLPAPER_DRAWABLES.length - 1, wallpaperIdx));
        context.getSharedPreferences(PREF_THEME_STUDIO, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_WALLPAPER_INDEX, activeWallpaperIndex)
                .apply();
    }

    public static void cycleChatWallpaper(Context context) {
        if (context == null) return;
        if (!themeLoaded) loadSavedTheme(context);
        activeChatWallpaperIndex = (activeChatWallpaperIndex + 1) % WALLPAPER_DRAWABLES.length;
        context.getSharedPreferences(PREF_THEME_STUDIO, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_CHAT_WALLPAPER_INDEX, activeChatWallpaperIndex)
                .apply();
    }

    public static void setWallpaperAlphaMode(Context context, int mode) {
        if (context == null) return;
        wallpaperAlphaMode = Math.max(0, Math.min(2, mode));
        context.getSharedPreferences(PREF_THEME_STUDIO, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_WALLPAPER_ALPHA_MODE, wallpaperAlphaMode)
                .apply();
    }

    public static void setButtonStyleMode(Context context, int mode) {
        if (context == null) return;
        buttonStyleMode = Math.max(0, Math.min(2, mode));
        context.getSharedPreferences(PREF_THEME_STUDIO, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_BUTTON_STYLE_MODE, buttonStyleMode)
                .apply();
    }

    /**
     * Configures the Activity window so that content sits safely below the Android status bar
     * and above the navigation bar, and applies the current Theme Set colors to system bars.
     */
    @SuppressWarnings("deprecation")
    public static void configureSafeActivityWindow(Activity activity) {
        if (activity == null) return;
        loadSavedTheme(activity);
        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                window.setStatusBarColor(COLOR_BG_DEEP);
                window.setNavigationBarColor(COLOR_BG_STAGE_TOP);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(true);
                    WindowInsetsController controller = window.getInsetsController();
                    if (controller != null) {
                        controller.setSystemBarsBehavior(
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                        controller.show(WindowInsets.Type.systemBars());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            activity.sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Throwable ignored) {
        }
    }

    public static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    public static float getWallpaperAlpha(Context context) {
        return getActiveWallpaperAlpha(context);
    }

    public static int getSelectedWallpaperIndex(Context context) {
        return getActiveWallpaperIndex(context);
    }

    public static void setSelectedWallpaperIndex(Context context, int index) {
        applyWallpaperOnly(context, index);
    }

    public static GradientDrawable roundedRect(Context context, int fillColor, float cornerDp, int strokeColor, float strokeDp) {
        return roundedBox(context, fillColor, cornerDp, strokeColor, strokeDp);
    }

    public static GradientDrawable roundedBox(Context context, int fillColor, float cornerDp, int strokeColor, float strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(context, cornerDp));
        if (strokeDp > 0 && strokeColor != 0) {
            gd.setStroke(dp(context, strokeDp), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable circle(Context context, int fillColor, int strokeColor, float strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(fillColor);
        if (strokeDp > 0 && strokeColor != 0) {
            gd.setStroke(dp(context, strokeDp), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable gradientBox(Context context, int startColor, int endColor, float cornerDp, int strokeColor, float strokeDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(context, cornerDp));
        if (strokeDp > 0 && strokeColor != 0) {
            gd.setStroke(dp(context, strokeDp), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable bottomScrimGradient(Context context, float cornerDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.argb(155, 8, 12, 24), Color.argb(240, 8, 12, 24)}
        );
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(context, cornerDp));
        return gd;
    }

    public static RippleDrawable rippleButton(Context context, int fillColor, float cornerDp, int strokeColor, float strokeDp) {
        GradientDrawable content = roundedBox(context, fillColor, cornerDp, strokeColor, strokeDp);
        GradientDrawable mask = roundedBox(context, Color.WHITE, cornerDp, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(80, 255, 255, 255)), content, mask);
    }

    public static RippleDrawable rippleGradientButton(Context context, int startColor, int endColor, float cornerDp) {
        float finalCorner = (buttonStyleMode == 2) ? 24f : cornerDp;
        int strokeCol = (buttonStyleMode == 1) ? COLOR_WARNING_AMBER : Color.argb(115, 255, 255, 255);
        float strokeW = (buttonStyleMode == 1) ? 1.5f : 1f;
        GradientDrawable content = gradientBox(context, startColor, endColor, finalCorner, strokeCol, strokeW);
        GradientDrawable mask = roundedBox(context, Color.WHITE, finalCorner, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(90, 255, 255, 255)), content, mask);
    }

    public static LinearLayout createCardContainer(Context context, int borderColor) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 15);
        card.setPadding(pad, pad, pad, pad);
        int stroke = borderColor != 0 ? borderColor : COLOR_BORDER_SUBTLE;
        int cardFill = Color.argb(228, Color.red(COLOR_CARD_BG), Color.green(COLOR_CARD_BG), Color.blue(COLOR_CARD_BG));
        card.setBackground(roundedBox(context, cardFill, 18f, stroke, 1.2f));
        return card;
    }

    public static LinearLayout createGradientCardContainer(Context context, int startColor, int endColor, int borderColor) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 15);
        card.setPadding(pad, pad, pad, pad);
        int stroke = borderColor != 0 ? borderColor : COLOR_BORDER_SUBTLE;
        card.setBackground(gradientBox(context, startColor, endColor, 18f, stroke, 1.2f));
        return card;
    }

    /**
     * Creates a crisp circular photo avatar with an outer ring border and optional online dot.
     */
    public static FrameLayout createCircularAvatar(
            Context context,
            int drawableResId,
            int sizeDp,
            int ringColor,
            boolean showOnlineDot
    ) {
        int outerPx = dp(context, sizeDp);
        int innerPx = dp(context, Math.max(20, sizeDp - 4));

        FrameLayout wrap = new FrameLayout(context);
        wrap.setLayoutParams(new LinearLayout.LayoutParams(outerPx, outerPx));
        wrap.setBackground(circle(context, COLOR_BG_DEEP, ringColor != 0 ? ringColor : COLOR_AGORA_CYAN, 2f));

        ImageView iv = new ImageView(context);
        iv.setImageResource(drawableResId != 0 ? drawableResId : R.drawable.img_avatar_0);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int sz = Math.min(view.getWidth(), view.getHeight());
                outline.setOval(0, 0, sz, sz);
            }
        });
        iv.setClipToOutline(true);
        wrap.addView(iv, new FrameLayout.LayoutParams(innerPx, innerPx, Gravity.CENTER));

        if (showOnlineDot) {
            View dot = new View(context);
            int dotPx = dp(context, Math.max(10, sizeDp / 4));
            dot.setBackground(circle(context, COLOR_LIVE_GREEN, COLOR_BG_DEEP, 2f));
            FrameLayout.LayoutParams dLp = new FrameLayout.LayoutParams(dotPx, dotPx, Gravity.BOTTOM | Gravity.END);
            wrap.addView(dot, dLp);
        }
        return wrap;
    }

    public static TextView createControlBarButton(Context context, String label, int bgColor, int textColor, int strokeColor, View.OnClickListener listener) {
        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        btn.setGravity(Gravity.CENTER);
        btn.setMinHeight(dp(context, 46));
        btn.setMinimumHeight(dp(context, 46));
        btn.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
        float corner = (buttonStyleMode == 2) ? 24f : 15f;
        if (bgColor == COLOR_AGORA_BLUE && strokeColor == 0) {
            btn.setBackground(rippleGradientButton(context, COLOR_AGORA_BLUE, COLOR_INDIGO_ACCENT, corner));
        } else {
            int autoStroke = strokeColor != 0
                    ? strokeColor
                    : (buttonStyleMode == 1 ? Color.argb(150, 245, 158, 11) : Color.argb(90, 255, 255, 255));
            btn.setBackground(rippleButton(context, bgColor, corner, autoStroke, 1.2f));
        }
        btn.setContentDescription(label);
        if (listener != null) {
            btn.setOnClickListener(listener);
        }
        return btn;
    }

    public static TextView createStatusChip(Context context, String text, int fillColor, int textColor, int strokeColor) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextColor(textColor);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(context, 36));
        chip.setPadding(dp(context, 13), dp(context, 6), dp(context, 13), dp(context, 6));
        if (fillColor == COLOR_AGORA_BLUE) {
            chip.setBackground(rippleGradientButton(context, COLOR_AGORA_BLUE, COLOR_INDIGO_ACCENT, 18f));
        } else {
            int effStroke = strokeColor != 0 ? strokeColor : Color.argb(65, 255, 255, 255);
            chip.setBackground(rippleButton(context, fillColor, 18f, effStroke, 1f));
        }
        return chip;
    }

    /**
     * Builds an inline, interactive Wallpaper & Theme Set Bar so users can switch themes and wallpapers
     * with a single tap right on the Home Page, Message Box, or Settings.
     */
    public static View buildInlineWallpaperThemeBar(final Activity activity, final Runnable onThemeChanged) {
        loadSavedTheme(activity);
        LinearLayout box = createGradientCardContainer(
                activity,
                COLOR_BG_STAGE_TOP,
                COLOR_CARD_BG,
                COLOR_AGORA_CYAN
        );

        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ThemePreset current = getActiveThemePreset(activity);
        TextView title = new TextView(activity);
        title.setText("🎨 Wallpaper & Theme Set: " + current.emoji + " " + current.title);
        title.setTextColor(COLOR_TEXT_PRIMARY);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView sub = new TextView(activity);
        sub.setText("🖼️ Wallpaper: " + getActiveWallpaperName(activity) + " • Tap any set below!");
        sub.setTextColor(COLOR_AGORA_CYAN);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);

        titleCol.addView(title);
        titleCol.addView(sub);

        TextView studioBtn = createStatusChip(
                activity,
                "🎨 Full Studio",
                COLOR_AGORA_BLUE,
                Color.WHITE,
                COLOR_AGORA_CYAN
        );
        studioBtn.setOnClickListener(v -> showThemeAndWallpaperStudioDialog(activity, onThemeChanged));

        headerRow.addView(titleCol);
        headerRow.addView(studioBtn);
        box.addView(headerRow);

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(activity, 10), 0, 0);

        for (int i = 0; i < THEME_PRESETS.length; i++) {
            final ThemePreset preset = THEME_PRESETS[i];
            boolean isSelected = (i == activeThemeIndex);

            FrameLayout tile = new FrameLayout(activity);
            int tileW = dp(activity, 124);
            int tileH = dp(activity, 72);
            tile.setLayoutParams(params(tileW, tileH, 0, 0, dp(activity, 8), 0));
            tile.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(activity, 14));
                }
            });
            tile.setClipToOutline(true);

            ImageView bg = new ImageView(activity);
            bg.setImageResource(preset.defaultWallpaperResId);
            bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            LinearLayout overlay = new LinearLayout(activity);
            overlay.setOrientation(LinearLayout.VERTICAL);
            overlay.setGravity(Gravity.BOTTOM);
            int pad = dp(activity, 8);
            overlay.setPadding(pad, pad, pad, pad);
            overlay.setBackground(gradientBox(
                    activity,
                    Color.argb(130, Color.red(preset.bgDeep), Color.green(preset.bgDeep), Color.blue(preset.bgDeep)),
                    Color.argb(225, Color.red(preset.primaryBtnStart), Color.green(preset.primaryBtnStart), Color.blue(preset.primaryBtnStart)),
                    14f,
                    isSelected ? Color.WHITE : preset.accentHighlight,
                    isSelected ? 2.5f : 1f
            ));

            TextView badge = new TextView(activity);
            badge.setText(isSelected ? "✅ ACTIVE SET" : (preset.emoji + " Theme #" + (i + 1)));
            badge.setTextColor(isSelected ? Color.rgb(253, 224, 71) : Color.WHITE);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView name = new TextView(activity);
            name.setText(preset.title);
            name.setSingleLine(true);
            name.setTextColor(Color.WHITE);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            overlay.addView(badge);
            overlay.addView(name);
            tile.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            tile.setOnClickListener(v -> {
                applyThemePreset(activity, preset.index);
                configureSafeActivityWindow(activity);
                Toast.makeText(activity, preset.emoji + " Applied Theme & Wallpaper: " + preset.title, Toast.LENGTH_SHORT).show();
                if (onThemeChanged != null) {
                    onThemeChanged.run();
                }
            });

            row.addView(tile);
        }

        scroll.addView(row);
        box.addView(scroll);
        return box;
    }

    /**
     * Opens the full interactive Wallpaper & Theme Set Studio Dialog.
     */
    public static void showThemeAndWallpaperStudioDialog(final Activity activity, final Runnable onThemeChanged) {
        if (activity == null) return;
        loadSavedTheme(activity);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(COLOR_BG_DEEP);
        scroll.addView(root);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        TextView header = new TextView(activity);
        header.setText("🎨 Complete A-to-Z Wallpaper, Theme & Button Studio");
        header.setTextColor(COLOR_TEXT_PRIMARY);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        root.addView(header);

        TextView sub = new TextView(activity);
        sub.setText("Select a full Theme Set (changes app colors, buttons & wallpaper) or pick a custom Wallpaper & Button Style below:");
        sub.setTextColor(COLOR_TEXT_SECONDARY);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
        root.addView(sub);

        // 1. Theme Sets Section
        TextView sec1 = new TextView(activity);
        sec1.setText("✨ 1. Choose Complete Theme + Wallpaper Set (6 Sets)");
        sec1.setTextColor(COLOR_AGORA_CYAN);
        sec1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        sec1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sec1.setPadding(0, 0, 0, dp(activity, 8));
        root.addView(sec1);

        for (int i = 0; i < THEME_PRESETS.length; i++) {
            final ThemePreset p = THEME_PRESETS[i];
            boolean active = (i == activeThemeIndex);

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            int cPad = dp(activity, 10);
            card.setPadding(cPad, cPad, cPad, cPad);
            card.setBackground(gradientBox(
                    activity,
                    p.cardBg,
                    p.cardElevated,
                    14f,
                    active ? Color.WHITE : p.accentHighlight,
                    active ? 2f : 1f
            ));

            ImageView thumb = new ImageView(activity);
            thumb.setImageResource(p.defaultWallpaperResId);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(activity, 10));
                }
            });
            thumb.setClipToOutline(true);
            card.addView(thumb, new LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56)));

            LinearLayout info = new LinearLayout(activity);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(activity, 10), 0, dp(activity, 8), 0);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tName = new TextView(activity);
            tName.setText(p.emoji + " " + p.title + (active ? "  ✓" : ""));
            tName.setTextColor(Color.WHITE);
            tName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            tName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView tSub = new TextView(activity);
            tSub.setText(p.subtitle);
            tSub.setTextColor(Color.rgb(226, 232, 240));
            tSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);

            info.addView(tName);
            info.addView(tSub);
            card.addView(info);

            TextView applyBtn = createStatusChip(
                    activity,
                    active ? "Applied ✓" : "Apply Set",
                    p.primaryBtnStart,
                    Color.WHITE,
                    p.accentHighlight
            );
            View.OnClickListener applyClick = v -> {
                applyThemePreset(activity, p.index);
                configureSafeActivityWindow(activity);
                Toast.makeText(activity, p.emoji + " Applied Theme Set: " + p.title, Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                if (onThemeChanged != null) onThemeChanged.run();
            };
            applyBtn.setOnClickListener(applyClick);
            card.setOnClickListener(applyClick);
            card.addView(applyBtn);

            root.addView(card, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, dp(activity, 8)));
        }

        // 2. Custom Wallpaper Only Selector
        TextView sec2 = new TextView(activity);
        sec2.setText("🖼️ 2. Pick Any Custom App Wallpaper (6 HD Backgrounds)");
        sec2.setTextColor(COLOR_AGORA_CYAN);
        sec2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        sec2.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sec2.setPadding(0, dp(activity, 10), 0, dp(activity, 8));
        root.addView(sec2);

        for (int w = 0; w < WALLPAPER_DRAWABLES.length; w++) {
            final int wIdx = w;
            boolean wActive = (w == activeWallpaperIndex);
            TextView wBtn = createControlBarButton(
                    activity,
                    (wActive ? "✅ " : "🖼️ ") + WALLPAPER_NAMES[w],
                    wActive ? COLOR_AGORA_BLUE : COLOR_CARD_ELEVATED,
                    Color.WHITE,
                    wActive ? COLOR_AGORA_CYAN : COLOR_BORDER_SUBTLE,
                    v -> {
                        applyWallpaperOnly(activity, wIdx);
                        Toast.makeText(activity, "🖼️ Wallpaper set to: " + WALLPAPER_NAMES[wIdx], Toast.LENGTH_SHORT).show();
                        if (dialogRef[0] != null) dialogRef[0].dismiss();
                        if (onThemeChanged != null) onThemeChanged.run();
                    }
            );
            root.addView(wBtn, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, dp(activity, 6)));
        }

        // 3. Wallpaper Brightness & Button Style Controls
        TextView sec3 = new TextView(activity);
        sec3.setText("🔘 3. Button ('Boton') Style & Wallpaper Brightness");
        sec3.setTextColor(COLOR_AGORA_CYAN);
        sec3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        sec3.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sec3.setPadding(0, dp(activity, 10), 0, dp(activity, 8));
        root.addView(sec3);

        LinearLayout btnStyleRow = new LinearLayout(activity);
        btnStyleRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] btnStyles = new String[]{"✨ Neon Glow", "👑 Gold Border", "💊 Crystal Pill"};
        for (int b = 0; b < btnStyles.length; b++) {
            final int bMode = b;
            boolean bActive = (buttonStyleMode == bMode);
            TextView bChip = createStatusChip(
                    activity,
                    btnStyles[b],
                    bActive ? COLOR_AGORA_BLUE : COLOR_CARD_ELEVATED,
                    Color.WHITE,
                    bActive ? COLOR_AGORA_CYAN : 0
            );
            bChip.setOnClickListener(v -> {
                setButtonStyleMode(activity, bMode);
                Toast.makeText(activity, "🔘 Button style: " + btnStyles[bMode], Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                if (onThemeChanged != null) onThemeChanged.run();
            });
            btnStyleRow.addView(bChip, params(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, dp(activity, 6), 0));
            ((LinearLayout.LayoutParams) bChip.getLayoutParams()).weight = 1f;
        }
        root.addView(btnStyleRow, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, dp(activity, 8)));

        LinearLayout alphaRow = new LinearLayout(activity);
        alphaRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] alphaModes = new String[]{"Balanced 42%", "Vivid HD 62%", "Dark Glass 24%"};
        for (int a = 0; a < alphaModes.length; a++) {
            final int aMode = a;
            boolean aActive = (wallpaperAlphaMode == aMode);
            TextView aChip = createStatusChip(
                    activity,
                    alphaModes[a],
                    aActive ? COLOR_LIVE_GREEN : COLOR_CARD_ELEVATED,
                    Color.WHITE,
                    0
            );
            aChip.setOnClickListener(v -> {
                setWallpaperAlphaMode(activity, aMode);
                Toast.makeText(activity, "🖼️ Wallpaper brightness: " + alphaModes[aMode], Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                if (onThemeChanged != null) onThemeChanged.run();
            });
            alphaRow.addView(aChip, params(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, dp(activity, 6), 0));
            ((LinearLayout.LayoutParams) aChip.getLayoutParams()).weight = 1f;
        }
        root.addView(alphaRow);

        dialogRef[0] = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🎨 Wallpaper & Theme Set Studio")
                .setView(scroll)
                .setPositiveButton("Done", null)
                .create();
        dialogRef[0].show();
    }

    public static View createSettingSwitchRow(
            Context context,
            String title,
            String subtitle,
            boolean checked,
            final BooleanToggleListener listener
    ) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 62));
        int vPad = dp(context, 10);
        int hPad = dp(context, 12);
        row.setPadding(hPad, vPad, hPad, vPad);
        row.setBackground(rippleButton(context, Color.argb(95, Color.red(COLOR_CARD_ELEVATED), Color.green(COLOR_CARD_ELEVATED), Color.blue(COLOR_CARD_ELEVATED)), 14f, COLOR_BORDER_SUBTLE, 1f));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        textCol.setPadding(0, 0, dp(context, 12), 0);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(COLOR_TEXT_PRIMARY);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(context);
            tvSub.setText(subtitle);
            tvSub.setTextColor(COLOR_TEXT_SECONDARY);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tvSub.setPadding(0, dp(context, 3), 0, 0);
            textCol.addView(tvSub);
        }

        final Switch sw = new Switch(context);
        sw.setChecked(checked);
        sw.setTextOn("ON");
        sw.setTextOff("OFF");
        sw.setMinimumWidth(dp(context, 52));
        sw.setMinimumHeight(dp(context, 48));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggled(isChecked);
            }
        });

        row.setOnClickListener(v -> sw.setChecked(!sw.isChecked()));

        row.addView(textCol);
        row.addView(sw);
        return row;
    }

    public static View createSettingActionRow(
            Context context,
            String title,
            String subtitle,
            String actionLabel,
            int actionBgColor,
            int actionTextColor,
            View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 62));
        int vPad = dp(context, 10);
        int hPad = dp(context, 12);
        row.setPadding(hPad, vPad, hPad, vPad);
        row.setBackground(rippleButton(context, Color.argb(95, Color.red(COLOR_CARD_ELEVATED), Color.green(COLOR_CARD_ELEVATED), Color.blue(COLOR_CARD_ELEVATED)), 14f, COLOR_BORDER_SUBTLE, 1f));
        if (listener != null) {
            row.setOnClickListener(listener);
        }

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        textCol.setPadding(0, 0, dp(context, 12), 0);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(COLOR_TEXT_PRIMARY);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(context);
            tvSub.setText(subtitle);
            tvSub.setTextColor(COLOR_TEXT_SECONDARY);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tvSub.setPadding(0, dp(context, 3), 0, 0);
            textCol.addView(tvSub);
        }

        TextView badge = createStatusChip(context, actionLabel, actionBgColor, actionTextColor, 0);
        if (listener != null) {
            badge.setOnClickListener(listener);
        }

        row.addView(textCol);
        row.addView(badge);
        return row;
    }

    public static View createStateBanner(Context context, String title, String message, int accentColor) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 14);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(roundedBox(context, COLOR_CARD_BG, 16f, accentColor, 1.2f));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView tvMsg = new TextView(context);
        tvMsg.setText(message);
        tvMsg.setTextColor(COLOR_TEXT_SECONDARY);
        tvMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tvMsg.setPadding(0, dp(context, 4), 0, 0);

        box.addView(tvTitle);
        box.addView(tvMsg);
        return box;
    }

    public interface GiftSentCallback {
        void onGiftSent(GiftEconomyManager.GiftItem gift, int comboMultiplier, GiftEconomyManager.WalletTransaction tx);
    }

    /**
     * Universal QLUVX Gift Panel Dialog implementing all required Gift Panel buttons:
     * - Category tabs: All, Popular, Luxury
     * - Combo multipliers: Combo x1, Combo x5, Combo x10
     * - Select Gift (interactive tile selection)
     * - Send Gift
     * - Close Panel
     */
    public static void showUnifiedGiftPanelDialog(
            final Activity activity,
            final GiftEconomyManager economy,
            final String senderName,
            final String receiverName,
            final GiftSentCallback callback
    ) {
        if (activity == null || economy == null) return;

        final AlertDialog[] dlgRef = new AlertDialog[1];
        final String[] activeCategory = new String[]{"All"};
        final int[] activeCombo = new int[]{1};
        final GiftEconomyManager.GiftItem[] selectedGift = new GiftEconomyManager.GiftItem[]{
                economy.getGiftsByCategory("All").isEmpty() ? null : economy.getGiftsByCategory("All").get(0)
        };

        ScrollView scroll = new ScrollView(activity);
        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 14);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(COLOR_BG_DEEP);
        scroll.addView(root);

        final Runnable[] renderPanel = new Runnable[1];
        renderPanel[0] = () -> {
            root.removeAllViews();

            // Header with Coin Balance + Quick Recharge
            LinearLayout topRow = new LinearLayout(activity);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView balTv = new TextView(activity);
            balTv.setText("🎁 Gift Panel • 🪙 " + economy.getCoinBalance() + " Coins\nTo: " + receiverName);
            balTv.setTextColor(COLOR_WARNING_AMBER);
            balTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            balTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            balTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView topUpBtn = createStatusChip(activity, "+ Recharge 1,500", Color.argb(80, 16, 185, 129), COLOR_LIVE_GREEN, COLOR_LIVE_GREEN);
            topUpBtn.setOnClickListener(v -> {
                economy.rechargeVerifiedPack(1500, "Gift Panel Instant Top-Up", null);
                Toast.makeText(activity, "✅ Recharged +1,500 Coins!", Toast.LENGTH_SHORT).show();
                renderPanel[0].run();
            });

            topRow.addView(balTv);
            topRow.addView(topUpBtn);
            root.addView(topRow, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, dp(activity, 10)));

            // 1. Category Tabs: All | Popular | Luxury
            LinearLayout catRow = new LinearLayout(activity);
            catRow.setOrientation(LinearLayout.HORIZONTAL);
            String[] cats = new String[]{"All", "Popular", "Luxury"};
            for (final String cat : cats) {
                boolean isCat = cat.equalsIgnoreCase(activeCategory[0]);
                TextView catBtn = createStatusChip(
                        activity,
                        cat,
                        isCat ? COLOR_AGORA_BLUE : COLOR_CARD_ELEVATED,
                        Color.WHITE,
                        isCat ? COLOR_AGORA_CYAN : COLOR_BORDER_SUBTLE
                );
                catBtn.setOnClickListener(v -> {
                    activeCategory[0] = cat;
                    java.util.List<GiftEconomyManager.GiftItem> list = filterGiftsForTab(economy, cat);
                    if (!list.isEmpty()) {
                        selectedGift[0] = list.get(0);
                    }
                    renderPanel[0].run();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(0, 0, dp(activity, 6), 0);
                catRow.addView(catBtn, lp);
            }
            root.addView(catRow, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, dp(activity, 10)));

            // 2. Select Gift Grid / List
            java.util.List<GiftEconomyManager.GiftItem> visibleGifts = filterGiftsForTab(economy, activeCategory[0]);
            for (final GiftEconomyManager.GiftItem g : visibleGifts) {
                boolean isSelected = (selectedGift[0] != null && selectedGift[0].id.equals(g.id));
                LinearLayout gRow = new LinearLayout(activity);
                gRow.setOrientation(LinearLayout.HORIZONTAL);
                gRow.setGravity(Gravity.CENTER_VERTICAL);
                int gp = dp(activity, 10);
                gRow.setPadding(gp, gp, gp, gp);
                gRow.setBackground(isSelected
                        ? gradientBox(activity, COLOR_AGORA_BLUE, COLOR_INDIGO_ACCENT, 14f, Color.WHITE, 2f)
                        : roundedBox(activity, COLOR_CARD_BG, 14f, COLOR_BORDER_SUBTLE, 1f));

                TextView gInfo = new TextView(activity);
                gInfo.setText(g.iconLabel + "  " + g.name + "  [" + g.category + "]\n🪙 " + g.coinPrice + " Coins •Combo x" + activeCombo[0] + " = " + (g.coinPrice * activeCombo[0]) + " Coins");
                gInfo.setTextColor(Color.WHITE);
                gInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                gInfo.setTypeface(Typeface.create("sans-serif-medium", isSelected ? Typeface.BOLD : Typeface.NORMAL));
                gInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView selectBtn = createStatusChip(
                        activity,
                        isSelected ? "✓ Selected" : "Select Gift",
                        isSelected ? Color.argb(90, 16, 185, 129) : COLOR_CARD_ELEVATED,
                        Color.WHITE,
                        0
                );
                View.OnClickListener selClick = v -> {
                    selectedGift[0] = g;
                    renderPanel[0].run();
                };
                selectBtn.setOnClickListener(selClick);
                gRow.setOnClickListener(selClick);

                gRow.addView(gInfo);
                gRow.addView(selectBtn);
                root.addView(gRow, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, dp(activity, 6)));
            }

            // 3. Combo Multipliers: Combo x1 | Combo x5 | Combo x10
            LinearLayout comboRow = new LinearLayout(activity);
            comboRow.setOrientation(LinearLayout.HORIZONTAL);
            int[] mults = new int[]{1, 5, 10};
            for (final int m : mults) {
                boolean isMult = (activeCombo[0] == m);
                TextView mBtn = createStatusChip(
                        activity,
                        "Combo x" + m,
                        isMult ? COLOR_WARNING_AMBER : COLOR_CARD_ELEVATED,
                        isMult ? Color.BLACK : Color.WHITE,
                        0
                );
                mBtn.setOnClickListener(v -> {
                    activeCombo[0] = m;
                    renderPanel[0].run();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(0, dp(activity, 6), dp(activity, 6), dp(activity, 10));
                comboRow.addView(mBtn, lp);
            }
            root.addView(comboRow);

            // 4. Send Gift & Close Panel Buttons
            LinearLayout bottomRow = new LinearLayout(activity);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView sendGiftBtn = createControlBarButton(
                    activity,
                    "🎁 Send Gift" + (selectedGift[0] != null ? (" (" + selectedGift[0].iconLabel + " x" + activeCombo[0] + ")") : ""),
                    COLOR_AGORA_BLUE,
                    Color.WHITE,
                    0,
                    v -> {
                        if (selectedGift[0] == null) return;
                        GiftEconomyManager.WalletTransaction tx = economy.sendGiftCombo(
                                selectedGift[0],
                                activeCombo[0],
                                senderName,
                                receiverName,
                                "gift_" + System.currentTimeMillis()
                        );
                        if (tx != null) {
                            Toast.makeText(activity, "🎉 Sent " + selectedGift[0].iconLabel + " " + selectedGift[0].name + " x" + activeCombo[0] + " to " + receiverName + "!", Toast.LENGTH_SHORT).show();
                            if (dlgRef[0] != null) dlgRef[0].dismiss();
                            if (callback != null) {
                                callback.onGiftSent(selectedGift[0], activeCombo[0], tx);
                            }
                        } else {
                            Toast.makeText(activity, "⚠️ Insufficient Coins! Tap '+ Recharge 1,500' at the top.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            sendGiftBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));

            TextView closePanelBtn = createControlBarButton(
                    activity,
                    "✕ Close Panel",
                    COLOR_CARD_ELEVATED,
                    COLOR_TEXT_SECONDARY,
                    COLOR_BORDER_SUBTLE,
                    v -> {
                        if (dlgRef[0] != null) dlgRef[0].dismiss();
                    }
            );
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);
            cLp.setMargins(dp(activity, 8), 0, 0, 0);
            closePanelBtn.setLayoutParams(cLp);

            bottomRow.addView(sendGiftBtn);
            bottomRow.addView(closePanelBtn);
            root.addView(bottomRow);
        };

        renderPanel[0].run();

        dlgRef[0] = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(scroll)
                .create();
        dlgRef[0].show();
    }

    private static java.util.List<GiftEconomyManager.GiftItem> filterGiftsForTab(GiftEconomyManager economy, String tab) {
        java.util.List<GiftEconomyManager.GiftItem> all = economy.getGiftsByCategory("All");
        if ("All".equalsIgnoreCase(tab)) {
            return all;
        }
        java.util.List<GiftEconomyManager.GiftItem> out = new java.util.ArrayList<>();
        for (GiftEconomyManager.GiftItem g : all) {
            if ("Popular".equalsIgnoreCase(tab)) {
                if ("Normal".equalsIgnoreCase(g.category) || "Popular".equalsIgnoreCase(g.category) || g.coinPrice <= 200) {
                    out.add(g);
                }
            } else if ("Luxury".equalsIgnoreCase(tab)) {
                if ("Luxury".equalsIgnoreCase(g.category) || "Royal".equalsIgnoreCase(g.category) || g.coinPrice >= 199) {
                    out.add(g);
                }
            }
        }
        return out.isEmpty() ? all : out;
    }

    public static LinearLayout.LayoutParams params(int w, int h, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }
}
