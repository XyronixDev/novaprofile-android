# NovaProfile — ProGuard rules (release, R8).
# Keep Room entities/DAOs and the WebView JS bridge surface intact.
-keep class com.novaprofiles.app.data.** { *; }
-keep class androidx.room.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
