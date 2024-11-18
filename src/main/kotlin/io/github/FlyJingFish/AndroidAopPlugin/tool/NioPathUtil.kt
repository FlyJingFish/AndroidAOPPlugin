@file:JvmName("NioPathUtil")
package io.github.FlyJingFish.AndroidAopPlugin.tool


import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path


/**
 * Resolves and normalizes path under [this] path.
 * I.e., resolve joins [this] and [relativePath] using file system separator
 */
fun Path.getResolvedPath(relativePath: String): Path {
    return resolve(relativePath).normalize()
}

/**
 * Returns normalised base and relative paths. Result of this function should satisfy for condition:
 * `resultBasePath.resolve(resultRelativePath) == basePath.resolve(relativePath).normalize()` where
 * resultBasePath is path with maximum length of all possible.
 *
 * For example:
 *  * for [this] = `/1/2/3/4/5` and [relativePath] = `../../a/b`,
 *    returns pair with base path = `/1/2/3` and relative path = `a/b`;
 *  * for [this] = `/1/2/3/4/5` and [relativePath] = `../..`,
 *    returns pair with base path = `/1/2/3` and empty relative path;
 *  * for [this] = `/1/2/3/4/5` and [relativePath] = `a/b`,
 *    returns pair with base path = `/1/2/3/4/5` and relative path = `a/b`.
 */
@ApiStatus.Internal
fun Path.relativizeToClosestAncestor(relativePath: String): Pair<Path, Path> {
    val normalizedPath = getResolvedPath(relativePath)
    val normalizedBasePath = checkNotNull(findAncestor(this, normalizedPath)) {
        """
      |Cannot resolve normalized base path for: $normalizedPath
      |  basePath = $this
      |  relativePath = $relativePath
    """.trimMargin()
    }
    val normalizedRelativePath = normalizedBasePath.relativize(normalizedPath)
    return normalizedBasePath to normalizedRelativePath
}


fun findAncestor(path1: Path, path2: Path): Path? {
    var ancestor = path1
    while (!path2.startsWith(ancestor)) {
        ancestor = ancestor.parent
    }
    return ancestor
}