package xyz.shoaky.sourcedownloader.core.component

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import xyz.shoaky.sourcedownloader.sdk.DownloadTask
import xyz.shoaky.sourcedownloader.sdk.SourceFileContent
import xyz.shoaky.sourcedownloader.sdk.TorrentDownloader
import xyz.shoaky.sourcedownloader.sdk.api.qbittorrent.*
import xyz.shoaky.sourcedownloader.sdk.component.ComponentProps
import xyz.shoaky.sourcedownloader.sdk.component.ComponentSupplier
import xyz.shoaky.sourcedownloader.sdk.component.ComponentType
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

class QbittorrentDownloader(private val client: QbittorrentClient) : TorrentDownloader {

    override fun submit(task: DownloadTask) {
        val torrentsAddRequest = TorrentsAddRequest(listOf(task.downloadUrl), task.downloadPath.toString(), task.category)
        val response = client.execute(torrentsAddRequest)
        if (QbittorrentClient.successResponse != response.body()) {
            log.error("")
        }
    }

    override fun defaultDownloadPath(): Path {
        val response = client.execute(AppGetDefaultSavePathRequest())
        if (response.statusCode() != HttpStatus.OK.value()) {
            throw RuntimeException("获取默认下载路径失败,code:${response.statusCode()} body:${response.body()}")
        }
        return Path.of(response.body())
    }

    override fun isFinished(task: DownloadTask): Boolean {
        //TODO
        return true
    }

    override fun rename(sourceFiles: List<SourceFileContent>, torrentHash: String?): Boolean {
        if (torrentHash == null) {
            log.error("torrentHash is null")
            return false
        }

        val result = sourceFiles.groupBy { it.targetFilePath().parent }
            .map { (path, files) ->
                val setLocation = client.execute(
                    TorrentsSetLocationRequest(
                        listOf(torrentHash),
                        path.toString()
                    ))
                val movingResult = files
                    .map {
                        val sourceFileName = it.fileDownloadPath.last().name
                        val targetFileName = it.targetFilePath().last().name
                        val renameFile =
                            client.execute(TorrentsRenameFileRequest(torrentHash, sourceFileName, targetFileName))
                        renameFile.statusCode() == HttpStatus.OK.value() && it.targetFilePath().exists()
                    }
                setLocation.statusCode() == HttpStatus.OK.value() && movingResult.all { it }
            }
        return result.all { it }
    }

    companion object {
        private val log = LoggerFactory.getLogger(QbittorrentDownloader::class.java)
    }

}

object QbittorrentSupplier : ComponentSupplier<QbittorrentDownloader> {
    override fun apply(props: ComponentProps): QbittorrentDownloader {
        val client = QbittorrentClient(props.parse())
        return QbittorrentDownloader(client)
    }

    override fun availableTypes(): List<ComponentType> {
        return listOf(
            ComponentType.downloader("qbittorrent"),
            ComponentType.fileMover("qbittorrent")
        )
    }
}