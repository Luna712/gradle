package com.lagradost.cloudstream3.gradle.tasks

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class MakePluginsJsonTask : DefaultTask() {

	@get:InputFiles
	abstract val entries: ConfigurableFileCollection

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun run() {
		val parser = JsonSlurper()

		val list = entries.files
			.filter { it.exists() }
			.map { file ->
				parser.parse(file) as Any
			}

		val json = JsonBuilder(
			list,
			JsonGenerator.Options()
				.excludeNulls()
				.build()
		).toPrettyString()

		outputFile.get().asFile.writeText(json)

		logger.lifecycle("Created ${outputFile.get().asFile}")
	}
}
