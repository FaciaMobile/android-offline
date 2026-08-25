# Facia SDK – Consumer ProGuard Rules
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep interface * {
    @retrofit2.http.* <methods>;
}

-keep class com.facia.faciasdk.FaciaAi { *; }

-keep class com.facia.faciasdk.Utils.Utilities { *; }

-keep class com.facia.faciasdk.Activity.Helpers.RequestListener { *; }

-keep class com.facia.faciasdk.ApiModels.** {
    <fields>;
    <methods>;
}

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**
