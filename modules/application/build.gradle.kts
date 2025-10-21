tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

dependencies {
    implementation(projects.modules.domain)
    implementation("org.springframework:spring-context")
    // SLF4J 로깅 의존성 추가
    implementation("org.slf4j:slf4j-api")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    // For using common dir
    implementation(project(":modules:common"))
}
