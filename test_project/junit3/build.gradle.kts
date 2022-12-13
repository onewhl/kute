plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:3.8.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed")
    }
}