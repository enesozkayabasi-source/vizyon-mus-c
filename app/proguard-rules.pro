# Vizyon Music R8 / ProGuard rules for Google Play Release AAB/APK builds

# Preserve annotations and line numbers for Play Console crash diagnostics
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Preserve @Keep models & members (Firestore models, DTOs, helpers)
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Preserve WebView @JavascriptInterface methods (YouTubeIframePlayerBox)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.example.ui.components.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Firebase Firestore POJOs & Messaging
-keep class com.example.model.** { *; }
-keepattributes Signature
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId *;
    @com.google.firebase.firestore.ServerTimestamp *;
    @com.google.firebase.firestore.Exclude *;
}

# Moshi & Kotlin reflection / generated JsonAdapters
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep,allowobfuscation,allowshrinking class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

# Retrofit & OkHttp
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
