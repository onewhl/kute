plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed")
    }
}