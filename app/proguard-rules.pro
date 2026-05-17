-keep class com.velvetwallet.app.data.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Room
-keep class androidx.room.** { *; }
