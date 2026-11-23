# Midad Studio R8 rules (release builds are minified and resource-shrunk).

# --- LiteRT / TensorFlow Lite -------------------------------------------------
# The Interpreter is a JNI entry point; native methods are bound by name.
-keep class org.tensorflow.lite.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
-dontwarn org.tensorflow.lite.**

# --- Media3 -------------------------------------------------------------------
# Transformer effects are instantiated directly; keep public names so error
# messages stay readable in release crash reports.
-keep class androidx.media3.transformer.** { *; }
-keep class androidx.media3.effect.** { *; }
-dontwarn androidx.media3.**

# --- App ----------------------------------------------------------------------
# ViewModels and state holders are constructed by the framework.
-keep,allowobfuscation class com.midad.studio.** extends androidx.lifecycle.ViewModel { *; }
# Keep readable stack traces (no obfuscation of the app's own frames is not
# possible with R8, but source file names help triage).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
