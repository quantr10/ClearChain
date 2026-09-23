# ClearChain release (R8/ProGuard) rules.
#
# Most libraries here (Room, Retrofit, OkHttp, Hilt, Firebase, kotlinx.serialization) ship
# their own consumer rules and need nothing extra. The two real risk spots are:
#
# 1. The SignalR Java client (com.microsoft.signalr) deserializes hub payloads with Gson,
#    reflecting on field names — R8 renaming/removing a field wouldn't crash, it would
#    just silently deserialize to null/default, which is much worse than a build failure.
# 2. Every data/domain/dto class that crosses a network or Room boundary (Retrofit body,
#    SignalR payload, Room entity) needs its shape preserved for the same reason.
#
# so both are kept wholesale below rather than trimmed field-by-field.

# ── SLF4J (transitive dependency, no binding on the classpath by design — this class's
#    absence is handled gracefully by slf4j-api itself at runtime) ───────────────────────
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ── Gson (transitive dependency of com.microsoft.signalr:signalr) ───────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod,InnerClasses
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── SignalR client ────────────────────────────────────────────────────────────────────
-keep class com.microsoft.signalr.** { *; }
-dontwarn com.microsoft.signalr.**

# ── App data shapes crossing Retrofit / SignalR / Room boundaries ───────────────────────
-keep class com.clearchain.app.data.remote.dto.** { *; }
-keep class com.clearchain.app.data.remote.signalr.** { *; }
-keep class com.clearchain.app.data.local.entity.** { *; }
-keep class com.clearchain.app.domain.model.** { *; }

# ── kotlinx.serialization ─────────────────────────────────────────────────────────────
# The library's own consumer rules cover the common cases; this keeps enum serializers
# specifically, since they're looked up by generated code R8 can't always see into.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
