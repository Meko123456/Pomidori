import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// The countdown, the cycle and the controller that ties them together: plain Kotlin with no
// Android in it, so the phone app and the watch app run the same timer, and its tests run on the
// JVM in a second. Java 17 bytecode, the same as the app compiles to.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
