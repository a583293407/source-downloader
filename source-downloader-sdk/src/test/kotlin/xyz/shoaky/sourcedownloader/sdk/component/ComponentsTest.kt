package xyz.shoaky.sourcedownloader.sdk.component

import org.junit.jupiter.api.Test
import xyz.shoaky.sourcedownloader.sdk.DownloadTask
import xyz.shoaky.sourcedownloader.sdk.FileContent
import xyz.shoaky.sourcedownloader.sdk.SourceContent
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ComponentsTest {

    @Test
    fun test() {
        val components = ComponentTopType.fromClass(DownloaderCp::class)
        assertEquals(1, components.size)
        assertEquals(ComponentTopType.DOWNLOADER, components.first())

        val components1 = ComponentTopType.fromClass(MultiCp::class)
        assertEquals(3, components1.size)
        assertContentEquals(
            listOf(ComponentTopType.DOWNLOADER, ComponentTopType.FILE_MOVER, ComponentTopType.TAGGER),
            components1
        )
    }
}

private class DownloaderCp : Downloader {
    override fun submit(task: DownloadTask) {
        throw NotImplementedError()
    }

    override fun defaultDownloadPath(): Path {
        throw NotImplementedError()
    }

}

private class MultiCp : TorrentDownloader, FileTagger {
    override fun isFinished(task: DownloadTask): Boolean? {
        throw NotImplementedError()
    }

    override fun submit(task: DownloadTask) {
        throw NotImplementedError()
    }

    override fun defaultDownloadPath(): Path {
        throw NotImplementedError()
    }

    override fun rename(sourceContent: SourceContent): Boolean {
        throw NotImplementedError()
    }

    override fun tag(fileContent: FileContent): String? {
        throw NotImplementedError()
    }

}