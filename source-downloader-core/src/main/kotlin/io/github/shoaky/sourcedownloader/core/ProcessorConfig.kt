package io.github.shoaky.sourcedownloader.core

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import io.github.shoaky.sourcedownloader.core.component.ComponentId
import io.github.shoaky.sourcedownloader.core.file.VariableErrorStrategy
import io.github.shoaky.sourcedownloader.core.processor.VariableConflictStrategy
import io.github.shoaky.sourcedownloader.sdk.DownloadOptions
import io.github.shoaky.sourcedownloader.sdk.PathPattern
import io.github.shoaky.sourcedownloader.sdk.component.*
import java.nio.file.Path
import java.time.Duration

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ProcessorConfig(
    val name: String,
    val triggers: List<ComponentId> = emptyList(),
    val source: ComponentId,
    @JsonAlias("variable-providers")
    val variableProviders: List<ComponentId> = emptyList(),
    @JsonAlias("file-resolver")
    val itemFileResolver: ComponentId,
    val downloader: ComponentId,
    @JsonAlias("mover")
    val fileMover: ComponentId = ComponentId("mover:general"),
    @JsonSerialize(using = ToStringSerializer::class)
    val savePath: Path,
    val options: Options = Options(),
) {

    fun sourceInstanceName(): String {
        return source.getInstanceName(Source::class)
    }

    fun providerInstanceNames(): List<String> {
        return variableProviders.map {
            it.getInstanceName(VariableProvider::class)
        }
    }

    fun fileResolverInstanceName(): String {
        return itemFileResolver.getInstanceName(ItemFileResolver::class)
    }

    fun downloaderInstanceName(): String {
        return downloader.getInstanceName(Downloader::class)
    }

    fun moverInstanceName(): String {
        return fileMover.getInstanceName(FileMover::class)
    }

    fun triggerInstanceNames(): List<String> {
        return triggers.map {
            it.getInstanceName(Trigger::class)
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    data class Options(
        @JsonAlias("item-filters")
        val sourceItemFilters: List<ComponentId> = emptyList(),
        @JsonAlias("file-filters")
        val fileContentFilters: List<ComponentId> = emptyList(),
        @JsonDeserialize(`as` = CorePathPattern::class)
        val savePathPattern: PathPattern = CorePathPattern.ORIGIN,
        @JsonDeserialize(`as` = CorePathPattern::class)
        val filenamePattern: PathPattern = CorePathPattern.ORIGIN,
        val runAfterCompletion: List<ComponentId> = emptyList(),
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        val renameTaskInterval: Duration = Duration.ofMinutes(5),
        val downloadOptions: DownloadOptions = DownloadOptions(),
        val variableConflictStrategy: VariableConflictStrategy = VariableConflictStrategy.SMART,
        val renameTimesThreshold: Int = 3,
        val provideMetadataVariables: Boolean = true,
        val saveProcessingContent: Boolean = true,
        val itemExpressionExclusions: List<String> = emptyList(),
        val itemExpressionInclusions: List<String> = emptyList(),
        val contentExpressionExclusions: List<String> = emptyList(),
        val contentExpressionInclusions: List<String> = emptyList(),
        val fileExpressionExclusions: List<String> = emptyList(),
        val fileExpressionInclusions: List<String> = emptyList(),
        val variableErrorStrategy: VariableErrorStrategy = VariableErrorStrategy.STAY,
        val touchItemDirectory: Boolean = true,
        val deleteEmptyDirectory: Boolean = true,
        val variableNameReplace: Map<String, String> = emptyMap(),
        val fileTaggers: List<ComponentId> = emptyList(),
        @JsonDeserialize(contentAs = CorePathPattern::class)
        val tagFilenamePattern: Map<String, PathPattern> = emptyMap(),
        @JsonDeserialize(contentAs = RegexVariableReplacer::class)
        val variableReplacers: List<VariableReplacer> = emptyList(),
        val fileReplacementDecider: ComponentId = ComponentId("never"),
        val fetchLimit: Int = 50,
        /**
         * 从Source获取Items后，更新pointer的模式，true:处理完这一批更新一次，false:处理完一个更新一次
         * 这个选项待定，可能会移除
         */
        val pointerBatchMode: Boolean = true,
        val category: String? = null,
        val tags: Set<String> = emptySet(),
        val itemErrorContinue: Boolean = true
    )

}


