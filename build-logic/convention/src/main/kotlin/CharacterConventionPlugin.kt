import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A Compose library that draws a character on a Canvas: just the Compose UI and foundation
 * libraries, no Material.
 */
class CharacterConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.version("compileSdk")
            defaultConfig {
                minSdk = libs.version("minSdk")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildFeatures {
                compose = true
            }
        }

        dependencies {
            "implementation"(platform(libs.library("androidx-compose-bom")))
            "implementation"(libs.library("androidx-compose-ui"))
            "implementation"(libs.library("androidx-compose-ui-graphics"))
            "implementation"(libs.library("androidx-compose-foundation"))
            "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
            "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
        }
    }
}
