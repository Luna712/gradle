package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.entities.PluginEntry
import com.lagradost.cloudstream3.gradle.makePluginEntry
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class MakePluginsJsonTask : DefaultTask() {

    @get:InputFiles
    abstract val cs3Files: ConfigurableFileCollection

    @get:InputFiles
    abstract val jarFiles: ConfigurableFileCollection

    @get:Input
    abstract val entriesJson: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        outputFile.get().asFile.writeText(entriesJson.get())
        logger.lifecycle("Created ${outputFile.get().asFile}")
    }
}
