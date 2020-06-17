import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.properties.dsl.props

plugins {
    id("eclipse")
    id("com.github.autostyle")
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
}

val skipAutostyle by props(true) // Remove true if formatting is configured.

val String.v: String get() = rootProject.extra["$this.version"] as String
val projectVersion = "threadtear".v

allprojects {
    group = "me.nov.threadtear"
    version = projectVersion

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

    if (!skipAutostyle) {
        apply(plugin = "com.github.autostyle")
        autostyle {
            kotlinGradle {
                ktlint()
            }
            format("markdown") {
                filter.include("**/*.md")
                endWithNewline()
            }
        }
    }

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

        if (!skipAutostyle) {
            autostyle {
                java {
                    importOrder("java", "javax", "org", "com")
                    removeUnusedImports()
                    eclipse {
                        configFile("${project.rootDir}/threadtear.eclipseformat.xml")
                    }
                }
            }
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
                }

                CrLfSpec(LineEndings.LF).run {
                    into("META-INF") {
                        filteringCharset = "UTF-8"
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        // This includes either project-specific license or a default one
                        if (file("$projectDir/LICENSE").exists()) {
                            textFrom("$projectDir/LICENSE")
                            rename { s -> "${project.name.toUpperCase()}_LICENSE" }
                        } else {
                            textFrom("$rootDir/LICENSE")
                            rename { s -> "${rootProject.name.toUpperCase()}_LICENSE" }
                        }
                    }
                }
            }
        }
    }
}
