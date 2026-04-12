package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.entities.PluginEntry
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
	abstract val statuses: ListProperty<String>

	@get:Input
	abstract val versions: ListProperty<Int>

	@get:Input
	abstract val authors: ListProperty<String>

	@get:Input
	abstract val descriptions: ListProperty<String>

	@get:Input
	abstract val repositoryUrls: ListProperty<String?>

	@get:Input
	abstract val languages: ListProperty<String>

	@get:Input
	abstract val iconUrls: ListProperty<String?>

	@get:Input
	abstract val apiVersions: ListProperty<Int>

	@get:Input
	abstract val tvTypes: ListProperty<String>

	@get:Input
	abstract val fileSizes: ListProperty<Long>

	@get:Input
	abstract val jarFileSizes: ListProperty<Long?>

	@get:Input
	abstract val jarUrls: ListProperty<String?>

	@get:Input
	abstract val jarHashes: ListProperty<String?>

	@get:Input
	abstract val fileHashes: ListProperty<String?>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun makePluginsJson() {
		val plugins = names.get().indices.map { i ->
			PluginEntry(
				name = names.get()[i],
				internalName = internalNames.get()[i],
				url = urls.get()[i],
				status = statuses.get()[i],
				version = versions.get()[i],
				authors = authors.get()[i],
				description = descriptions.get()[i],
				repositoryUrl = repositoryUrls.get()[i],
				language = languages.get()[i],
				iconUrl = iconUrls.get()[i],
				apiVersion = apiVersions.get()[i],
				tvTypes = tvTypes.get()[i],
				fileSize = fileSizes.get()[i],
				jarFileSize = jarFileSizes.get()[i],
				jarUrl = jarUrls.get()[i],
				jarHash = jarHashes.get()[i],
				fileHash = fileHashes.get()[i]
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
