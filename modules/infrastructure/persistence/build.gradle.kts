plugins {
    id("org.springframework.boot") version "3.4.4" apply false
    id("io.spring.dependency-management")
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

dependencies {
    implementation(projects.modules.domain)
    implementation(projects.modules.application)

    implementation(libs.spring.boot.starter.jpa)
    runtimeOnly(libs.database.h2)
    runtimeOnly(libs.database.mariadb)

    // ✅ Liquibase + PostgreSQL
    implementation("org.liquibase:liquibase-core:4.29.2")
    implementation("org.postgresql:postgresql:42.7.3")

    testImplementation(libs.spring.boot.starter.test) {
        exclude(module = "mockito-core")
    }
    testImplementation(libs.database.h2)
}

repositories {
    mavenCentral()
}
