package com.lagradost.cloudstream3.gradle.configuration

import com.lagradost.cloudstream3.gradle.ApkInfo
import com.lagradost.cloudstream3.gradle.download
import com.lagradost.cloudstream3.gradle.getCloudstream
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import java.net.URI
import javax.inject.Inject

abstract class CloudStreamConfigurationProvider : IConfigurationProvider {

    override val name: String
        get() = "cloudstream"

    @get:Inject
    abstract val progressLoggerFactory: ProgressLoggerFactory

    override fun provide(project: Project, dependency: Dependency) {
        val extension = project.extensions.getCloudstream()
        if (extension.apkinfo == null) {
            extension.apkinfo = ApkInfo(extension, dependency.version ?: "pre-release")
        }

        val apkinfo = extension.apkinfo!!
        apkinfo.cache.mkdirs()
        if (!apkinfo.jarFile.exists()) {
            project.logger.lifecycle("Fetching JAR: ${apkinfo.jarFile.name}")
            val url = URI("${apkinfo.urlPrefix}/classes.jar").toURL()
            val progressLogger = progressLoggerFactory.newOperation("Download JAR").apply {
                description = "Download JAR"
            }

            try {
                progressLogger.started()
                url.download(apkinfo.jarFile, progressLogger)
            } finally {
                progressLogger.completed()
            }
        }

        project.dependencies.add("compileOnly", project.files(apkinfo.jarFile))
    }
}
