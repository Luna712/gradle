package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.GradleException
import java.io.File

abstract class EnsureJarCompatibilityTask : DefaultTask() {

    @get:InputFile
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract val isCrossPlatform: Property<Boolean>

    @TaskAction
    fun checkJar() {
        if (!isCrossPlatform.get()) return

        val jar = jarFile.get().asFile
        if (!jar.exists()) throw GradleException("Jar file does not exist: ${jar.absolutePath}")

        val outputFile = project.layout.buildDirectory.file("jdeps-output.txt").get().asFile

        project.exec {
            commandLine("jdeps", "--print-module-deps", jar.absolutePath)
            standardOutput = outputFile.outputStream()
            errorOutput = System.err
            isIgnoreExitValue = true
        }

        val output = outputFile.readText().trim()
        when {
            output.isEmpty() -> logger.warn("No output from jdeps! Cannot analyze jar file for Android imports!")
            "android." in output -> throw GradleException(
                "The cross-platform jar file contains Android imports! " +
                        "This will cause compatibility issues.\nRemove 'isCrossPlatform = true' or remove the Android imports."
            )
            else -> logger.lifecycle("SUCCESS: The cross-platform jar file does not contain Android imports")
        }
    }
}
