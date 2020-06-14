import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.crlf.CrLfSpec


plugins {
    `java-library`
    id("eclipse")
    id("com.github.johnrengelman.shadow")
    id("com.github.vlsi.crlf")
}

val String.v: String get() = rootProject.extra["$this.version"] as String
val projectVersion = "threadtear".v

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

configurations {
    all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }
}

fun DependencyHandlerScope.externalLib(libraryName: String) {
    compileOnly(files("${rootProject.rootDir}/libs/$libraryName.jar"))
    runtimeOnly(files("${rootProject.rootDir}/libs/$libraryName.jar"))
}

dependencies {
	implementation("commons-io:commons-io")

    implementation("org.apache.commons:commons-configuration2")
    implementation("commons-beanutils:commons-beanutils")

    implementation("com.github.weisj:darklaf-core") { isChanging = true }
    implementation("com.github.weisj:darklaf-theme") { isChanging = true }
    implementation("com.github.weisj:darklaf-property-loader") { isChanging = true }

    implementation("org.ow2.asm:asm")
    implementation("org.ow2.asm:asm-tree")
    implementation("org.ow2.asm:asm-analysis")
    implementation("org.ow2.asm:asm-util")

    implementation("com.github.leibnitz27:cfr") { isChanging = true }
    implementation("com.fifesoft:rsyntaxtextarea")
    implementation("com.github.jgraph:jgraphx")
    implementation("ch.qos.logback:logback-classic")

    externalLib("fernflower-15-05-20")
}

sourceSets {
    main {
        java.srcDirs("src");
        resources.srcDirs("src");
    }
}

tasks.shadowJar {
    archiveBaseName.set(project.name)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    transform(LicenseTransformer::class.java) {
        destinationPath = "META-INF/licenses/LICENSES.txt"
        include("META-INF/LICENSE", "META-INF/LICENSE.txt")
        exclude("META-INF/THREADTEAR_LICENSE")
    }
    transform(LicenseTransformer::class.java) {
        destinationPath = "META-INF/licenses/NOTICES.txt"
        include("META-INF/NOTICE", "META-INF/NOTICE.txt")
    }
    relocate("META-INF", "META-INF/licenses") {
        includes.addAll(listOf(
            "META-INF/*LICENSE*",
            "META-INF/*NOTICE*",
            "META-INF/AL2.0",
            "META-INF/LGPL2.1"
        ))
        exclude("META-INF/THREADTEAR_LICENSE")
    }
}

val fatJar by tasks.registering {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Build a runnable jar with all dependencies"
    dependsOn(tasks.shadowJar)
}

allprojects {
    group = "me.nov.threadtear"
    version = projectVersion

    tasks.withType<AbstractArchiveTask>().configureEach {
        // Ensure builds are reproducible
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirMode = "775".toInt(8)
        fileMode = "664".toInt(8)
    }

    plugins.withType<JavaLibraryPlugin> {
        dependencies {
            val bom = platform(project(":threadtear-dependencies-bom"))
            "api"(bom)
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
            }

            withType<ProcessResources>().configureEach {
                from(source) {
                    include("**/*.properties")
                    filteringCharset = "UTF-8"
                    // apply native2ascii conversion since Java 8 expects properties to have ascii symbols only
                    filter(org.apache.tools.ant.filters.EscapeUnicode::class)
                }
            }

            withType<Javadoc>().configureEach {
                (options as StandardJavadocDocletOptions).apply {
                    quiet()
                    locale = "en"
                    docEncoding = "UTF-8"
                    charSet = "UTF-8"
                    encoding = "UTF-8"
                    docTitle = "Threadtear ${project.name} API"
                    windowTitle = "Threadtear ${project.name} API"
                    header = "<b>Threadtear</b>"
                    addBooleanOption("Xdoclint:none", true)
                    addStringOption("source", "8")
                    if (JavaVersion.current().isJava9Compatible) {
                        addBooleanOption("html5", true)
                        links("https://docs.oracle.com/javase/9/docs/api/")
                    } else {
                        links("https://docs.oracle.com/javase/8/docs/api/")
                    }
                }
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "GPL-3.0"
                    attributes["Implementation-Title"] = project.name
                    attributes["Implementation-Version"] = project.version
                    attributes["Specification-Vendor"] = "Threadtear"
                    attributes["Specification-Version"] = project.version
                    attributes["Specification-Title"] = "Threadtear"
                    attributes["Implementation-Vendor"] = "Threadtear"
                    attributes["Implementation-Vendor-Id"] = project.group
                    attributes["Main-Class"] = "me.nov.threadtear.Threadtear"
                }

                CrLfSpec(LineEndings.LF).run {
                    into("META-INF") {
                        filteringCharset = "UTF-8"
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        // This includes either project-specific license or a default one
                        if (file("$projectDir/LICENSE").exists()) {
                            textFrom("$projectDir/LICENSE")
                        } else {
                            textFrom("$rootDir/LICENSE")
                        }
                        rename { s -> "${project.name.toUpperCase()}_LICENSE" }
                    }
                }
            }
        }
    }
}
