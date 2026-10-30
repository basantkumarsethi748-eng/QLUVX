package com.example.qluvx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoicePartyFragment extends Fragment {

    private static final String VOICE_ROOM_ID = "qluvx_voice_99012";
    private boolean isMicMuted = false;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_party, container, false);

        sessionManager = new SessionManager(requireContext());

        // Join Socket.io Voice Party Room & Request Agora RTC Stream Token
        SocketManager.getInstance().joinRoom(VOICE_ROOM_ID, sessionManager.getUserId(), sessionManager.getUserName());
        requestAgoraVoiceToken();

        Button btnMicToggle = view.findViewById(R.id.ivMicToggle);
        Button btnSoundboard = view.findViewById(R.id.ivSoundboard);
        Button btnGift = view.findViewById(R.id.ivGift);
        Button btnLuckySpin = view.findViewById(R.id.btnLuckySpin);

        btnMicToggle.setOnClickListener(v -> {
            isMicMuted = !isMicMuted;
            btnMicToggle.setText(isMicMuted ? "🔇 Mic Muted" : "🎙️ Mic ON");
            Toast.makeText(getContext(), isMicMuted ? "Microphone Muted" : "Microphone Live", Toast.LENGTH_SHORT).show();
        });

        btnSoundboard.setOnClickListener(v ->
                Toast.makeText(getContext(), "🔊 Playing DJ Airhorn & Applause SFX!", Toast.LENGTH_SHORT).show()
        );

        btnGift.setOnClickListener(v -> {
            Toast.makeText(getContext(), "🚀 Sent Golden Interstellar Rocket to Host!", Toast.LENGTH_SHORT).show();
            ApiClient.getApiService()
                    .sendGift(new ApiService.GiftSendRequest(
                            sessionManager.getUserId(),
                            "host_julian",
                            VOICE_ROOM_ID,
                            "interstellar_rocket",
                            1000
                    ))
                    .enqueue(new Callback<ApiService.GiftSendResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiService.GiftSendResponse> call, @NonNull Response<ApiService.GiftSendResponse> response) {
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiService.GiftSendResponse> call, @NonNull Throwable t) {
                        }
                    });
        });

        btnLuckySpin.setOnClickListener(v ->
                Toast.makeText(getContext(), "🎡 Lucky Wheel Spun! You won +250 Gold Coins!", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void requestAgoraVoiceToken() {
        ApiClient.getApiService()
                .getAgoraToken(new ApiService.AgoraTokenRequest(VOICE_ROOM_ID, 1001, "PUBLISHER"))
                .enqueue(new Callback<ApiService.AgoraTokenResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.AgoraTokenResponse> call, @NonNull Response<ApiService.AgoraTokenResponse> response) {
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.AgoraTokenResponse> call, @NonNull Throwable t) {
                    }
                });
    }

    @Override
    public void onDestroyView() {
        if (sessionManager != null) {
            SocketManager.getInstance().leaveRoom(VOICE_ROOM_ID, sessionManager.getUserId());
        }
        super.onDestroyView();
    }
}
