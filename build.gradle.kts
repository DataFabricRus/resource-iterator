plugins {
    kotlin("multiplatform")
    id("maven-publish")
    signing
}

group = "cc.datafabric"
version = "1.2-SNAPSHOT"

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

publishing {
    publications {
        withType<MavenPublication> {
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
    sign(publishing.publications["jvm"])
}

tasks.getByName("signJvmPublication") {
    enabled = project.hasProperty("sign")
}