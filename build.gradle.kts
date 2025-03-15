plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.googleService) apply false
    kotlin("android") version "1.9.22" apply false
    kotlin("kapt") version "1.9.22" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
