import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A full demo screen: everything a character gets, plus Material 3 for the surrounding UI. */
class DemoConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(CharacterConventionPlugin::class.java)

        dependencies {
            "implementation"(libs.library("androidx-compose-material3"))
        }
    }
}
