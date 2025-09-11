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

# MRD SDK ProGuard rules
# Keep all SDK classes and methods to prevent obfuscation
-keep class com.manridy.sdk.** { *; }
-keep class com.mrd.** { *; }
-keepclassmembers class com.manridy.sdk.** { *; }
-keepclassmembers class com.mrd.** { *; }

# Keep enums for SDK
-keepclassmembers enum com.manridy.sdk.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep callback interfaces
-keep interface com.manridy.sdk.**$*Callback { *; }
-keep interface com.manridy.sdk.**$*Listener { *; }