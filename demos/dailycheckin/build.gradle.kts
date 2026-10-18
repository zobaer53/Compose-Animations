plugins {
    alias(libs.plugins.composeanimations.demo)
}

android {
    namespace = "com.zobaer53.composeanimations.demos.dailycheckin"
}

dependencies {
    implementation(project(":characters:sprout"))
    implementation(project(":characters:stormcloud"))
}
