package com.lagradost.cloudstream3.gradle

import com.lagradost.cloudstream3.gradle.configuration.registerConfigurations
import com.lagradost.cloudstream3.gradle.tasks.registerTasks
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import javax.inject.Inject

abstract class CloudstreamPlugin : Plugin<Project> {

    @get:Inject
    abstract val progressLoggerFactory: ProgressLoggerFactory

    override fun apply(project: Project) {
        project.extensions.create("cloudstream", CloudstreamExtension::class.java, project)

        registerTasks(project)
        registerConfigurations(project, progressLoggerFactory)
    }
}
