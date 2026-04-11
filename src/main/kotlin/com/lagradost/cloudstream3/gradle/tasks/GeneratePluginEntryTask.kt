package com.lagradost.cloudstream3.gradle.tasks

import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GeneratePluginEntryTask : DefaultTask() {

	@get:Input
	abstract val entryJson: Property<String>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun run() {
		outputFile.get().asFile.writeText(entryJson.get())
		logger.lifecycle("Created ${outputFile.get().asFile}")
	}
}
