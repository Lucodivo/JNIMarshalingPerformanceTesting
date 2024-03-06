# prevent R8 from eliminating external methods and classes with external methods
# without this line, JNI's RegisterNatives function may fail if an unused method is
# eliminated from the codebase through code shrinking
-keepclasseswithmembers class com.inasweaterpoorlyknit.jniplayground.** { native <methods>; }
-keepclassmembers class com.inasweaterpoorlyknit.jniplayground.** { native <methods>; }