package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.makeManifest
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import java.io.File

const val TASK_GROUP = "cloudstream"

fun registerTasks(project: Project) {
    val extension = project.extensions.getCloudstream()
    val intermediates = project.buildDir.resolve("intermediates")

    if (project.rootProject.tasks.findByName("makePluginsJson") == null) {
        project.rootProject.tasks.register("makePluginsJson", MakePluginsJsonTask::class.java) { task ->
            task.group = TASK_GROUP
            task.outputs.upToDateWhen { false }
            task.outputFile.set(task.project.layout.buildDirectory.file("plugins.json"))
            task.pluginEntriesJson.set(
                task.project.provider {
                    val lst = task.project.allprojects.mapNotNull { sub ->
                        sub.extensions.findCloudstream()?.let { sub.makePluginEntry() }
                    }
                    JsonBuilder(lst, JsonGenerator.Options().excludeNulls().build()).toPrettyString()
                }
            )
        }
    }

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        it.group = TASK_GROUP
    }

    val pluginClassFile = intermediatesDir.map { it.file("pluginClass") }

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) { task ->
        task.group = TASK_GROUP

        task.pluginClassFile.set(pluginClassFile)
        task.outputFile.set(intermediatesDir.map { dir -> dir.file("classes.dex") })

        val android = project.extensions.findByName("android") as? BaseExtension
            ?: error("Android plugin not found")
        task.minSdk.set(android.defaultConfig.minSdk ?: 21)
        task.bootClasspath.from(android.bootClasspath)

        val extension = project.extensions.getCloudstream()
        task.pluginClassName.set(extension.pluginClassName)

        val kotlinTask = project.tasks.findByName("compileDebugKotlin") as KotlinCompile?
        if (kotlinTask != null) {
            task.dependsOn(kotlinTask)
            task.input.from(kotlinTask.destinationDirectory)
        }

        task.doLast {
            extension.pluginClassName = task.pluginClassName.orNull
        }
    }

    val compileResources =
        project.tasks.register("compileResources", CompileResourcesTask::class.java) {
            it.group = TASK_GROUP

            val processManifestTask =
                project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
            it.dependsOn(processManifestTask)

            val android = project.extensions.getByName("android") as BaseExtension
            it.input.set(android.sourceSets.getByName("main").res.srcDirs.single())
            it.manifestFile.set(processManifestTask.manifestOutputFile)

            it.outputFile.set(intermediates.resolve("res.apk"))

            it.doLast { _ ->
                val resApkFile = it.outputFile.asFile.get()

                if (resApkFile.exists()) {
                    project.tasks.named("make", AbstractCopyTask::class.java) {
                        it.from(project.zipTree(resApkFile)) { copySpec ->
                            copySpec.exclude("AndroidManifest.xml")
                        }
                    }
                }
            }
        }

    val compilePluginJar = project.tasks.register("compilePluginJar", CompilePluginJarTask::class.java) { task ->
        task.group = TASK_GROUP
        task.dependsOn("createFullJarDebug") // Ensure JAR is built before copying
        task.dependsOn("compileDex") // compileDex creates pluginClass
        val jarTask = project.tasks.named("createFullJarDebug")

        task.hasCrossPlatformSupport.set(extension.isCrossPlatform)
        task.pluginClassFile.set(pluginClassFile)
        task.pluginClassName.set(extension.pluginClassName)
        task.jarInputFile.fileProvider(jarTask.map { it.outputs.files.singleFile })
        task.targetJarFile.set(project.layout.buildDirectory.file("${project.name}.jar"))
        task.jarFileSize.set(extension.jarFileSize)
        
        task.doLast {
            extension.pluginClassName = task.pluginClassName.orNull
            extension.jarFileSize = task.jarFileSize.orNull
        }
    }

    val ensureJarCompatibility = project.tasks.register("ensureJarCompatibility") {
        it.group = TASK_GROUP
        it.dependsOn("compilePluginJar")
        it.doLast { task ->
            if (!extension.isCrossPlatform) {
                return@doLast
            }

            val jarFile = File("${project.buildDir}/${project.name}.jar")
            if (!jarFile.exists()) {
                throw GradleException("Jar file does not exist.")
                return@doLast
            }

            // Run jdeps command
            try {
                val jdepsOutput = ByteArrayOutputStream()
                val jdepsCommand = listOf("jdeps", "--print-module-deps", jarFile.absolutePath)

                project.exec { execTask ->
                    execTask.setCommandLine(jdepsCommand)
                    execTask.setStandardOutput(jdepsOutput)
                    execTask.setErrorOutput(System.err)
                    execTask.setIgnoreExitValue(true)
                }

                val output = jdepsOutput.toString()

                // Check if 'android.' is in the output
                if (output.isEmpty()) {
                    task.logger.warn("No output from jdeps! Cannot analyze jar file for Android imports!")
                } else if (output.contains("android.")) {
                    throw GradleException("The cross-platform jar file contains Android imports! This will cause compatibility issues.\nRemove 'isCrossPlatform = true' or remove the Android imports.")
                } else {
                    task.logger.lifecycle("SUCCESS: The cross-platform jar file does not contain Android imports")
                }
            } catch (e: org.gradle.process.internal.ExecException) {
                task.logger.warn("Jdeps failed! Cannot analyze jar file for Android imports!")
            }
        }
    }

    project.afterEvaluate {
        val make = project.tasks.register("make", Zip::class.java) {
            val compileDexTask = compileDex.get()
            it.dependsOn(compileDexTask)
            if (extension.isCrossPlatform) {
                it.dependsOn(compilePluginJar)
            }

            val manifestFile = intermediates.resolve("manifest.json")
            it.from(manifestFile)
            it.doFirst {
                if (extension.pluginClassName == null) {
                    if (pluginClassFile.exists()) {
                        extension.pluginClassName = pluginClassFile.readText()
                    }
                }

                manifestFile.writeText(
                    JsonBuilder(
                        project.makeManifest(),
                        JsonGenerator.Options()
                            .excludeNulls()
                            .build()
                    ).toString()
                )
            }

            it.from(compileDexTask.outputFile)

            val zip = it as Zip
            if (extension.requiresResources) {
                zip.dependsOn(compileResources.get())
            }
            zip.isPreserveFileTimestamps = false
            zip.archiveBaseName.set(project.name)
            zip.archiveExtension.set("cs3")
            zip.archiveVersion.set("")
            zip.destinationDirectory.set(project.buildDir)

            it.doLast { task ->
                extension.fileSize = task.outputs.files.singleFile.length()
                task.logger.lifecycle("Made Cloudstream package at ${task.outputs.files.singleFile}")
            }
        }
        project.rootProject.tasks.getByName("makePluginsJson").dependsOn(make)
    }

    project.tasks.register("cleanCache", CleanCacheTask::class.java) {
        it.group = TASK_GROUP
    }

    project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
        it.group = TASK_GROUP
        it.dependsOn("make")
    }
}
