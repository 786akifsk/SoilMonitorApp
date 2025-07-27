# Keep Supabase models
-keep class io.supabase.** { *; }

# Keep your own models used in JSON parsing
-keep class com.example.myapplication.** { *; }

# Avoid removing view-related stuff
-keep class android.view.** { *; }
