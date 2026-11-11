plugins {
    id("com.android.application") version "9.0.1" apply false
    // O AGP 9 inclui o plugin Kotlin para Android; não aplicar kotlin-android.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.8" apply false
}
