# Reglas R8 de release. NO PROBADAS en dispositivo: validar un build release completo
# (login, mapa, QR, notificaciones) antes de publicar.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Voyager identifica las pantallas por su clase.
-keep class * extends cafe.adriel.voyager.core.screen.Screen { *; }

# kotlinx.serialization: serializers generados.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.vibra.bus.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor / SLF4J: referencias opcionales ausentes en Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
