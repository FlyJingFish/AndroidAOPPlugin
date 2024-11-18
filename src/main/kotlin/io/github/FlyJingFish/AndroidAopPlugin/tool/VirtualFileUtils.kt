package io.github.FlyJingFish.AndroidAopPlugin.tool


import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.SystemIndependent
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString


val VirtualFile.isFile: Boolean
    get() = isValid && !isDirectory



private fun VirtualFile.relativizeToClosestAncestor(
    relativePath: String
): Pair<VirtualFile, Path> {
    val basePath = Paths.get(path)
    val (normalizedBasePath, normalizedRelativePath) = basePath.relativizeToClosestAncestor(relativePath)
    var baseVirtualFile = this
    repeat(basePath.nameCount - normalizedBasePath.nameCount) {
        baseVirtualFile = checkNotNull(baseVirtualFile.parent) {
            """
        |Cannot resolve base virtual file for $baseVirtualFile
        |  basePath = $path
        |  relativePath = $relativePath
      """.trimMargin()
        }
    }
    return baseVirtualFile to normalizedRelativePath
}

private inline fun VirtualFile.getResolvedVirtualFile(
    relativePath: String,
    getChild: VirtualFile.(String, Boolean) -> VirtualFile
): VirtualFile {
    val (baseVirtualFile, normalizedRelativePath) = relativizeToClosestAncestor(relativePath)
    var virtualFile = baseVirtualFile
    if (normalizedRelativePath.pathString.isNotEmpty()) {
        val names = normalizedRelativePath.map { it.pathString }
        for ((i, name) in names.withIndex()) {
            if (!virtualFile.isDirectory) {
                throw IOException("""
          |Expected directory instead of file: $virtualFile
          |  basePath = $path
          |  relativePath = $relativePath
        """.trimMargin())
            }
            virtualFile = virtualFile.getChild(name, i == names.lastIndex)
        }
    }
    return virtualFile
}

@RequiresReadLock
fun VirtualFile.findFileOrDirectory(relativePath: @SystemIndependent String): VirtualFile? {
    return getResolvedVirtualFile(relativePath) { name, _ ->
        findChild(name) ?: return null // return from findFileOrDirectory
    }
}

@RequiresReadLock
fun VirtualFile.findFile(relativePath: @SystemIndependent String): VirtualFile? {
    val file = findFileOrDirectory(relativePath) ?: return null
    if (!file.isFile) {
        throw IOException("""
      |Expected file instead of directory: $file
      |  basePath = $path
      |  relativePath = $relativePath
    """.trimMargin())
    }
    return file
}
