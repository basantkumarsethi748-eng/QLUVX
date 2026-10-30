package com.example.qluvx;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Production Retrofit 2 REST API interface for QLUVX (Walla / Blued Architecture):
 * 1. Authentication & OTP Login
 * 2. PostGIS / Redis GEOSEARCH Nearby Users Discovery via GPS
 * 3. Agora RTC Token Generation for Live Video PK & 8-Seat Voice Party Rooms
 * 4. Virtual Gift Economy & Wallet Debit/Credit
 */
public interface ApiService {

    @POST("api/v1/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("api/v1/auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body OtpVerifyRequest request);

    @GET("api/v1/users/nearby")
    Call<NearbyUsersResponse> getNearbyUsers(
            @Query("lat") double latitude,
            @Query("lng") double longitude,
            @Query("radiusKm") double radiusKm,
            @Query("role") String roleFilter
    );

    @POST("api/v1/agora/token")
    Call<AgoraTokenResponse> getAgoraToken(@Body AgoraTokenRequest request);

    @POST("api/v1/wallet/gift")
    Call<GiftSendResponse> sendGift(@Body GiftSendRequest request);

    @POST("api/v1/wallet/verify-iap")
    Call<IapVerifyResponse> verifyIapPurchase(@Body IapVerifyRequest request);

    @POST("api/v1/payouts/request")
    Call<PayoutResponseDto> requestDiamondPayout(@Body PayoutRequestDto request);

    // =========================================================================
    // GSON DTO MODELS
    // =========================================================================

    class LoginRequest {
        @SerializedName("phone")
        public String phone;

        @SerializedName("provider")
        public String provider;

        public LoginRequest(String phone, String provider) {
            this.phone = phone;
            this.provider = provider;
        }
    }

    class OtpVerifyRequest {
        @SerializedName("phone")
        public String phone;

        @SerializedName("otp")
        public String otp;

        public OtpVerifyRequest(String phone, String otp) {
            this.phone = phone;
            this.otp = otp;
        }
    }

    class AuthResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("token")
        public String token;

        @SerializedName("userId")
        public String userId;

        @SerializedName("userName")
        public String userName;

        @SerializedName("role")
        public String role;

        @SerializedName("coins")
        public int coins;

        @SerializedName("diamonds")
        public int diamonds;
    }

    class NearbyUserDto {
        @SerializedName("userId")
        public String userId;

        @SerializedName("name")
        public String name;

        @SerializedName("age")
        public int age;

        @SerializedName("distanceKm")
        public double distanceKm;

        @SerializedName("role")
        public String role;

        @SerializedName("isOnline")
        public boolean isOnline;

        @SerializedName("avatarUrl")
        public String avatarUrl;
    }

    class NearbyUsersResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("users")
        public List<NearbyUserDto> users;
    }

    class AgoraTokenRequest {
        @SerializedName("channelName")
        public String channelName;

        @SerializedName("uid")
        public int uid;

        @SerializedName("role")
        public String role; // "PUBLISHER" or "SUBSCRIBER"

        public AgoraTokenRequest(String channelName, int uid, String role) {
            this.channelName = channelName;
            this.uid = uid;
            this.role = role;
        }
    }

    class AgoraTokenResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("appId")
        public String appId;

        @SerializedName("channelName")
        public String channelName;

        @SerializedName("token")
        public String token;

        @SerializedName("uid")
        public int uid;
    }

    class GiftSendRequest {
        @SerializedName("senderId")
        public String senderId;

        @SerializedName("receiverId")
        public String receiverId;

        @SerializedName("roomId")
        public String roomId;

        @SerializedName("giftId")
        public String giftId;

        @SerializedName("coinCost")
        public int coinCost;

        public GiftSendRequest(String senderId, String receiverId, String roomId, String giftId, int coinCost) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.roomId = roomId;
            this.giftId = giftId;
            this.coinCost = coinCost;
        }
    }

    class GiftSendResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("remainingCoins")
        public int remainingCoins;

        @SerializedName("receiverDiamonds")
        public int receiverDiamonds;

        @SerializedName("pkScoreRed")
        public int pkScoreRed;

        @SerializedName("pkScoreBlue")
        public int pkScoreBlue;
    }

    class IapVerifyRequest {
        @SerializedName("userId")
        public String userId;

        @SerializedName("productId")
        public String productId;

        @SerializedName("purchaseToken")
        public String purchaseToken;

        @SerializedName("orderId")
        public String orderId;

        @SerializedName("priceInr")
        public int priceInr;

        public IapVerifyRequest(String userId, String productId, String purchaseToken, String orderId, int priceInr) {
            this.userId = userId;
            this.productId = productId;
            this.purchaseToken = purchaseToken;
            this.orderId = orderId;
            this.priceInr = priceInr;
        }
    }

    class IapVerifyResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("creditedCoins")
        public int creditedCoins;

        @SerializedName("newCoinBalance")
        public int newCoinBalance;

        @SerializedName("isVipActive")
        public boolean isVipActive;

        @SerializedName("message")
        public String message;
    }

    class PayoutRequestDto {
        @SerializedName("userId")
        public String userId;

        @SerializedName("diamondsAmount")
        public int diamondsAmount;

        @SerializedName("payoutInr")
        public double payoutInr;

        @SerializedName("method")
        public String method; // "UPI", "IMPS", "PAYPAL"

        @SerializedName("accountDetails")
        public String accountDetails;

        public PayoutRequestDto(String userId, int diamondsAmount, double payoutInr, String method, String accountDetails) {
            this.userId = userId;
            this.diamondsAmount = diamondsAmount;
            this.payoutInr = payoutInr;
            this.method = method;
            this.accountDetails = accountDetails;
        }
    }

    class PayoutResponseDto {
        @SerializedName("success")
        public boolean success;

        @SerializedName("payoutId")
        public String payoutId;

        @SerializedName("remainingDiamonds")
        public int remainingDiamonds;

        @SerializedName("payoutInr")
        public double payoutInr;

        @SerializedName("status")
        public String status;
    }
}
