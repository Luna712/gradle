package com.lagradost.cloudstream3.gradle

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
