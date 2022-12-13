plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.testng:testng:7.6.1")
}

tasks.test {
    useTestNG()
    testLogging {
        events("passed", "failed")
    }
}