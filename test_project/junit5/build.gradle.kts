plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed")
    }
}