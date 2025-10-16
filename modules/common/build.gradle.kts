tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

dependencies {
    implementation(projects.modules.domain)
    implementation(libs.bundles.jackson)
    implementation(libs.spring.boot.starter.logging)
    implementation(libs.spring.boot.starter.web)
}
