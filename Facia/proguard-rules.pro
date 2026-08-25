# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-ignorewarnings

-keepattributes InnerClasses

# Keep Retrofit annotations
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class com.facia.faciasdk.FaciaAi

-keepclassmembers class com.facia.faciasdk.FaciaAi** {

*;

}

-keep class com.facia.faciasdk.Utils.Utilities

-keepclassmembers class com.facia.faciasdk.Utils.Utilities {

*;

}

-keep class com.facia.faciasdk.Activity.Helpers.RequestListener

-keepclassmembers class com.facia.faciasdk.Activity.Helpers.RequestListener {

*;

}

# Keep all SDK API models and their fields
-keep class com.facia.faciasdk.ApiModels.** {
    <fields>;
    <methods>;
}

# Keep Gson annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


-keep interface retrofit2.Call {
    *;
}

# Keep Gson-related classes
-keep class com.google.gson.stream.** {
    *;
}

-keep class com.google.gson.** {
    *;
}