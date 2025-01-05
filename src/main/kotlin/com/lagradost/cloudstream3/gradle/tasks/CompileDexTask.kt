package com.lagradost.cloudstream3.gradle.tasks

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.ClassFileInputs
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.google.common.io.Closer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

abstract class CompileDexTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    val input: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val pluginClassFile: RegularFileProperty

    @TaskAction
    fun compileDex() {
        val android = project.extensions.getByType(BaseExtension::class.java)

        val minSdk = android.defaultConfig.minSdk ?: 21
        val dexOutputDir = outputFile.get().asFile.parentFile

        Closer.create().use { closer ->
            val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
                DexParameters(
                    minSdkVersion = minSdk,
                    debuggable = true,
                    dexPerClass = false,
                    withDesugaring = minSdk >= 24,
                    desugarBootclasspath = ClassFileProviderFactory(android.bootClasspath.map(File::toPath))
                        .also { closer.register(it) },
                    desugarClasspath = ClassFileProviderFactory(emptyList())
                        .also { closer.register(it) },
                    coreLibDesugarConfig = null,
                    enableApiModeling = false,
                    messageReceiver = MessageReceiverImpl(
                        ErrorFormatMode.HUMAN_READABLE,
                        LoggerFactory.getLogger(CompileDexTask::class.java)
                    )
                )
            )

            val classFiles = input.files.flatMap { inputDir ->
                ClassFileInputs.fromPath(inputDir.toPath())
                    .use { it.entries { _, _ -> true }.toList() }
            }

            dexBuilder.convert(
                input = classFiles.stream(),
                globalSyntheticsOutput = null,
                dexOutput = dexOutputDir.toPath()
            )

            classFiles.forEach { file ->
                val reader = ClassReader(file.readAllBytes())
                val classNode = ClassNode()
                reader.accept(classNode, 0)

                val annotations = (classNode.visibleAnnotations.orEmpty() + classNode.invisibleAnnotations.orEmpty())
                annotations.find { it.desc == "Lcom/lagradost/cloudstream3/plugins/CloudstreamPlugin;" }?.let {
                    val cloudstream = project.extensions.getByType(CloudstreamExtension::class.java)
                    check(cloudstream.pluginClassName == null) { "Only 1 active plugin class per project is supported" }
                    cloudstream.pluginClassName = classNode.name.replace('/', '.').also { name ->
                        pluginClassFile.asFile.orNull?.writeText(name)
                    }
                }
            }
        }

        logger.lifecycle("Compiled dex to ${outputFile.get().asFile}")
    }
}
