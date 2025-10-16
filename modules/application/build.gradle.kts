tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

dependencies {
    implementation(projects.modules.domain)
    // Only need Spring annotations (@Service) for this module
    implementation("org.springframework:spring-context")

    // For using common dir
    implementation(project(":modules:common"))

    // For Using TestPgClient (핵사고날 아키텍처의 경계 위반으로 인한 주석 처리)
    // implementation(project(":modules:external:pg-client"))
}
