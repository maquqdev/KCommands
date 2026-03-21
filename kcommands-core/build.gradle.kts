plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    compileOnly(libs.paper.api)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.paper.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "kcommands-core"
            version = project.version.toString()
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
