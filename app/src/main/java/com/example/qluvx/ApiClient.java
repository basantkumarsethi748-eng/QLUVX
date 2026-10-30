package com.example.qluvx;

import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit 2 + OkHttp 3 Client for QLUVX Backend API.
 * Attaches Bearer JWT authentication headers and configures timeouts and logging.
 */
public class ApiClient {

    private static final String DEFAULT_BASE_URL = "https://api.qluvx.app/";
    private static volatile Retrofit retrofit = null;
    private static volatile String authToken = "";
    private static volatile String customBaseUrl = "";

    private ApiClient() {
    }

    public static void setAuthToken(String token) {
        authToken = token != null ? token.trim() : "";
    }

    public static synchronized void setCustomBaseUrl(String url) {
        String normalized = "";
        if (url != null) {
            String trimmed = url.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                normalized = trimmed.endsWith("/") ? trimmed : (trimmed + "/");
            }
        }
        if (!normalized.equals(customBaseUrl)) {
            customBaseUrl = normalized;
            retrofit = null;
        }
    }

    public static String getResolvedBaseUrl() {
        if (customBaseUrl != null && !customBaseUrl.isEmpty()) {
            return customBaseUrl;
        }
        String configured = BuildConfig.AGORA_TOKEN_SERVER_URL;
        if (configured != null) {
            String trimmed = configured.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed.endsWith("/") ? trimmed : (trimmed + "/");
            }
        }
        return DEFAULT_BASE_URL;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    Interceptor authInterceptor = chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("X-Client-App", "QLUVX-Android-1.0")
                                .header("ngrok-skip-browser-warning", "true");
                        if (authToken != null && !authToken.isEmpty()) {
                            builder.header("Authorization", "Bearer " + authToken);
                        }
                        return chain.proceed(builder.build());
                    };

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
                            .addInterceptor(loggingInterceptor)
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(getResolvedBaseUrl())
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
