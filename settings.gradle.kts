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

// Reusable Canvas characters. They only draw; they know nothing about the screens using them.
include(":characters:dog")
