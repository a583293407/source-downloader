package xyz.shoaky.sourcedownloader.component.resolver

import xyz.shoaky.sourcedownloader.sdk.SourceItem
import xyz.shoaky.sourcedownloader.sdk.component.ItemFileResolver
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath

object SystemFileResolver : ItemFileResolver {
    override fun resolveFiles(sourceItem: SourceItem): List<Path> {
        val path = sourceItem.downloadUri.toPath()
        if (path.isDirectory()) {
            return Files.list(path).sorted().toList()
        }
        return listOf(path)
    }
}