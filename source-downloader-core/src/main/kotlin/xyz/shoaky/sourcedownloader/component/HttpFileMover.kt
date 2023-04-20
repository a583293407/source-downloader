package xyz.shoaky.sourcedownloader.component

import okio.ByteString.Companion.decodeBase64
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import xyz.shoaky.sourcedownloader.SourceDownloaderApplication.Companion.log
import xyz.shoaky.sourcedownloader.sdk.SourceContent
import xyz.shoaky.sourcedownloader.sdk.component.FileMover
import xyz.shoaky.sourcedownloader.sdk.util.Http
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

open class HttpFileMover(
    private val serverUrl: String,
    private val username: String? = null,
    private val password: String? = null
) : FileMover {
    override fun rename(sourceContent: SourceContent): Boolean {
        // 后面异步
        val responses = sourceContent.sourceFiles.map {
            val createFile = createFile(it.fileDownloadPath, it.targetPath())
            if (createFile.statusCode() != HttpStatus.CREATED.value()) {
                log.error("Failed to create file: ${it.targetPath()}")
            }
            createFile
        }
        return responses.all { it.statusCode() == HttpStatus.CREATED.value() }
    }

    private fun createFile(filePath: Path, targetPath: Path): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI(serverUrl + targetPath))
            .expectContinue(true)
        if (username != null && password != null) {
            val authorization = "$username:$password".decodeBase64()
            builder.header(HttpHeaders.AUTHORIZATION, "Basic $authorization")
        }
        builder.PUT(HttpRequest.BodyPublishers.ofFile(filePath))
        return Http.client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

}