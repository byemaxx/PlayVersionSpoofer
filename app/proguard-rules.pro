# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all Xposed related classes
-keep class com.mymod.playspoofer.xposed.** { *; }

# Keep MainActivity and other Activities
-keep class com.mymod.playspoofer.ui.activity.MainActivity { *; }

# Keep all classes with @Keep annotation
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keep @androidx.annotation.Keep interface * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Compose specific rules
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**