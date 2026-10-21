package com.example.qluvx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class VoiceWaveViews {

    public static class ConnectionStatusDotView extends View {
        private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private AgoraVoiceManager.ConnectionStatus status = AgoraVoiceManager.ConnectionStatus.DISCONNECTED;
        private float pulsePhase = 0f;

        public ConnectionStatusDotView(Context context) {
            super(context);
            corePaint.setStyle(Paint.Style.FILL);
            haloPaint.setStyle(Paint.Style.FILL);
            setContentDescription("Agora connection status indicator");
        }

        public void setConnectionStatus(AgoraVoiceManager.ConnectionStatus status) {
            this.status = status != null ? status : AgoraVoiceManager.ConnectionStatus.DISCONNECTED;
            invalidate();
        }

        public AgoraVoiceManager.ConnectionStatus getConnectionStatus() {
            return status;
        }

        public void stepPulse() {
            pulsePhase += 0.18f;
            if (pulsePhase > (float) (Math.PI * 2)) {
                pulsePhase -= (float) (Math.PI * 2);
            }
            invalidate();
        }

        public static int getStatusColor(AgoraVoiceManager.ConnectionStatus status) {
            if (status == null) return Color.rgb(255, 64, 96);
            switch (status) {
                case CONNECTED:
                    return Color.rgb(0, 230, 118); // Green dot: CONNECTED
                case CONNECTING:
                    return Color.rgb(255, 215, 64); // Amber dot: CONNECTING or RECONNECTING
                case DISCONNECTED:
                default:
                    return Color.rgb(255, 64, 96); // Red dot: DISCONNECTED or FAILED
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float maxR = Math.min(w, h) / 2f;
            float coreR = maxR * 0.52f;

            int color = getStatusColor(status);
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            // Animated outer pulse halo
            float pulse = (float) ((Math.sin(pulsePhase) + 1.0) * 0.5);
            float haloR = coreR + (maxR - coreR) * (0.35f + 0.65f * pulse);
            int haloAlpha = status == AgoraVoiceManager.ConnectionStatus.DISCONNECTED
                    ? (int) (55 + 45 * pulse)
                    : (int) (130 * (1.0f - 0.55f * pulse));
            haloPaint.setColor(Color.argb(Math.max(25, haloAlpha), r, g, b));
            canvas.drawCircle(cx, cy, haloR, haloPaint);

            // Solid crisp status dot core
            corePaint.setColor(color);
            canvas.drawCircle(cx, cy, coreR, corePaint);
        }
    }

    public static class SeatAvatarView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dashedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private VoiceRoomModels.SpeakerSeat seat;
        private boolean isLocalUser = false;
        private float wavePhase = 0f;

        public SeatAvatarView(Context context) {
            super(context);
            float density = context.getResources().getDisplayMetrics().density;

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(2.5f * density);

            circlePaint.setStyle(Paint.Style.FILL);

            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2.2f * density);

            dashedPaint.setStyle(Paint.Style.STROKE);
            dashedPaint.setStrokeWidth(1.8f * density);
            dashedPaint.setColor(Color.argb(110, 255, 255, 255));
            dashedPaint.setPathEffect(new DashPathEffect(new float[]{6f * density, 5f * density}, 0));

            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            badgePaint.setStyle(Paint.Style.FILL);
        }

        public void bindSeat(VoiceRoomModels.SpeakerSeat seat, boolean isLocalUser) {
            this.seat = seat;
            this.isLocalUser = isLocalUser;
            invalidate();
        }

        public void stepAnimation() {
            wavePhase += 0.16f;
            if (wavePhase > (float) (Math.PI * 2)) {
                wavePhase -= (float) (Math.PI * 2);
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (seat == null) return;

            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float baseRadius = Math.min(w, h) * 0.34f;
            float density = getResources().getDisplayMetrics().density;

            if (seat.isOccupied) {
                int color = seat.avatarColor;
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);

                // Animated neon voice waves when speaking and unmuted
                if (!seat.isMuted && seat.speakingVolume > 8) {
                    float intensity = Math.min(1f, seat.speakingVolume / 200f);
                    for (int i = 0; i < 2; i++) {
                        float pulse = (float) ((Math.sin(wavePhase + i * Math.PI) + 1.0) * 0.5);
                        float waveR = baseRadius + (pulse * 12f * density * (0.5f + intensity));
                        int alpha = (int) ((1.0f - pulse) * 190 * (0.4f + 0.6f * intensity));
                        ringPaint.setColor(Color.argb(Math.max(20, alpha), r, g, b));
                        canvas.drawCircle(cx, cy, waveR, ringPaint);
                    }
                }

                // Avatar circle fill
                circlePaint.setShader(new LinearGradient(
                        cx - baseRadius, cy - baseRadius,
                        cx + baseRadius, cy + baseRadius,
                        Color.argb(235, r, g, b),
                        Color.argb(255, Math.max(12, r / 3), Math.max(12, g / 3), Math.max(24, b / 3)),
                        Shader.TileMode.CLAMP
                ));
                canvas.drawCircle(cx, cy, baseRadius, circlePaint);
                circlePaint.setShader(null);

                // Outer ring border (highlighted if local user or host)
                if (isLocalUser) {
                    borderPaint.setColor(Color.rgb(0, 230, 118));
                } else if (seat.seatIndex == 0) {
                    borderPaint.setColor(Color.rgb(255, 215, 64));
                } else {
                    borderPaint.setColor(color);
                }
                canvas.drawCircle(cx, cy, baseRadius, borderPaint);

                // Initials
                String initials = getInitials(seat.userName);
                textPaint.setTextSize(baseRadius * 0.68f);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = cy - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(initials, cx, textY, textPaint);

                // Mic status badge at bottom-right of circle
                float badgeCx = cx + baseRadius * 0.72f;
                float badgeCy = cy + baseRadius * 0.72f;
                float badgeR = Math.max(8f * density, baseRadius * 0.30f);

                badgePaint.setColor(Color.rgb(12, 12, 22));
                canvas.drawCircle(badgeCx, badgeCy, badgeR + 1.5f * density, badgePaint);

                if (seat.isMuted) {
                    badgePaint.setColor(Color.rgb(255, 64, 96));
                } else if (seat.speakingVolume > 12) {
                    badgePaint.setColor(Color.rgb(0, 230, 118));
                } else {
                    badgePaint.setColor(Color.rgb(0, 200, 255));
                }
                canvas.drawCircle(badgeCx, badgeCy, badgeR, badgePaint);

                textPaint.setTextSize(badgeR * 1.15f);
                Paint.FontMetrics bfm = textPaint.getFontMetrics();
                canvas.drawText(seat.isMuted ? "×" : "•", badgeCx, badgeCy - (bfm.ascent + bfm.descent) / 2f, textPaint);
            } else {
                // Empty or locked seat
                circlePaint.setColor(Color.argb(150, 22, 22, 38));
                canvas.drawCircle(cx, cy, baseRadius, circlePaint);
                canvas.drawCircle(cx, cy, baseRadius, dashedPaint);

                textPaint.setColor(seat.isLocked ? Color.argb(160, 255, 110, 140) : Color.argb(200, 200, 200, 235));
                textPaint.setTextSize(baseRadius * 0.75f);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = cy - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(seat.isLocked ? "🔒" : "+", cx, textY, textPaint);
                textPaint.setColor(Color.WHITE);
            }
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "U";
            String clean = name.replace("(You)", "").trim();
            String[] parts = clean.split("\\s+");
            if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            }
            return clean.substring(0, Math.min(2, clean.length())).toUpperCase();
        }
    }

    public static class LiveSpectrumView extends View {
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF barRect = new RectF();
        private final float[] barLevels = new float[18];
        private int currentVolume = 0;
        private float tick = 0f;

        public LiveSpectrumView(Context context) {
            super(context);
            barPaint.setStyle(Paint.Style.FILL);
        }

        public void updateVolume(int maxRealVolume255) {
            this.currentVolume = Math.max(0, maxRealVolume255);
            tick += 0.35f;
            for (int i = 0; i < barLevels.length; i++) {
                if (currentVolume <= 0) {
                    barLevels[i] = 0.12f;
                } else {
                    float wave = (float) Math.abs(Math.sin(tick + i * 0.55f));
                    float scale = Math.min(1f, currentVolume / 180f);
                    barLevels[i] = 0.12f + wave * 0.88f * scale;
                }
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            int count = barLevels.length;
            float spacing = w / (count * 1.6f);
            float barW = spacing * 0.9f;
            float totalW = count * barW + (count - 1) * (spacing - barW);
            float startX = (w - totalW) / 2f;

            for (int i = 0; i < count; i++) {
                float level = Math.max(0.15f, barLevels[i]);
                float barH = h * level;
                float left = startX + i * spacing;
                float top = (h - barH) / 2f;
                barRect.set(left, top, left + barW, top + barH);

                if (i % 3 == 0) {
                    barPaint.setColor(Color.rgb(255, 55, 180));
                } else if (i % 3 == 1) {
                    barPaint.setColor(Color.rgb(0, 229, 255));
                } else {
                    barPaint.setColor(Color.rgb(124, 77, 255));
                }
                canvas.drawRoundRect(barRect, barW / 2f, barW / 2f, barPaint);
            }
        }
    }

    public static class FloatingReactionsView extends View {
        private static class Particle {
            String emoji;
            float x;
            float y;
            float vy;
            float swayPhase;
            float alpha;
        }

        private final List<Particle> particles = new ArrayList<>();
        private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();

        public FloatingReactionsView(Context context) {
            super(context);
            float density = context.getResources().getDisplayMetrics().density;
            emojiPaint.setTextSize(26f * density);
            emojiPaint.setTextAlign(Paint.Align.CENTER);
        }

        public void spawnReaction(String emoji) {
            float w = getWidth() > 0 ? getWidth() : 800f;
            float h = getHeight() > 0 ? getHeight() : 1200f;
            for (int i = 0; i < 3; i++) {
                Particle p = new Particle();
                p.emoji = emoji;
                p.x = w * 0.72f + (random.nextFloat() - 0.5f) * (w * 0.28f);
                p.y = h * 0.82f + random.nextFloat() * 40f;
                p.vy = 4.5f + random.nextFloat() * 3.5f;
                p.swayPhase = random.nextFloat() * 6.28f;
                p.alpha = 1.0f;
                particles.add(p);
            }
            invalidate();
        }

        public void stepParticles() {
            if (particles.isEmpty()) return;
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.y -= p.vy;
                p.swayPhase += 0.12f;
                p.x += (float) Math.sin(p.swayPhase) * 1.8f;
                p.alpha -= 0.014f;
                if (p.alpha <= 0.02f || p.y < 40f) {
                    it.remove();
                }
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (Particle p : particles) {
                emojiPaint.setAlpha((int) (Math.max(0f, Math.min(1f, p.alpha)) * 255));
                canvas.drawText(p.emoji, p.x, p.y, emojiPaint);
            }
        }
    }
}
