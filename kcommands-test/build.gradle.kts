plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":kcommands-core"))
    compileOnly(libs.paper.api)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
}
