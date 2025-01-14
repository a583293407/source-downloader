package io.github.shoaky.sourcedownloader.component.resolver

import io.github.shoaky.sourcedownloader.sdk.SourceFile
import io.github.shoaky.sourcedownloader.sdk.SourceItem
import io.github.shoaky.sourcedownloader.sdk.component.ItemFileResolver
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath
import kotlin.io.path.walk

object SystemFileResolver : ItemFileResolver {
    @OptIn(ExperimentalPathApi::class)
    override fun resolveFiles(sourceItem: SourceItem): List<SourceFile> {
        val path = sourceItem.downloadUri.toPath()
        if (path.isDirectory()) {
            return path.walk().sorted().map { SourceFile(it) }.toList()
        }
        return listOf(SourceFile(path))
    }
}