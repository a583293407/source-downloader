package io.github.shoaky.sourcedownloader.component

import io.github.shoaky.sourcedownloader.SourceDownloaderApplication.Companion.log
import io.github.shoaky.sourcedownloader.sdk.SourceContent
import io.github.shoaky.sourcedownloader.sdk.component.FileMover
import io.github.shoaky.sourcedownloader.sdk.util.encodeBase64
import io.github.shoaky.sourcedownloader.sdk.util.http.httpClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

open class HttpFileMover(
    private val url: URI,
    private val username: String? = null,
    private val password: String? = null,
    private val deleteSource: Boolean = true
) : FileMover {
    override fun move(sourceContent: SourceContent): Boolean {
        // 后面异步
        val responses = sourceContent.sourceFiles.map {
            val createFile = createFile(it.fileDownloadPath, it.targetPath())
            if (createFile.statusCode() != HttpStatus.CREATED.value()) {
                log.error("Failed to create file: ${it.targetPath()}")
            } else {
                if (deleteSource) {
                    it.fileDownloadPath.toFile().delete()
                }
            }
            createFile
        }
        return responses.all { it.statusCode() == HttpStatus.CREATED.value() }
    }

    override fun exists(paths: List<Path>): Boolean {
        return paths.asSequence().map {
            val request = buildAuthRequest(it)
                .method(PROPFIND, HttpRequest.BodyPublishers.noBody())
                .build()
            httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        }.any { it.statusCode() == HttpStatus.NOT_FOUND.value() }
    }

    override fun createDirectories(path: Path) {
        val request = buildAuthRequest(path)
            .method(MKCOL, HttpRequest.BodyPublishers.noBody())
            .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    override fun replace(sourceContent: SourceContent): Boolean {
        return move(sourceContent)
    }

    private fun createFile(filePath: Path, targetPath: Path): HttpResponse<String> {
        val builder = buildAuthRequest(targetPath)
            .expectContinue(true)
        builder.PUT(HttpRequest.BodyPublishers.ofFile(filePath))
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun buildAuthRequest(targetPath: Path): HttpRequest.Builder {
        val uri = UriComponentsBuilder.fromUri(url)
            .path(targetPath.toString())
            .build()
            .toUri()
        val builder = HttpRequest.newBuilder(uri)
        if (username != null && password != null) {
            val authorization = "$username:$password".encodeBase64()
            builder.header(HttpHeaders.AUTHORIZATION, "Basic $authorization")
        }
        log.debug("Request uri: {}", uri)
        return builder
    }

    companion object {
        private const val MKCOL = "MKCOL"
        private const val PROPFIND = "PROPFIND"
    }

}