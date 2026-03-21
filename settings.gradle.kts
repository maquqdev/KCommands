plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "KCommands"

include("kcommands-core")
include("kcommands-test")
include("kcommands-example")
