package io.freund.adrian.emfjsonschema.utils

import org.eclipse.emf.common.util.URI
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.set
import kotlin.io.path.isDirectory

fun Path.toEmfUri(): URI = URI.createFileURI(this.toAbsolutePath().toString())

fun File.toEmfUri(): URI = URI.createFileURI(this.absoluteFile.toString())

fun getFilesRecursive(paths: List<Path>): List<Path> {
    val files = mutableListOf<Path>()
    paths.forEach { path ->
        if (path.isDirectory()) {
            Files.walk(path).forEach { path ->
                if (!path.isDirectory()) {
                    files.add(path)
                }
            }
        } else {
            files.add(path)
        }
    }

    return files
}
