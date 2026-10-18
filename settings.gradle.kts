pluginManagement {
    // Convention plugins shared by every module; see build-logic/.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ComposeAnimations"

// The showcase app that lists and opens every demo.
include(":app")

// Reusable Canvas characters. They only draw; they know nothing about the screens using them.
include(":characters:dog")
include(":characters:sprout")
include(":characters:stormcloud")

// Full screens built around the characters.
include(":demos:forgetpassword")
