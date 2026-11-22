# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# MediaPipe Tasks GenAI (local model inference)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# javax.lang.model.* only exists on a desktop JDK (annotation processors such
# as AutoValue/javapoet ship inside some AARs). They are never executed at
# runtime on Android, so R8 just needs permission to drop the references.
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
