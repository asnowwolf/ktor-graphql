val ktor_version = "1.6.4"
val kotlin_version = "1.5.31"

plugins {
    kotlin("jvm") version "1.5.31"
    `maven-publish`
}
group = "wang.ralph.common"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("com.expediagroup:graphql-kotlin-schema-generator:5.2.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")

    testImplementation("io.ktor:ktor-jackson:$ktor_version")
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
        }
    }
}
