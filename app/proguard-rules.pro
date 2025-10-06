# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# === General Rules ===
# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === Hilt / Dagger ===
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.** <methods>;
}

# === Retrofit & OkHttp ===
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# === Gson ===
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep all data models (adjust package name to match your project)
-keep class com.voicenotesai.data.model.** { *; }
-keep class com.voicenotesai.data.remote.model.** { *; }
-keep class com.voicenotesai.data.local.entity.** { *; }

# === Room Database ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}

# === Jetpack Compose ===
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.tooling.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.** { *; }

# === DataStore ===
-keep class androidx.datastore.*.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# === Kotlin Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# === Kotlin Serialization (if used) ===
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.voicenotesai.**$$serializer { *; }
-keepclassmembers class com.voicenotesai.** {
    *** Companion;
}
-keepclasseswithmembers class com.voicenotesai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# === Android Security Crypto ===
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# === Keep ViewModels ===
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# === Navigation ===
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.**

# === Reflection warnings ===
-dontwarn java.lang.invoke.StringConcatFactory
