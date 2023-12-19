plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
}

group = "cc.datafabric"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val junitVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

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

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    sign(publishing.publications["maven"])
}

tasks.test {
    useJUnitPlatform()
}

tasks.getByName("signMavenPublication") {
    enabled = project.hasProperty("sign")
}

kotlin {
    jvmToolchain(11)
}