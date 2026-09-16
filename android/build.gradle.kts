plugins {
    id("com.android.application") version "8.13.2" apply false
}

// Standalone Veytrix Android builds must not inherit mismatched Kotlin stdlib variants
// from transitive AndroidX dependencies. Keep all Kotlin stdlib artifacts aligned.
subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("1.8.22")
                because("avoid duplicate kotlin-stdlib / kotlin-stdlib-jdk7 / kotlin-stdlib-jdk8 classes in the standalone Autopilot APK")
            }
        }
    }
}
