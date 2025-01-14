package io.github.shoaky.sourcedownloader.component

import io.github.shoaky.sourcedownloader.sdk.SourceContent
import io.github.shoaky.sourcedownloader.sdk.component.RunAfterCompletion
import kotlin.io.path.*

object DeleteEmptyDirectory : RunAfterCompletion {
    @OptIn(ExperimentalPathApi::class)
    override fun accept(t: SourceContent) {
        t.sourceFiles.firstOrNull()?.run {
            val directoryPath = this.fileDownloadRootDirectory()
            if (directoryPath != null && directoryPath.walk(PathWalkOption.INCLUDE_DIRECTORIES)
                    .all { it.isDirectory() }
            ) {
                directoryPath.deleteRecursively()
            }
        }
    }
}