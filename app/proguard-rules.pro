# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep model classes used for serialization in metadata cache
-keep class com.gorilla.music.data.model.** { *; }
