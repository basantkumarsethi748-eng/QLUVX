package com.example.qluvx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "QLUVX_USER_SESSION";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_ROLE = "userRole"; // Top, Bottom, Versatile
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_USER_COINS = "userCoins";
    private static final String KEY_USER_DIAMONDS = "userDiamonds";
    private static final String KEY_IS_VIP = "isVipMember";
    private static final String KEY_AVATAR_RES_ID = "avatarResId";
    private static final String KEY_USER_BIO = "userBio";
    private static final String KEY_HEIGHT_CM = "userHeightCm";
    private static final String KEY_WEIGHT_KG = "userWeightKg";
    private static final String KEY_HIDE_DISTANCE = "hideDistance";
    private static final String KEY_GHOST_MODE = "incognitoGhostMode";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createLoginSession(String userId, String userName, String role) {
        createLoginSession(userId, userName, role, "qluvx_jwt_" + System.currentTimeMillis());
    }

    public void createLoginSession(String userId, String userName, String role, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_AUTH_TOKEN, token != null ? token : "");
        if (!prefs.contains(KEY_USER_COINS)) {
            editor.putInt(KEY_USER_COINS, 25000);
        }
        if (!prefs.contains(KEY_USER_DIAMONDS)) {
            editor.putInt(KEY_USER_DIAMONDS, 8500);
        }
        editor.putBoolean(KEY_IS_VIP, true);
        editor.apply();

        ApiClient.setAuthToken(token);
    }

    public void updateProfileSettings(String userName,
                                      String bio,
                                      String role,
                                      int heightCm,
                                      int weightKg,
                                      boolean hideDistance,
                                      boolean ghostMode) {
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_BIO, bio);
        editor.putString(KEY_USER_ROLE, role);
        editor.putInt(KEY_HEIGHT_CM, heightCm);
        editor.putInt(KEY_WEIGHT_KG, weightKg);
        editor.putBoolean(KEY_HIDE_DISTANCE, hideDistance);
        editor.putBoolean(KEY_GHOST_MODE, ghostMode);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Julian Vance");
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "QL-884920");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "Versatile");
    }

    public String getUserBio() {
        return prefs.getString(KEY_USER_BIO, "Gym lover, music & late night voice party host 👑");
    }

    public int getUserHeightCm() {
        return prefs.getInt(KEY_HEIGHT_CM, 178);
    }

    public int getUserWeightKg() {
        return prefs.getInt(KEY_WEIGHT_KG, 74);
    }

    public boolean isHideDistance() {
        return prefs.getBoolean(KEY_HIDE_DISTANCE, false);
    }

    public boolean isGhostMode() {
        return prefs.getBoolean(KEY_GHOST_MODE, false);
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "qluvx_jwt_royal_token");
    }

    public int getUserCoins() {
        return prefs.getInt(KEY_USER_COINS, 25000);
    }

    public void setUserCoins(int coins) {
        editor.putInt(KEY_USER_COINS, Math.max(0, coins));
        editor.apply();
    }

    public int getUserDiamonds() {
        return prefs.getInt(KEY_USER_DIAMONDS, 8500);
    }

    public void setUserDiamonds(int diamonds) {
        editor.putInt(KEY_USER_DIAMONDS, Math.max(0, diamonds));
        editor.apply();
    }

    public boolean isVipMember() {
        return prefs.getBoolean(KEY_IS_VIP, true);
    }

    public void setVipMember(boolean isVip) {
        editor.putBoolean(KEY_IS_VIP, isVip);
        editor.apply();
    }

    public int getAvatarResId() {
        return prefs.getInt(KEY_AVATAR_RES_ID, R.drawable.img_avatar_0);
    }

    public void setAvatarResId(int resId) {
        editor.putInt(KEY_AVATAR_RES_ID, resId);
        editor.apply();
    }

    public void logoutUser() {
        SocketManager.getInstance().disconnect();
        ApiClient.setAuthToken("");
        editor.clear();
        editor.apply();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
