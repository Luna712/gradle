package com.lagradost.cloudstream3.gradle.tasks

import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import com.lagradost.cloudstream3.gradle.download
import com.lagradost.cloudstream3.gradle.createProgressLogger

abstract class GenSourcesTask : DefaultTask() {

	@get:Input
	abstract val urlPrefix: Property<String>

	@get:OutputFile
	abstract val sourcesJarFile: RegularFileProperty

	@TaskAction
	fun genSources() {
		val url = URI("${urlPrefix.get()}/app-sources.jar").toURL()
		url.download(
			sourcesJarFile.get().asFile,
			createProgressLogger(project, "Download sources")
		)
	}
}
