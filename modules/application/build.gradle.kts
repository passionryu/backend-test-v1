tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.modules.domain)
    implementation("org.springframework:spring-context")
    // SLF4J 로깅 의존성 추가
    implementation("org.slf4j:slf4j-api")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    // For using common dir
    implementation(project(":modules:common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.spring.mockk)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bundles.test)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
