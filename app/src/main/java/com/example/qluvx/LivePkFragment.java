package com.example.qluvx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LivePkFragment extends Fragment implements SocketManager.SocketEventListener {

    private static final String PK_BATTLE_ID = "pk_battle_kabir_vs_zayn";
    private int redScore = 14500;
    private int blueScore = 11200;
    private TextView tvScoreRed;
    private TextView tvScoreBlue;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_pk, container, false);

        sessionManager = new SessionManager(requireContext());
        tvScoreRed = view.findViewById(R.id.tvScoreRed);
        tvScoreBlue = view.findViewById(R.id.tvScoreBlue);
        Button btnGiftRed = view.findViewById(R.id.btnGiftRed);
        Button btnGiftBlue = view.findViewById(R.id.btnGiftBlue);

        SocketManager.getInstance().addListener(this);
        SocketManager.getInstance().joinRoom(PK_BATTLE_ID, sessionManager.getUserId(), sessionManager.getUserName());

        // Request Agora Live PK Video Token from backend
        ApiClient.getApiService()
                .getAgoraToken(new ApiService.AgoraTokenRequest(PK_BATTLE_ID, 2001, "SUBSCRIBER"))
                .enqueue(new Callback<ApiService.AgoraTokenResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.AgoraTokenResponse> call, @NonNull Response<ApiService.AgoraTokenResponse> response) {
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.AgoraTokenResponse> call, @NonNull Throwable t) {
                    }
                });

        btnGiftRed.setOnClickListener(v -> {
            redScore += 500;
            tvScoreRed.setText("🔥 RED: " + redScore);
            Toast.makeText(getContext(), "🌹 Sent +500 PK Points to Host Red!", Toast.LENGTH_SHORT).show();

            SocketManager.getInstance().sendPkGiftVote(PK_BATTLE_ID, "RED", "Rose Bouquet", 500);
            dispatchGiftApiCall("host_kabir_red", "rose_500", 500);
        });

        btnGiftBlue.setOnClickListener(v -> {
            blueScore += 500;
            tvScoreBlue.setText(blueScore + " :BLUE ❄️");
            Toast.makeText(getContext(), "🏎️ Sent +500 PK Points to Host Blue!", Toast.LENGTH_SHORT).show();

            SocketManager.getInstance().sendPkGiftVote(PK_BATTLE_ID, "BLUE", "Luxury Sports Car", 500);
            dispatchGiftApiCall("host_zayn_blue", "car_500", 500);
        });

        return view;
    }

    private void dispatchGiftApiCall(String receiverId, String giftId, int coins) {
        ApiClient.getApiService()
                .sendGift(new ApiService.GiftSendRequest(
                        sessionManager.getUserId(),
                        receiverId,
                        PK_BATTLE_ID,
                        giftId,
                        coins
                ))
                .enqueue(new Callback<ApiService.GiftSendResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.GiftSendResponse> call, @NonNull Response<ApiService.GiftSendResponse> response) {
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.GiftSendResponse> call, @NonNull Throwable t) {
                    }
                });
    }

    @Override
    public void onPkScoreUpdated(String battleId, int updatedRed, int updatedBlue, String lastGiftName) {
        if (!isAdded()) return;
        this.redScore = updatedRed;
        this.blueScore = updatedBlue;
        if (tvScoreRed != null) {
            tvScoreRed.setText("🔥 RED: " + redScore);
        }
        if (tvScoreBlue != null) {
            tvScoreBlue.setText(blueScore + " :BLUE ❄️");
        }
    }

    @Override
    public void onDestroyView() {
        SocketManager.getInstance().removeListener(this);
        if (sessionManager != null) {
            SocketManager.getInstance().leaveRoom(PK_BATTLE_ID, sessionManager.getUserId());
        }
        super.onDestroyView();
    }
}
