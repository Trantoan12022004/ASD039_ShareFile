# ---------- MẶC ĐỊNH CÓ -----------------

# support-design
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }
-ignorewarnings

# support v4
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }


# support-v7-appcompat
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }


# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule


# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

#ffmpegkit
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.smartexception.**


# google ads
-keep class com.google.android.gms.internal.** { *; }

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.yourpackage.model.** { *; }
-keep class com.google.gson.** { *; }

-ignorewarnings
-keep class * {
    public private *;
}

-dontwarn com.androidlab.bokehoverlay.**

# ---------- NẾU CALL API HOẶC DÙNG FIREBASE READTIME THÌ ADD CLASS VS MODEL SỬ DỤNG   -----------------


-keep class com.example.basekotlin.ui.splash.SplashActivity.** { *; }
-keep class com.example.basekotlin.ads.** { *; }


#Debug/release đều minifyEnabled true → bắt buộc keep DTO:
-keep class com.example.basekotlin.data.remote.jamendo.** { *; }
-keepclassmembers class com.example.basekotlin.data.remote.jamendo.** { *; }