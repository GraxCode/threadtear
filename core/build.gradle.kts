plugins {
    `java-library`
}

fun DependencyHandlerScope.externalLib(libraryName: String) {
    compileOnly(files("${rootProject.rootDir}/libs/$libraryName.jar"))
    runtimeOnly(files("${rootProject.rootDir}/libs/$libraryName.jar"))
}

dependencies {
    implementation("commons-io:commons-io")

    implementation("org.apache.commons:commons-configuration2")
    implementation("commons-beanutils:commons-beanutils")

    implementation("org.ow2.asm:asm")
    implementation("org.ow2.asm:asm-tree")
    implementation("org.ow2.asm:asm-analysis")
    implementation("org.ow2.asm:asm-util")

    implementation("com.github.leibnitz27:cfr") { isChanging = true }
    implementation("ch.qos.logback:logback-classic")

    externalLib("fernflower-15-05-20")
}
