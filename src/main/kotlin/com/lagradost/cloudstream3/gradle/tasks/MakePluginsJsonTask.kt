package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.entities.PluginEntry
import com.lagradost.cloudstream3.gradle.sha256
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class MakePluginsJsonTask : DefaultTask() {

	@get:Input
	abstract val names: ListProperty<String>

	@get:Input
	abstract val internalNames: ListProperty<String>

	@get:Input
	abstract val urls: ListProperty<String>

	@get:Input
	abstract val versions: ListProperty<Int>

	@get:Input
	abstract val jarFiles: ListProperty<java.io.File>

	@get:Input
	abstract val cs3Files: ListProperty<java.io.File>

	@get:Input
	abstract val basePluginEntries: ListProperty<PluginEntry>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun makePluginsJson() {

		val plugins = names.get().indices.map { i ->

			val base = basePluginEntries.get()[i]

			val jarFile = jarFiles.get()[i]
			val cs3File = cs3Files.get()[i]

			base.copy(
				fileSize = cs3File.length(),
				jarFileSize = jarFile.length(),
				fileHash = sha256(cs3File),
				jarHash = sha256(jarFile),
				jarUrl = base.jarUrl
			)
		}

		outputFile.get().asFile.writeText(
			JsonBuilder(
				plugins,
				JsonGenerator.Options().excludeNulls().build()
			).toPrettyString()
		)

		logger.lifecycle("Created ${outputFile.get().asFile}")
	}
}
