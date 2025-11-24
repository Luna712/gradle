package com.lagradost.cloudstream3.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import java.io.File

/**
 * Compatibility layer for AGP 9, maintaining backward compatibility with AGP 8
 * for Android library modules. Provides access to minSdk, bootClasspath,
 * and the main res source directory in a way that works across both versions.
 *
 * This class can be removed once support for AGP 8 is no longer required.
 */
internal class LibraryExtensionCompat(private val project: Project) {

    private val android = project.extensions.findByName("android")
        ?: error("Android plugin not found")

    val minSdk: Int
        get() = when (android) {
            is BaseExtension -> android.defaultConfig.minSdk ?: 21
            is LibraryExtension -> android.defaultConfig.minSdk ?: 21
            else -> error("Android plugin found, but it's not a library module")
        }

    val bootClasspath: Any
        get() = when (android) {
            is BaseExtension -> android.bootClasspath
            is LibraryExtension -> project.extensions
                .findByType(LibraryAndroidComponentsExtension::class.java)
                ?.sdkComponents
                ?.bootClasspath
                ?: error("LibraryAndroidComponentsExtension not found")
            else -> error("Unknown Android extension type")
        }

    val mainResSrcDir: File
        get() = when (android) {
            is BaseExtension -> android.sourceSets.getByName("main").res.srcDirs.single()
            is LibraryExtension -> android.sourceSets.getByName("main").res.srcDirs.single()
            else -> error("Unknown Android extension type")
        }
}
