plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.github.jengelman.gradle.plugins:shadow:5.2.0")
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

configure<KotlinDslPluginOptions> {
    experimentalWarning.set(false)
}
