# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# MediaPipe Tasks GenAI (local model inference)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
