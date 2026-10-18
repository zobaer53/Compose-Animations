plugins {
    `kotlin-dsl`
}

dependencies {
    // compileOnly: at build time the root project already has these plugins on its classpath.
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("character") {
            id = libs.plugins.composeanimations.character.get().pluginId
            implementationClass = "CharacterConventionPlugin"
        }
        register("demo") {
            id = libs.plugins.composeanimations.demo.get().pluginId
            implementationClass = "DemoConventionPlugin"
        }
    }
}
