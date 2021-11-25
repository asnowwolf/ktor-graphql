val ktor_version = "1.6.4"
val kotlin_version = "1.5.31"

plugins {
    kotlin("jvm") version "1.5.31"
    `maven-publish`
    signing
}
group = "wang.ralph.common"
version = "2.4.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("com.expediagroup:graphql-kotlin-schema-generator:5.2.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")

    testImplementation("io.ktor:ktor-jackson:$ktor_version")
    testImplementation("io.ktor:ktor-auth:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
            sourceSets {
                main {
                    java {
                        srcDirs("src/main/kotlin")
                    }
                }
            }
        }
        withType<MavenPublication> {
            pom {
                name.set("${project.group}:${project.name}")
                url.set("https://github.com/asnowwolf/ktor-graphql")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                organization {
                    name.set("Ralph WANG")
                    name.set("https://blog.ralph.wang/")
                }
                developers {
                    developer {
                        name.set("Ralph WANG")
                        email.set("asnowwolf@gmail.com")
                        organization.set("Ralph WANG")
                        organizationUrl.set("https://blog.ralph.wang/")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/asnowwolf/ktor-graphql.git")
                    developerConnection.set("scm:git:git://github.com/asnowwolf/ktor-graphql.git")
                    url.set("https://github.com/asnowwolf/ktor-graphql")
                }

                // child projects need to be evaluated before their description can be read
                val mavenPom = this
                afterEvaluate {
                    mavenPom.description.set(project.description)
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(project.property("sonatype.snapshot-url") as String)
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = project.property("sonatype.username") as String
                password = project.property("sonatype.password") as String
            }
        }
    }
}

signing {
    sign(configurations.archives.get())
}
