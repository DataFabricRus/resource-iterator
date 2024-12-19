import java.security.MessageDigest

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.dokka")
    signing
}

group = "io.github.datafabricrus"
version = "1.4"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(11)
    }
    js {
        browser()
        nodejs()
        binaries.library()
    }

    withSourcesJar(true)

    val kotlinVersion: String by project

    sourceSets {
        val commonMain by getting
        val jvmMain by getting
        val jsMain by getting

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmTest by getting {
            dependencies {
                val junitVersion: String by project
                implementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
                implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
            }
        }
    }
}

tasks.register<Jar>("javadocJar") {
    dependsOn("dokkaHtml")
    archiveClassifier.set("javadoc")
    from(buildDir.resolve("dokka/html"))
}

tasks.named("publishToMavenLocal") {
    doLast {
        println("================================================")
        println("Generate MD5 files")
        println("================================================")
        val mavenLocalDir = file(repositories.mavenLocal().url)
        val artifactPathAsString = project.group.toString().replace('.', '/') + "/${project.name}"
        val artifactFile = mavenLocalDir.resolve(artifactPathAsString)
        val files = sequenceOf(artifactFile.toString(), "$artifactFile-jvm", "$artifactFile-js")
            .map { it + "/${version}" }
            .map { file(it) }
            .flatMap {
                it.walkTopDown()
            }
            .filter {
                it.isFile && (it.extension == "jar" || it.extension == "pom" || it.extension == "module")
            }
        files.forEach { file ->
            val md5 = MessageDigest.getInstance("MD5")
                .digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            val sha1 = MessageDigest.getInstance("SHA-1")
                .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

            file.resolveSibling("${file.name}.md5").writeText(md5)
            file.resolveSibling("${file.name}.sha1").writeText(sha1)
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {

            artifact(tasks["javadocJar"]) {
                classifier = "javadoc"
            }

            println("================================================")
            println("$groupId:$artifactId:$version")
            println("================================================")
            pom {
                name.set("${project.group}:${project.name}")
                description.set("Resource Iterator")
                url.set("https://github.com/DataFabricRus/resource-iterator")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/DataFabricRus/resource-iterator.git")
                    developerConnection.set("scm:git:ssh://github.com/DataFabricRus/resource-iterator.git")
                    url.set("https://github.com/DataFabricRus/resource-iterator")
                }
                developers {
                    developer {
                        id.set("sszuev")
                        name.set("Sergei Zuev")
                        email.set("sss.zuev@gmail.com")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn(tasks.withType<Sign>())
}