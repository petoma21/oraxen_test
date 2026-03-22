rootProject.name = "oraxen"

val isCI = System.getenv("CI") != null

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mineinabyss.com/releases")
    }
}

plugins {
    // allows for better class redefinitions with run-paper
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
//    repositories {
//        maven("https://repo.mineinabyss.com/releases")
//        maven("https://repo.mineinabyss.com/snapshots")
//        mavenLocal()
//    }

    versionCatalogs {
        create("oraxenLibs") {
            from(files("gradle/oraxenLibs.versions.toml"))
        }
    }
}

// 1.21.8 (Paper) 向けの最小構成
include(
    "core",
    "v1_21_R5"
)
