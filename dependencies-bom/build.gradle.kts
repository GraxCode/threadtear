plugins {
    `java-platform`
}

val String.v: String get() = rootProject.extra["$this.version"] as String

// Note: Gradle allows to declare dependency on "bom" as "api",
// and it makes the contraints to be transitively visible
// However Maven can't express that, so the approach is to use Gradle resolution
// and generate pom files with resolved versions
// See https://github.com/gradle/gradle/issues/9866

fun DependencyConstraintHandlerScope.apiv(
        notation: String,
        versionProp: String = notation.substringAfterLast(':')
) =
        "api"(notation + ":" + versionProp.v)

fun DependencyConstraintHandlerScope.runtimev(
        notation: String,
        versionProp: String = notation.substringAfterLast(':')
) =
        "runtime"(notation + ":" + versionProp.v)

dependencies {
    // Parenthesis are needed here: https://github.com/gradle/gradle/issues/9248
    (constraints) {
        // api means "the dependency is for both compilation and runtime"
        // runtime means "the dependency is only for runtime, not for compilation"
        // In other words, marking dependency as "runtime" would avoid accidental
        // dependency on it during compilation
        apiv("commons-io:commons-io")
        apiv("org.apache.commons:commons-configuration2")
        apiv("commons-beanutils:commons-beanutils")
        apiv("com.github.leibnitz27:cfr")
        apiv("com.fifesoft:rsyntaxtextarea")
        apiv("com.github.jgraph:jgraphx")
        apiv("ch.qos.logback:logback-classic")
        apiv("com.github.weisj:darklaf-core","darklaf")
        apiv("com.github.weisj:darklaf-theme","darklaf")
        apiv("com.github.weisj:darklaf-property-loader", "darklaf")
        apiv("org.ow2.asm:asm", "asm")
        apiv("org.ow2.asm:asm-tree", "asm")
        apiv("org.ow2.asm:asm-analysis", "asm")
        apiv("org.ow2.asm:asm-util", "asm")
        apiv("org.ow2.asm:asm-commons", "asm")
    }
}
