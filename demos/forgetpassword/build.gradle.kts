plugins {
    alias(libs.plugins.composeanimations.demo)
}

android {
    namespace = "com.zobaer53.composeanimations.demos.forgetpassword"
}

dependencies {
    implementation(project(":characters:dog"))
}
