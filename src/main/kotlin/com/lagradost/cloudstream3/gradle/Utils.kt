package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import com.lagradost.cloudstream3.gradle.entities.PluginEntry
import java.io.File
import java.security.MessageDigest

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")

    file.inputStream().use { fis ->
        val buffer = ByteArray(8192)
        var read = fis.read(buffer)
        while (read != -1) {
            digest.update(buffer, 0, read)
            read = fis.read(buffer)
        }
    }
    return "sha256-" + digest.digest().joinToString("") { "%02x".format(it) }
}

fun makePluginEntry(
    name: String,
    version: Int?,
    fileSize: Long?,
    fileHash: String?,
    jarFileSize: Long?,
    jarHash: String?,
    extension: CloudstreamExtension,
): PluginEntry {
    val repo = extension.repository
    return PluginEntry(
        url = (if (repo == null) "" else repo.getRawLink("${name}.cs3", extension.buildBranch)),
        status = extension.status,
        version = version ?: -1,
        name = name,
        internalName = name,
        authors = extension.authors,
        description = extension.description,
        repositoryUrl = repo?.url,
        language = extension.language,
        iconUrl = extension.iconUrl,
        apiVersion = extension.apiVersion,
        tvTypes = extension.tvTypes,
        fileSize = fileSize,
        jarFileSize = jarFileSize,
        jarUrl = (if (repo == null || jarFileSize == null) null
                  else repo.getRawLink("${name}.jar", extension.buildBranch)),
        jarHash = jarHash,
        fileHash = fileHash
    )
}
