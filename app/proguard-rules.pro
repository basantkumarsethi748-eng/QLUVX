# Agora RTC SDK
-keep class io.agora.** { *; }
-dontwarn io.agora.**
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Retrofit & OkHttp
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson DTO Models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.qluvx.** { *; }

# Socket.io
-keep class io.socket.** { *; }
-keep class org.json.** { *; }
-dontwarn io.socket.**
