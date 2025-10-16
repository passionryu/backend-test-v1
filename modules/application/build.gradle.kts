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

    // For Using TestPgClient
    implementation(project(":modules:external:pg-client"))

    // For using common dir
    implementation(project(":modules:common"))

}
