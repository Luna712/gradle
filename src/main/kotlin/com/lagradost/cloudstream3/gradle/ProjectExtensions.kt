package com.lagradost.cloudstream3.gradle

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getCloudstream().configuration()

fun Project.android(configuration: LibraryExtension.() -> Unit) {
    val android = extensions.getByName("android") as LibraryExtension
    android.apply {
        project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        configuration()
    }
}
