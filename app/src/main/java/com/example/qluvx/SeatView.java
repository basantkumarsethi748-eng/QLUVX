package com.example.qluvx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Custom Android View class representing a single voice room seat.
 * Includes an ImageView for the user's avatar (using official Agora sample avatar portraits
 * and join/ban icons) and a circular progress drawable layer (@drawable/bg_seat_volume_progress)
 * for the Agora active speaker volume indicator (0..255).
 */
public class SeatView extends LinearLayout {

    private View activeSpeakerFrame;
    private ProgressBar volumeProgressBar;
    private ImageView avatarImageView;
    private TextView micBadgeView;
    private TextView seatNameText;
    private TextView seatStatusText;

    private VoiceRoomModels.SpeakerSeat boundSeat;
    private boolean isLocalUser = false;
    private float wavePhase = 0f;
    private float smoothedVolume = 0f;

    public SeatView(Context context) {
        super(context);
        init(context);
    }

    public SeatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_voice_seat, this, true);
        activeSpeakerFrame = findViewById(R.id.seat_active_speaker_frame);
        volumeProgressBar = findViewById(R.id.seat_volume_progress_bar);
        avatarImageView = findViewById(R.id.seat_avatar_image);
        micBadgeView = findViewById(R.id.seat_mic_badge);
        seatNameText = findViewById(R.id.seat_name_text);
        seatStatusText = findViewById(R.id.seat_status_text);

        if (avatarImageView != null) {
            avatarImageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int size = Math.min(view.getWidth(), view.getHeight());
                    outline.setOval(0, 0, size, size);
                }
            });
            avatarImageView.setClipToOutline(true);
        }
    }

    public ImageView getAvatarImageView() {
        return avatarImageView;
    }

    public ProgressBar getVolumeProgressBar() {
        return volumeProgressBar;
    }

    public View getActiveSpeakerFrame() {
        return activeSpeakerFrame;
    }

    /**
     * Directly updates the circular progress drawable layer for the Agora active speaker
     * volume indicator (0..255 from IRtcEngineEventHandler.onAudioVolumeIndication).
     */
    public void setVolumeIndicatorProgress(int volume255) {
        int clamped = Math.max(0, Math.min(255, volume255));
        if (volumeProgressBar != null) {
            volumeProgressBar.setProgress(clamped);
            Drawable progressDrawable = volumeProgressBar.getProgressDrawable();
            if (progressDrawable != null) {
                progressDrawable.setLevel((clamped * 10000) / 255);
            }
        }
    }

    public void bindSeat(VoiceRoomModels.SpeakerSeat seat, boolean isLocal) {
        this.boundSeat = seat;
        this.isLocalUser = isLocal;
        if (seat == null) return;

        int seatNumber = seat.seatIndex + 1;

        if (seat.isOccupied) {
            int avatarRes = seat.avatarResId != 0
                    ? seat.avatarResId
                    : VoiceRoomModels.getAvatarResId(seat.uid + seat.seatIndex);
            avatarImageView.clearColorFilter();
            avatarImageView.setPadding(0, 0, 0, 0);
            avatarImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatarImageView.setImageResource(avatarRes);

            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(seat.avatarColor);
            circleBg.setStroke(
                    UiStyleHelper.dp(getContext(), 2f),
                    isLocal ? UiStyleHelper.COLOR_LIVE_GREEN : Color.argb(180, 255, 255, 255)
            );
            avatarImageView.setBackground(circleBg);

            micBadgeView.setVisibility(View.VISIBLE);
            if (seat.isMuted) {
                micBadgeView.setBackgroundResource(R.drawable.bg_mic_badge_muted);
                micBadgeView.setText("🔇");
            } else {
                micBadgeView.setBackgroundResource(R.drawable.bg_mic_badge_on);
                micBadgeView.setText("🎙");
            }

            seatNameText.setText(isLocal ? ("You (#" + seatNumber + ")") : seat.userName);
            seatNameText.setTextColor(isLocal ? UiStyleHelper.COLOR_LIVE_GREEN : UiStyleHelper.COLOR_TEXT_PRIMARY);

            int activeVol = seat.isMuted ? 0 : seat.speakingVolume;
            setVolumeIndicatorProgress(activeVol);

            if (seat.isMuted) {
                seatStatusText.setText("Muted");
                seatStatusText.setTextColor(UiStyleHelper.COLOR_DANGER_RED);
                activeSpeakerFrame.setAlpha(0f);
            } else if (activeVol > 8) {
                seatStatusText.setText("Speaking • " + activeVol);
                seatStatusText.setTextColor(UiStyleHelper.COLOR_AGORA_CYAN);
            } else {
                seatStatusText.setText(seat.seatIndex == 0 ? "Anchor • Mic On" : "Mic On");
                seatStatusText.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            }
        } else {
            int pad = UiStyleHelper.dp(getContext(), 10f);
            avatarImageView.clearColorFilter();
            avatarImageView.setPadding(pad, pad, pad, pad);
            avatarImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            avatarImageView.setImageResource(seat.isLocked ? R.drawable.ic_ban : R.drawable.ic_join);
            avatarImageView.setBackgroundResource(R.drawable.bg_seat_idle_frame);

            micBadgeView.setVisibility(View.GONE);
            setVolumeIndicatorProgress(0);
            activeSpeakerFrame.setAlpha(0f);
            activeSpeakerFrame.setScaleX(1f);
            activeSpeakerFrame.setScaleY(1f);

            seatNameText.setText("Seat " + seatNumber);
            seatNameText.setTextColor(UiStyleHelper.COLOR_TEXT_SECONDARY);
            seatStatusText.setText(seat.isLocked ? "Closed" : "Open");
            seatStatusText.setTextColor(seat.isLocked
                    ? UiStyleHelper.COLOR_DANGER_RED
                    : Color.argb(150, 148, 163, 184));
        }
    }

    /**
     * Smoothly interpolates and animates both the circular progress drawable layer and
     * the outer active speaker halo frame based on Agora's onAudioVolumeIndication (0..255).
     */
    public void stepActiveSpeakerAnimation() {
        if (boundSeat == null) return;

        float targetVol = (boundSeat.isOccupied && !boundSeat.isMuted) ? boundSeat.speakingVolume : 0f;
        smoothedVolume = smoothedVolume * 0.65f + targetVol * 0.35f;

        if (smoothedVolume > 5f) {
            setVolumeIndicatorProgress(Math.round(smoothedVolume));
            wavePhase += 0.24f;
            if (wavePhase > (float) (Math.PI * 2.0)) {
                wavePhase -= (float) (Math.PI * 2.0);
            }
            float normalizedVol = Math.min(1f, smoothedVolume / 180f);
            float pulse = (float) ((Math.sin(wavePhase) + 1.0) * 0.5);
            float scale = 1.0f + (0.06f + 0.14f * normalizedVol) * pulse;
            float alpha = 0.45f + 0.55f * normalizedVol * (1.0f - 0.35f * pulse);

            activeSpeakerFrame.setScaleX(scale);
            activeSpeakerFrame.setScaleY(scale);
            activeSpeakerFrame.setAlpha(Math.max(0f, Math.min(1f, alpha)));
        } else {
            smoothedVolume = 0f;
            setVolumeIndicatorProgress(0);
            activeSpeakerFrame.setAlpha(0f);
            activeSpeakerFrame.setScaleX(1f);
            activeSpeakerFrame.setScaleY(1f);
        }
    }
}
