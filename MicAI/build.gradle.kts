// Top-level build file for MIC AI.
// Versions are pinned to the same stable line used by the Zalith Launcher
// module in this repository (AGP 8.2.2 / Kotlin 2.0.21) so the project
// builds with JDK 17 and Android Gradle Plugin 8.2.x.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
