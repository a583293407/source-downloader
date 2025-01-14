package io.github.shoaky.sourcedownloader.component

import io.github.shoaky.sourcedownloader.SourceDownloaderApplication.Companion.log
import io.github.shoaky.sourcedownloader.sdk.SourceContent
import io.github.shoaky.sourcedownloader.sdk.component.RunAfterCompletion
import org.springframework.util.StreamUtils

class RunCommand(
    private val command: List<String>,
    private val withSubjectSummary: Boolean = false
) : RunAfterCompletion {

    override fun accept(sourceContent: SourceContent) {
        val process = run(sourceContent)
        if (process.waitFor() != 0) {
            val result = StreamUtils.copyToString(process.inputStream, Charsets.UTF_8)
            log.warn("mikan completed task script exit code is not 0, result:$result")
        }
        if (log.isDebugEnabled) {
            val result = StreamUtils.copyToString(process.inputStream, Charsets.UTF_8)
            log.debug("script result is:$result")
        }
    }

    private fun process(sourceContent: SourceContent): Process {
        val cmds = command.toMutableList()
        if (withSubjectSummary) {
            cmds.add(sourceContent.summaryContent())
        }

        if (log.isDebugEnabled) {
            log.debug("run command: ${cmds.joinToString(" ")}")
        }
        val processBuilder = ProcessBuilder(*cmds.toTypedArray())
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        return processBuilder.start()
    }

    fun run(sourceContent: SourceContent): Process {
        return process(sourceContent)
    }
}

