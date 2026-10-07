// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.spotless)
}

// Formatter for the Kotlin sources. These mirror the [*.{kt,kts}] section of
// .editorconfig at the repository root; Spotless does not pass that file
// through to ktlint, so the rules it must not enforce are repeated here.
//
// Each is off because it contradicts a convention this project keeps on
// purpose: star imports are the agreed import style, @Composable functions
// are PascalCase, screens hold several composables per file, and DTO fields
// are annotated with a trailing comment in place.
val ktlintRules = mapOf(
    // 120 stays the guide in .editorconfig, but ktlint cannot wrap a long line,
    // it can only fail on one. 327 lines are over, several of them Compose
    // argument lists past 900 characters, so this is left to the author.
    "max_line_length" to "off",
    "ktlint_standard_no-wildcard-imports" to "disabled",
    "ktlint_standard_function-naming" to "disabled",
    "ktlint_standard_filename" to "disabled",
    "ktlint_standard_discouraged-comment-location" to "disabled"
)

subprojects {
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        // Match .editorconfig and what git stores. Left at its default, Spotless
        // resolves line endings through core.autocrlf and rewrites every file to
        // CRLF, which is churn and contradicts the declared standard.
        lineEndings = com.diffplug.spotless.LineEnding.UNIX

        kotlin {
            target("src/**/*.kt")
            ktlint("1.0.1").editorConfigOverride(ktlintRules)
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.0.1").editorConfigOverride(ktlintRules)
        }
    }
}
