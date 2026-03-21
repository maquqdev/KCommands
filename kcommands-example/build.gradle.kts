plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":kcommands-core"))
    compileOnly(libs.paper.api)
}
