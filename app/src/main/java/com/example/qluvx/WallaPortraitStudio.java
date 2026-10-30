package com.example.qluvx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.LruCache;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

/**
 * Real High-Definition Male Portrait Photography & Live Media Studio for Walla / Blued (QLUVX).
 * Uses Glide to load real HD male portrait photography from Unsplash across all screens
 * (Nearby Grid, Live Stories, Live PK, 8-Seat Voice Party, Messages, 5s Burn Photos, and Profile).
 * Zero cartoon or vector face illustrations.
 */
public class WallaPortraitStudio {

    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(16);

    // 12 Curated Real High-Resolution Male Portrait Photos from Unsplash (Gym, Travel, Stylish, Rooftop)
    private static final String[] REAL_HD_MALE_PHOTOS = new String[]{
            // 0: Alex, 25 (Exact user-requested Unsplash HD male portrait #1)
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=900&q=85",
            // 1: Ryan, 28 (Exact user-requested Unsplash HD male portrait #2)
            "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=900&q=85",
            // 2: Kabir, 24 (Exact user-requested Unsplash HD male portrait #3)
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=900&q=85",
            // 3: Zayn, 26 (Exact user-requested Unsplash HD male portrait #4)
            "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=900&q=85",
            // 4: Liam, 23 (Stylish model portrait)
            "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=900&q=85",
            // 5: Noah, 27 (Athletic coastal male portrait)
            "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=900&q=85",
            // 6: Aarav, 25 / Julian Vance (Sharp modern male portrait)
            "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=900&q=85",
            // 7: Ethan, 29 (Fitness & tailored style male portrait)
            "https://images.unsplash.com/photo-1534030347209-467a5b0ad3e6?auto=format&fit=crop&w=900&q=85",
            // 8: Rohan, 22 (Urban jacket male portrait)
            "https://images.unsplash.com/photo-1488161628813-04466f872be2?auto=format&fit=crop&w=900&q=85",
            // 9: Vikram, 28 (Bearded athletic male portrait)
            "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=900&q=85",
            // 10: Lucas, 26 (Travel & gym male portrait)
            "https://images.unsplash.com/photo-1504257432389-52343af06ae3?auto=format&fit=crop&w=900&q=85",
            // 11: Dev, 24 (Nightlife & fashion male portrait)
            "https://images.unsplash.com/photo-1513956589380-bad6acb9b9d4?auto=format&fit=crop&w=900&q=85"
    };

    // Real High-Resolution 5s Disappearing Secret Photos (Fitness / Gym / Poolside / Travel shots)
    private static final String[] REAL_HD_SECRET_BURN_PHOTOS = new String[]{
            "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=1000&q=85",
            "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&w=1000&q=85",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1000&q=85",
            "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=1000&q=85"
    };

    // Real Looping HD Video Stream URLs for Challenger (Host 2) in Dual PK Battle
    private static final String[] REAL_LIVE_PK_VIDEO_STREAMS = new String[]{
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4"
    };

    private static final String REAL_PROFILE_COVER_URL =
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=85";

    public static String getGuyPhotoUrl(int index) {
        int safeIdx = Math.abs(index) % REAL_HD_MALE_PHOTOS.length;
        return REAL_HD_MALE_PHOTOS[safeIdx];
    }

    public static String getSecretBurnPhotoUrl(int index) {
        int safeIdx = Math.abs(index) % REAL_HD_SECRET_BURN_PHOTOS.length;
        return REAL_HD_SECRET_BURN_PHOTOS[safeIdx];
    }

    public static String getLiveChallengerVideoUrl(int index) {
        int safeIdx = Math.abs(index) % REAL_LIVE_PK_VIDEO_STREAMS.length;
        return REAL_LIVE_PK_VIDEO_STREAMS[safeIdx];
    }

    /**
     * Loads a real HD male portrait photo from Unsplash into the target ImageView using Glide.
     */
    public static void loadGuyPhoto(Context context, ImageView imageView, int index) {
        if (context == null || imageView == null) return;
        String url = getGuyPhotoUrl(index);
        Drawable placeholder = createSleekDarkShimmerDrawable(index);
        try {
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(220))
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageDrawable(placeholder);
        }
    }

    /**
     * Loads a real HD 5-second disappearing photo into the full-screen burn dialog via Glide.
     */
    public static void loadSecretBurnPhoto(Context context, ImageView imageView, int index) {
        if (context == null || imageView == null) return;
        String url = getSecretBurnPhotoUrl(index);
        Drawable placeholder = createSleekDarkShimmerDrawable(index);
        try {
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(placeholder)
                    .error(getGuyPhotoUrl(index))
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(180))
                    .into(imageView);
        } catch (Exception e) {
            loadGuyPhoto(context, imageView, index);
        }
    }

    /**
     * Loads a real HD nightlife cover photo into the Profile cover banner via Glide.
     */
    public static void loadProfileCoverPhoto(Context context, ImageView imageView) {
        if (context == null || imageView == null) return;
        Drawable fallback = getProfileCoverDrawable(context);
        try {
            Glide.with(context)
                    .load(REAL_PROFILE_COVER_URL)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(fallback)
                    .error(fallback)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(250))
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageDrawable(fallback);
        }
    }

    private static Drawable createSleekDarkShimmerDrawable(int index) {
        int[][] accents = new int[][]{
                {0xFF241038, 0xFF0A0A14},
                {0xFF0D2538, 0xFF0A0A14},
                {0xFF2D1024, 0xFF0A0A14},
                {0xFF1A163B, 0xFF0A0A14}
        };
        int[] pair = accents[Math.abs(index) % accents.length];
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{pair[0], pair[1]}
        );
        gd.setShape(GradientDrawable.RECTANGLE);
        return gd;
    }

    public static Drawable getVoiceRoomWallpaper(Context context, boolean isVelvetRose) {
        return getVoiceRoomWallpaperByIndex(context, isVelvetRose ? 2 : 0);
    }

    /**
     * Returns one of the 4 Luxury Voice Party Room Wallpapers:
     * 0 = Cyber Galaxy, 1 = Royal Gold Palace, 2 = Velvet Rose, 3 = Midnight Neon.
     */
    public static Drawable getVoiceRoomWallpaperByIndex(Context context, int themeIndex) {
        int safeTheme = Math.abs(themeIndex) % 4;
        String key = "wallpaper_theme_" + safeTheme;
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderRoomWallpaperBitmapByTheme(720, 1280, safeTheme);
            BITMAP_CACHE.put(key, bmp);
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    public static Drawable getProfileCoverDrawable(Context context) {
        String key = "profile_cover_deluxe";
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderProfileCoverBitmap(800, 360);
            BITMAP_CACHE.put(key, bmp);
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    public static Drawable getImperialDragonFrameDrawable(Context context) {
        String key = "imperial_dragon_frame";
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderImperialDragonFrameBitmap(320, 320);
            BITMAP_CACHE.put(key, bmp);
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    private static Bitmap renderRoomWallpaperBitmapByTheme(int w, int h, int themeIndex) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int topColor;
        int midColor;
        int botColor = 0xFF070710;
        int glow1;
        int glow2;
        int glow3;

        if (themeIndex == 1) {
            // 1: Royal Gold Palace
            topColor = 0xFF281C06;
            midColor = 0xFF171005;
            botColor = 0xFF0A0805;
            glow1 = 0xFFF1C40F;
            glow2 = 0xFFFF8F00;
            glow3 = 0xFFFFD54F;
        } else if (themeIndex == 2) {
            // 2: Velvet Rose
            topColor = 0xFF23061C;
            midColor = 0xFF140517;
            botColor = 0xFF08050E;
            glow1 = 0xFFFF2A7A;
            glow2 = 0xFFF1C40F;
            glow3 = 0xFF9C27B0;
        } else if (themeIndex == 3) {
            // 3: Midnight Neon
            topColor = 0xFF051D24;
            midColor = 0xFF0A0B24;
            botColor = 0xFF05060F;
            glow1 = 0xFF00E676;
            glow2 = 0xFF00E5FF;
            glow3 = 0xFFFF2A7A;
        } else {
            // 0: Cyber Galaxy
            topColor = 0xFF080B24;
            midColor = 0xFF0A0A18;
            botColor = 0xFF070710;
            glow1 = 0xFF00E5FF;
            glow2 = 0xFFFF2A7A;
            glow3 = 0xFF7C4DFF;
        }

        p.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{topColor, midColor, botColor},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);

        p.setShader(new RadialGradient(w * 0.5f, h * 0.25f, w * 0.74f,
                withAlpha(glow1, 78), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.5f, h * 0.25f, w * 0.74f, p);

        p.setShader(new RadialGradient(w * 0.15f, h * 0.52f, w * 0.55f,
                withAlpha(glow2, 58), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.15f, h * 0.52f, w * 0.55f, p);

        p.setShader(new RadialGradient(w * 0.85f, h * 0.62f, w * 0.58f,
                withAlpha(glow3, 62), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.85f, h * 0.62f, w * 0.58f, p);

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2.2f);
        ring.setColor(withAlpha(glow1, 48));
        canvas.drawCircle(w * 0.5f, h * 0.25f, w * 0.26f, ring);
        ring.setColor(withAlpha(glow2, 36));
        canvas.drawCircle(w * 0.5f, h * 0.25f, w * 0.38f, ring);

        if (themeIndex == 1) {
            // Royal Gold Palace columns/arch accent lines
            ring.setColor(withAlpha(0xFFF1C40F, 55));
            ring.setStrokeWidth(3f);
            canvas.drawRoundRect(new RectF(w * 0.06f, h * 0.08f, w * 0.94f, h * 0.70f), 42f, 42f, ring);
        }

        p.setShader(null);
        float[][] stars = {
                {0.12f, 0.11f, 3.5f}, {0.28f, 0.08f, 2.5f}, {0.74f, 0.10f, 4.0f},
                {0.88f, 0.16f, 2.8f}, {0.18f, 0.34f, 3.0f}, {0.82f, 0.36f, 3.5f},
                {0.09f, 0.68f, 2.5f}, {0.91f, 0.72f, 3.2f}, {0.42f, 0.15f, 2.2f},
                {0.62f, 0.21f, 2.8f}, {0.33f, 0.48f, 2.4f}, {0.67f, 0.50f, 3.0f}
        };
        for (int i = 0; i < stars.length; i++) {
            p.setColor(i % 2 == 0 ? withAlpha(0xFFF1C40F, 165) : withAlpha(0xFFFFFFFF, 135));
            canvas.drawCircle(w * stars[i][0], h * stars[i][1], stars[i][2], p);
        }

        return bmp;
    }

    private static Bitmap renderProfileCoverBitmap(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xFF1F092B, 0xFF0F1634, 0xFF0A0A14},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);

        p.setShader(new RadialGradient(w * 0.78f, h * 0.32f, w * 0.45f,
                withAlpha(0xFFF1C40F, 95), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.78f, h * 0.32f, w * 0.45f, p);

        p.setShader(new RadialGradient(w * 0.22f, h * 0.45f, w * 0.42f,
                withAlpha(0xFFFF2A7A, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.22f, h * 0.45f, w * 0.42f, p);

        return bmp;
    }

    private static Bitmap renderImperialDragonFrameBitmap(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float rOuter = w * 0.44f;
        float rInner = w * 0.37f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setShader(new RadialGradient(cx, cy, w * 0.49f,
                new int[]{Color.TRANSPARENT, withAlpha(0xFFF1C40F, 130), withAlpha(0xFFFF2A7A, 90), Color.TRANSPARENT},
                new float[]{0.68f, 0.82f, 0.93f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, w * 0.49f, p);

        p.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xFFFFF3B0, 0xFFF1C40F, 0xFFD4AF37, 0xFFFF8F00, 0xFFFFE082},
                null, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.065f);
        canvas.drawCircle(cx, cy, (rOuter + rInner) * 0.5f, p);

        Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(w * 0.016f);
        innerRim.setColor(0xFFFF2A7A);
        canvas.drawCircle(cx, cy, rInner, innerRim);

        Paint pearl = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            float px = cx + (float) Math.cos(angle) * rOuter;
            float py = cy + (float) Math.sin(angle) * rOuter;
            pearl.setColor(i % 2 == 0 ? 0xFFFFF59D : 0xFFFF2A7A);
            canvas.drawCircle(px, py, w * 0.025f, pearl);
        }

        Path crown = new Path();
        crown.moveTo(cx - w * 0.16f, cy - rOuter + w * 0.04f);
        crown.lineTo(cx - w * 0.21f, cy - rOuter - w * 0.05f);
        crown.lineTo(cx - w * 0.08f, cy - rOuter + w * 0.01f);
        crown.lineTo(cx, cy - rOuter - w * 0.07f);
        crown.lineTo(cx + w * 0.08f, cy - rOuter + w * 0.01f);
        crown.lineTo(cx + w * 0.21f, cy - rOuter - w * 0.05f);
        crown.lineTo(cx + w * 0.16f, cy - rOuter + w * 0.04f);
        crown.close();

        Paint crownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crownPaint.setShader(new LinearGradient(cx - w * 0.2f, 0, cx + w * 0.2f, w * 0.2f,
                0xFFFFE082, 0xFFFF8F00, Shader.TileMode.CLAMP));
        canvas.drawPath(crown, crownPaint);

        pearl.setColor(0xFFFF1744);
        canvas.drawCircle(cx, cy - rOuter - w * 0.01f, w * 0.024f, pearl);

        RectF ribbon = new RectF(cx - w * 0.34f, h * 0.82f, cx + w * 0.34f, h * 0.96f);
        Paint ribbonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ribbonPaint.setShader(new LinearGradient(ribbon.left, ribbon.top, ribbon.right, ribbon.bottom,
                0xFFD4AF37, 0xFFFFE082, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(ribbon, w * 0.05f, w * 0.05f, ribbonPaint);

        innerRim.setColor(0xFF4A2C00);
        innerRim.setStrokeWidth(w * 0.008f);
        canvas.drawRoundRect(ribbon, w * 0.05f, w * 0.05f, innerRim);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF1A0F00);
        textPaint.setTextSize(w * 0.068f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🐉 DRAGON VIP", cx, h * 0.915f, textPaint);

        return bmp;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }
}
