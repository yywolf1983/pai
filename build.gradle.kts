// --- build.gradle.kts (Root Project Level) ---
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Define version catalogs for easy dependency management (Modern Gradle approach)
// This assumes you also have a libs.versions.toml file, which is best practice.
// For this exercise, we define versions directly here if libs.versions.toml is absent.
// However, using 'libs' accessor is cleaner.

// If you don't have libs.versions.toml, this file defines the versions:
// val libs = the function that reads the TOML file
// Since we don't see the TOML, we'll keep the structure clean and assume the 'libs' accessor works.



// Define tasks that run after the build finishes
tasks.register("cleanBuild") {
    dependsOn("clean")
}

// Task to run a full build and then run the app on a connected device
tasks.register("fullBuildAndRun") {
    dependsOn(tasks.named("assembleDebug"))
    doLast {
        println("\n=========================================")
        println("✨ Full Build Complete! Now installing via adb...")
        println("=========================================")
        val process = Runtime.getRuntime().exec(arrayOf("adb", "install", "app-debug.apk"))
        process.waitFor()
    }
}