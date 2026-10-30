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
import android.util.LruCache;

/**
 * Self-contained High-Resolution Visual Studio for Walla / Blued Luxury UI.
 * Generates crisp portrait photography-style bitmaps, live streamer studio feeds,
 * Cyber Galaxy / Velvet Rose room wallpapers, and the Imperial Gold Dragon Avatar Frame
 * with zero external dependencies or network lag.
 */
public class WallaPortraitStudio {

    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(28);

    // 12 Distinct Handsome Guy Palettes: [bgTop, bgBottom, rimLeft, rimRight, skinTone, hairColor, shirtColor, styleType]
    private static final int[][] GUY_THEMES = new int[][]{
            // 0: Alex, 25 (Hot Pink & Cyan Lounge, Athletic Henley)
            {0xFF1A0B2E, 0xFF0A0A14, 0xFFFF2A7A, 0xFF00E5FF, 0xFFE8B49A, 0xFF1C1412, 0xFF1E2238, 0},
            // 1: Ryan, 28 (Cyan & Gold Penthouse, Black Leather Jacket)
            {0xFF091E2C, 0xFF0A0A14, 0xFF00E5FF, 0xFFF1C40F, 0xFFDFA689, 0xFF231815, 0xFF14161D, 1},
            // 2: Kabir, 24 (Crimson Velvet Club, Fitted V-Neck)
            {0xFF2B091E, 0xFF0A0A14, 0xFFFF2A7A, 0xFFFF9100, 0xFFD2997A, 0xFF120E0D, 0xFF281524, 2},
            // 3: Zayn, 26 (Electric Violet & Cyan Studio, Streetwear Hoodie)
            {0xFF1B1035, 0xFF0A0A14, 0xFF7C4DFF, 0xFF00E5FF, 0xFFE2AD91, 0xFF181210, 0xFF1B1E32, 3},
            // 4: Liam, 23 (Golden Hour Sunset Loft, White/Dark Open Collar)
            {0xFF281812, 0xFF0A0A14, 0xFFF1C40F, 0xFFFF2A7A, 0xFFF0C0A6, 0xFF3A2418, 0xFF23263A, 0},
            // 5: Noah, 27 (Emerald & Cyan Cyber Bar, Sleek Turtleneck)
            {0xFF082426, 0xFF0A0A14, 0xFF00E676, 0xFF00E5FF, 0xFFD99E80, 0xFF1A1412, 0xFF121C24, 1},
            // 6: Aarav, 25 (Royal Gold & Magenta VIP Suite, Tailored Blazer)
            {0xFF24142E, 0xFF0A0A14, 0xFFF1C40F, 0xFFFF2A7A, 0xFFCE9272, 0xFF110D0C, 0xFF19192B, 2},
            // 7: Ethan, 29 (Deep Blue & Neon Pink Rooftop, Fitted Gym Tank)
            {0xFF0D1B38, 0xFF0A0A14, 0xFF00E5FF, 0xFFFF2A7A, 0xFFE5B196, 0xFF2A1B14, 0xFF1D2236, 3},
            // 8: Rohan, 22 (Neon Rose & Violet Studio, Bomber Jacket)
            {0xFF2A0D26, 0xFF0A0A14, 0xFFFF2A7A, 0xFF7C4DFF, 0xFFD49B7B, 0xFF15100F, 0xFF221A30, 0},
            // 9: Vikram, 28 (Amber & Cyan Whiskey Lounge, Dark Shirt)
            {0xFF221810, 0xFF0A0A14, 0xFFF1C40F, 0xFF00E5FF, 0xFFCA8E6E, 0xFF100C0B, 0xFF161A28, 1},
            // 10: Lucas, 26 (Cyber Cyan & Magenta Arcade, Crewneck)
            {0xFF0C2233, 0xFF0A0A14, 0xFF00E5FF, 0xFFFF2A7A, 0xFFECBAA0, 0xFF322016, 0xFF1A2436, 2},
            // 11: Dev, 24 (Velvet Purple & Gold Lounge, Designer Jacket)
            {0xFF1F0F2E, 0xFF0A0A14, 0xFF9C27B0, 0xFFF1C40F, 0xFFD19676, 0xFF140F0E, 0xFF1C182B, 3}
    };

    public static Drawable getGuyPortraitDrawable(Context context, int index) {
        int safeIdx = Math.abs(index) % GUY_THEMES.length;
        String key = "guy_portrait_" + safeIdx;
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderGuyPortraitBitmap(420, 560, safeIdx, false, false);
            BITMAP_CACHE.put(key, bmp);
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    public static Bitmap getGuyPortraitBitmap(Context context, int index) {
        int safeIdx = Math.abs(index) % GUY_THEMES.length;
        String key = "guy_portrait_" + safeIdx;
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderGuyPortraitBitmap(420, 560, safeIdx, false, false);
            BITMAP_CACHE.put(key, bmp);
        }
        return bmp;
    }

    public static Drawable getLiveStreamerFeedDrawable(Context context, boolean isRedSide) {
        String key = isRedSide ? "pk_streamer_red" : "pk_streamer_blue";
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderGuyPortraitBitmap(540, 780, isRedSide ? 0 : 1, true, isRedSide);
            BITMAP_CACHE.put(key, bmp);
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    public static Drawable getVoiceRoomWallpaper(Context context, boolean isVelvetRose) {
        String key = isVelvetRose ? "wallpaper_velvet_rose" : "wallpaper_cyber_galaxy";
        Bitmap bmp = BITMAP_CACHE.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = renderRoomWallpaperBitmap(720, 1280, isVelvetRose);
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

    private static Bitmap renderGuyPortraitBitmap(int w, int h, int themeIdx, boolean isStreamerStudio, boolean isRedHost) {
        int[] t = GUY_THEMES[themeIdx % GUY_THEMES.length];
        int bgTop = isStreamerStudio ? (isRedHost ? 0xFF33081C : 0xFF071F38) : t[0];
        int bgBottom = 0xFF0A0A14;
        int rimLeft = isStreamerStudio ? (isRedHost ? 0xFFFF2A7A : 0xFF00E5FF) : t[2];
        int rimRight = isStreamerStudio ? (isRedHost ? 0xFFF1C40F : 0xFF7C4DFF) : t[3];
        int skinBase = t[4];
        int hairColor = t[5];
        int outfitColor = t[6];
        int styleType = t[7];

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 1. Deep Studio Backdrop Gradient
        p.setShader(new LinearGradient(0, 0, 0, h, bgTop, bgBottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);

        // 2. Ambient Neon Bokeh Lights
        p.setShader(new RadialGradient(w * 0.18f, h * 0.25f, w * 0.55f,
                withAlpha(rimLeft, 115), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.18f, h * 0.25f, w * 0.55f, p);

        p.setShader(new RadialGradient(w * 0.85f, h * 0.35f, w * 0.50f,
                withAlpha(rimRight, 105), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.85f, h * 0.35f, w * 0.50f, p);

        // Studio Neon Light Bars for Live Streamers or Penthouse Bokeh
        p.setShader(null);
        if (isStreamerStudio) {
            Paint neonBar = new Paint(Paint.ANTI_ALIAS_FLAG);
            neonBar.setStrokeWidth(w * 0.018f);
            neonBar.setStrokeCap(Paint.Cap.ROUND);
            neonBar.setColor(withAlpha(rimLeft, 190));
            canvas.drawLine(w * 0.12f, h * 0.10f, w * 0.12f, h * 0.48f, neonBar);
            neonBar.setColor(withAlpha(rimRight, 180));
            canvas.drawLine(w * 0.88f, h * 0.14f, w * 0.88f, h * 0.52f, neonBar);
        } else {
            // Soft bokeh orbs
            p.setColor(withAlpha(rimLeft, 55));
            canvas.drawCircle(w * 0.22f, h * 0.18f, w * 0.09f, p);
            p.setColor(withAlpha(rimRight, 50));
            canvas.drawCircle(w * 0.78f, h * 0.22f, w * 0.07f, p);
            p.setColor(withAlpha(0xFFF1C40F, 40));
            canvas.drawCircle(w * 0.82f, h * 0.12f, w * 0.045f, p);
        }

        float cx = w * 0.5f;
        float headCy = h * 0.36f;
        float headRx = w * 0.21f;
        float headRy = h * 0.165f;

        // 3. Broad Athletic Shoulders & Torso
        Path torso = new Path();
        torso.moveTo(cx - w * 0.44f, h);
        torso.cubicTo(cx - w * 0.42f, h * 0.72f, cx - w * 0.28f, h * 0.63f, cx - w * 0.11f, h * 0.60f);
        torso.lineTo(cx + w * 0.11f, h * 0.60f);
        torso.cubicTo(cx + w * 0.28f, h * 0.63f, cx + w * 0.42f, h * 0.72f, cx + w * 0.44f, h);
        torso.close();

        p.setShader(new LinearGradient(cx - w * 0.4f, h * 0.6f, cx + w * 0.4f, h,
                blendColor(outfitColor, rimLeft, 0.22f),
                blendColor(outfitColor, 0xFF05050A, 0.5f),
                Shader.TileMode.CLAMP));
        canvas.drawPath(torso, p);

        // Rim light on shoulders
        Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(w * 0.014f);
        rimPaint.setShader(new LinearGradient(0, h * 0.6f, w, h * 0.85f,
                rimLeft, rimRight, Shader.TileMode.CLAMP));
        canvas.drawPath(torso, rimPaint);

        // 4. Muscular Neck & Collar Shadow
        p.setShader(new LinearGradient(cx - w * 0.09f, h * 0.48f, cx + w * 0.09f, h * 0.66f,
                darken(skinBase, 0.88f), darken(skinBase, 0.70f), Shader.TileMode.CLAMP));
        RectF neckRect = new RectF(cx - w * 0.095f, h * 0.47f, cx + w * 0.095f, h * 0.65f);
        canvas.drawRoundRect(neckRect, w * 0.04f, w * 0.04f, p);

        // V-Neck / Jacket Lapels
        Path collar = new Path();
        collar.moveTo(cx - w * 0.10f, h * 0.59f);
        collar.lineTo(cx, h * 0.70f);
        collar.lineTo(cx + w * 0.10f, h * 0.59f);
        collar.close();
        p.setShader(new LinearGradient(cx, h * 0.58f, cx, h * 0.70f,
                darken(skinBase, 0.85f), darken(skinBase, 0.62f), Shader.TileMode.CLAMP));
        canvas.drawPath(collar, p);

        if (styleType == 1 || styleType == 3) {
            // Sleek Jacket Collar / Lapel lines
            Paint lapel = new Paint(Paint.ANTI_ALIAS_FLAG);
            lapel.setStyle(Paint.Style.STROKE);
            lapel.setStrokeWidth(w * 0.012f);
            lapel.setColor(withAlpha(0xFFFFFFFF, 45));
            canvas.drawLine(cx - w * 0.13f, h * 0.60f, cx - w * 0.04f, h * 0.76f, lapel);
            canvas.drawLine(cx + w * 0.13f, h * 0.60f, cx + w * 0.04f, h * 0.76f, lapel);
        }

        // Silver/Gold Chain Necklace detail
        Paint chain = new Paint(Paint.ANTI_ALIAS_FLAG);
        chain.setStyle(Paint.Style.STROKE);
        chain.setStrokeWidth(w * 0.006f);
        chain.setColor(0xFFD4AF37);
        RectF chainOval = new RectF(cx - w * 0.085f, h * 0.56f, cx + w * 0.085f, h * 0.67f);
        canvas.drawArc(chainOval, 15, 150, false, chain);

        // 5. Ears
        p.setShader(null);
        p.setColor(darken(skinBase, 0.86f));
        canvas.drawOval(new RectF(cx - headRx * 1.12f, headCy - headRy * 0.15f, cx - headRx * 0.88f, headCy + headRy * 0.32f), p);
        canvas.drawOval(new RectF(cx + headRx * 0.88f, headCy - headRy * 0.15f, cx + headRx * 1.12f, headCy + headRy * 0.32f), p);

        // 6. Sculpted Male Face & Chiseled Jawline
        Path facePath = new Path();
        facePath.moveTo(cx - headRx, headCy - headRy * 0.25f);
        facePath.cubicTo(cx - headRx, headCy - headRy * 1.05f, cx + headRx, headCy - headRy * 1.05f, cx + headRx, headCy - headRy * 0.25f);
        facePath.lineTo(cx + headRx * 0.92f, headCy + headRy * 0.38f);
        facePath.cubicTo(cx + headRx * 0.75f, headCy + headRy * 0.82f, cx + headRx * 0.28f, headCy + headRy * 1.05f, cx, headCy + headRy * 1.08f);
        facePath.cubicTo(cx - headRx * 0.28f, headCy + headRy * 1.05f, cx - headRx * 0.75f, headCy + headRy * 0.82f, cx - headRx * 0.92f, headCy + headRy * 0.38f);
        facePath.close();

        p.setShader(new LinearGradient(cx - headRx, headCy - headRy, cx + headRx, headCy + headRy,
                blendColor(skinBase, rimLeft, 0.16f),
                darken(skinBase, 0.82f),
                Shader.TileMode.CLAMP));
        canvas.drawPath(facePath, p);

        // Neon Rim Highlight on Cheek & Jaw
        Paint faceRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        faceRim.setStyle(Paint.Style.STROKE);
        faceRim.setStrokeWidth(w * 0.009f);
        faceRim.setShader(new LinearGradient(cx - headRx, headCy, cx + headRx, headCy,
                withAlpha(rimLeft, 190), withAlpha(rimRight, 170), Shader.TileMode.CLAMP));
        canvas.drawPath(facePath, faceRim);

        // 7. Designer Stubble / Jaw Shading (for mature masculine look)
        if (themeIdx % 2 == 1 || isStreamerStudio) {
            Path beardPath = new Path();
            beardPath.moveTo(cx - headRx * 0.90f, headCy + headRy * 0.18f);
            beardPath.quadTo(cx, headCy + headRy * 0.58f, cx + headRx * 0.90f, headCy + headRy * 0.18f);
            beardPath.lineTo(cx + headRx * 0.88f, headCy + headRy * 0.42f);
            beardPath.cubicTo(cx + headRx * 0.65f, headCy + headRy * 0.92f, cx + headRx * 0.25f, headCy + headRy * 1.06f, cx, headCy + headRy * 1.08f);
            beardPath.cubicTo(cx - headRx * 0.25f, headCy + headRy * 1.06f, cx - headRx * 0.65f, headCy + headRy * 0.92f, cx - headRx * 0.88f, headCy + headRy * 0.42f);
            beardPath.close();
            p.setShader(null);
            p.setColor(withAlpha(hairColor, 68));
            canvas.drawPath(beardPath, p);
        }

        // 8. Expressive Eyes, Brows, Nose & Confident Smile
        p.setShader(null);
        float eyeY = headCy - headRy * 0.06f;
        float eyeOffsetX = headRx * 0.42f;

        // Eyebrows
        Paint browPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        browPaint.setStyle(Paint.Style.STROKE);
        browPaint.setStrokeWidth(w * 0.015f);
        browPaint.setStrokeCap(Paint.Cap.ROUND);
        browPaint.setColor(hairColor);
        canvas.drawLine(cx - eyeOffsetX - headRx * 0.20f, eyeY - headRy * 0.18f,
                cx - eyeOffsetX + headRx * 0.18f, eyeY - headRy * 0.21f, browPaint);
        canvas.drawLine(cx + eyeOffsetX - headRx * 0.18f, eyeY - headRy * 0.21f,
                cx + eyeOffsetX + headRx * 0.20f, eyeY - headRy * 0.18f, browPaint);

        // Eyes
        p.setColor(0xFFF8F9FA);
        canvas.drawOval(new RectF(cx - eyeOffsetX - headRx * 0.17f, eyeY - headRy * 0.07f,
                cx - eyeOffsetX + headRx * 0.17f, eyeY + headRy * 0.07f), p);
        canvas.drawOval(new RectF(cx + eyeOffsetX - headRx * 0.17f, eyeY - headRy * 0.07f,
                cx + eyeOffsetX + headRx * 0.17f, eyeY + headRy * 0.07f), p);

        // Irises & Catchlights
        p.setColor(themeIdx % 3 == 0 ? 0xFF3E2723 : (themeIdx % 3 == 1 ? 0xFF263238 : 0xFF4E342E));
        canvas.drawCircle(cx - eyeOffsetX, eyeY, headRx * 0.085f, p);
        canvas.drawCircle(cx + eyeOffsetX, eyeY, headRx * 0.085f, p);
        p.setColor(0xFF0A0A10);
        canvas.drawCircle(cx - eyeOffsetX, eyeY, headRx * 0.045f, p);
        canvas.drawCircle(cx + eyeOffsetX, eyeY, headRx * 0.045f, p);
        p.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx - eyeOffsetX + headRx * 0.028f, eyeY - headRy * 0.025f, headRx * 0.022f, p);
        canvas.drawCircle(cx + eyeOffsetX + headRx * 0.028f, eyeY - headRy * 0.025f, headRx * 0.022f, p);

        // Nose Bridge & Shadow
        Paint nosePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nosePaint.setStyle(Paint.Style.STROKE);
        nosePaint.setStrokeWidth(w * 0.008f);
        nosePaint.setStrokeCap(Paint.Cap.ROUND);
        nosePaint.setColor(darken(skinBase, 0.72f));
        Path nosePath = new Path();
        nosePath.moveTo(cx - headRx * 0.02f, eyeY + headRy * 0.05f);
        nosePath.lineTo(cx - headRx * 0.05f, headCy + headRy * 0.30f);
        nosePath.quadTo(cx, headCy + headRy * 0.35f, cx + headRx * 0.06f, headCy + headRy * 0.30f);
        canvas.drawPath(nosePath, nosePaint);

        // Confident Warm Smile
        Paint lipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lipPaint.setStyle(Paint.Style.STROKE);
        lipPaint.setStrokeWidth(w * 0.013f);
        lipPaint.setStrokeCap(Paint.Cap.ROUND);
        lipPaint.setColor(blendColor(darken(skinBase, 0.68f), 0xFFD85A6C, 0.42f));
        Path smile = new Path();
        smile.moveTo(cx - headRx * 0.25f, headCy + headRy * 0.55f);
        smile.quadTo(cx, headCy + headRy * 0.67f, cx + headRx * 0.26f, headCy + headRy * 0.53f);
        canvas.drawPath(smile, lipPaint);

        // 9. Modern Textured Haircut
        Path hairPath = new Path();
        hairPath.moveTo(cx - headRx * 1.04f, headCy - headRy * 0.12f);
        hairPath.cubicTo(cx - headRx * 1.15f, headCy - headRy * 0.95f,
                cx - headRx * 0.45f, headCy - headRy * 1.38f,
                cx + headRx * 0.08f, headCy - headRy * 1.30f);
        hairPath.cubicTo(cx + headRx * 0.68f, headCy - headRy * 1.35f,
                cx + headRx * 1.14f, headCy - headRy * 0.88f,
                cx + headRx * 1.04f, headCy - headRy * 0.12f);
        // Textured fringe wave
        hairPath.cubicTo(cx + headRx * 0.65f, headCy - headRy * 0.58f,
                cx + headRx * 0.15f, headCy - headRy * 0.42f,
                cx - headRx * 0.25f, headCy - headRy * 0.56f);
        hairPath.cubicTo(cx - headRx * 0.65f, headCy - headRy * 0.68f,
                cx - headRx * 0.90f, headCy - headRy * 0.42f,
                cx - headRx * 1.04f, headCy - headRy * 0.12f);
        hairPath.close();

        p.setShader(new LinearGradient(cx - headRx, headCy - headRy * 1.3f, cx + headRx, headCy,
                blendColor(hairColor, rimLeft, 0.20f), hairColor, Shader.TileMode.CLAMP));
        canvas.drawPath(hairPath, p);

        // 10. If Live Streamer Studio: Draw Professional Condenser Microphone & Studio Headphones
        if (isStreamerStudio) {
            Paint gear = new Paint(Paint.ANTI_ALIAS_FLAG);
            // Studio Mic on Boom Arm in foreground
            float micX = isRedHost ? cx + w * 0.20f : cx - w * 0.20f;
            float micY = h * 0.72f;
            gear.setColor(0xFF1E2230);
            gear.setStrokeWidth(w * 0.025f);
            canvas.drawLine(isRedHost ? w : 0, h * 0.88f, micX, micY + h * 0.06f, gear);

            // Mic capsule
            RectF micCapsule = new RectF(micX - w * 0.055f, micY - h * 0.06f, micX + w * 0.055f, micY + h * 0.06f);
            gear.setShader(new LinearGradient(micCapsule.left, micCapsule.top, micCapsule.right, micCapsule.bottom,
                    0xFF454B66, 0xFF161824, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(micCapsule, w * 0.05f, w * 0.05f, gear);

            // Neon LED ring on mic
            gear.setShader(null);
            gear.setColor(rimLeft);
            canvas.drawRect(micX - w * 0.055f, micY, micX + w * 0.055f, micY + h * 0.008f, gear);
        }

        // 11. Subtle Bottom Vignette for Text Legibility
        p.setShader(new LinearGradient(0, h * 0.58f, 0, h,
                Color.TRANSPARENT, 0xE6070710, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.58f, w, h, p);

        return bmp;
    }

    private static Bitmap renderRoomWallpaperBitmap(int w, int h, boolean isVelvetRose) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        int topColor = isVelvetRose ? 0xFF1D061A : 0xFF080B24;
        int midColor = isVelvetRose ? 0xFF120517 : 0xFF0A0A18;
        int botColor = 0xFF070710;

        p.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{topColor, midColor, botColor},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);

        // Glowing Nebula / Velvet Waves
        int glow1 = isVelvetRose ? 0xFFFF2A7A : 0xFF00E5FF;
        int glow2 = isVelvetRose ? 0xFFF1C40F : 0xFFFF2A7A;
        int glow3 = isVelvetRose ? 0xFF9C27B0 : 0xFF7C4DFF;

        p.setShader(new RadialGradient(w * 0.5f, h * 0.28f, w * 0.72f,
                withAlpha(glow1, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.5f, h * 0.28f, w * 0.72f, p);

        p.setShader(new RadialGradient(w * 0.15f, h * 0.52f, w * 0.55f,
                withAlpha(glow2, 55), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.15f, h * 0.52f, w * 0.55f, p);

        p.setShader(new RadialGradient(w * 0.85f, h * 0.62f, w * 0.58f,
                withAlpha(glow3, 60), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.85f, h * 0.62f, w * 0.58f, p);

        // Concentric Cyber Stage Rings in Center
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2f);
        ring.setColor(withAlpha(glow1, 40));
        canvas.drawCircle(w * 0.5f, h * 0.29f, w * 0.26f, ring);
        ring.setColor(withAlpha(glow2, 30));
        canvas.drawCircle(w * 0.5f, h * 0.29f, w * 0.38f, ring);

        // Stardust Particles
        p.setShader(null);
        float[][] stars = {
                {0.12f, 0.11f, 3.5f}, {0.28f, 0.08f, 2.5f}, {0.74f, 0.10f, 4.0f},
                {0.88f, 0.16f, 2.8f}, {0.18f, 0.34f, 3.0f}, {0.82f, 0.36f, 3.5f},
                {0.09f, 0.68f, 2.5f}, {0.91f, 0.72f, 3.2f}, {0.42f, 0.15f, 2.2f},
                {0.62f, 0.21f, 2.8f}, {0.33f, 0.48f, 2.4f}, {0.67f, 0.50f, 3.0f}
        };
        for (int i = 0; i < stars.length; i++) {
            p.setColor(i % 2 == 0 ? withAlpha(0xFFF1C40F, 150) : withAlpha(0xFFFFFFFF, 130));
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

        // Luxury Gold Wave Arcs
        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(3f);
        arcPaint.setColor(withAlpha(0xFFF1C40F, 70));
        canvas.drawArc(new RectF(-w * 0.2f, h * 0.15f, w * 1.2f, h * 1.4f), 190, 160, false, arcPaint);
        arcPaint.setColor(withAlpha(0xFF00E5FF, 55));
        canvas.drawArc(new RectF(-w * 0.1f, h * 0.28f, w * 1.1f, h * 1.5f), 190, 160, false, arcPaint);

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

        // 1. Outer Golden Dragon Aura Glow
        p.setShader(new RadialGradient(cx, cy, w * 0.49f,
                new int[]{Color.TRANSPARENT, withAlpha(0xFFF1C40F, 130), withAlpha(0xFFFF2A7A, 90), Color.TRANSPARENT},
                new float[]{0.68f, 0.82f, 0.93f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, w * 0.49f, p);

        // 2. Ornate Dragon Scale Ring
        p.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xFFFFF3B0, 0xFFF1C40F, 0xFFD4AF37, 0xFFFF8F00, 0xFFFFE082},
                null, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.065f);
        canvas.drawCircle(cx, cy, (rOuter + rInner) * 0.5f, p);

        // 3. Inner Crimson-Gold Rim
        Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(w * 0.016f);
        innerRim.setColor(0xFFFF2A7A);
        canvas.drawCircle(cx, cy, rInner, innerRim);

        // 4. 8 Golden Dragon Spikes / Pearls around the circumference
        Paint pearl = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            float px = cx + (float) Math.cos(angle) * rOuter;
            float py = cy + (float) Math.sin(angle) * rOuter;
            pearl.setColor(i % 2 == 0 ? 0xFFFFF59D : 0xFFFF2A7A);
            canvas.drawCircle(px, py, w * 0.025f, pearl);
        }

        // 5. Top Imperial Dragon Crown Crest
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

        // Ruby gem in crown center
        pearl.setColor(0xFFFF1744);
        canvas.drawCircle(cx, cy - rOuter - w * 0.01f, w * 0.024f, pearl);

        // 6. Bottom Imperial Gold Dragon Ribbon
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

    private static int darken(int color, float factor) {
        return Color.rgb(
                Math.min(255, Math.max(0, Math.round(Color.red(color) * factor))),
                Math.min(255, Math.max(0, Math.round(Color.green(color) * factor))),
                Math.min(255, Math.max(0, Math.round(Color.blue(color) * factor)))
        );
    }

    private static int blendColor(int c1, int c2, float ratio) {
        float inv = 1f - ratio;
        return Color.rgb(
                Math.round(Color.red(c1) * inv + Color.red(c2) * ratio),
                Math.round(Color.green(c1) * inv + Color.green(c2) * ratio),
                Math.round(Color.blue(c1) * inv + Color.blue(c2) * ratio)
        );
    }
}
