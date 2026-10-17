# Moshi's Kotlin reflection adapter needs model names and constructors at runtime.
-keep class bizora.app.data.model.** { *; }
-keep class bizora.app.data.remote.** { *; }
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,EnclosingMethod,InnerClasses

# AndroidX Room & WorkManager (required for AdMob / WorkDatabase reflection instantiation)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
    *;
}
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.** { *; }
-dontwarn androidx.work.impl.**

# ML Kit Barcode Scanning & Firebase Components
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
    *;
}
-keep class com.google.mlkit.** {
    <init>();
    *;
}
-dontwarn com.google.mlkit.**

# Google Mobile Ads / Play Services Ads
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Biometric & CameraX
-keep class androidx.biometric.** { *; }
-keep class androidx.camera.** { *; }
-keep class com.google.zxing.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

