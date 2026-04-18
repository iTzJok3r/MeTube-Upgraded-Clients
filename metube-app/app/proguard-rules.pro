# MeTube Client ProGuard rules

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Gson models
-keep class com.itzjok3r.metubeapp.model.** { *; }

# Socket.IO
-keep class io.socket.** { *; }
-keep class com.github.nkzawa.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
