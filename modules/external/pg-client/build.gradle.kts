tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

dependencies {
    // implementation(projects.modules.application) -> 순환 참조의 원인 추정 : external - application
    implementation(projects.modules.domain)
    implementation(libs.spring.boot.starter.web)

    // For using common dir
    implementation(project(":modules:common"))

}
