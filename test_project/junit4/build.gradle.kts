plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("pl.pragmatists:JUnitParams:1.1.1")
    testImplementation("com.tngtech.junit.dataprovider:junit4-dataprovider:2.9")
    testImplementation("io.qameta.allure:allure-junit4:2.20.0")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed")
    }
}