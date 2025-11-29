# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# Keep Kotlin metadata
-keepattributes *Annotation*, Signature, Exception

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Markwon classes
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Keep Prism4j classes
-keep class io.noties.prism4j.** { *; }
-dontwarn io.noties.prism4j.**

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data classes (models)
-keep class notebad.prabe.sh.core.model.** { *; }

# Keep ViewModel classes
-keep class notebad.prabe.sh.ui.viewmodel.** { *; }

# Keep sealed classes for state handling
-keep class notebad.prabe.sh.ui.state.** { *; }

# General Android rules
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}