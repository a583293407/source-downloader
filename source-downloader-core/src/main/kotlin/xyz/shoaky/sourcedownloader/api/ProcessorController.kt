package xyz.shoaky.sourcedownloader.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.shoaky.sourcedownloader.core.ProcessorConfigStorage
import xyz.shoaky.sourcedownloader.core.SdComponentManager
import xyz.shoaky.sourcedownloader.core.config.ProcessorConfig
import xyz.shoaky.sourcedownloader.sdk.SourceItem
import xyz.shoaky.sourcedownloader.sdk.component.ComponentException

@RestController
@RequestMapping("/api/processor")
private class ProcessorController(
    private val componentManager: SdComponentManager,
    private val configStorages: List<ProcessorConfigStorage>
) {

    @GetMapping("/config/{processorName}")
    fun getConfig(@PathVariable processorName: String): ProcessorConfig? {
        return configStorages.flatMap { it.getAllProcessorConfig() }
            .firstOrNull { it.name == processorName }
    }

    @GetMapping("/dry-run/{processorName}")
    fun dryRun(@PathVariable processorName: String): List<DryRunResult> {
        val sourceProcessor = (componentManager.getProcessor(processorName)
            ?: throw ComponentException.processorMissing("processor $processorName not found"))
        return sourceProcessor.dryRun()
            .map { pc ->
                val fileResult = pc.sourceContent.sourceFiles.map { file ->
                    mapOf(
                        "from" to "${file.fileDownloadPath}",
                        "to" to "${file.targetPath()}",
                        "variables" to file.patternVariables.variables(),
                    )
                }
                val sourceContent = pc.sourceContent
                val variables = sourceContent.sharedPatternVariables.variables()
                DryRunResult(sourceContent.sourceItem, variables,
                    fileResult, pc.status.name)
            }
    }

}

data class DryRunResult(
    val sourceItem: SourceItem,
    val sharedVariables: Map<String, Any>,
    val fileResult: List<Map<String, Any>>,
    val status: String
)