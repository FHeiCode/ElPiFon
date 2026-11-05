# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class de.elpifon.app.data.** { *** Companion; }
-keepclasseswithmembers class de.elpifon.app.data.** { kotlinx.serialization.KSerializer serializer(...); }
