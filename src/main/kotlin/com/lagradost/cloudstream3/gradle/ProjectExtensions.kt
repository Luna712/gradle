package com.lagradost.cloudstream3.gradle

import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getCloudstream().configuration()

fun Project.android(configuration: LibraryExtension.() -> Unit) {
    extensions.getByName<LibraryExtension>("android").apply {
        project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        configuration()
    }
}

fun Project.android(configuration: BaseExtension.() -> Unit) =
    extensions.getByName<BaseExtension>("android").configuration()
